package cl.costoStock.myapplication.domain.validation

/**
 * Validaciones de formularios, separadas de la UI.
 * Cada función devuelve el mensaje de error o `null` si el valor es válido.
 */
object Validators {
    const val MIN_PASSWORD_LENGTH = 8

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val NAME_REGEX = Regex("^\\p{L}[\\p{L} .'-]*$")

    fun validateName(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Ingresa tu nombre"
            v.length < 2 -> "El nombre debe tener al menos 2 caracteres"
            v.length > 50 -> "El nombre no puede superar los 50 caracteres"
            !NAME_REGEX.matches(v) -> "El nombre solo puede contener letras y espacios"
            else -> null
        }
    }

    fun validateEmail(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Ingresa tu correo electrónico"
            !EMAIL_REGEX.matches(v) -> "Ingresa un correo válido (ej: nombre@correo.cl)"
            else -> null
        }
    }

    fun validatePassword(value: String): String? = when {
        value.isEmpty() -> "Ingresa una contraseña"
        value.length < MIN_PASSWORD_LENGTH -> "Debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
        value.any { it.isWhitespace() } -> "No puede contener espacios"
        value.none { it.isLetter() } -> "Debe incluir al menos una letra"
        value.none { it.isDigit() } -> "Debe incluir al menos un número"
        else -> null
    }

    /** En el login solo se exige que no esté vacía (las reglas aplican al registrarse). */
    fun validateLoginPassword(value: String): String? =
        if (value.isEmpty()) "Ingresa tu contraseña" else null

    fun validateConfirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Repite tu contraseña"
        confirm != password -> "Las contraseñas no coinciden"
        else -> null
    }

    fun validateTerms(accepted: Boolean): String? =
        if (accepted) null else "Debes aceptar los términos para continuar"

    /** Fortaleza de 0 (muy débil) a 4 (fuerte). */
    fun passwordStrength(value: String): Int {
        if (value.isEmpty()) return 0
        var score = 0
        if (value.length >= 8) score++
        if (value.length >= 12) score++
        if (value.any { it.isUpperCase() } && value.any { it.isLowerCase() }) score++
        if (value.any { it.isDigit() }) score++
        if (value.any { !it.isLetterOrDigit() }) score++
        return minOf(score, 4)
    }
}
