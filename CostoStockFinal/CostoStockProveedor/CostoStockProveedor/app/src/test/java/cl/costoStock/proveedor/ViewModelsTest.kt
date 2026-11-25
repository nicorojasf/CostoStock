package cl.costoStock.proveedor

import cl.costoStock.proveedor.data.model.EconomicIndicators
import cl.costoStock.proveedor.data.model.Product
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.model.Seller
import cl.costoStock.proveedor.data.repository.AuthRepository
import cl.costoStock.proveedor.data.repository.AuthResult
import cl.costoStock.proveedor.data.repository.IndicatorsRepository
import cl.costoStock.proveedor.data.repository.ProductRepository
import cl.costoStock.proveedor.data.repository.SyncState
import cl.costoStock.proveedor.testutil.MainDispatcherRule
import cl.costoStock.proveedor.testutil.indicators
import cl.costoStock.proveedor.testutil.product
import cl.costoStock.proveedor.ui.auth.LoginViewModel
import cl.costoStock.proveedor.ui.auth.RegisterViewModel
import cl.costoStock.proveedor.ui.products.ProductFilter
import cl.costoStock.proveedor.ui.products.ProductsViewModel
import cl.costoStock.proveedor.ui.profile.ProfileViewModel
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val productRepository = mockk<ProductRepository>(relaxed = true)
    private val indicatorsRepository = mockk<IndicatorsRepository>(relaxed = true)

    private val products = MutableStateFlow(
        listOf(
            product(1L, price = 1000, stock = 3, name = "Lámpara LED"),
            product(2L, price = 2000, stock = 0, name = "Mouse inalámbrico"),
            product(3L, price = 500, stock = 4, status = ProductStatus.PAUSED, name = "Vasos de vidrio")
        )
    )
    private val sync = MutableStateFlow<SyncState>(SyncState.Ready)

    @Before
    fun setUp() {
        every { productRepository.observeMyProducts() } returns products
        every { productRepository.sync } returns sync
        every { indicatorsRepository.indicators } returns MutableStateFlow<EconomicIndicators?>(indicators)
        coEvery { indicatorsRepository.refresh(any()) } returns true
    }

    private fun <T> TestScope.collectInto(flow: kotlinx.coroutines.flow.Flow<T>, sink: MutableList<T>? = null) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { sink?.add(it) } }
    }

    // ------------------------------- Login -------------------------------

    @Test
    fun loginValidaYMuestraErroresDelServidor() = runTest {
        val vm = LoginViewModel(authRepository)
        vm.submit()
        assertNotNull(vm.state.value.emailError)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }

        vm.onEmailBlur()
        vm.onPasswordBlur()
        vm.onEmailChange("tienda@mail.cl")
        vm.onPasswordChange("Clave123")
        vm.togglePasswordVisibility()

        coEvery { authRepository.login(any(), any()) } returns AuthResult.InvalidCredentials
        vm.submit()
        assertEquals("Correo o contraseña incorrectos", vm.state.value.formError)

        coEvery { authRepository.login(any(), any()) } returns AuthResult.Error("Sin conexión")
        vm.submit()
        assertEquals("Sin conexión", vm.state.value.formError)

        coEvery { authRepository.login(any(), any()) } returns AuthResult.Success
        vm.submit()
        assertTrue(vm.state.value.loggedIn)
    }

    // ------------------------------- Registro -------------------------------

    @Test
    fun registroCompletoConTodasLasRespuestas() = runTest {
        val vm = RegisterViewModel(authRepository)
        vm.submit()
        assertNotNull(vm.state.value.rutError)
        assertNotNull(vm.state.value.phoneError)

        vm.onNameBlur(); vm.onRutBlur(); vm.onPhoneBlur(); vm.onEmailBlur(); vm.onPasswordBlur(); vm.onConfirmBlur()
        vm.onNameChange("Tienda Ana")
        vm.onRutChange("12.345.678-5")
        vm.onPhoneChange("9 1234 5678")
        vm.onEmailChange("tienda@mail.cl")
        vm.onPasswordChange("Clave1234")
        vm.onConfirmChange("Clave1234")
        vm.onTermsChange(true)
        vm.togglePasswordVisibility()
        assertNull(vm.state.value.rutError)
        assertTrue(vm.state.value.passwordStrength > 0)

        coEvery { authRepository.register(any(), any(), any(), any(), any()) } returns AuthResult.RutTaken
        vm.submit()
        assertEquals("Este RUT ya está registrado", vm.state.value.rutError)

        coEvery { authRepository.register(any(), any(), any(), any(), any()) } returns AuthResult.EmailTaken
        vm.submit()
        assertEquals("Este correo ya está registrado", vm.state.value.emailError)

        coEvery { authRepository.register(any(), any(), any(), any(), any()) } returns AuthResult.Error("Sin conexión")
        vm.submit()
        assertEquals("Sin conexión", vm.state.value.formError)

        coEvery { authRepository.register(any(), any(), any(), any(), any()) } returns AuthResult.InvalidCredentials
        vm.submit()
        assertNotNull(vm.state.value.formError)

        coEvery { authRepository.register(any(), any(), any(), any(), any()) } returns AuthResult.Success
        vm.submit()
        assertTrue(vm.state.value.registered)
    }

    // ------------------------------- Mis publicaciones -------------------------------

    private fun TestScope.productsViewModel(messages: MutableList<String>): ProductsViewModel {
        val vm = ProductsViewModel(productRepository, indicatorsRepository)
        collectInto(vm.state)
        collectInto(vm.messages, messages)
        return vm
    }

    @Test
    fun resumenFiltrosYBusqueda() = runTest {
        val vm = productsViewModel(mutableListOf())
        val s = vm.state.value

        assertEquals(3, s.totalCount)
        assertEquals(1, s.summary.active)
        assertEquals(1, s.summary.paused)
        assertEquals(7, s.summary.units)
        assertEquals(1000L * 3 + 500L * 4, s.summary.inventoryValue)
        assertEquals(indicators, s.indicators)
        assertNull(s.error)

        vm.onFilterChange(ProductFilter.OUT_OF_STOCK)
        assertEquals(listOf(2L), vm.state.value.products.map { it.id })
        vm.onFilterChange(ProductFilter.PAUSED)
        assertEquals(listOf(3L), vm.state.value.products.map { it.id })
        vm.onFilterChange(ProductFilter.ACTIVE)
        assertEquals(listOf(1L), vm.state.value.products.map { it.id })
        vm.onFilterChange(ProductFilter.ALL)
        vm.onQueryChange("mouse")
        assertEquals(listOf(2L), vm.state.value.products.map { it.id })

        sync.value = SyncState.Error("Sin conexión")
        assertEquals("Sin conexión", vm.state.value.error)
        products.value = emptyList<Product>()
        sync.value = SyncState.Loading
        assertTrue(vm.state.value.isLoading)
    }

    @Test
    fun pausarEliminarYReintentar() = runTest {
        val messages = mutableListOf<String>()
        val vm = productsViewModel(messages)
        val active = products.value[0]
        val paused = products.value[2]

        coEvery { productRepository.setStatus(1L, ProductStatus.PAUSED) } returns true
        vm.togglePause(active)
        coEvery { productRepository.setStatus(3L, ProductStatus.ACTIVE) } returns true
        vm.togglePause(paused)
        coEvery { productRepository.setStatus(1L, ProductStatus.PAUSED) } returns false
        every { productRepository.lastError } returns "Sin conexión"
        vm.togglePause(active)

        coEvery { productRepository.delete(1L) } returns true
        vm.delete(active)
        coEvery { productRepository.delete(1L) } returns false
        vm.delete(active)

        coEvery { productRepository.refresh() } returns true
        vm.retry()

        assertEquals(
            listOf(
                "Publicación pausada", "Publicación activada", "Sin conexión",
                "Publicación eliminada", "Sin conexión", "Publicaciones actualizadas"
            ),
            messages
        )
        coVerify { indicatorsRepository.refresh(true) }
    }

    // ------------------------------- Perfil -------------------------------

    private val seller = Seller(1L, "Tienda Ana", "12345678-5", "tienda@mail.cl", "+56912345678", null, 0L)

    @Test
    fun perfilEstadisticasEdicionYCierre() = runTest {
        every { authRepository.observeCurrentSeller() } returns MutableStateFlow(seller)
        val messages = mutableListOf<String>()
        val vm = ProfileViewModel(authRepository, productRepository)
        collectInto(vm.state)
        collectInto(vm.messages, messages)

        assertEquals(3, vm.state.value.totalProducts)
        assertEquals(1, vm.state.value.activeProducts)
        assertFalse(vm.state.value.isLoading)

        vm.onPhotoCaptured("/cache/foto.jpg")
        coVerify { authRepository.updatePhoto("/cache/foto.jpg") }

        vm.openEdit()
        assertTrue(vm.edit.value.visible)
        assertEquals("Tienda Ana", vm.edit.value.name)

        vm.onEditNameChange("")
        vm.onEditPhoneChange("123")
        vm.saveEdit()
        assertNotNull(vm.edit.value.nameError)
        assertNotNull(vm.edit.value.phoneError)
        vm.onEditNameBlur()
        vm.onEditPhoneBlur()
        vm.onEditNameChange("Tienda Nueva")
        vm.onEditPhoneChange("+56 9 8765 4321")
        assertNull(vm.edit.value.nameError)
        assertNull(vm.edit.value.phoneError)

        coEvery { authRepository.updateProfile(any(), any()) } returns "Sin conexión"
        vm.saveEdit()
        assertTrue(vm.edit.value.visible)
        assertFalse(vm.edit.value.isSaving)

        coEvery { authRepository.updateProfile(any(), any()) } returns null
        vm.saveEdit()
        assertFalse(vm.edit.value.visible)

        vm.openEdit()
        vm.closeEdit()
        assertFalse(vm.edit.value.visible)
        vm.logout()
        verify { authRepository.logout() }

        assertEquals(listOf("Foto de perfil actualizada", "Sin conexión", "Datos actualizados"), messages)
    }
}
