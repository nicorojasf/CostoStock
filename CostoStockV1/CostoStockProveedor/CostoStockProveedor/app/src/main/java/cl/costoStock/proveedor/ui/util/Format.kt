package cl.costoStock.proveedor.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val chile: Locale = Locale.forLanguageTag("es-CL")

private val clpFormat: NumberFormat =
    NumberFormat.getCurrencyInstance(chile).apply { maximumFractionDigits = 0 }

/** Formatea un monto entero en pesos chilenos, ej: $12.990 */
fun formatClp(amount: Int): String = clpFormat.format(amount.toLong())

/** Formatea un monto grande (Long) en pesos chilenos. */
fun formatClp(amount: Long): String = clpFormat.format(amount)

/** Ej: "octubre de 2026" */
fun formatMonthYear(millis: Long): String =
    SimpleDateFormat("MMMM 'de' yyyy", chile).format(Date(millis))
