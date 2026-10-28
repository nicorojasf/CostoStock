package cl.costoStock.proveedor

import cl.costoStock.proveedor.domain.validation.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    // ---------- RUT ----------
    @Test fun rut_valido_conFormato() = assertNull(Validators.validateRut("12.345.678-5"))
    @Test fun rut_valido_sinFormato() = assertNull(Validators.validateRut("123456785"))
    @Test fun rut_valido_otroEjemplo() = assertNull(Validators.validateRut("7.654.321-6"))
    @Test fun rut_valido_conDigitoK() = assertNull(Validators.validateRut("12.345.670-k"))
    @Test fun rut_digitoVerificadorIncorrecto() = assertNotNull(Validators.validateRut("12.345.678-9"))
    @Test fun rut_formatoInvalido() = assertNotNull(Validators.validateRut("abc"))
    @Test fun rut_vacio() = assertNotNull(Validators.validateRut(""))
    @Test fun rut_normalizado() = assertEquals("12345678-5", Validators.normalizeRut("12.345.678-5"))
    @Test fun rut_formateadoParaMostrar() = assertEquals("12.345.678-5", Validators.formatRut("12345678-5"))

    // ---------- Teléfono ----------
    @Test fun telefono_valido_conFormato() = assertNull(Validators.validatePhone("+56 9 1234 5678"))
    @Test fun telefono_valido_sinPrefijo() = assertNull(Validators.validatePhone("912345678"))
    @Test fun telefono_invalido() = assertNotNull(Validators.validatePhone("12345"))
    @Test fun telefono_fijoNoEsCelular() = assertNotNull(Validators.validatePhone("221234567"))
    @Test fun telefono_normalizado() = assertEquals("+56912345678", Validators.normalizePhone("9 1234 5678"))
    @Test fun telefono_formateadoParaMostrar() =
        assertEquals("+56 9 1234 5678", Validators.formatPhone("+56912345678"))

    // ---------- Cuenta ----------
    @Test fun nombre_valido() = assertNull(Validators.validateName("Ropa Vintage Ñuñoa"))
    @Test fun nombre_vacio() = assertNotNull(Validators.validateName("   "))
    @Test fun nombre_soloNumeros() = assertNotNull(Validators.validateName("12345"))
    @Test fun correo_valido() = assertNull(Validators.validateEmail("tienda@correo.cl"))
    @Test fun correo_invalido() = assertNotNull(Validators.validateEmail("tienda@correo"))
    @Test fun contrasena_valida() = assertNull(Validators.validatePassword("clave1234"))
    @Test fun contrasena_corta() = assertNotNull(Validators.validatePassword("abc123"))
    @Test fun contrasena_sinNumero() = assertNotNull(Validators.validatePassword("abcdefghi"))
    @Test fun contrasena_sinLetra() = assertNotNull(Validators.validatePassword("123456789"))
    @Test fun contrasena_conEspacios() = assertNotNull(Validators.validatePassword("clave 1234"))
    @Test fun confirmacion_noCoincide() = assertNotNull(Validators.validateConfirmPassword("clave1234", "clave12345"))
    @Test fun confirmacion_coincide() = assertNull(Validators.validateConfirmPassword("clave1234", "clave1234"))
    @Test fun terminos_noAceptados() = assertNotNull(Validators.validateTerms(false))
    @Test fun terminos_aceptados() = assertNull(Validators.validateTerms(true))
    @Test fun fortaleza_vacia() = assertEquals(0, Validators.passwordStrength(""))
    @Test fun fortaleza_altaConMezcla() =
        assertEquals(4, Validators.passwordStrength("Abcdefgh1234!"))

    // ---------- Publicación ----------
    @Test fun titulo_valido() = assertNull(Validators.validateTitle("Chaqueta de mezclilla talla M"))
    @Test fun titulo_muyCorto() = assertNotNull(Validators.validateTitle("Chaqueta"))
    @Test fun titulo_muyLargo() = assertNotNull(Validators.validateTitle("a".repeat(61)))
    @Test fun categoria_requerida() = assertNotNull(Validators.validateCategory(null))
    @Test fun categoria_valida() = assertNull(Validators.validateCategory("Moda"))
    @Test fun condicion_requerida() = assertNotNull(Validators.validateCondition(null))
    @Test fun condicion_valida() = assertNull(Validators.validateCondition("USED"))
    @Test fun descripcion_muyCorta() = assertNotNull(Validators.validateDescription("Muy buena"))
    @Test fun descripcion_valida() =
        assertNull(Validators.validateDescription("Poco uso, sin manchas ni roturas, talla M."))
    @Test fun descripcion_muyLarga() = assertNotNull(Validators.validateDescription("x".repeat(501)))

    @Test fun precio_valido() = assertNull(Validators.validatePrice("12990"))
    @Test fun precio_minimo() = assertNull(Validators.validatePrice("100"))
    @Test fun precio_bajoElMinimo() = assertNotNull(Validators.validatePrice("99"))
    @Test fun precio_conPuntos() = assertNotNull(Validators.validatePrice("12.990"))
    @Test fun precio_conLetras() = assertNotNull(Validators.validatePrice("abc"))
    @Test fun precio_vacio() = assertNotNull(Validators.validatePrice(""))
    @Test fun precio_demasiadoAlto() = assertNotNull(Validators.validatePrice("100000000"))
    @Test fun precio_parseado() = assertEquals(12990, Validators.parsePrice(" 12990 "))

    @Test fun stock_valido() = assertNull(Validators.validateStock("5"))
    @Test fun stock_cero() = assertNotNull(Validators.validateStock("0"))
    @Test fun stock_decimal() = assertNotNull(Validators.validateStock("2.5"))
    @Test fun stock_limite() = assertNull(Validators.validateStock("9999"))
    @Test fun stock_sobreElLimite() = assertNotNull(Validators.validateStock("10000"))

    @Test fun comuna_requerida() = assertNotNull(Validators.validateComuna(""))
    @Test fun comuna_valida() = assertNull(Validators.validateComuna("Ñuñoa"))

    @Test fun fotos_ninguna() = assertNotNull(Validators.validatePhotos(0))
    @Test fun fotos_una() = assertNull(Validators.validatePhotos(1))
    @Test fun fotos_tres() = assertNull(Validators.validatePhotos(3))
    @Test fun fotos_sobreElMaximo() = assertNotNull(Validators.validatePhotos(4))
}
