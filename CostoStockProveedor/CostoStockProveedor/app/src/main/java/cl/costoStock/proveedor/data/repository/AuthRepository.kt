package cl.costoStock.proveedor.data.repository

import cl.costoStock.proveedor.data.DataChangeNotifier
import cl.costoStock.proveedor.data.local.SellerDao
import cl.costoStock.proveedor.data.model.Seller
import cl.costoStock.proveedor.data.session.SessionManager
import cl.costoStock.proveedor.data.storage.PhotoStorage
import cl.costoStock.proveedor.domain.validation.Validators
import cl.costoStock.proveedor.security.PasswordHasher
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
    data object RutTaken : AuthResult
}

class AuthRepository(
    private val sellerDao: SellerDao,
    private val session: SessionManager,
    private val photoStorage: PhotoStorage,
    private val changes: DataChangeNotifier
) {
    suspend fun register(
        name: String,
        rut: String,
        email: String,
        phone: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val normalizedRut = Validators.normalizeRut(rut)
        if (sellerDao.findCredentialsByEmail(normalizedEmail) != null) {
            return@withContext AuthResult.EmailTaken
        }
        if (sellerDao.existsByRut(normalizedRut)) return@withContext AuthResult.RutTaken

        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash(password, salt)
        val id = sellerDao.insert(
            name = name.trim(),
            rut = normalizedRut,
            email = normalizedEmail,
            phone = Validators.normalizePhone(phone),
            passwordHash = hash,
            salt = salt
        )
        if (id == -1L) return@withContext AuthResult.EmailTaken
        session.login(id)
        AuthResult.Success
    }

    suspend fun login(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val credentials = sellerDao.findCredentialsByEmail(email.trim().lowercase())
            if (credentials == null) {
                // Hash de relleno para que el tiempo de respuesta no revele si el correo existe.
                PasswordHasher.hash(password, DUMMY_SALT)
                return@withContext AuthResult.InvalidCredentials
            }
            if (!PasswordHasher.verify(password, credentials.salt, credentials.passwordHash)) {
                return@withContext AuthResult.InvalidCredentials
            }
            session.login(credentials.seller.id)
            AuthResult.Success
        }

    fun logout() = session.logout()

    fun observeCurrentSeller(): Flow<Seller?> =
        combine(session.currentSellerId, changes.version) { id, _ -> id }
            .map { id -> if (id == null) null else sellerDao.findById(id) }
            .flowOn(Dispatchers.IO)

    /** [tempPath] es la foto recién tomada (en la caché); se copia al almacenamiento privado. */
    suspend fun updatePhoto(tempPath: String) = withContext(Dispatchers.IO) {
        val sellerId = session.currentSellerId.value ?: return@withContext
        val previousPath = sellerDao.findById(sellerId)?.photoPath
        val newPath = photoStorage.persistAvatar(sellerId, tempPath)
        sellerDao.updatePhoto(sellerId, newPath)
        photoStorage.delete(previousPath)
    }

    suspend fun updateProfile(name: String, phone: String) = withContext(Dispatchers.IO) {
        val sellerId = session.currentSellerId.value ?: return@withContext
        sellerDao.updateProfile(sellerId, name.trim(), Validators.normalizePhone(phone))
    }

    private companion object {
        const val DUMMY_SALT = "00112233445566778899aabbccddeeff"
    }
}
