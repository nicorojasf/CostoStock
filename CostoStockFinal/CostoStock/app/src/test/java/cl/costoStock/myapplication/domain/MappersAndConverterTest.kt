package cl.costoStock.myapplication.domain

import cl.costoStock.myapplication.data.model.OrderStatus
import cl.costoStock.myapplication.data.remote.ApiErrors
import cl.costoStock.myapplication.data.remote.dto.ClienteDto
import cl.costoStock.myapplication.data.remote.dto.IndicadorDto
import cl.costoStock.myapplication.data.remote.dto.IndicadoresDto
import cl.costoStock.myapplication.data.remote.dto.PedidoDto
import cl.costoStock.myapplication.data.remote.dto.PedidoItemDto
import cl.costoStock.myapplication.data.remote.isVisibleForBuyers
import cl.costoStock.myapplication.data.remote.toIndicators
import cl.costoStock.myapplication.data.remote.toOrder
import cl.costoStock.myapplication.data.remote.toProduct
import cl.costoStock.myapplication.data.remote.toUser
import cl.costoStock.myapplication.domain.currency.CurrencyConverter
import cl.costoStock.myapplication.testutil.indicators
import cl.costoStock.myapplication.testutil.jsonBody
import cl.costoStock.myapplication.testutil.productoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersAndConverterTest {

    @Test
    fun productoDtoSeConvierteEnProducto() {
        val p = productoDto(7L, precio = 12990, stock = 3).toProduct()!!
        assertEquals(7L, p.id)
        assertEquals(12990, p.price)
        assertEquals(3, p.stock)
        assertEquals("Hogar", p.category)
    }

    @Test
    fun productoSinIdSeDescartaYValoresNulosTienenRespaldo() {
        assertNull(productoDto(null).toProduct())
        val p = productoDto(1L, precio = null, stock = null).copy(categoria = null, emoji = null).toProduct()!!
        assertEquals(0, p.price)
        assertEquals("Otros", p.category)
        assertEquals("🛒", p.emoji)
    }

    @Test
    fun soloPublicacionesActivasSonVisibles() {
        assertTrue(productoDto(1L, estado = "ACTIVE").isVisibleForBuyers)
        assertTrue(productoDto(1L, estado = null).isVisibleForBuyers)
        assertFalse(productoDto(1L, estado = "PAUSED").isVisibleForBuyers)
    }

    @Test
    fun clienteDtoSeConvierteEnUsuario() {
        assertEquals("Ana", ClienteDto(3L, "Ana", "ana@mail.cl", 0L).toUser()!!.name)
        assertNull(ClienteDto(null, "Ana", "ana@mail.cl", 0L).toUser())
    }

    @Test
    fun pedidoDtoSeConvierteEnPedido() {
        val dto = PedidoDto(
            id = 9L, clienteId = 1L, creadoEn = 100L, neto = 840, iva = 160, total = 1000,
            estado = "CANCELADO", cantidadTotal = null,
            items = listOf(
                PedidoItemDto(1L, "Lámpara", "💡", 500, 2, 1000),
                PedidoItemDto(null, "Sin id", null, 1, 1, 1)
            )
        )
        val order = dto.toOrder()!!
        assertEquals(OrderStatus.CANCELLED, order.status)
        assertEquals(1, order.lines.size)
        assertEquals(2, order.itemCount)
        assertEquals(1000, order.lines[0].subtotal)
        assertEquals(OrderStatus.CONFIRMED, dto.copy(estado = "CONFIRMADO").toOrder()!!.status)
        assertNull(dto.copy(id = null).toOrder())
    }

    @Test
    fun indicadoresRequierenDolarYUf() {
        val dto = IndicadoresDto(
            fecha = "2026-11-20",
            uf = IndicadorDto("uf", "UF", "Pesos", "2026-11-20", 40000.0),
            dolar = IndicadorDto("dolar", "Dólar", "Pesos", "2026-11-19", 950.5),
            euro = null,
            utm = null
        )
        val result = dto.toIndicators()!!
        assertEquals(950.5, result.dolar, 0.0)
        assertEquals("2026-11-19", result.date)
        assertNull(dto.copy(dolar = null).toIndicators())
        assertNull(dto.copy(uf = null).toIndicators())
    }

    @Test
    fun conversionDePesosADolaresYUf() {
        assertEquals(12.99, CurrencyConverter.clpToUsd(12990, 1000.0), 0.0001)
        assertEquals(0.5, CurrencyConverter.clpToUf(20000, 40000.0), 0.0001)
        assertEquals(0.0, CurrencyConverter.clpToUsd(1000, 0.0), 0.0)
        assertEquals(0.0, CurrencyConverter.clpToUf(1000, 0.0), 0.0)
        assertEquals("US$ 1,234.50", CurrencyConverter.formatUsd(1234.5))
        assertTrue(CurrencyConverter.formatUf(0.5).startsWith("UF 0"))
        assertTrue(CurrencyConverter.formatIndicator(950.5).startsWith("$ 950"))
        assertTrue(CurrencyConverter.equivalence(20000, indicators).startsWith("≈ US$ 20.00 · UF"))
    }

    @Test
    fun fechaIsoSeMuestraComoDiaMesAnio() {
        assertEquals("20-11-2026", CurrencyConverter.formatDate("2026-11-20T03:00:00.000Z"))
        assertEquals("hoy", CurrencyConverter.formatDate("hoy"))
    }

    @Test
    fun erroresDeLosMicroserviciosSeLeen() {
        val error = ApiErrors.parse(jsonBody("""{"status":409,"mensaje":"Stock insuficiente","detalles":["Lámpara"]}"""))
        assertEquals("Stock insuficiente", error!!.mensaje)
        assertEquals(listOf("Lámpara"), error.detalles)
        assertNull(ApiErrors.parse(jsonBody("no es json {")))
        assertNull(ApiErrors.parse(jsonBody("")))
        assertNull(ApiErrors.parse(null))
    }
}
