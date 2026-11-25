package cl.costoStock.myapplication.data

import cl.costoStock.myapplication.data.local.CartDao
import cl.costoStock.myapplication.data.local.CartItem
import cl.costoStock.myapplication.data.model.Product
import cl.costoStock.myapplication.data.remote.ApiErrors
import cl.costoStock.myapplication.data.remote.PedidosApi
import cl.costoStock.myapplication.data.remote.dto.PedidoDto
import cl.costoStock.myapplication.data.repository.CartRepository
import cl.costoStock.myapplication.data.repository.CartUpdateResult
import cl.costoStock.myapplication.data.repository.ProductRepository
import cl.costoStock.myapplication.data.session.SessionManager
import cl.costoStock.myapplication.domain.cart.CheckoutResult
import cl.costoStock.myapplication.testutil.jsonBody
import cl.costoStock.myapplication.testutil.product
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CartRepositoryTest {

    private val cartDao = mockk<CartDao>(relaxed = true)
    private val productRepository = mockk<ProductRepository>(relaxed = true)
    private val pedidosApi = mockk<PedidosApi>()
    private val session = mockk<SessionManager>(relaxed = true)
    private val userId = MutableStateFlow<Long?>(1L)
    private val products = MutableStateFlow<List<Product>>(listOf(product(1L, price = 1000, stock = 3), product(2L, price = 500)))
    private lateinit var repository: CartRepository

    @Before
    fun setUp() {
        every { session.currentUserId } returns userId
        every { productRepository.products } returns products
        every { productRepository.findCached(any()) } answers { products.value.firstOrNull { it.id == firstArg<Long>() } }
        coEvery { productRepository.refresh() } returns true
        repository = CartRepository(cartDao, productRepository, pedidosApi, session, DataChangeNotifier(), Dispatchers.Unconfined)
    }

    @Test
    fun elCarritoCruzaCantidadesLocalesConElCatalogo() = runTest {
        every { cartDao.getItems(1L) } returns listOf(CartItem(1L, 2), CartItem(99L, 1))

        val lines = repository.observeCart().first()

        assertEquals(1, lines.size)
        assertEquals(2000, lines[0].subtotal)

        userId.value = null
        assertTrue(repository.observeCart().first().isEmpty())
    }

    @Test
    fun agregarRespetaElStock() = runTest {
        every { cartDao.quantityOf(1L, 1L) } returns 2
        assertEquals(CartUpdateResult.Ok, repository.addProduct(1L))
        verify { cartDao.setQuantity(1L, 1L, 3) }

        every { cartDao.quantityOf(1L, 1L) } returns 3
        assertEquals(CartUpdateResult.StockLimit(3), repository.addProduct(1L))

        assertEquals(CartUpdateResult.NotAvailable, repository.addProduct(99L))
        userId.value = null
        assertEquals(CartUpdateResult.NotAvailable, repository.addProduct(1L))
    }

    @Test
    fun cambiarCantidadQuitarYVaciar() = runTest {
        assertEquals(CartUpdateResult.Ok, repository.setQuantity(1L, 2))
        verify { cartDao.setQuantity(1L, 1L, 2) }
        assertEquals(CartUpdateResult.StockLimit(3), repository.setQuantity(1L, 9))
        assertEquals(CartUpdateResult.Ok, repository.setQuantity(1L, 0))
        verify { cartDao.remove(1L, 1L) }
        assertEquals(CartUpdateResult.NotAvailable, repository.setQuantity(99L, 1))

        repository.remove(2L)
        verify { cartDao.remove(1L, 2L) }
        repository.clear()
        verify { cartDao.clear(1L) }

        userId.value = null
        assertEquals(CartUpdateResult.NotAvailable, repository.setQuantity(1L, 1))
    }

    @Test
    fun checkoutEnviaElPedidoAMsPedidosYVaciaElCarrito() = runTest {
        every { cartDao.getItems(1L) } returns listOf(CartItem(1L, 2), CartItem(2L, 1), CartItem(99L, 1))
        coEvery { pedidosApi.crear(any()) } returns Response.success(
            PedidoDto(10L, 1L, 0L, 2101, 399, 2500, "CONFIRMADO", 3, emptyList())
        )

        val result = repository.checkout()

        assertEquals(2500, (result as CheckoutResult.Success).summary.total)
        assertEquals(3, result.summary.itemCount)
        coVerify { pedidosApi.crear(match { it.clienteId == 1L && it.items.size == 2 }) }
        verify { cartDao.remove(1L, 99L) } // producto que ya no está publicado
        verify { cartDao.clear(1L) }
        coVerify { productRepository.refresh() }
    }

    @Test
    fun checkoutSinCuerpoCalculaElResumenLocal() = runTest {
        every { cartDao.getItems(1L) } returns listOf(CartItem(2L, 2))
        coEvery { pedidosApi.crear(any()) } returns Response.success(null)

        assertEquals(1000, (repository.checkout() as CheckoutResult.Success).summary.total)
    }

    @Test
    fun checkoutVacioOSinSesion() = runTest {
        every { cartDao.getItems(1L) } returns emptyList()
        assertEquals(CheckoutResult.Empty, repository.checkout())
        userId.value = null
        assertEquals(CheckoutResult.Empty, repository.checkout())
    }

    @Test
    fun checkoutSinStockInformaLosProductos() = runTest {
        every { cartDao.getItems(1L) } returns listOf(CartItem(1L, 2))
        coEvery { pedidosApi.crear(any()) } returns
            Response.error(409, jsonBody("""{"mensaje":"Stock insuficiente","detalles":["Producto 1"]}"""))
        assertEquals(CheckoutResult.OutOfStock(listOf("Producto 1")), repository.checkout())

        coEvery { pedidosApi.crear(any()) } returns Response.error(409, jsonBody("""{"mensaje":"Sin stock"}"""))
        assertEquals(CheckoutResult.Failure("Sin stock"), repository.checkout())
    }

    @Test
    fun checkoutConErroresDeRed() = runTest {
        every { cartDao.getItems(1L) } returns listOf(CartItem(1L, 1))

        coEvery { pedidosApi.crear(any()) } returns Response.error(503, jsonBody("""{"mensaje":"Servicio caído"}"""))
        assertEquals(CheckoutResult.Failure("Servicio caído"), repository.checkout())

        coEvery { pedidosApi.crear(any()) } throws IOException()
        assertEquals(CheckoutResult.Failure(ApiErrors.NETWORK), repository.checkout())

        coEvery { pedidosApi.crear(any()) } throws IllegalStateException()
        assertEquals(CheckoutResult.Failure(ApiErrors.UNEXPECTED), repository.checkout())
    }
}
