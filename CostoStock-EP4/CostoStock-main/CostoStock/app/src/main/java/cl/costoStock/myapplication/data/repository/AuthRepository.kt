package cl.costoStock.myapplication.data.repository

import android.graphics.Bitmap
import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.local.UserDao
import cl.costoStock.myapplication.data.model.User
import cl.costoStock.myapplication.data.remote.ApiErrors
import cl.costoStock.myapplication.data.remote.UsuariosApi
import cl.costoStock.myapplication.data.remote.dto.ClienteDto
import cl.costoStock.myapplication.data.remote.dto.ClienteRegistroRequest
import cl.costoStock.myapplication.data.remote.dto.ClienteUpdateRequest
import cl.costoStock.myapplication.data.remote.dto.LoginRequest
import cl.costoStock.myapplication.data.remote.toUser
import cl.costoStock.myapplication.data.session.SessionManager
import cl.costoStock.myapplication.data.storage.PhotoStorage
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface AuthResult {
    data object Success : AuthResult
    data object InvalidCredentials : AuthResult
    data object EmailTaken : AuthResult
    /** Sin conexión o error del servidor; [message] se muestra tal cual al usuario. */
    data class Error(val message: String) : AuthResult
}

/**
 * Registro, login y perfil del cliente. Las cuentas viven en el microservicio ms-usuarios
 * (que guarda la contraseña con BCrypt); en el teléfono solo queda el id de sesión y una copia
 * local de nombre/correo/foto.
 */
class AuthRepository(
    private val api: UsuariosApi,
    private val userDao: UserDao,
    private val session: SessionManager,
    private val photoStorage: PhotoStorage,
    private val changes: DataChangeNotifier,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun register(name: String, email: String, password: String): AuthResult =
        withContext(io) {
            call {
                val response = api.registrar(
                    ClienteRegistroRequest(name.trim(), email.trim().lowercase(), password)
                )
                when {
                    response.isSuccessful -> startSession(response.body())
                    response.code() == 409 -> AuthResult.EmailTaken
                    else -> AuthResult.Error(
                        ApiErrors.parse(response.errorBody())?.mensaje ?: ApiErrors.UNEXPECTED
                    )
                }
            }
        }

    suspend fun login(email: String, password: String): AuthResult =
        withContext(io) {
            call {
                val response = api.login(LoginRequest(email.trim().lowercase(), password))
                when {
                    response.isSuccessful -> startSession(response.body())
                    response.code() == 401 -> AuthResult.InvalidCredentials
                    else -> AuthResult.Error(
                        ApiErrors.parse(response.errorBody())?.mensaje ?: ApiErrors.UNEXPECTED
                    )
                }
            }
        }

    fun logout() = session.logout()

    /** Si la sesión guardada apunta a un usuario que ya no está en la base local, se cierra. */
    fun ensureValidSession() {
        val id = session.currentUserId.value ?: return
        if (userDao.findById(id) == null) session.logout()
    }

    fun observeCurrentUser(): Flow<User?> =
        combine(session.currentUserId, changes.version) { id, _ -> id }
            .map { id -> if (id == null) null else userDao.findById(id) }
            .flowOn(io)

    suspend fun updatePhoto(bitmap: Bitmap) = withContext(io) {
        val userId = session.currentUserId.value ?: return@withContext
        val previousPath = userDao.findById(userId)?.photoPath
        val newPath = photoStorage.save(userId, bitmap)
        userDao.updatePhoto(userId, newPath)
        photoStorage.delete(previousPath)
    }

    /** Actualiza el nombre en ms-usuarios (PUT). Devuelve null si salió bien o el mensaje de error. */
    suspend fun updateName(name: String): String? = withContext(io) {
        val userId = session.currentUserId.value ?: return@withContext ApiErrors.UNEXPECTED
        try {
            val response = api.actualizar(userId, ClienteUpdateRequest(name.trim()))
            val user = response.body()?.toUser()
            if (response.isSuccessful && user != null) {
                userDao.upsert(user.id, user.name, user.email)
                null
            } else {
                ApiErrors.parse(response.errorBody())?.mensaje ?: ApiErrors.UNEXPECTED
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ApiErrors.NETWORK
        } catch (e: Exception) {
            ApiErrors.UNEXPECTED
        }
    }

    /** Elimina la cuenta en ms-usuarios (DELETE) y los datos locales. Devuelve null si salió bien. */
    suspend fun deleteAccount(): String? = withContext(io) {
        val userId = session.currentUserId.value ?: return@withContext ApiErrors.UNEXPECTED
        try {
            val response = api.eliminar(userId)
            if (response.isSuccessful || response.code() == 404) {
                photoStorage.delete(userDao.findById(userId)?.photoPath)
                userDao.delete(userId) // el carrito se borra en cascada
                session.logout()
                null
            } else {
                ApiErrors.parse(response.errorBody())?.mensaje ?: ApiErrors.UNEXPECTED
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ApiErrors.NETWORK
        } catch (e: Exception) {
            ApiErrors.UNEXPECTED
        }
    }

    private fun startSession(dto: ClienteDto?): AuthResult {
        val user = dto?.toUser() ?: return AuthResult.Error(ApiErrors.UNEXPECTED)
        userDao.upsert(user.id, user.name, user.email)
        session.login(user.id)
        return AuthResult.Success
    }

    private inline fun call(block: () -> AuthResult): AuthResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AuthResult.Error(ApiErrors.NETWORK)
    } catch (e: Exception) {
        AuthResult.Error(ApiErrors.UNEXPECTED)
    }
}
