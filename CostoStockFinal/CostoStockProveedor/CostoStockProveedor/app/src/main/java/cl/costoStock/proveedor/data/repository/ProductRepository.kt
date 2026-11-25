package cl.costoStock.proveedor.data.repository

import cl.costoStock.proveedor.data.model.Product
import cl.costoStock.proveedor.data.model.ProductDraft
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.remote.ApiErrors
import cl.costoStock.proveedor.data.remote.ProductosApi
import cl.costoStock.proveedor.data.remote.dto.EstadoRequest
import cl.costoStock.proveedor.data.remote.toProduct
import cl.costoStock.proveedor.data.remote.toRequest
import cl.costoStock.proveedor.data.session.SessionManager
import cl.costoStock.proveedor.data.storage.PhotoStorage
import cl.costoStock.proveedor.domain.catalog.Catalog
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estado de la sincronización con ms-productos. */
sealed interface SyncState {
    data object Loading : SyncState
    data object Ready : SyncState
    data class Error(val message: String) : SyncState
}

/**
 * Publicaciones del proveedor con sesión, guardadas en el microservicio ms-productos
 * (CRUD: GET, POST, PUT, PATCH estado y DELETE). La lista se mantiene en memoria y se refresca
 * después de cada cambio y cada [refreshIntervalMs] mientras la pantalla está visible, para ver
 * bajar el stock cuando los clientes compran.
 */
class ProductRepository(
    private val api: ProductosApi,
    private val session: SessionManager,
    private val photoStorage: PhotoStorage,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val refreshIntervalMs: Long = AUTO_REFRESH_MS
) {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    private val _sync = MutableStateFlow<SyncState>(SyncState.Loading)
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    /** Último error de una operación de escritura, para mostrarlo al usuario. */
    var lastError: String? = null
        private set

    fun observeMyProducts(): Flow<List<Product>> = channelFlow {
        launch {
            while (isActive) {
                refresh()
                delay(refreshIntervalMs)
            }
        }
        _products.asStateFlow().collect { send(it) }
    }

    /** Descarga las publicaciones del proveedor. Si falla conserva la última lista. */
    suspend fun refresh(): Boolean = withContext(io) {
        val sellerId = session.currentSellerId.value
        if (sellerId == null) {
            _products.value = emptyList()
            return@withContext false
        }
        try {
            _products.value = api.listarPorProveedor(sellerId).mapNotNull { it.toProduct() }
            _sync.value = SyncState.Ready
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            _sync.value = SyncState.Error(ApiErrors.NETWORK)
            false
        } catch (e: Exception) {
            _sync.value = SyncState.Error(ApiErrors.UNEXPECTED)
            false
        }
    }

    suspend fun getMyProduct(id: Long): Product? = withContext(io) {
        val sellerId = session.currentSellerId.value ?: return@withContext null
        _products.value.firstOrNull { it.id == id }?.let { return@withContext it }
        try {
            api.obtener(id).body()?.toProduct()?.takeIf { it.sellerId == sellerId }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Crea la publicación (POST). Devuelve su id o null si no hay sesión o falla el guardado. */
    suspend fun publish(draft: ProductDraft): Long? = withContext(io) {
        val sellerId = session.currentSellerId.value ?: return@withContext null
        val savedPaths = draft.imagePaths.map { photoStorage.copyProductPhoto(it) }
        val created = write {
            api.crear(draft.toRequest(sellerId, Catalog.emoji(draft.category), savedPaths))
        }?.toProduct()
        if (created == null) {
            // Se borran solo las copias nuevas: las fotos temporales quedan para reintentar.
            photoStorage.deleteAll(savedPaths - draft.imagePaths.toSet())
            null
        } else {
            photoStorage.deleteAll(draft.imagePaths - savedPaths.toSet())
            _products.value = listOf(created) + _products.value
            created.id
        }
    }

    /** Actualiza la publicación (PUT) y borra del disco las fotos que ya no se usan. */
    suspend fun update(id: Long, draft: ProductDraft): Boolean = withContext(io) {
        val sellerId = session.currentSellerId.value ?: return@withContext false
        val previous = _products.value.firstOrNull { it.id == id }
        val savedPaths = draft.imagePaths.map { photoStorage.copyProductPhoto(it) }
        val updated = write {
            api.actualizar(id, draft.toRequest(sellerId, Catalog.emoji(draft.category), savedPaths))
        }?.toProduct()
        if (updated == null) {
            photoStorage.deleteAll(savedPaths - draft.imagePaths.toSet())
            return@withContext false
        }
        replace(updated)
        // Temporales ya copiadas y fotos que se quitaron de la publicación.
        photoStorage.deleteAll(draft.imagePaths - savedPaths.toSet())
        if (previous != null) photoStorage.deleteAll(previous.imagePaths - savedPaths.toSet())
        true
    }

    /** Pausa o activa la publicación (PATCH). */
    suspend fun setStatus(id: Long, status: ProductStatus): Boolean = withContext(io) {
        val updated = write { api.cambiarEstado(id, EstadoRequest(status.name)) }?.toProduct()
            ?: return@withContext false
        replace(updated)
        true
    }

    /** Elimina la publicación (DELETE) y sus fotos locales. */
    suspend fun delete(id: Long): Boolean = withContext(io) {
        val product = _products.value.firstOrNull { it.id == id }
        val ok = try {
            val response = api.eliminar(id)
            if (!response.isSuccessful) {
                lastError = ApiErrors.parse(response.errorBody())?.mensaje ?: ApiErrors.UNEXPECTED
            }
            response.isSuccessful
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            lastError = ApiErrors.NETWORK
            false
        } catch (e: Exception) {
            lastError = ApiErrors.UNEXPECTED
            false
        }
        if (ok) {
            _products.value = _products.value.filterNot { it.id == id }
            product?.let { photoStorage.deleteAll(it.imagePaths) }
        }
        ok
    }

    private fun replace(product: Product) {
        _products.value = _products.value.map { if (it.id == product.id) product else it }
    }

    /** Ejecuta una escritura y devuelve el cuerpo si fue exitosa; si no, guarda el error. */
    private inline fun <T> write(block: () -> retrofit2.Response<T>): T? = try {
        val response = block()
        if (response.isSuccessful) {
            lastError = null
            response.body()
        } else {
            lastError = ApiErrors.parse(response.errorBody())?.mensaje ?: ApiErrors.UNEXPECTED
            null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        lastError = ApiErrors.NETWORK
        null
    } catch (e: Exception) {
        lastError = ApiErrors.UNEXPECTED
        null
    }

    companion object {
        const val AUTO_REFRESH_MS = 20_000L
    }
}
