package cl.costoStock.myapplication.data.repository

import android.graphics.Bitmap
import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.local.UserDao
import cl.costoStock.myapplication.data.model.User
import cl.costoStock.myapplication.data.session.SessionManager
import cl.costoStock.myapplication.data.storage.PhotoStorage
import cl.costoStock.myapplication.security.PasswordHasher
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
}

class AuthRepository(
    private val userDao: UserDao,
    private val session: SessionManager,
    private val photoStorage: PhotoStorage,
    private val changes: DataChangeNotifier
) {
    suspend fun register(name: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val normalizedEmail = email.trim().lowercase()
            if (userDao.findCredentialsByEmail(normalizedEmail) != null) {
                return@withContext AuthResult.EmailTaken
            }
            val salt = PasswordHasher.newSalt()
            val hash = PasswordHasher.hash(password, salt)
            val id = userDao.insert(name.trim(), normalizedEmail, hash, salt)
            if (id == -1L) return@withContext AuthResult.EmailTaken
            session.login(id)
            AuthResult.Success
        }

    suspend fun login(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val credentials = userDao.findCredentialsByEmail(email.trim().lowercase())
            if (credentials == null) {
                // Hash de relleno para que el tiempo de respuesta no revele si el correo existe.
                PasswordHasher.hash(password, DUMMY_SALT)
                return@withContext AuthResult.InvalidCredentials
            }
            if (!PasswordHasher.verify(password, credentials.salt, credentials.passwordHash)) {
                return@withContext AuthResult.InvalidCredentials
            }
            session.login(credentials.user.id)
            AuthResult.Success
        }

    fun logout() = session.logout()

    fun observeCurrentUser(): Flow<User?> =
        combine(session.currentUserId, changes.version) { id, _ -> id }
            .map { id -> if (id == null) null else userDao.findById(id) }
            .flowOn(Dispatchers.IO)

    suspend fun updatePhoto(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val userId = session.currentUserId.value ?: return@withContext
        val previousPath = userDao.findById(userId)?.photoPath
        val newPath = photoStorage.save(userId, bitmap)
        userDao.updatePhoto(userId, newPath)
        photoStorage.delete(previousPath)
    }

    private companion object {
        const val DUMMY_SALT = "00112233445566778899aabbccddeeff"
    }
}
