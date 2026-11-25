package cl.costoStock.proveedor

import cl.costoStock.proveedor.data.model.ProductCondition
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.remote.ApiErrors
import cl.costoStock.proveedor.data.remote.IndicadoresApi
import cl.costoStock.proveedor.data.remote.dto.IndicadorDto
import cl.costoStock.proveedor.data.remote.dto.IndicadoresDto
import cl.costoStock.proveedor.data.remote.dto.ProveedorDto
import cl.costoStock.proveedor.data.remote.toIndicators
import cl.costoStock.proveedor.data.remote.toProduct
import cl.costoStock.proveedor.data.remote.toRequest
import cl.costoStock.proveedor.data.remote.toSeller
import cl.costoStock.proveedor.data.repository.IndicatorsRepository
import cl.costoStock.proveedor.domain.currency.CurrencyConverter
import cl.costoStock.proveedor.testutil.draft
import cl.costoStock.proveedor.testutil.indicators
import cl.costoStock.proveedor.testutil.jsonBody
import cl.costoStock.proveedor.testutil.productoDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MappersAndConverterTest {

    @Test
    fun proveedorDtoSeConvierteEnSeller() {
        val seller = ProveedorDto(3L, "Tienda", "12345678-5", "t@mail.cl", "+56912345678", 99L).toSeller()!!
        assertEquals("Tienda", seller.name)
        assertEquals(99L, seller.createdAt)
        assertNull(seller.photoPath)
        assertNull(ProveedorDto(null, null, null, null, null, null).toSeller())
        assertEquals(0L, ProveedorDto(1L, null, null, null, null, null).toSeller()!!.createdAt)
    }

    @Test
    fun productoDtoSeConvierteEnPublicacion() {
        val active = productoDto(5L).toProduct()!!
        assertEquals(ProductStatus.ACTIVE, active.status)
        assertEquals(ProductCondition.NEW, active.condition)
        assertEquals(listOf("/fotos/5.jpg"), active.imagePaths)

        val paused = productoDto(6L, estado = "PAUSED", condicion = "USED").toProduct()!!
        assertEquals(ProductStatus.PAUSED, paused.status)
        assertEquals(ProductCondition.USED, paused.condition)

        val empty = productoDto(7L).copy(
            proveedorId = null, nombre = null, categoria = null, descripcion = null, precio = null, stock = null,
            comuna = null, emoji = null, imagenes = null, creadoEn = null, actualizadoEn = null
        ).toProduct()!!
        assertEquals("Otros", empty.category)
        assertEquals("🛒", empty.emoji)
        assertTrue(empty.imagePaths.isEmpty())
        assertNull(productoDto(null).toProduct())
    }

    @Test
    fun borradorSeConvierteEnRequest() {
        val request = draft(listOf("/a.jpg")).toRequest(sellerId = 9L, emoji = "🏠", imagePaths = listOf("/b.jpg"))
        assertEquals(9L, request.proveedorId)
        assertEquals("USED", request.condicion)
        assertEquals(listOf("/b.jpg"), request.imagenes)
        assertEquals(12_990, request.precio)
    }

    @Test
    fun conversionDeMonedasYErrores() {
        assertEquals("US$ 12.99", CurrencyConverter.formatUsd(CurrencyConverter.clpToUsd(12_990, 1000.0)))
        assertEquals(0.0, CurrencyConverter.clpToUsd(1, 0.0), 0.0)
        assertEquals(0.0, CurrencyConverter.clpToUf(1, 0.0), 0.0)
        assertTrue(CurrencyConverter.equivalence(40_000, indicators).endsWith("UF 1,00"))
        assertTrue(CurrencyConverter.formatIndicator(1000.0).startsWith("$ 1.000"))
        assertEquals("20-11-2026", CurrencyConverter.formatDate(indicators.date))
        assertEquals("x", CurrencyConverter.formatDate("x"))

        assertEquals("No", ApiErrors.parse(jsonBody("""{"mensaje":"No"}"""))!!.mensaje)
        assertNull(ApiErrors.parse(jsonBody("{roto")))
        assertNull(ApiErrors.parse(null))
    }

    private val dto = IndicadoresDto(
        fecha = "2026-11-20",
        uf = IndicadorDto("uf", "UF", "Pesos", null, 40000.0),
        dolar = IndicadorDto("dolar", "Dólar", "Pesos", null, 950.0),
        euro = null,
        utm = IndicadorDto("utm", "UTM", "Pesos", null, 68000.0)
    )

    @Test
    fun indicadoresDesdeLaApiExterna() = runTest {
        assertEquals("2026-11-20", dto.toIndicators()!!.date)
        assertNull(dto.copy(uf = null).toIndicators())
        assertNull(dto.copy(dolar = null).toIndicators())

        val api = mockk<IndicadoresApi>()
        var now = 0L
        coEvery { api.obtener() } returns dto
        val repository = IndicatorsRepository(api, UnconfinedTestDispatcher(testScheduler)) { now }

        assertTrue(repository.refresh())
        assertTrue(repository.refresh())
        coVerify(exactly = 1) { api.obtener() }

        now = IndicatorsRepository.CACHE_MS + 1
        coEvery { api.obtener() } throws IOException()
        assertTrue(repository.refresh()) // falla, pero conserva los datos anteriores
        assertEquals(950.0, repository.indicators.value!!.dolar, 0.0)

        val empty = IndicatorsRepository(api, UnconfinedTestDispatcher(testScheduler))
        assertFalse(empty.refresh())
    }
}
