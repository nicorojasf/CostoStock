package cl.costoStock.myapplication.data

import cl.costoStock.myapplication.data.model.OrderStatus
import cl.costoStock.myapplication.data.remote.ApiErrors
import cl.costoStock.myapplication.data.remote.PedidosApi
import cl.costoStock.myapplication.data.remote.dto.PedidoDto
import cl.costoStock.myapplication.data.repository.OrderRepository
import cl.costoStock.myapplication.data.repository.Outcome
import cl.costoStock.myapplication.data.repository.ProductRepository
import cl.costoStock.myapplication.data.session.SessionManager
import cl.costoStock.myapplication.testutil.jsonBody
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OrderRepositoryTest {

    private val api = mockk<PedidosApi>()
    private val session = mockk<SessionManager>(relaxed = true)
    private val productRepository = mockk<ProductRepository>(relaxed = true)
    private val userId = MutableStateFlow<Long?>(1L)
    private lateinit var repository: OrderRepository

    private fun pedido(id: Long, estado: String) = PedidoDto(id, 1L, 0L, 840, 160, 1000, estado, 1, emptyList())

    @Before
    fun setUp() {
        every { session.currentUserId } returns userId
        coEvery { productRepository.refresh() } returns true
        repository = OrderRepository(api, session, productRepository, Dispatchers.Unconfined)
    }

    @Test
    fun listaLosPedidosDelCliente() = runTest {
        coEvery { api.listar(1L) } returns listOf(pedido(1L, "CONFIRMADO"), pedido(2L, "CANCELADO"))

        val orders = (repository.myOrders() as Outcome.Ok).value

        assertEquals(2, orders.size)
        assertEquals(OrderStatus.CANCELLED, orders[1].status)
    }

    @Test
    fun sinSesionNoHayPedidosYSinRedHayError() = runTest {
        userId.value = null
        assertTrue((repository.myOrders() as Outcome.Ok).value.isEmpty())

        userId.value = 1L
        coEvery { api.listar(1L) } throws IOException()
        assertEquals(ApiErrors.NETWORK, (repository.myOrders() as Outcome.Fail).message)

        coEvery { api.listar(1L) } throws IllegalStateException()
        assertEquals(ApiErrors.UNEXPECTED, (repository.myOrders() as Outcome.Fail).message)
    }

    @Test
    fun cancelarDevuelveElPedidoActualizadoYRecargaElStock() = runTest {
        coEvery { api.cancelar(5L) } returns Response.success(pedido(5L, "CANCELADO"))

        assertEquals(OrderStatus.CANCELLED, (repository.cancel(5L) as Outcome.Ok).value.status)
        coVerify { productRepository.refresh() }

        coEvery { api.cancelar(5L) } returns Response.error(409, jsonBody("""{"mensaje":"El pedido ya está cancelado"}"""))
        assertEquals("El pedido ya está cancelado", (repository.cancel(5L) as Outcome.Fail).message)

        coEvery { api.cancelar(5L) } returns Response.success(null)
        assertEquals(ApiErrors.UNEXPECTED, (repository.cancel(5L) as Outcome.Fail).message)
    }

    @Test
    fun eliminarPedido() = runTest {
        coEvery { api.eliminar(5L) } returns Response.success(Unit)
        assertTrue(repository.delete(5L) is Outcome.Ok)

        coEvery { api.eliminar(5L) } returns Response.error(409, jsonBody("""{"mensaje":"Solo cancelados"}"""))
        assertEquals("Solo cancelados", (repository.delete(5L) as Outcome.Fail).message)

        coEvery { api.eliminar(5L) } returns Response.error(500, jsonBody(""))
        assertEquals(ApiErrors.UNEXPECTED, (repository.delete(5L) as Outcome.Fail).message)
    }
}
