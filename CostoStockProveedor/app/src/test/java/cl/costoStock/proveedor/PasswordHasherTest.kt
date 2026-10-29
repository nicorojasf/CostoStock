package cl.costoStock.proveedor

import cl.costoStock.proveedor.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun hashNoContieneLaContrasena() {
        val hash = PasswordHasher.hash("clave1234", PasswordHasher.newSalt())
        assertFalse(hash.contains("clave1234"))
        assertEquals(64, hash.length)
    }
}
