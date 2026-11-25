package cl.costoStock.myapplication

import cl.costoStock.myapplication.data.repository.AuthRepository
import cl.costoStock.myapplication.domain.location.ComunaLocator
import cl.costoStock.myapplication.domain.validation.Validators
import cl.costoStock.myapplication.testutil.MainDispatcherRule
import cl.costoStock.myapplication.ui.auth.ForgotPasswordViewModel
import cl.costoStock.myapplication.ui.auth.ResetStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordAndLocationTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()

    // ------------------------------- Recuperar contraseña -------------------------------

    @Test
    fun correoInvalidoNoLlamaAlServidor() = runTest {
        val vm = ForgotPasswordViewModel(repository)
        vm.onEmailChange("no-es-correo")
        vm.submitEmail()

        assertNotNull(vm.state.value.emailError)
        assertEquals(1, vm.state.value.shakeCount)
        coVerify(exactly = 0) { repository.requestPasswordReset(any()) }

        vm.onEmailChange("ana@mail.cl")
        assertNull(vm.state.value.emailError)
    }

    @Test
    fun flujoCompletoCorreoCodigoYNuevaContrasena() = runTest {
        coEvery { repository.requestPasswordReset("ana@mail.cl") } returns null
        coEvery { repository.resetPassword("ana@mail.cl", "123456", "Nueva1234") } returns null
        val vm = ForgotPasswordViewModel(repository)

        vm.onEmailChange("ana@mail.cl")
        vm.submitEmail()
        assertEquals(ResetStep.CODE, vm.state.value.step)
        assertTrue(vm.state.value.info!!.contains("ana@mail.cl"))

        vm.onCodeChange("12a3-456789") // solo dígitos, máximo 6
        assertEquals("123456", vm.state.value.code)
        vm.onPasswordChange("Nueva1234")
        vm.togglePasswordVisibility()
        vm.onConfirmChange("Nueva1234")
        assertTrue(vm.state.value.passwordStrength > 0)
        assertTrue(vm.state.value.passwordVisible)

        vm.submitReset()
        assertEquals(ResetStep.DONE, vm.state.value.step)
    }

    @Test
    fun erroresDelSegundoPaso() = runTest {
        coEvery { repository.requestPasswordReset(any()) } returns null
        val vm = ForgotPasswordViewModel(repository)
        vm.onEmailChange("ana@mail.cl")
        vm.submitEmail()

        vm.submitReset()
        assertNotNull(vm.state.value.codeError)
        assertNotNull(vm.state.value.passwordError)
        assertNotNull(vm.state.value.confirmError)
        coVerify(exactly = 0) { repository.resetPassword(any(), any(), any()) }

        vm.onCodeChange("123456")
        vm.onPasswordChange("Nueva1234")
        vm.onConfirmChange("Otra1234")
        vm.submitReset() // valida de nuevo sin llamar al servidor
        assertEquals("Las contraseñas no coinciden", vm.state.value.confirmError)
        vm.onConfirmChange("Nueva1234")
        assertNull(vm.state.value.codeError)
        assertNull(vm.state.value.confirmError)

        coEvery { repository.resetPassword(any(), any(), any()) } returns "Código incorrecto"
        vm.submitReset()
        assertEquals("Código incorrecto", vm.state.value.formError)
        assertEquals(ResetStep.CODE, vm.state.value.step)

        vm.resendCode()
        assertEquals("Te enviamos un código nuevo", vm.state.value.info)
        assertEquals("", vm.state.value.code)

        coEvery { repository.requestPasswordReset(any()) } returns "Sin conexión"
        vm.resendCode()
        assertEquals("Sin conexión", vm.state.value.formError)

        vm.changeEmail()
        assertEquals(ResetStep.EMAIL, vm.state.value.step)
        vm.submitEmail()
        assertEquals("Sin conexión", vm.state.value.formError)
        assertEquals(ResetStep.EMAIL, vm.state.value.step)
    }

    @Test
    fun validadorDelCodigo() {
        assertNotNull(Validators.validateResetCode(""))
        assertNotNull(Validators.validateResetCode("12345"))
        assertNotNull(Validators.validateResetCode("12345a"))
        assertNull(Validators.validateResetCode("123456"))
    }

    // ------------------------------- GPS → comuna -------------------------------

    @Test
    fun coordenadasSeConviertenEnLaComunaMasCercana() {
        // Plaza de Armas de Santiago y Mall Plaza Vespucio (La Florida).
        assertEquals("Santiago", ComunaLocator.nearest(-33.4378, -70.6504))
        assertEquals("La Florida", ComunaLocator.nearest(-33.5180, -70.5990))
        // Solo entre las candidatas indicadas.
        assertEquals("Ñuñoa", ComunaLocator.nearest(-33.4378, -70.6504, listOf("Ñuñoa", "Maipú")))
        // Fuera de la RM (Valparaíso) o sin candidatas conocidas.
        assertNull(ComunaLocator.nearest(-33.0472, -71.6127))
        assertNull(ComunaLocator.nearest(-33.4378, -70.6504, listOf("Comuna inventada")))
    }

    @Test
    fun distanciaHaversine() {
        val santiago = ComunaLocator.centroides.getValue("Santiago")
        val maipu = ComunaLocator.centroides.getValue("Maipú")
        val km = ComunaLocator.distanciaKm(santiago, maipu)
        assertTrue("distancia $km", km in 8.0..12.0)
        assertEquals(0.0, ComunaLocator.distanciaKm(santiago, santiago), 0.0001)
    }
}
