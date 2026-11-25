package cl.costoStock.proveedor.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash de contraseñas con PBKDF2 + sal aleatoria por usuario.
 * Las contraseñas nunca se guardan en texto plano.
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA1" // disponible desde API 24
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private val random = SecureRandom()

    fun newSalt(): String = ByteArray(SALT_BYTES).also { random.nextBytes(it) }.toHex()

    fun hash(password: String, saltHex: String): String {
        val spec = PBEKeySpec(password.toCharArray(), saltHex.decodeHex(), ITERATIONS, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    /** Comparación en tiempo constante. */
    fun verify(password: String, saltHex: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(
            hash(password, saltHex).toByteArray(),
            expectedHash.toByteArray()
        )

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.decodeHex(): ByteArray =
        ByteArray(length / 2) { i -> substring(2 * i, 2 * i + 2).toInt(16).toByte() }
}
