package cl.costoStock.myapplication.data

import android.graphics.Bitmap
import cl.costoStock.myapplication.data.local.UserDao
import cl.costoStock.myapplication.data.model.User
import cl.costoStock.myapplication.data.remote.ApiErrors
import cl.costoStock.myapplication.data.remote.UsuariosApi
import cl.costoStock.myapplication.data.remote.dto.ClienteDto
import cl.costoStock.myapplication.data.repository.AuthRepository
import cl.costoStock.myapplication.data.repository.AuthResult
import cl.costoStock.myapplication.data.session.SessionManager
import cl.costoStock.myapplication.data.storage.PhotoStorage
import cl.costoStock.myapplication.testutil.jsonBody
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private val api = mockk<UsuariosApi>()
    private val userDao = mockk<UserDao>(relaxed = true)
    private val session = mockk<SessionManager>(relaxed = true)
    private val photoStorage = mockk<PhotoStorage>(relaxed = true)
    private val userId = MutableStateFlow<Long?>(null)
    private lateinit var repository: AuthRepository

    private val dto = ClienteDto(5L, "Ana", "ana@mail.cl", 0L)

    @Before
    fun setUp() {
        every { session.currentUserId } returns userId
        repository = AuthRepository(api, userDao, session, photoStorage, DataChangeNotifier(), UnconfinedTestDispatcher())
    }

    @Test
    fun registroExitosoGuardaCopiaLocalEIniciaSesion() = runTest {
        coEvery { api.registrar(any()) } returns Response.success(dto)

        assertEquals(AuthResult.Success, repository.register(" Ana ", " ANA@mail.cl ", "Clave123"))

        coVerify { api.registrar(match { it.email == "ana@mail.cl" && it.nombre == "Ana" }) }
        verify { userDao.upsert(5L, "Ana", "ana@mail.cl") }
        verify { session.login(5L) }
    }

    @Test
    fun registroConCorreoDuplicadoDevuelveEmailTaken() = runTest {
        coEvery { api.registrar(any()) } returns Response.error(409, jsonBody("""{"mensaje":"duplicado"}"""))
        assertEquals(AuthResult.EmailTaken, repository.register("Ana", "ana@mail.cl", "Clave123"))
    }

    @Test
    fun registroConErrorDelServidorMuestraSuMensaje() = runTest {
        coEvery { api.registrar(any()) } returns Response.error(400, jsonBody("""{"mensaje":"Datos inválidos"}"""))
        assertEquals(AuthResult.Error("Datos inválidos"), repository.register("Ana", "ana@mail.cl", "Clave123"))
    }

    @Test
    fun registroSinCuerpoEsErrorInesperado() = runTest {
        coEvery { api.registrar(any()) } returns Response.success(null)
        assertEquals(AuthResult.Error(ApiErrors.UNEXPECTED), repository.register("Ana", "ana@mail.cl", "Clave123"))
    }

    @Test
    fun loginCorrectoIncorrectoYSinConexion() = runTest {
        coEvery { api.login(any()) } returns Response.success(dto)
        assertEquals(AuthResult.Success, repository.login("ana@mail.cl", "Clave123"))

        coEvery { api.login(any()) } returns Response.error(401, jsonBody("{}"))
        assertEquals(AuthResult.InvalidCredentials, repository.login("ana@mail.cl", "mala"))

        coEvery { api.login(any()) } returns Response.error(500, jsonBody(""))
        assertEquals(AuthResult.Error(ApiErrors.UNEXPECTED), repository.login("ana@mail.cl", "mala"))

        coEvery { api.login(any()) } throws IOException("sin red")
        assertEquals(AuthResult.Error(ApiErrors.NETWORK), repository.login("ana@mail.cl", "Clave123"))

        coEvery { api.login(any()) } throws IllegalStateException("json")
        assertEquals(AuthResult.Error(ApiErrors.UNEXPECTED), repository.login("ana@mail.cl", "Clave123"))
    }

    @Test
    fun observaElUsuarioConSesion() = runTest {
        val user = User(5L, "Ana", "ana@mail.cl", null)
        every { userDao.findById(5L) } returns user

        assertNull(repository.observeCurrentUser().first())
        userId.value = 5L
        assertEquals(user, repository.observeCurrentUser().first())
    }

    @Test
    fun sesionInvalidaSeCierraYLogout() {
        repository.ensureValidSession()
        verify(exactly = 0) { session.logout() }

        userId.value = 5L
        every { userDao.findById(5L) } returns null
        repository.ensureValidSession()
        verify(exactly = 1) { session.logout() }

        repository.logout()
        verify(exactly = 2) { session.logout() }
    }

    @Test
    fun actualizarFotoReemplazaLaAnterior() = runTest {
        val bitmap = mockk<Bitmap>()
        repository.updatePhoto(bitmap)
        verify(exactly = 0) { photoStorage.save(any(), any()) }

        userId.value = 5L
        every { userDao.findById(5L) } returns User(5L, "Ana", "ana@mail.cl", "/vieja.jpg")
        every { photoStorage.save(5L, bitmap) } returns "/nueva.jpg"
        repository.updatePhoto(bitmap)

        verify { userDao.updatePhoto(5L, "/nueva.jpg") }
        verify { photoStorage.delete("/vieja.jpg") }
    }

    @Test
    fun actualizarNombreEnElMicroservicio() = runTest {
        assertEquals(ApiErrors.UNEXPECTED, repository.updateName("Ana"))

        userId.value = 5L
        coEvery { api.actualizar(5L, any()) } returns Response.success(dto.copy(nombre = "Ana María"))
        assertNull(repository.updateName("Ana María"))
        verify { userDao.upsert(5L, "Ana María", "ana@mail.cl") }

        coEvery { api.actualizar(5L, any()) } returns Response.error(400, jsonBody("""{"mensaje":"Nombre inválido"}"""))
        assertEquals("Nombre inválido", repository.updateName("A"))

        coEvery { api.actualizar(5L, any()) } throws IOException()
        assertEquals(ApiErrors.NETWORK, repository.updateName("Ana"))

        coEvery { api.actualizar(5L, any()) } throws RuntimeException()
        assertEquals(ApiErrors.UNEXPECTED, repository.updateName("Ana"))
    }

    @Test
    fun eliminarCuentaBorraDatosLocales() = runTest {
        assertEquals(ApiErrors.UNEXPECTED, repository.deleteAccount())

        userId.value = 5L
        every { userDao.findById(5L) } returns User(5L, "Ana", "ana@mail.cl", "/foto.jpg")
        coEvery { api.eliminar(5L) } returns Response.success(Unit)
        assertNull(repository.deleteAccount())
        verify { photoStorage.delete("/foto.jpg") }
        verify { userDao.delete(5L) }
        verify { session.logout() }

        coEvery { api.eliminar(5L) } returns Response.error(500, jsonBody("""{"mensaje":"Error"}"""))
        assertEquals("Error", repository.deleteAccount())

        coEvery { api.eliminar(5L) } throws IOException()
        assertEquals(ApiErrors.NETWORK, repository.deleteAccount())

        coEvery { api.eliminar(5L) } throws RuntimeException()
        assertEquals(ApiErrors.UNEXPECTED, repository.deleteAccount())
    }
}
