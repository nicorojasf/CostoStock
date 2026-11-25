package cl.costoStock.proveedor.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash de contraseñas con PBKDF2-HMAC-SHA256 + sal aleatoria por usuario.
 * Las contraseñas nunca se guardan en texto plano.
 *
 * Las cuentas creadas con la versión anterior (PBKDF2-SHA1, 10.000 iteraciones) siguen
 * pudiendo iniciar sesión: [verify] prueba primero el esquema actual y luego el antiguo.
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256" // disponible desde API 26 (minSdk = 26)
    // OWASP recomienda 600.000 para SHA-256; se usa un valor menor para que el login siga siendo
    // fluido en teléfonos de gama baja. Súbelo si tus pruebas lo permiten.
    private const val ITERATIONS = 210_000
    private const val LEGACY_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val LEGACY_ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private val random = SecureRandom()

    fun newSalt(): String = ByteArray(SALT_BYTES).also { random.nextBytes(it) }.toHex()

    fun hash(password: String, saltHex: String): String =
        derive(password, saltHex, ALGORITHM, ITERATIONS)

    /** Comparación en tiempo constante. */
    fun verify(password: String, saltHex: String, expectedHash: String): Boolean {
        val expected = expectedHash.toByteArray()
        if (MessageDigest.isEqual(hash(password, saltHex).toByteArray(), expected)) return true
        // Cuentas antiguas (antes de subir a SHA-256).
        val legacy = derive(password, saltHex, LEGACY_ALGORITHM, LEGACY_ITERATIONS)
        return MessageDigest.isEqual(legacy.toByteArray(), expected)
    }

    private fun derive(password: String, saltHex: String, algorithm: String, iterations: Int): String {
        val spec = PBEKeySpec(password.toCharArray(), saltHex.decodeHex(), iterations, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.decodeHex(): ByteArray =
        ByteArray(length / 2) { i -> substring(2 * i, 2 * i + 2).toInt(16).toByte() }
}
