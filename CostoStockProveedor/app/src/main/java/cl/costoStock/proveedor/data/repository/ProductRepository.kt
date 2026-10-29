package cl.costoStock.proveedor.data.repository

import cl.costoStock.proveedor.data.DataChangeNotifier
import cl.costoStock.proveedor.data.local.ProductDao
import cl.costoStock.proveedor.data.model.Product
import cl.costoStock.proveedor.data.model.ProductDraft
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.session.SessionManager
import cl.costoStock.proveedor.data.storage.PhotoStorage
import cl.costoStock.proveedor.domain.catalog.Catalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Publicaciones del proveedor con sesión activa. */
class ProductRepository(
    private val productDao: ProductDao,
    private val session: SessionManager,
    private val photoStorage: PhotoStorage,
    private val changes: DataChangeNotifier
) {
    fun observeMyProducts(): Flow<List<Product>> =
        combine(session.currentSellerId, changes.version) { id, _ -> id }
            .map { id -> if (id == null) emptyList() else productDao.getBySeller(id) }
            .flowOn(Dispatchers.IO)

    suspend fun getMyProduct(id: Long): Product? = withContext(Dispatchers.IO) {
        val sellerId = session.currentSellerId.value ?: return@withContext null
        productDao.findById(id, sellerId)
    }

    /** Crea la publicación. Devuelve su id o null si no hay sesión o falla el guardado. */
    suspend fun publish(draft: ProductDraft): Long? = withContext(Dispatchers.IO) {
        val sellerId = session.currentSellerId.value ?: return@withContext null
        val savedPaths = draft.imagePaths.map { photoStorage.persistProductPhoto(it) }
        val id = productDao.insert(sellerId, draft, Catalog.emoji(draft.category), savedPaths)
        if (id == -1L) {
            photoStorage.deleteAll(savedPaths)
            null
        } else id
    }

    /** Actualiza la publicación y borra del disco las fotos que ya no se usan. */
    suspend fun update(id: Long, draft: ProductDraft): Boolean = withContext(Dispatchers.IO) {
        val sellerId = session.currentSellerId.value ?: return@withContext false
        val previous = productDao.findById(id, sellerId) ?: return@withContext false
        val savedPaths = draft.imagePaths.map { photoStorage.persistProductPhoto(it) }
        val ok = productDao.update(id, sellerId, draft, Catalog.emoji(draft.category), savedPaths)
        if (ok) photoStorage.deleteAll(previous.imagePaths - savedPaths.toSet())
        ok
    }

    suspend fun setStatus(id: Long, status: ProductStatus) = withContext(Dispatchers.IO) {
        val sellerId = session.currentSellerId.value ?: return@withContext
        productDao.setStatus(id, sellerId, status)
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val sellerId = session.currentSellerId.value ?: return@withContext
        val product = productDao.findById(id, sellerId) ?: return@withContext
        productDao.delete(id, sellerId)
        photoStorage.deleteAll(product.imagePaths)
    }
}
