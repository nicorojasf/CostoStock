package cl.costoStock.proveedor.domain.validation

/**
 * Todas las validaciones de formularios, separadas de la UI (Kotlin puro, probable con tests).
 * Cada función devuelve el mensaje de error o `null` si el valor es válido.
 */
object Validators {
    const val MIN_PASSWORD_LENGTH = 8
    const val NAME_MAX = 60
    const val TITLE_MIN = 10
    const val TITLE_MAX = 60
    const val DESCRIPTION_MIN = 20
    const val DESCRIPTION_MAX = 500
    const val MIN_PRICE = 100
    const val MAX_PRICE = 99_999_999
    const val MAX_STOCK = 9_999
    const val MAX_PHOTOS = 3

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val RUT_REGEX = Regex("^\\d{7,8}[0-9K]$")
    private val PHONE_REGEX = Regex("^(\\+?56)?9\\d{8}$")

    // ------------------------------ Cuenta ------------------------------

    fun validateName(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Ingresa el nombre de tu comercio o tu nombre"
            v.length < 2 -> "El nombre debe tener al menos 2 caracteres"
            v.length > NAME_MAX -> "El nombre no puede superar los $NAME_MAX caracteres"
            v.none { it.isLetter() } -> "El nombre debe incluir letras"
            else -> null
        }
    }

    fun validateRut(value: String): String? {
        if (value.isBlank()) return "Ingresa tu RUT"
        val clean = cleanRut(value)
        if (!RUT_REGEX.matches(clean)) return "Formato inválido (ej: 12.345.678-5)"
        return if (isRutCheckDigitValid(clean)) null else "RUT inválido: el dígito verificador no coincide"
    }

    fun validateEmail(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Ingresa tu correo electrónico"
            !EMAIL_REGEX.matches(v) -> "Ingresa un correo válido (ej: nombre@correo.cl)"
            else -> null
        }
    }

    fun validatePhone(value: String): String? {
        if (value.isBlank()) return "Ingresa un teléfono de contacto"
        val clean = value.replace(Regex("[\\s-]"), "")
        return if (PHONE_REGEX.matches(clean)) null else "Celular chileno inválido (ej: +56 9 1234 5678)"
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

    // ------------------------------ Publicación ------------------------------

    fun validateTitle(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Ingresa el título de tu publicación"
            v.length < TITLE_MIN -> "Sé más específico: mínimo $TITLE_MIN caracteres (ej: Chaqueta de mezclilla talla M)"
            v.length > TITLE_MAX -> "El título no puede superar los $TITLE_MAX caracteres"
            else -> null
        }
    }

    fun validateCategory(value: String?): String? =
        if (value.isNullOrBlank()) "Selecciona una categoría" else null

    fun validateCondition(value: String?): String? =
        if (value.isNullOrBlank()) "Indica si el producto es nuevo o usado" else null

    fun validateDescription(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Describe tu producto"
            v.length < DESCRIPTION_MIN -> "Agrega más detalles: mínimo $DESCRIPTION_MIN caracteres"
            v.length > DESCRIPTION_MAX -> "La descripción no puede superar los $DESCRIPTION_MAX caracteres"
            else -> null
        }
    }

    fun validatePhotos(count: Int): String? = when {
        count < 1 -> "Agrega al menos una foto de tu producto"
        count > MAX_PHOTOS -> "Puedes subir hasta $MAX_PHOTOS fotos"
        else -> null
    }

    /** El precio se escribe como número entero en pesos, sin puntos ni signos. */
    fun validatePrice(text: String): String? {
        val v = text.trim()
        if (v.isEmpty()) return "Ingresa el precio"
        val number = v.toLongOrNull() ?: return "Usa solo números, sin puntos ni signos"
        return when {
            number < MIN_PRICE -> "El precio mínimo es $$MIN_PRICE"
            number > MAX_PRICE -> "El precio es demasiado alto"
            else -> null
        }
    }

    fun validateStock(text: String): String? {
        val v = text.trim()
        if (v.isEmpty()) return "Ingresa las unidades disponibles"
        val number = v.toIntOrNull() ?: return "Debe ser un número entero"
        return when {
            number < 1 -> "Debe haber al menos 1 unidad disponible"
            number > MAX_STOCK -> "El máximo es $MAX_STOCK unidades"
            else -> null
        }
    }

    fun validateComuna(value: String?): String? =
        if (value.isNullOrBlank()) "Selecciona la comuna donde está el producto" else null

    fun parsePrice(text: String): Int? = text.trim().toIntOrNull()

    // ------------------------------ Utilidades de formato ------------------------------

    fun cleanRut(raw: String): String =
        raw.replace(".", "").replace("-", "").replace(" ", "").uppercase()

    /** RUT en formato de almacenamiento: 12345678-5 */
    fun normalizeRut(raw: String): String {
        val clean = cleanRut(raw)
        return if (clean.length < 2) clean else "${clean.dropLast(1)}-${clean.last()}"
    }

    /** RUT para mostrar: 12.345.678-5 */
    fun formatRut(normalized: String): String {
        val clean = cleanRut(normalized)
        if (clean.length < 2) return clean
        val body = clean.dropLast(1).reversed().chunked(3).joinToString(".").reversed()
        return "$body-${clean.last()}"
    }

    /** Teléfono en formato de almacenamiento: +569XXXXXXXX */
    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        val national = if (digits.startsWith("56")) digits.drop(2) else digits
        return "+56$national"
    }

    /** Teléfono para mostrar: +56 9 1234 5678 */
    fun formatPhone(normalized: String): String {
        val digits = normalized.filter { it.isDigit() }
        if (digits.length != 11) return normalized
        return "+56 ${digits[2]} ${digits.substring(3, 7)} ${digits.substring(7)}"
    }

    // Módulo 11 del RUT chileno
    private fun isRutCheckDigitValid(cleanRut: String): Boolean {
        val body = cleanRut.dropLast(1)
        val dv = cleanRut.last()
        var sum = 0
        var multiplier = 2
        for (c in body.reversed()) {
            sum += (c - '0') * multiplier
            multiplier = if (multiplier == 7) 2 else multiplier + 1
        }
        val expected = when (val remainder = 11 - (sum % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + remainder
        }
        return dv == expected
    }
}
