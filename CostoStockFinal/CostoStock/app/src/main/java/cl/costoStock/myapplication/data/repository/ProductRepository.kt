package cl.costoStock.myapplication.data.repository

import cl.costoStock.myapplication.data.model.Product
import cl.costoStock.myapplication.data.remote.ApiErrors
import cl.costoStock.myapplication.data.remote.ProductosApi
import cl.costoStock.myapplication.data.remote.isVisibleForBuyers
import cl.costoStock.myapplication.data.remote.toProduct
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
 * Catálogo obtenido del microservicio ms-productos. Se guarda en memoria y se actualiza:
 * - al abrir el catálogo y cada [refreshIntervalMs] mientras está visible (datos en tiempo real),
 * - después de comprar o cancelar un pedido (el stock cambia).
 */
class ProductRepository(
    private val api: ProductosApi,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val refreshIntervalMs: Long = AUTO_REFRESH_MS
) {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _sync = MutableStateFlow<SyncState>(SyncState.Loading)
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    fun observeProducts(): Flow<List<Product>> = channelFlow {
        launch {
            while (isActive) {
                refresh()
                delay(refreshIntervalMs)
            }
        }
        products.collect { send(it) }
    }

    fun findCached(id: Long): Product? = _products.value.firstOrNull { it.id == id }

    /** Descarga el catálogo. Si falla conserva la última lista y deja el error en [sync]. */
    suspend fun refresh(): Boolean = withContext(io) {
        try {
            _products.value = api.listar(soloDisponibles = false)
                .filter { it.isVisibleForBuyers }
                .mapNotNull { it.toProduct() }
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

    companion object {
        const val AUTO_REFRESH_MS = 20_000L
    }
}
