package cl.costoStock.proveedor.data.repository

import cl.costoStock.proveedor.data.DataChangeNotifier
import cl.costoStock.proveedor.data.local.SellerDao
import cl.costoStock.proveedor.data.model.Seller
import cl.costoStock.proveedor.data.remote.ApiErrors
import cl.costoStock.proveedor.data.remote.UsuariosApi
import cl.costoStock.proveedor.data.remote.dto.LoginRequest
import cl.costoStock.proveedor.data.remote.dto.MensajeDto
import cl.costoStock.proveedor.data.remote.dto.RestablecerRequest
import cl.costoStock.proveedor.data.remote.dto.SolicitarCodigoRequest
import cl.costoStock.proveedor.data.remote.dto.ProveedorDto
import cl.costoStock.proveedor.data.remote.dto.ProveedorRegistroRequest
import cl.costoStock.proveedor.data.remote.dto.ProveedorUpdateRequest
import cl.costoStock.proveedor.data.remote.toSeller
import cl.costoStock.proveedor.data.session.SessionManager
import cl.costoStock.proveedor.data.storage.PhotoStorage
import cl.costoStock.proveedor.domain.validation.Validators
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed interface AuthResult {
    data object Success : AuthResult
    data object InvalidCredentials : AuthResult
    data object EmailTaken : AuthResult
    data object RutTaken : AuthResult
    /** Sin conexión o error del servidor; [message] se muestra tal cual al usuario. */
    data class Error(val message: String) : AuthResult
}

/**
 * Registro, login y perfil del proveedor contra el microservicio ms-usuarios (la contraseña se
 * guarda allí con BCrypt). En el teléfono queda el id de sesión y una copia local de sus datos.
 */
class AuthRepository(
    private val api: UsuariosApi,
    private val sellerDao: SellerDao,
    private val session: SessionManager,
    private val photoStorage: PhotoStorage,
    private val changes: DataChangeNotifier,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun register(
        name: String,
        rut: String,
        email: String,
        phone: String,
        password: String
    ): AuthResult = withContext(io) {
        call {
            val response = api.registrar(
                ProveedorRegistroRequest(
                    nombre = name.trim(),
                    rut = Validators.normalizeRut(rut),
                    email = email.trim().lowercase(),
                    telefono = Validators.normalizePhone(phone),
                    password = password
                )
            )
            when {
                response.isSuccessful -> startSession(response.body())
                response.code() == 409 -> {
                    val message = ApiErrors.parse(response.errorBody())?.mensaje.orEmpty()
                    if (message.contains("RUT", ignoreCase = true)) AuthResult.RutTaken else AuthResult.EmailTaken
                }
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

    /** Cierra la sesión guardada si falta el proveedor local o el token (versiones anteriores). */
    fun ensureValidSession() {
        val id = session.currentSellerId.value ?: return
        if (sellerDao.findById(id) == null || session.token == null) session.logout()
    }

    /** Recuperar contraseña, paso 1. Devuelve null si se envió el código o el mensaje de error. */
    suspend fun requestPasswordReset(email: String): String? = withContext(io) {
        message { api.solicitarCodigo(SolicitarCodigoRequest(email.trim().lowercase(), ACCOUNT_TYPE)) }
    }

    /** Recuperar contraseña, paso 2. Devuelve null si se cambió o el mensaje de error. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): String? = withContext(io) {
        message {
            api.restablecer(RestablecerRequest(email.trim().lowercase(), ACCOUNT_TYPE, code.trim(), newPassword))
        }
    }

    fun observeCurrentSeller(): Flow<Seller?> =
        combine(session.currentSellerId, changes.version) { id, _ -> id }
            .map { id -> if (id == null) null else sellerDao.findById(id) }
            .flowOn(io)

    /** [tempPath] es la foto recién tomada (en la caché); se copia al almacenamiento privado. */
    suspend fun updatePhoto(tempPath: String) = withContext(io) {
        val sellerId = session.currentSellerId.value ?: return@withContext
        val previousPath = sellerDao.findById(sellerId)?.photoPath
        val newPath = photoStorage.persistAvatar(sellerId, tempPath)
        sellerDao.updatePhoto(sellerId, newPath)
        photoStorage.delete(previousPath)
    }

    /** Actualiza nombre y teléfono en ms-usuarios (PUT). Devuelve null si salió bien o el error. */
    suspend fun updateProfile(name: String, phone: String): String? = withContext(io) {
        val sellerId = session.currentSellerId.value ?: return@withContext ApiErrors.UNEXPECTED
        try {
            val response = api.actualizar(
                sellerId, ProveedorUpdateRequest(name.trim(), Validators.normalizePhone(phone))
            )
            val seller = response.body()?.toSeller()
            if (response.isSuccessful && seller != null) {
                sellerDao.upsert(seller)
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

    private fun startSession(dto: ProveedorDto?): AuthResult {
        val seller = dto?.toSeller() ?: return AuthResult.Error(ApiErrors.UNEXPECTED)
        val token = dto?.token ?: return AuthResult.Error(ApiErrors.UNEXPECTED)
        sellerDao.upsert(seller)
        session.login(seller.id, token)
        return AuthResult.Success
    }

    /** Ejecuta una llamada y devuelve null si fue exitosa, o el mensaje de error para mostrar. */
    private inline fun message(block: () -> Response<MensajeDto>): String? = try {
        val response = block()
        if (response.isSuccessful) null
        else ApiErrors.parse(response.errorBody())?.mensaje ?: ApiErrors.UNEXPECTED
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ApiErrors.NETWORK
    } catch (e: Exception) {
        ApiErrors.UNEXPECTED
    }

    private companion object {
        const val ACCOUNT_TYPE = "PROVEEDOR"
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
