package cl.costoStock.myapplication.domain.currency

import cl.costoStock.myapplication.data.model.EconomicIndicators
import java.util.Locale

/** Conversión de precios en pesos a dólares y UF usando los indicadores de mindicador.cl. */
object CurrencyConverter {

    private val chile: Locale = Locale.forLanguageTag("es-CL")

    fun clpToUsd(clp: Int, dolar: Double): Double = if (dolar <= 0.0) 0.0 else clp / dolar

    fun clpToUf(clp: Int, uf: Double): Double = if (uf <= 0.0) 0.0 else clp / uf

    /** Ej: US$ 1,234.56 */
    fun formatUsd(value: Double): String = String.format(Locale.US, "US$ %,.2f", value)

    /** Ej: UF 0,52 */
    fun formatUf(value: Double): String = "UF " + String.format(chile, "%,.2f", value)

    /** Valor de un indicador en pesos con decimales, ej: $ 38.512,30 */
    fun formatIndicator(value: Double): String = "$ " + String.format(chile, "%,.2f", value)

    /** Texto de equivalencia para mostrar bajo un precio, ej: ≈ US$ 13.50 · UF 0,33 */
    fun equivalence(clp: Int, indicators: EconomicIndicators): String =
        "≈ ${formatUsd(clpToUsd(clp, indicators.dolar))} · ${formatUf(clpToUf(clp, indicators.uf))}"

    /** Toma la parte de fecha (yyyy-MM-dd) de un ISO-8601 y la deja como dd-MM-yyyy. */
    fun formatDate(iso: String): String {
        val day = iso.take(10)
        val parts = day.split("-")
        return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else day
    }
}
