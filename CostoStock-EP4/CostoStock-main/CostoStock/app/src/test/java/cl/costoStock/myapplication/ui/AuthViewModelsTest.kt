package cl.costoStock.myapplication.ui

import cl.costoStock.myapplication.data.repository.AuthRepository
import cl.costoStock.myapplication.data.repository.AuthResult
import cl.costoStock.myapplication.testutil.MainDispatcherRule
import cl.costoStock.myapplication.ui.auth.LoginViewModel
import cl.costoStock.myapplication.ui.auth.RegisterViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>(relaxed = true)

    // ------------------------------- Login -------------------------------

    @Test
    fun loginConCamposVaciosMuestraErroresSinLlamarAlServidor() = runTest {
        val vm = LoginViewModel(repository)
        vm.submit()

        assertNotNull(vm.state.value.emailError)
        assertNotNull(vm.state.value.passwordError)
        assertEquals(1, vm.state.value.shakeCount)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun loginExitoso() = runTest {
        coEvery { repository.login("ana@mail.cl", "Clave123") } returns AuthResult.Success
        val vm = LoginViewModel(repository)
        vm.onEmailChange("ana@mail.cl")
        vm.onPasswordChange("Clave123")
        vm.togglePasswordVisibility()
        vm.submit()

        assertTrue(vm.state.value.loggedIn)
        assertTrue(vm.state.value.passwordVisible)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun loginFallidoYSinConexion() = runTest {
        val vm = LoginViewModel(repository)
        vm.onEmailChange("ana@mail.cl")
        vm.onPasswordChange("mala")

        coEvery { repository.login(any(), any()) } returns AuthResult.InvalidCredentials
        vm.submit()
        assertEquals("Correo o contraseña incorrectos", vm.state.value.formError)

        coEvery { repository.login(any(), any()) } returns AuthResult.Error("Sin conexión")
        vm.submit()
        assertEquals("Sin conexión", vm.state.value.formError)
        assertFalse(vm.state.value.loggedIn)
    }

    @Test
    fun loginValidaAlSalirDelCampoYAlCorregir() = runTest {
        val vm = LoginViewModel(repository)
        vm.onEmailBlur()
        vm.onPasswordBlur()
        assertNotNull(vm.state.value.emailError)
        assertNotNull(vm.state.value.passwordError)

        vm.onEmailChange("ana@mail.cl")
        vm.onPasswordChange("x")
        assertNull(vm.state.value.emailError)
        assertNull(vm.state.value.passwordError)
    }

    // ------------------------------- Registro -------------------------------

    private fun filledRegister(): RegisterViewModel = RegisterViewModel(repository).apply {
        onNameChange("Ana Pérez")
        onEmailChange("ana@mail.cl")
        onPasswordChange("Clave1234")
        onConfirmChange("Clave1234")
        onTermsChange(true)
    }

    @Test
    fun registroInvalidoMarcaTodosLosCampos() = runTest {
        val vm = RegisterViewModel(repository)
        vm.submit()
        val s = vm.state.value

        assertNotNull(s.nameError)
        assertNotNull(s.emailError)
        assertNotNull(s.passwordError)
        assertNotNull(s.confirmError)
        assertNotNull(s.termsError)
        coVerify(exactly = 0) { repository.register(any(), any(), any()) }
    }

    @Test
    fun registroExitoso() = runTest {
        coEvery { repository.register(any(), any(), any()) } returns AuthResult.Success
        val vm = filledRegister()
        vm.togglePasswordVisibility()
        vm.submit()

        assertTrue(vm.state.value.registered)
        assertTrue(vm.state.value.passwordStrength >= 2)
    }

    @Test
    fun registroConRespuestasDeError() = runTest {
        val vm = filledRegister()

        coEvery { repository.register(any(), any(), any()) } returns AuthResult.EmailTaken
        vm.submit()
        assertEquals("Este correo ya está registrado", vm.state.value.emailError)

        coEvery { repository.register(any(), any(), any()) } returns AuthResult.Error("Sin conexión")
        vm.submit()
        assertEquals("Sin conexión", vm.state.value.formError)

        coEvery { repository.register(any(), any(), any()) } returns AuthResult.InvalidCredentials
        vm.submit()
        assertNotNull(vm.state.value.formError)
        assertFalse(vm.state.value.registered)
    }

    @Test
    fun registroValidaCampoPorCampo() = runTest {
        val vm = RegisterViewModel(repository)
        vm.onNameBlur()
        vm.onEmailBlur()
        vm.onPasswordBlur()
        vm.onConfirmBlur()
        assertNotNull(vm.state.value.nameError)

        vm.onNameChange("Ana")
        vm.onEmailChange("ana@mail.cl")
        vm.onPasswordChange("Clave1234")
        vm.onConfirmChange("Otra1234")
        vm.onConfirmBlur()
        assertNull(vm.state.value.nameError)
        assertNull(vm.state.value.emailError)
        assertEquals("Las contraseñas no coinciden", vm.state.value.confirmError)

        vm.onPasswordChange("Otra1234")
        assertNull(vm.state.value.confirmError)
    }
}
