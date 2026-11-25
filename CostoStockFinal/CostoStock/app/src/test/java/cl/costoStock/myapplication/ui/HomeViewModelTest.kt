package cl.costoStock.myapplication.ui

import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.data.model.EconomicIndicators
import cl.costoStock.myapplication.data.model.Product
import cl.costoStock.myapplication.data.repository.CartRepository
import cl.costoStock.myapplication.data.repository.CartUpdateResult
import cl.costoStock.myapplication.data.repository.IndicatorsRepository
import cl.costoStock.myapplication.data.repository.ProductRepository
import cl.costoStock.myapplication.data.repository.SyncState
import cl.costoStock.myapplication.testutil.MainDispatcherRule
import cl.costoStock.myapplication.testutil.indicators
import cl.costoStock.myapplication.testutil.product
import cl.costoStock.myapplication.ui.home.HomeFilters
import cl.costoStock.myapplication.ui.home.HomeViewModel
import cl.costoStock.myapplication.ui.home.SortOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val productRepository = mockk<ProductRepository>(relaxed = true)
    private val cartRepository = mockk<CartRepository>(relaxed = true)
    private val indicatorsRepository = mockk<IndicatorsRepository>(relaxed = true)

    private val products = MutableStateFlow(
        listOf(
            product(1L, price = 3000, stock = 5, category = "Hogar", comuna = "Maipú"),
            product(2L, price = 1000, stock = 0, category = "Moda", comuna = "Santiago"),
            product(3L, price = 2000, stock = 50, category = "Hogar", comuna = "Santiago")
        )
    )
    private val sync = MutableStateFlow<SyncState>(SyncState.Ready)
    private val cart = MutableStateFlow<List<CartLine>>(emptyList())
    private val indicatorsFlow = MutableStateFlow<EconomicIndicators?>(indicators)

    @Before
    fun setUp() {
        every { productRepository.observeProducts() } returns products
        every { productRepository.sync } returns sync
        every { cartRepository.observeCart() } returns cart
        every { indicatorsRepository.indicators } returns indicatorsFlow
        coEvery { indicatorsRepository.refresh(any()) } returns true
    }

    private fun kotlinx.coroutines.test.TestScope.newViewModel(): HomeViewModel {
        val vm = HomeViewModel(productRepository, cartRepository, indicatorsRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    @Test
    fun estadoInicialConCatalogoIndicadoresYDestacados() = runTest {
        val vm = newViewModel()
        val s = vm.state.value

        assertEquals(3, s.items.size)
        assertEquals(listOf("Hogar", "Moda"), s.categories)
        assertEquals(listOf("Maipú", "Santiago"), s.comunas)
        assertEquals(listOf(1L, 3L), s.featured.map { it.id })
        assertEquals(indicators, s.indicators)
        assertFalse(s.isLoading)
        assertNull(s.error)
        coVerify { indicatorsRepository.refresh(any()) }
    }

    @Test
    fun filtrosBusquedaYOrden() = runTest {
        val vm = newViewModel()

        vm.onCategorySelected("Hogar")
        assertEquals(listOf(1L, 3L), vm.state.value.items.map { it.product.id })

        vm.onComunaSelected("Santiago")
        assertEquals(listOf(3L), vm.state.value.items.map { it.product.id })

        vm.clearFilters()
        vm.onOrderSelected(SortOrder.PRICE_ASC)
        assertEquals(listOf(2L, 3L, 1L), vm.state.value.items.map { it.product.id })
        vm.onOrderSelected(SortOrder.PRICE_DESC)
        assertEquals(listOf(1L, 3L, 2L), vm.state.value.items.map { it.product.id })

        vm.clearFilters()
        vm.toggleLowStock()
        assertEquals(listOf(1L), vm.state.value.items.map { it.product.id })
        assertTrue(vm.filters.value.isActive)

        vm.clearFilters()
        vm.onQueryChange("producto 2")
        assertEquals(listOf(2L), vm.state.value.items.map { it.product.id })
        vm.onQueryChange("descripción 3")
        assertEquals(listOf(3L), vm.state.value.items.map { it.product.id })
        vm.onQueryChange("moda")
        assertEquals(listOf(2L), vm.state.value.items.map { it.product.id })

        vm.clearFilters()
        assertFalse(vm.filters.value.isActive)
        assertEquals(HomeFilters(), vm.filters.value)
    }

    @Test
    fun cantidadEnCarritoYErrorDeConexion() = runTest {
        val vm = newViewModel()
        cart.value = listOf(CartLine(products.value[0], 2))
        assertEquals(2, vm.state.value.inCart[1L])
        assertEquals(2, vm.state.value.items.first { it.product.id == 1L }.inCart)

        sync.value = SyncState.Error("Sin conexión")
        assertEquals("Sin conexión", vm.state.value.error)

        products.value = emptyList<Product>()
        sync.value = SyncState.Loading
        assertTrue(vm.state.value.isLoading)
    }

    @Test
    fun agregarAlCarritoEmiteMensajes() = runTest {
        val vm = newViewModel()
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.collect { messages += it } }
        val p = products.value[0]

        coEvery { cartRepository.addProduct(1L) } returns CartUpdateResult.Ok
        vm.addToCart(p)
        coEvery { cartRepository.addProduct(1L) } returns CartUpdateResult.StockLimit(5)
        vm.addToCart(p)
        coEvery { cartRepository.addProduct(1L) } returns CartUpdateResult.NotAvailable
        vm.addToCart(p)
        coEvery { cartRepository.setQuantity(1L, 9) } returns CartUpdateResult.StockLimit(5)
        vm.changeQuantity(p, 9)
        coEvery { cartRepository.setQuantity(1L, 2) } returns CartUpdateResult.Ok
        vm.changeQuantity(p, 2)

        assertEquals(4, messages.size)
        assertTrue(messages[0].contains("agregado"))
        assertTrue(messages[1].contains("5 unidades"))
        assertEquals("No se pudo agregar el producto", messages[2])
    }

    @Test
    fun ubicacionGpsFiltraPorLaComunaMasCercanaConProductos() = runTest {
        val vm = newViewModel()
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.collect { messages += it } }

        vm.onLocationDetected(-33.4378, -70.6504) // Plaza de Armas
        assertEquals("Santiago", vm.filters.value.comuna)
        vm.onLocationDetected(-33.5100, -70.7600) // Maipú
        assertEquals("Maipú", vm.filters.value.comuna)
        vm.onLocationDetected(-33.0472, -71.6127) // Valparaíso: fuera de la RM
        assertTrue(messages.last().contains("fuera de la Región Metropolitana"))

        products.value = listOf(product(9L, comuna = "Lo Barnechea"))
        vm.onLocationDetected(-33.6117, -70.5758) // Puente Alto: a más de 30 km
        assertEquals("Aún no hay productos publicados cerca de ti", messages.last())

        vm.onLocationUnavailable("Sin permiso de ubicación")
        assertEquals("Sin permiso de ubicación", messages.last())
    }

    @Test
    fun reintentarRecargaCatalogoEIndicadores() = runTest {
        val vm = newViewModel()
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.collect { messages += it } }

        coEvery { productRepository.refresh() } returns true
        vm.retry()
        coEvery { productRepository.refresh() } returns false
        vm.retry()

        assertEquals(listOf("Catálogo actualizado"), messages)
        coVerify(exactly = 2) { indicatorsRepository.refresh(true) }
    }
}
