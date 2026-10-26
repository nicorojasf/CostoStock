package cl.costoStock.myapplication.domain.cart

import cl.costoStock.myapplication.data.model.CartLine

data class CartSummary(
    val itemCount: Int,
    /** Valor neto (sin IVA). */
    val net: Int,
    /** IVA contenido en el total. */
    val iva: Int,
    /** Total a pagar (los precios del catálogo ya incluyen IVA). */
    val total: Int
) {
    companion object {
        val EMPTY = CartSummary(0, 0, 0, 0)
    }
}

sealed interface CheckoutResult {
    data class Success(val summary: CartSummary) : CheckoutResult
    data object Empty : CheckoutResult
    data class OutOfStock(val productNames: List<String>) : CheckoutResult
}

/** Lógica pura de totales del carrito (sin dependencias de Android). */
object CartCalculator {
    const val IVA_PERCENT = 19

    fun summarize(lines: List<CartLine>): CartSummary {
        val total = lines.sumOf { it.subtotal }
        // net = total / 1,19 redondeado al peso más cercano, con enteros.
        val net = ((total.toLong() * 100 + 59) / (100 + IVA_PERCENT)).toInt()
        return CartSummary(
            itemCount = lines.sumOf { it.quantity },
            net = net,
            iva = total - net,
            total = total
        )
    }
}
