package cl.costoStock.myapplication.data.repository

import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.local.CartDao
import cl.costoStock.myapplication.data.local.ProductDao
import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.data.session.SessionManager
import cl.costoStock.myapplication.domain.cart.CheckoutResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface CartUpdateResult {
    data object Ok : CartUpdateResult
    data class StockLimit(val available: Int) : CartUpdateResult
    data object NotAvailable : CartUpdateResult
}

class CartRepository(
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val session: SessionManager,
    private val changes: DataChangeNotifier
) {
    /** Carrito del usuario con sesión activa; se re-emite ante cualquier cambio en la BD. */
    fun observeCart(): Flow<List<CartLine>> =
        combine(session.currentUserId, changes.version) { id, _ -> id }
            .map { id -> if (id == null) emptyList<CartLine>() else cartDao.getCart(id) }
            .flowOn(Dispatchers.IO)

    suspend fun addProduct(productId: Long): CartUpdateResult = withContext(Dispatchers.IO) {
        val userId = session.currentUserId.value ?: return@withContext CartUpdateResult.NotAvailable
        val product = productDao.findById(productId) ?: return@withContext CartUpdateResult.NotAvailable
        val next = cartDao.quantityOf(userId, productId) + 1
        if (next > product.stock) {
            CartUpdateResult.StockLimit(product.stock)
        } else {
            cartDao.setQuantity(userId, productId, next)
            CartUpdateResult.Ok
        }
    }

    suspend fun setQuantity(productId: Long, quantity: Int): CartUpdateResult =
        withContext(Dispatchers.IO) {
            val userId = session.currentUserId.value
                ?: return@withContext CartUpdateResult.NotAvailable
            if (quantity <= 0) {
                cartDao.remove(userId, productId)
                return@withContext CartUpdateResult.Ok
            }
            val product = productDao.findById(productId)
                ?: return@withContext CartUpdateResult.NotAvailable
            if (quantity > product.stock) {
                CartUpdateResult.StockLimit(product.stock)
            } else {
                cartDao.setQuantity(userId, productId, quantity)
                CartUpdateResult.Ok
            }
        }

    suspend fun remove(productId: Long) = withContext(Dispatchers.IO) {
        session.currentUserId.value?.let { cartDao.remove(it, productId) }
        Unit
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        session.currentUserId.value?.let { cartDao.clear(it) }
        Unit
    }

    suspend fun checkout(): CheckoutResult = withContext(Dispatchers.IO) {
        val userId = session.currentUserId.value ?: return@withContext CheckoutResult.Empty
        cartDao.checkout(userId)
    }
}
