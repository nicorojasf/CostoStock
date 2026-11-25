package cl.costoStock.proveedor

import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.remote.ApiErrors
import cl.costoStock.proveedor.data.remote.ProductosApi
import cl.costoStock.proveedor.data.repository.ProductRepository
import cl.costoStock.proveedor.data.repository.SyncState
import cl.costoStock.proveedor.data.session.SessionManager
import cl.costoStock.proveedor.data.storage.PhotoStorage
import cl.costoStock.proveedor.testutil.draft
import cl.costoStock.proveedor.testutil.jsonBody
import cl.costoStock.proveedor.testutil.productoDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryTest {

    private val api = mockk<ProductosApi>()
    private val session = mockk<SessionManager>(relaxed = true)
    private val photoStorage = mockk<PhotoStorage>(relaxed = true)
    private val sellerId = MutableStateFlow<Long?>(1L)

    @Before
    fun setUp() {
        every { session.currentSellerId } returns sellerId
        every { photoStorage.copyProductPhoto(any()) } answers { "/products/" + firstArg<String>().substringAfterLast('/') }
    }

    private fun TestScope.repository() =
        ProductRepository(api, session, photoStorage, UnconfinedTestDispatcher(testScheduler), refreshIntervalMs = 1_000)

    private suspend fun ProductRepository.loaded(vararg ids: Long): ProductRepository {
        coEvery { api.listarPorProveedor(1L) } returns ids.map { productoDto(it) }
        refresh()
        return this
    }

    @Test
    fun observarDescargaLasPublicacionesDelProveedor() = runTest {
        coEvery { api.listarPorProveedor(1L) } returns listOf(productoDto(1L), productoDto(null))
        val repo = repository()

        val list = repo.observeMyProducts().first { it.isNotEmpty() }

        assertEquals(listOf(1L), list.map { it.id })
        assertEquals(SyncState.Ready, repo.sync.value)
    }

    @Test
    fun refreshSinSesionOSinConexion() = runTest {
        val repo = repository()
        coEvery { api.listarPorProveedor(1L) } throws IOException()
        assertFalse(repo.refresh())
        assertEquals(SyncState.Error(ApiErrors.NETWORK), repo.sync.value)

        coEvery { api.listarPorProveedor(1L) } throws IllegalStateException()
        assertFalse(repo.refresh())
        assertEquals(SyncState.Error(ApiErrors.UNEXPECTED), repo.sync.value)

        sellerId.value = null
        assertFalse(repo.refresh())
    }

    @Test
    fun obtenerUnaPublicacionDesdeCacheOApi() = runTest {
        val repo = repository().loaded(1L)
        assertEquals(1L, repo.getMyProduct(1L)?.id)

        coEvery { api.obtener(2L) } returns Response.success(productoDto(2L))
        assertEquals(2L, repo.getMyProduct(2L)?.id)

        coEvery { api.obtener(3L) } returns Response.success(productoDto(3L, proveedorId = 99L))
        assertNull(repo.getMyProduct(3L)) // es de otro proveedor

        coEvery { api.obtener(4L) } throws IOException()
        assertNull(repo.getMyProduct(4L))

        sellerId.value = null
        assertNull(repo.getMyProduct(1L))
    }

    @Test
    fun publicarCopiaLasFotosYAgregaALaLista() = runTest {
        val repo = repository()
        coEvery { api.crear(any()) } returns Response.success(productoDto(10L))

        assertEquals(10L, repo.publish(draft(listOf("/cache/a.jpg"))))

        coVerify { api.crear(match { it.imagenes == listOf("/products/a.jpg") && it.proveedorId == 1L && it.emoji == "🏠" }) }
        verify { photoStorage.deleteAll(listOf("/cache/a.jpg")) } // borra la temporal ya copiada
        assertEquals(10L, repo.getMyProduct(10L)?.id)
        assertNull(repo.lastError)
    }

    @Test
    fun publicarSinConexionConservaLasFotosTemporales() = runTest {
        val repo = repository()
        coEvery { api.crear(any()) } throws IOException()

        assertNull(repo.publish(draft(listOf("/cache/a.jpg"))))

        assertEquals(ApiErrors.NETWORK, repo.lastError)
        verify { photoStorage.deleteAll(listOf("/products/a.jpg")) } // solo la copia nueva

        coEvery { api.crear(any()) } returns Response.error(400, jsonBody("""{"mensaje":"Título muy corto"}"""))
        assertNull(repo.publish(draft()))
        assertEquals("Título muy corto", repo.lastError)

        coEvery { api.crear(any()) } throws IllegalStateException()
        assertNull(repo.publish(draft()))
        assertEquals(ApiErrors.UNEXPECTED, repo.lastError)

        sellerId.value = null
        assertNull(repo.publish(draft()))
    }

    @Test
    fun editarReemplazaLaPublicacionYBorraFotosQuitadas() = runTest {
        val repo = repository().loaded(1L)
        coEvery { api.actualizar(1L, any()) } returns Response.success(productoDto(1L).copy(nombre = "Editado"))

        assertTrue(repo.update(1L, draft(listOf("/cache/nueva.jpg"))))

        assertEquals("Editado", repo.getMyProduct(1L)?.name)
        verify { photoStorage.deleteAll(listOf("/fotos/1.jpg")) } // la foto anterior ya no se usa

        coEvery { api.actualizar(1L, any()) } returns Response.error(404, jsonBody("{}"))
        assertFalse(repo.update(1L, draft()))
        assertEquals(ApiErrors.UNEXPECTED, repo.lastError)

        sellerId.value = null
        assertFalse(repo.update(1L, draft()))
    }

    @Test
    fun pausarYActivar() = runTest {
        val repo = repository().loaded(1L)
        coEvery { api.cambiarEstado(1L, any()) } returns Response.success(productoDto(1L, estado = "PAUSED"))

        assertTrue(repo.setStatus(1L, ProductStatus.PAUSED))
        assertEquals(ProductStatus.PAUSED, repo.getMyProduct(1L)?.status)
        coVerify { api.cambiarEstado(1L, match { it.estado == "PAUSED" }) }

        coEvery { api.cambiarEstado(1L, any()) } throws IOException()
        assertFalse(repo.setStatus(1L, ProductStatus.ACTIVE))
        assertEquals(ApiErrors.NETWORK, repo.lastError)
    }

    @Test
    fun eliminar() = runTest {
        val repo = repository().loaded(1L, 2L)

        coEvery { api.eliminar(1L) } returns Response.success(Unit)
        assertTrue(repo.delete(1L))
        verify { photoStorage.deleteAll(listOf("/fotos/1.jpg")) }

        coEvery { api.eliminar(2L) } returns Response.error(500, jsonBody("""{"mensaje":"Error"}"""))
        assertFalse(repo.delete(2L))
        assertEquals("Error", repo.lastError)

        coEvery { api.eliminar(2L) } throws IOException()
        assertFalse(repo.delete(2L))
        assertEquals(ApiErrors.NETWORK, repo.lastError)

        coEvery { api.eliminar(2L) } throws IllegalStateException()
        assertFalse(repo.delete(2L))
        assertEquals(ApiErrors.UNEXPECTED, repo.lastError)
    }
}
