package cl.costoStock.myapplication.data.repository

import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.local.CartDao
import cl.costoStock.myapplication.data.local.CartItem
import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.data.model.Product
import cl.costoStock.myapplication.data.remote.ApiErrors
import cl.costoStock.myapplication.data.remote.PedidosApi
import cl.costoStock.myapplication.data.remote.dto.PedidoItemRequest
import cl.costoStock.myapplication.data.remote.dto.PedidoRequest
import cl.costoStock.myapplication.data.remote.toOrder
import cl.costoStock.myapplication.data.session.SessionManager
import cl.costoStock.myapplication.domain.cart.CartCalculator
import cl.costoStock.myapplication.domain.cart.CartSummary
import cl.costoStock.myapplication.domain.cart.CheckoutResult
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
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

/**
 * Carrito: las cantidades se guardan en SQLite (dato local del teléfono) y se cruzan con el
 * catálogo de ms-productos. Al confirmar, la compra se envía a ms-pedidos.
 */
class CartRepository(
    private val cartDao: CartDao,
    private val productRepository: ProductRepository,
    private val pedidosApi: PedidosApi,
    private val session: SessionManager,
    private val changes: DataChangeNotifier,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    /** Carrito del usuario con sesión; se re-emite ante cambios en la BD o en el catálogo. */
    fun observeCart(): Flow<List<CartLine>> =
        combine(session.currentUserId, changes.version, productRepository.products) { id, _, products ->
            id to products
        }.map { (id, products) ->
            if (id == null) emptyList() else buildLines(cartDao.getItems(id), products)
        }.flowOn(io)

    suspend fun addProduct(productId: Long): CartUpdateResult = withContext(io) {
        val userId = session.currentUserId.value ?: return@withContext CartUpdateResult.NotAvailable
        val product = productRepository.findCached(productId)
            ?: return@withContext CartUpdateResult.NotAvailable
        val next = cartDao.quantityOf(userId, productId) + 1
        if (next > product.stock) {
            CartUpdateResult.StockLimit(product.stock)
        } else {
            cartDao.setQuantity(userId, productId, next)
            CartUpdateResult.Ok
        }
    }

    suspend fun setQuantity(productId: Long, quantity: Int): CartUpdateResult =
        withContext(io) {
            val userId = session.currentUserId.value
                ?: return@withContext CartUpdateResult.NotAvailable
            if (quantity <= 0) {
                cartDao.remove(userId, productId)
                return@withContext CartUpdateResult.Ok
            }
            val product = productRepository.findCached(productId)
                ?: return@withContext CartUpdateResult.NotAvailable
            if (quantity > product.stock) {
                CartUpdateResult.StockLimit(product.stock)
            } else {
                cartDao.setQuantity(userId, productId, quantity)
                CartUpdateResult.Ok
            }
        }

    suspend fun remove(productId: Long) = withContext(io) {
        session.currentUserId.value?.let { cartDao.remove(it, productId) }
        Unit
    }

    suspend fun clear() = withContext(io) {
        session.currentUserId.value?.let { cartDao.clear(it) }
        Unit
    }

    /**
     * Envía la compra a ms-pedidos (POST /api/pedidos). El microservicio valida y descuenta el stock
     * en ms-productos; si todo sale bien se vacía el carrito local y se recarga el catálogo.
     */
    suspend fun checkout(): CheckoutResult = withContext(io) {
        val userId = session.currentUserId.value ?: return@withContext CheckoutResult.Empty
        val items = cartDao.getItems(userId)
        val lines = buildLines(items, productRepository.products.value)
        // Productos que ya no están publicados: se quitan del carrito.
        val visibleIds = lines.map { it.product.id }.toSet()
        items.filter { it.productId !in visibleIds }.forEach { cartDao.remove(userId, it.productId) }
        if (lines.isEmpty()) return@withContext CheckoutResult.Empty

        try {
            val request = PedidoRequest(userId, lines.map { PedidoItemRequest(it.product.id, it.quantity) })
            val response = pedidosApi.crear(request)
            if (response.isSuccessful) {
                val order = response.body()?.toOrder()
                val summary = order?.let { CartSummary(it.itemCount, it.net, it.iva, it.total) }
                    ?: CartCalculator.summarize(lines)
                cartDao.clear(userId)
                productRepository.refresh()
                CheckoutResult.Success(summary)
            } else {
                val error = ApiErrors.parse(response.errorBody())
                if (response.code() == 409) {
                    productRepository.refresh()
                    val names = error?.detalles.orEmpty()
                    if (names.isNotEmpty()) CheckoutResult.OutOfStock(names)
                    else CheckoutResult.Failure(error?.mensaje ?: "Stock insuficiente")
                } else {
                    CheckoutResult.Failure(error?.mensaje ?: ApiErrors.UNEXPECTED)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            CheckoutResult.Failure(ApiErrors.NETWORK)
        } catch (e: Exception) {
            CheckoutResult.Failure(ApiErrors.UNEXPECTED)
        }
    }

    private fun buildLines(items: List<CartItem>, products: List<Product>): List<CartLine> {
        val byId = products.associateBy { it.id }
        return items.mapNotNull { item -> byId[item.productId]?.let { CartLine(it, item.quantity) } }
    }
}
