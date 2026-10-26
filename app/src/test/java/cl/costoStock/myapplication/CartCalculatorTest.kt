package cl.costoStock.myapplication

import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.data.model.Product
import cl.costoStock.myapplication.domain.cart.CartCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class CartCalculatorTest {
    private fun product(id: Long, price: Int) =
        Product(id, "P$id", "Moda", "desc", price, 100, "Santiago", "🛍️")

    @Test
    fun carritoVacioTotalCero() {
        val s = CartCalculator.summarize(emptyList())
        assertEquals(0, s.total)
        assertEquals(0, s.itemCount)
    }

    @Test
    fun totalEsLaSumaDeLosPreciosConIva() {
        val lines = listOf(CartLine(product(1, 1000), 2), CartLine(product(2, 500), 1))
        val s = CartCalculator.summarize(lines)
        assertEquals(2500, s.total)
        assertEquals(2101, s.net)
        assertEquals(399, s.iva)
        assertEquals(3, s.itemCount)
    }

    @Test
    fun netoMasIvaSiempreSumaElTotal() {
        val s = CartCalculator.summarize(listOf(CartLine(product(1, 1290), 1)))
        assertEquals(1084, s.net)
        assertEquals(s.total, s.net + s.iva)
    }
}
