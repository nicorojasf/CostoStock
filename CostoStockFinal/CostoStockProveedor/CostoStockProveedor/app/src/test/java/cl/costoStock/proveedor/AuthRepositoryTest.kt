package cl.costoStock.proveedor

import cl.costoStock.proveedor.data.DataChangeNotifier
import cl.costoStock.proveedor.data.local.SellerDao
import cl.costoStock.proveedor.data.model.Seller
import cl.costoStock.proveedor.data.remote.ApiErrors
import cl.costoStock.proveedor.data.remote.UsuariosApi
import cl.costoStock.proveedor.data.remote.dto.MensajeDto
import cl.costoStock.proveedor.data.remote.dto.ProveedorDto
import cl.costoStock.proveedor.data.repository.AuthRepository
import cl.costoStock.proveedor.data.repository.AuthResult
import cl.costoStock.proveedor.data.session.SessionManager
import cl.costoStock.proveedor.data.storage.PhotoStorage
import cl.costoStock.proveedor.testutil.jsonBody
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private val api = mockk<UsuariosApi>()
    private val sellerDao = mockk<SellerDao>(relaxed = true)
    private val session = mockk<SessionManager>(relaxed = true)
    private val photoStorage = mockk<PhotoStorage>(relaxed = true)
    private val sellerId = MutableStateFlow<Long?>(null)
    private lateinit var repository: AuthRepository

    private val dto = ProveedorDto(4L, "Tienda Ana", "12345678-5", "tienda@mail.cl", "+56912345678", 1L, token = "tok", rol = "PROVEEDOR")
    private val seller = Seller(4L, "Tienda Ana", "12345678-5", "tienda@mail.cl", "+56912345678", "/foto.jpg", 1L)

    @Before
    fun setUp() {
        every { session.currentSellerId } returns sellerId
        repository = AuthRepository(api, sellerDao, session, photoStorage, DataChangeNotifier(), Dispatchers.Unconfined)
    }

    @Test
    fun registroNormalizaRutTelefonoYCorreo() = runTest {
        coEvery { api.registrar(any()) } returns Response.success(dto)

        val result = repository.register(" Tienda Ana ", "12.345.678-5", " TIENDA@mail.cl ", "9 1234 5678", "Clave123")

        assertEquals(AuthResult.Success, result)
        coVerify {
            api.registrar(match {
                it.rut == "12345678-5" && it.telefono == "+56912345678" && it.email == "tienda@mail.cl" && it.nombre == "Tienda Ana"
            })
        }
        verify { sellerDao.upsert(match { it.id == 4L }) }
        verify { session.login(4L, "tok") }
    }

    @Test
    fun registroDuplicadoDistingueCorreoYRut() = runTest {
        coEvery { api.registrar(any()) } returns Response.error(409, jsonBody("""{"mensaje":"Este RUT ya está registrado"}"""))
        assertEquals(AuthResult.RutTaken, repository.register("T", "12345678-5", "t@mail.cl", "912345678", "Clave123"))

        coEvery { api.registrar(any()) } returns Response.error(409, jsonBody("""{"mensaje":"Este correo ya está registrado"}"""))
        assertEquals(AuthResult.EmailTaken, repository.register("T", "12345678-5", "t@mail.cl", "912345678", "Clave123"))

        coEvery { api.registrar(any()) } returns Response.error(400, jsonBody("""{"mensaje":"RUT inválido"}"""))
        assertEquals(AuthResult.Error("RUT inválido"), repository.register("T", "1", "t@mail.cl", "9", "Clave123"))

        coEvery { api.registrar(any()) } throws IOException()
        assertEquals(AuthResult.Error(ApiErrors.NETWORK), repository.register("T", "1", "t@mail.cl", "9", "Clave123"))
    }

    @Test
    fun loginEnTodosLosCasos() = runTest {
        coEvery { api.login(any()) } returns Response.success(dto)
        assertEquals(AuthResult.Success, repository.login("tienda@mail.cl", "Clave123"))

        coEvery { api.login(any()) } returns Response.error(401, jsonBody("{}"))
        assertEquals(AuthResult.InvalidCredentials, repository.login("tienda@mail.cl", "x"))

        coEvery { api.login(any()) } returns Response.error(500, jsonBody(""))
        assertEquals(AuthResult.Error(ApiErrors.UNEXPECTED), repository.login("tienda@mail.cl", "x"))

        coEvery { api.login(any()) } returns Response.success(null)
        assertEquals(AuthResult.Error(ApiErrors.UNEXPECTED), repository.login("tienda@mail.cl", "x"))

        coEvery { api.login(any()) } throws IllegalStateException()
        assertEquals(AuthResult.Error(ApiErrors.UNEXPECTED), repository.login("tienda@mail.cl", "x"))
    }

    @Test
    fun sesionYProveedorActual() = runTest {
        assertNull(repository.observeCurrentSeller().first())
        repository.ensureValidSession()
        verify(exactly = 0) { session.logout() }

        sellerId.value = 4L
        every { sellerDao.findById(4L) } returns seller
        assertEquals(seller, repository.observeCurrentSeller().first())
        repository.ensureValidSession()
        verify(exactly = 0) { session.logout() }

        every { session.token } returns null
        repository.ensureValidSession() // sin token (versión anterior): se cierra
        verify(exactly = 1) { session.logout() }

        every { sellerDao.findById(4L) } returns null
        every { session.token } returns "tok"
        repository.ensureValidSession()
        repository.logout()
        verify(exactly = 3) { session.logout() }
    }

    @Test
    fun recuperarContrasenaYRespuestaSinToken() = runTest {
        coEvery { api.solicitarCodigo(any()) } returns Response.success(MensajeDto("ok"))
        assertNull(repository.requestPasswordReset("TIENDA@mail.cl"))
        coVerify { api.solicitarCodigo(match { it.email == "tienda@mail.cl" && it.tipo == "PROVEEDOR" }) }

        coEvery { api.restablecer(any()) } returns Response.error(400, jsonBody("""{"mensaje":"Código incorrecto"}"""))
        assertEquals("Código incorrecto", repository.resetPassword("tienda@mail.cl", "000000", "Nueva1234"))
        coEvery { api.restablecer(any()) } returns Response.success(MensajeDto("ok"))
        assertNull(repository.resetPassword("tienda@mail.cl", "123456", "Nueva1234"))
        coEvery { api.restablecer(any()) } throws IOException()
        assertEquals(ApiErrors.NETWORK, repository.resetPassword("tienda@mail.cl", "123456", "Nueva1234"))
        coEvery { api.restablecer(any()) } throws IllegalStateException()
        assertEquals(ApiErrors.UNEXPECTED, repository.resetPassword("tienda@mail.cl", "123456", "Nueva1234"))
        coEvery { api.restablecer(any()) } returns Response.error(500, jsonBody(""))
        assertEquals(ApiErrors.UNEXPECTED, repository.resetPassword("tienda@mail.cl", "123456", "Nueva1234"))

        coEvery { api.login(any()) } returns Response.success(dto.copy(token = null))
        assertEquals(AuthResult.Error(ApiErrors.UNEXPECTED), repository.login("tienda@mail.cl", "Clave123"))
    }

    @Test
    fun actualizarFoto() = runTest {
        repository.updatePhoto("/cache/tmp.jpg")
        verify(exactly = 0) { photoStorage.persistAvatar(any(), any()) }

        sellerId.value = 4L
        every { sellerDao.findById(4L) } returns seller
        every { photoStorage.persistAvatar(4L, "/cache/tmp.jpg") } returns "/avatars/nueva.jpg"
        repository.updatePhoto("/cache/tmp.jpg")

        verify { sellerDao.updatePhoto(4L, "/avatars/nueva.jpg") }
        verify { photoStorage.delete("/foto.jpg") }
    }

    @Test
    fun actualizarPerfilEnElMicroservicio() = runTest {
        assertEquals(ApiErrors.UNEXPECTED, repository.updateProfile("Tienda", "912345678"))

        sellerId.value = 4L
        coEvery { api.actualizar(4L, any()) } returns Response.success(dto.copy(nombre = "Tienda Nueva"))
        assertNull(repository.updateProfile("Tienda Nueva", "9 1234 5678"))
        coVerify { api.actualizar(4L, match { it.telefono == "+56912345678" }) }
        verify { sellerDao.upsert(match { it.name == "Tienda Nueva" }) }

        coEvery { api.actualizar(4L, any()) } returns Response.error(400, jsonBody("""{"mensaje":"Teléfono inválido"}"""))
        assertEquals("Teléfono inválido", repository.updateProfile("Tienda", "1"))

        coEvery { api.actualizar(4L, any()) } throws IOException()
        assertEquals(ApiErrors.NETWORK, repository.updateProfile("Tienda", "912345678"))

        coEvery { api.actualizar(4L, any()) } throws RuntimeException()
        assertEquals(ApiErrors.UNEXPECTED, repository.updateProfile("Tienda", "912345678"))
    }
}
