package cl.costoStock.myapplication.ui

import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.data.model.EconomicIndicators
import cl.costoStock.myapplication.data.repository.CartRepository
import cl.costoStock.myapplication.data.repository.CartUpdateResult
import cl.costoStock.myapplication.data.repository.IndicatorsRepository
import cl.costoStock.myapplication.data.repository.ProductRepository
import cl.costoStock.myapplication.domain.cart.CartSummary
import cl.costoStock.myapplication.domain.cart.CheckoutResult
import cl.costoStock.myapplication.testutil.MainDispatcherRule
import cl.costoStock.myapplication.testutil.indicators
import cl.costoStock.myapplication.testutil.line
import cl.costoStock.myapplication.ui.cart.CartViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
class CartAndMainViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val cartRepository = mockk<CartRepository>(relaxed = true)
    private val productRepository = mockk<ProductRepository>(relaxed = true)
    private val indicatorsRepository = mockk<IndicatorsRepository>(relaxed = true)
    private val cart = MutableStateFlow(listOf(line(1L, 2, price = 1000, stock = 5), line(2L, 1, price = 500)))

    @Before
    fun setUp() {
        every { cartRepository.observeCart() } returns cart
        every { indicatorsRepository.indicators } returns MutableStateFlow<EconomicIndicators?>(indicators)
        coEvery { productRepository.refresh() } returns true
        coEvery { indicatorsRepository.refresh(any()) } returns true
    }

    private fun TestScope.newViewModel(messages: MutableList<String>): CartViewModel {
        val vm = CartViewModel(cartRepository, productRepository, indicatorsRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.collect { messages += it } }
        return vm
    }

    @Test
    fun resumenDelCarritoConIndicadores() = runTest {
        val vm = newViewModel(mutableListOf())
        val s = vm.state.value

        assertEquals(2500, s.summary.total)
        assertEquals(3, s.summary.itemCount)
        assertEquals(indicators, s.indicators)
        assertFalse(s.hasStockIssues)
        coVerify { productRepository.refresh() }

        cart.value = listOf(line(1L, 9, stock = 5))
        assertTrue(vm.state.value.hasStockIssues)
    }

    @Test
    fun confirmarCompraMuestraElDialogo() = runTest {
        val vm = newViewModel(mutableListOf())
        val summary = CartSummary(3, 2101, 399, 2500)
        coEvery { cartRepository.checkout() } returns CheckoutResult.Success(summary)

        vm.checkout()

        assertEquals(summary, vm.state.value.confirmation)
        assertFalse(vm.state.value.isProcessing)
        vm.dismissConfirmation()
        assertNull(vm.state.value.confirmation)
    }

    @Test
    fun erroresAlConfirmarSeMuestranComoMensaje() = runTest {
        val messages = mutableListOf<String>()
        val vm = newViewModel(messages)

        coEvery { cartRepository.checkout() } returns CheckoutResult.Empty
        vm.checkout()
        coEvery { cartRepository.checkout() } returns CheckoutResult.OutOfStock(listOf("Lámpara"))
        vm.checkout()
        coEvery { cartRepository.checkout() } returns CheckoutResult.Failure("Sin conexión")
        vm.checkout()

        assertEquals("Tu carrito está vacío", messages[0])
        assertTrue(messages[1].contains("Lámpara"))
        assertEquals("Sin conexión", messages[2])
    }

    @Test
    fun cambiarQuitarYVaciar() = runTest {
        val messages = mutableListOf<String>()
        val vm = newViewModel(messages)
        val first = cart.value[0]

        coEvery { cartRepository.setQuantity(1L, 9) } returns CartUpdateResult.StockLimit(5)
        vm.changeQuantity(first, 9)
        coEvery { cartRepository.setQuantity(1L, 1) } returns CartUpdateResult.Ok
        vm.changeQuantity(first, 1)
        vm.remove(first)
        vm.clearCart()

        assertEquals("Solo hay 5 unidades disponibles", messages[0])
        assertTrue(messages[1].contains("eliminado"))
        coVerify { cartRepository.remove(1L) }
        coVerify { cartRepository.clear() }
    }

    @Test
    fun mainViewModelCuentaLasUnidadesDelCarrito() = runTest {
        val vm = MainViewModel(cartRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.cartCount.collect {} }

        assertEquals(3, vm.cartCount.value)
        cart.value = emptyList<CartLine>()
        assertEquals(0, vm.cartCount.value)
    }
}
