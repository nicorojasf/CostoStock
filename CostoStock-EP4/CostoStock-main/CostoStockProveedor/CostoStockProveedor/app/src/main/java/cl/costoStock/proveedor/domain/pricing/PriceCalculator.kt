package cl.costoStock.proveedor.domain.pricing

data class PriceBreakdown(
    /** Valor neto (sin IVA). */
    val net: Int,
    /** IVA contenido en el precio. */
    val iva: Int,
    /** Precio final publicado (IVA incluido). */
    val total: Int
)

/** Misma regla que el carrito de la app de cliente: los precios publicados ya incluyen IVA. */
object PriceCalculator {
    const val IVA_PERCENT = 19

    fun breakdown(price: Int): PriceBreakdown {
        // net = price / 1,19 redondeado al peso más cercano, usando solo enteros.
        val net = ((price.toLong() * 100 + 59) / (100 + IVA_PERCENT)).toInt()
        return PriceBreakdown(net = net, iva = price - net, total = price)
    }
}
