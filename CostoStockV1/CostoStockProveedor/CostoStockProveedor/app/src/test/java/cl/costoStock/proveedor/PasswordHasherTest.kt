package cl.costoStock.proveedor

import cl.costoStock.proveedor.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordHasherTest {
    @Test
    fun verificaContrasenaCorrecta() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("clave1234", salt)
        assertTrue(PasswordHasher.verify("clave1234", salt, hash))
    }

    @Test
    fun rechazaContrasenaIncorrecta() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("clave1234", salt)
        assertFalse(PasswordHasher.verify("clave12345", salt, hash))
    }

    @Test
    fun saltsDistintasGeneranHashesDistintos() {
        val h1 = PasswordHasher.hash("clave1234", PasswordHasher.newSalt())
        val h2 = PasswordHasher.hash("clave1234", PasswordHasher.newSalt())
        assertNotEquals(h1, h2)
    }

    @Test
    fun aceptaHashAntiguoPbkdf2Sha1() {
        val salt = PasswordHasher.newSalt()
        val saltBytes = ByteArray(salt.length / 2) { i -> salt.substring(2 * i, 2 * i + 2).toInt(16).toByte() }
        val spec = PBEKeySpec("clave1234".toCharArray(), saltBytes, 10_000, 256)
        val legacy = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
            .joinToString("") { "%02x".format(it) }
        assertTrue(PasswordHasher.verify("clave1234", salt, legacy))
        assertFalse(PasswordHasher.verify("otraClave99", salt, legacy))
    }

    @Test
    fun hashNuevoNoEsElDelEsquemaAntiguo() {
        val salt = PasswordHasher.newSalt()
        val saltBytes = ByteArray(salt.length / 2) { i -> salt.substring(2 * i, 2 * i + 2).toInt(16).toByte() }
        val spec = PBEKeySpec("clave1234".toCharArray(), saltBytes, 10_000, 256)
        val legacy = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
            .joinToString("") { "%02x".format(it) }
        assertNotEquals(legacy, PasswordHasher.hash("clave1234", salt))
    }

    @Test
    fun hashNoContieneLaContrasena() {
        val hash = PasswordHasher.hash("clave1234", PasswordHasher.newSalt())
        assertFalse(hash.contains("clave1234"))
        assertEquals(64, hash.length)
    }
}
