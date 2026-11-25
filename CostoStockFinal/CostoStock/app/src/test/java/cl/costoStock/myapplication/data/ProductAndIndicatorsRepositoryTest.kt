package cl.costoStock.myapplication.data

import cl.costoStock.myapplication.data.remote.ApiErrors
import cl.costoStock.myapplication.data.remote.IndicadoresApi
import cl.costoStock.myapplication.data.remote.ProductosApi
import cl.costoStock.myapplication.data.remote.dto.IndicadorDto
import cl.costoStock.myapplication.data.remote.dto.IndicadoresDto
import cl.costoStock.myapplication.data.repository.IndicatorsRepository
import cl.costoStock.myapplication.data.repository.ProductRepository
import cl.costoStock.myapplication.data.repository.SyncState
import cl.costoStock.myapplication.testutil.productoDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductAndIndicatorsRepositoryTest {

    private val productosApi = mockk<ProductosApi>()
    private val indicadoresApi = mockk<IndicadoresApi>()

    @Test
    fun refreshDescargaSoloPublicacionesActivas() = runTest {
        coEvery { productosApi.listar(false) } returns listOf(
            productoDto(1L), productoDto(2L, estado = "PAUSED"), productoDto(null)
        )
        val repository = ProductRepository(productosApi, UnconfinedTestDispatcher(testScheduler))

        assertEquals(SyncState.Loading, repository.sync.value)
        assertTrue(repository.refresh())
        assertEquals(listOf(1L), repository.products.value.map { it.id })
        assertEquals(SyncState.Ready, repository.sync.value)
        assertEquals(1L, repository.findCached(1L)?.id)
        assertNull(repository.findCached(2L))
    }

    @Test
    fun sinConexionConservaLaListaYMarcaError() = runTest {
        coEvery { productosApi.listar(false) } returns listOf(productoDto(1L))
        val repository = ProductRepository(productosApi, UnconfinedTestDispatcher(testScheduler))
        repository.refresh()

        coEvery { productosApi.listar(false) } throws IOException("sin red")
        assertFalse(repository.refresh())
        assertEquals(1, repository.products.value.size)
        assertEquals(SyncState.Error(ApiErrors.NETWORK), repository.sync.value)

        coEvery { productosApi.listar(false) } throws IllegalStateException()
        assertFalse(repository.refresh())
        assertEquals(SyncState.Error(ApiErrors.UNEXPECTED), repository.sync.value)
    }

    @Test
    fun observarElCatalogoLoDescargaAutomaticamente() = runTest {
        coEvery { productosApi.listar(false) } returns listOf(productoDto(1L), productoDto(2L))
        val repository = ProductRepository(productosApi, UnconfinedTestDispatcher(testScheduler), refreshIntervalMs = 1_000)

        val products = repository.observeProducts().first { it.isNotEmpty() }

        assertEquals(2, products.size)
        coVerify(atLeast = 1) { productosApi.listar(false) }
    }

    private val indicadoresDto = IndicadoresDto(
        fecha = "2026-11-20",
        uf = IndicadorDto("uf", "UF", "Pesos", "2026-11-20", 40000.0),
        dolar = IndicadorDto("dolar", "Dólar", "Pesos", "2026-11-20", 950.0),
        euro = IndicadorDto("euro", "Euro", "Pesos", "2026-11-20", 1020.0),
        utm = null
    )

    @Test
    fun indicadoresSeDescarganYQuedanEnCache() = runTest {
        var now = 0L
        coEvery { indicadoresApi.obtener() } returns indicadoresDto
        val repository = IndicatorsRepository(indicadoresApi, UnconfinedTestDispatcher(testScheduler)) { now }

        assertTrue(repository.refresh())
        assertEquals(950.0, repository.indicators.value!!.dolar, 0.0)

        // Dentro del tiempo de caché no se vuelve a llamar a la API.
        now = 1_000L
        assertTrue(repository.refresh())
        coVerify(exactly = 1) { indicadoresApi.obtener() }

        // Forzado o con la caché vencida sí se llama.
        assertTrue(repository.refresh(force = true))
        now = IndicatorsRepository.CACHE_MS * 2
        assertTrue(repository.refresh())
        coVerify(exactly = 3) { indicadoresApi.obtener() }
    }

    @Test
    fun siLaApiExternaFallaLaAppSigueSinIndicadores() = runTest {
        coEvery { indicadoresApi.obtener() } throws IOException("sin red")
        val repository = IndicatorsRepository(indicadoresApi, UnconfinedTestDispatcher(testScheduler))

        assertFalse(repository.refresh())
        assertNull(repository.indicators.value)

        coEvery { indicadoresApi.obtener() } returns indicadoresDto.copy(dolar = null)
        assertFalse(repository.refresh(force = true))
    }
}
