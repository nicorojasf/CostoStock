package cl.costoStock.myapplication.ui

import android.graphics.Bitmap
import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.data.model.Order
import cl.costoStock.myapplication.data.model.OrderStatus
import cl.costoStock.myapplication.data.model.User
import cl.costoStock.myapplication.data.repository.AuthRepository
import cl.costoStock.myapplication.data.repository.CartRepository
import cl.costoStock.myapplication.data.repository.OrderRepository
import cl.costoStock.myapplication.data.repository.Outcome
import cl.costoStock.myapplication.testutil.MainDispatcherRule
import cl.costoStock.myapplication.testutil.line
import cl.costoStock.myapplication.testutil.order
import cl.costoStock.myapplication.ui.orders.OrdersViewModel
import cl.costoStock.myapplication.ui.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileAndOrdersViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    // ------------------------------- Perfil -------------------------------

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val cartRepository = mockk<CartRepository>(relaxed = true)

    private fun TestScope.profileViewModel(messages: MutableList<String>): ProfileViewModel {
        every { authRepository.observeCurrentUser() } returns MutableStateFlow(User(1L, "Ana", "ana@mail.cl", null))
        every { cartRepository.observeCart() } returns MutableStateFlow<List<CartLine>>(listOf(line(1L, 3)))
        val vm = ProfileViewModel(authRepository, cartRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.collect { messages += it } }
        return vm
    }

    @Test
    fun perfilMuestraUsuarioYUnidadesDelCarrito() = runTest {
        val messages = mutableListOf<String>()
        val vm = profileViewModel(messages)

        assertEquals("Ana", vm.state.value.user?.name)
        assertEquals(3, vm.state.value.cartItems)

        vm.onPhotoCaptured(mockk<Bitmap>())
        coVerify { authRepository.updatePhoto(any()) }
        assertEquals("Foto de perfil actualizada", messages.last())

        vm.logout()
        verify { authRepository.logout() }
    }

    @Test
    fun editarNombreValidaYGuardaEnElMicroservicio() = runTest {
        val messages = mutableListOf<String>()
        val vm = profileViewModel(messages)

        vm.openEdit()
        assertTrue(vm.edit.value.visible)
        assertEquals("Ana", vm.edit.value.name)

        vm.onEditNameChange("")
        vm.saveEdit()
        assertNotNull(vm.edit.value.error)
        vm.onEditNameChange("Ana María")
        assertNull(vm.edit.value.error)

        coEvery { authRepository.updateName("Ana María") } returns "Sin conexión"
        vm.saveEdit()
        assertEquals("Sin conexión", vm.edit.value.error)
        assertFalse(vm.edit.value.isSaving)

        coEvery { authRepository.updateName("Ana María") } returns null
        vm.saveEdit()
        assertFalse(vm.edit.value.visible)
        assertEquals("Nombre actualizado", messages.last())

        vm.openEdit()
        vm.closeEdit()
        assertFalse(vm.edit.value.visible)
    }

    @Test
    fun eliminarCuenta() = runTest {
        val messages = mutableListOf<String>()
        val vm = profileViewModel(messages)

        coEvery { authRepository.deleteAccount() } returns "Sin conexión"
        vm.deleteAccount()
        assertFalse(vm.accountDeleted.value)
        assertFalse(vm.isDeleting.value)
        assertEquals("Sin conexión", messages.last())

        coEvery { authRepository.deleteAccount() } returns null
        vm.deleteAccount()
        assertTrue(vm.accountDeleted.value)
    }

    // ------------------------------- Mis pedidos -------------------------------

    private val orderRepository = mockk<OrderRepository>()

    private fun TestScope.ordersViewModel(messages: MutableList<String>): OrdersViewModel {
        val vm = OrdersViewModel(orderRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.collect { messages += it } }
        return vm
    }

    @Test
    fun cargaLosPedidosYCalculaElTotalComprado() = runTest {
        coEvery { orderRepository.myOrders() } returns Outcome.Ok(
            listOf(order(1L, total = 2000), order(2L, OrderStatus.CANCELLED, total = 5000))
        )
        val vm = ordersViewModel(mutableListOf())

        assertEquals(2, vm.state.value.orders.size)
        assertEquals(2000, vm.state.value.totalSpent)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun errorAlCargarPedidos() = runTest {
        coEvery { orderRepository.myOrders() } returns Outcome.Fail("Sin conexión")
        val vm = ordersViewModel(mutableListOf())

        assertEquals("Sin conexión", vm.state.value.error)

        coEvery { orderRepository.myOrders() } returns Outcome.Ok(emptyList<Order>())
        vm.load()
        assertNull(vm.state.value.error)
    }

    @Test
    fun cancelarYEliminarPedidos() = runTest {
        val messages = mutableListOf<String>()
        coEvery { orderRepository.myOrders() } returns Outcome.Ok(listOf(order(1L), order(2L, OrderStatus.CANCELLED)))
        val vm = ordersViewModel(messages)

        coEvery { orderRepository.cancel(1L) } returns Outcome.Ok(order(1L, OrderStatus.CANCELLED))
        vm.cancel(vm.state.value.orders[0])
        assertEquals(OrderStatus.CANCELLED, vm.state.value.orders[0].status)
        assertNull(vm.state.value.busyOrderId)

        coEvery { orderRepository.cancel(1L) } returns Outcome.Fail("Ya cancelado")
        vm.cancel(vm.state.value.orders[0])
        assertEquals("Ya cancelado", messages.last())

        coEvery { orderRepository.delete(2L) } returns Outcome.Ok(Unit)
        vm.delete(vm.state.value.orders[1])
        assertEquals(listOf(1L), vm.state.value.orders.map { it.id })

        coEvery { orderRepository.delete(1L) } returns Outcome.Fail("Solo cancelados")
        vm.delete(vm.state.value.orders[0])
        assertEquals("Solo cancelados", messages.last())
        assertEquals(4, messages.size)
    }
}
