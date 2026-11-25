package cl.costoStock.myapplication

import cl.costoStock.myapplication.domain.validation.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {
    @Test fun emailValido() = assertNull(Validators.validateEmail("ana@correo.cl"))
    @Test fun emailVacio() = assertNotNull(Validators.validateEmail("  "))
    @Test fun emailSinArroba() = assertNotNull(Validators.validateEmail("ana.correo.cl"))
    @Test fun emailSinDominio() = assertNotNull(Validators.validateEmail("ana@correo"))

    @Test fun nombreValido() = assertNull(Validators.validateName("María José"))
    @Test fun nombreConNumeros() = assertNotNull(Validators.validateName("Ana123"))
    @Test fun nombreCorto() = assertNotNull(Validators.validateName("A"))

    @Test fun passwordValida() = assertNull(Validators.validatePassword("clave1234"))
    @Test fun passwordCorta() = assertNotNull(Validators.validatePassword("abc12"))
    @Test fun passwordSinNumero() = assertNotNull(Validators.validatePassword("solo letras"))
    @Test fun passwordSinLetra() = assertNotNull(Validators.validatePassword("12345678"))
    @Test fun passwordConEspacios() = assertNotNull(Validators.validatePassword("clave 1234"))

    @Test fun confirmacionIgual() = assertNull(Validators.validateConfirmPassword("abc12345", "abc12345"))
    @Test fun confirmacionDistinta() = assertNotNull(Validators.validateConfirmPassword("abc12345", "abc12346"))

    @Test fun terminosRequeridos() {
        assertNotNull(Validators.validateTerms(false))
        assertNull(Validators.validateTerms(true))
    }

    @Test fun fortalezaCreceConLaComplejidad() {
        assertEquals(0, Validators.passwordStrength(""))
        assert(Validators.passwordStrength("Abcdef12!xyz") > Validators.passwordStrength("abc"))
        assertEquals(4, Validators.passwordStrength("Abcdef12!xyzQ"))
    }
}
