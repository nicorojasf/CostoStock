package cl.costoStock.myapplication.ui.util

import java.text.NumberFormat
import java.util.Locale

private val clpFormat: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL")).apply {
        maximumFractionDigits = 0
    }

/** Formatea un monto entero en pesos chilenos, ej: $12.990 */
fun formatClp(amount: Int): String = clpFormat.format(amount.toLong())

fun categoryEmoji(category: String): String = when (category) {
    "Moda" -> "👗"
    "Hogar" -> "🏠"
    "Bebés y niños" -> "🍼"
    "Tecnología" -> "💻"
    "Mascotas" -> "🐾"
    else -> "🛒"
}
