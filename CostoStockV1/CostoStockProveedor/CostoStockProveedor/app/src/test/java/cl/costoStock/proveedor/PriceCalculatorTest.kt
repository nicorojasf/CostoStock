package cl.costoStock.proveedor

import cl.costoStock.proveedor.domain.pricing.PriceCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class PriceCalculatorTest {

    @Test
    fun desglose_precioRedondo() {
        val b = PriceCalculator.breakdown(11_900)
        assertEquals(10_000, b.net)
        assertEquals(1_900, b.iva)
        assertEquals(11_900, b.total)
    }

    @Test
    fun desglose_netoMasIvaSiempreSumaElTotal() {
        listOf(100, 999, 1_000, 12_990, 45_990, 1_234_567).forEach { price ->
            val b = PriceCalculator.breakdown(price)
            assertEquals(price, b.net + b.iva)
        }
    }

    @Test
    fun desglose_redondeaAlPesoMasCercano() {
        // 1.000 / 1,19 = 840,33 -> 840
        val b = PriceCalculator.breakdown(1_000)
        assertEquals(840, b.net)
        assertEquals(160, b.iva)
    }
}
