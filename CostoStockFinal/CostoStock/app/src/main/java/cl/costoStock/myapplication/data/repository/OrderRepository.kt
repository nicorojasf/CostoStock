package cl.costoStock.myapplication.data.repository

import cl.costoStock.myapplication.data.model.Order
import cl.costoStock.myapplication.data.remote.ApiErrors
import cl.costoStock.myapplication.data.remote.ApiException
import cl.costoStock.myapplication.data.remote.PedidosApi
import cl.costoStock.myapplication.data.remote.toOrder
import cl.costoStock.myapplication.data.session.SessionManager
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Historial de compras del cliente en ms-pedidos (leer, cancelar y eliminar). */
class OrderRepository(
    private val api: PedidosApi,
    private val session: SessionManager,
    private val productRepository: ProductRepository,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun myOrders(): Outcome<List<Order>> = withContext(io) {
        val userId = session.currentUserId.value ?: return@withContext Outcome.Ok(emptyList())
        safe { api.listar(userId).mapNotNull { it.toOrder() } }
    }

    /** Cancela la compra (PATCH); ms-pedidos devuelve el stock a ms-productos. */
    suspend fun cancel(orderId: Long): Outcome<Order> = withContext(io) {
        safe {
            val response = api.cancelar(orderId)
            if (!response.isSuccessful) {
                throw ApiException(ApiErrors.parse(response.errorBody())?.mensaje ?: ApiErrors.UNEXPECTED)
            }
            productRepository.refresh()
            response.body()?.toOrder() ?: throw ApiException(ApiErrors.UNEXPECTED)
        }
    }

    /** Elimina del historial una compra cancelada (DELETE). */
    suspend fun delete(orderId: Long): Outcome<Unit> = withContext(io) {
        safe {
            val response = api.eliminar(orderId)
            if (!response.isSuccessful) {
                throw ApiException(ApiErrors.parse(response.errorBody())?.mensaje ?: ApiErrors.UNEXPECTED)
            }
        }
    }

    private inline fun <T> safe(block: () -> T): Outcome<T> = try {
        Outcome.Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        Outcome.Fail(e.message ?: ApiErrors.UNEXPECTED)
    } catch (e: IOException) {
        Outcome.Fail(ApiErrors.NETWORK)
    } catch (e: Exception) {
        Outcome.Fail(ApiErrors.UNEXPECTED)
    }
}
