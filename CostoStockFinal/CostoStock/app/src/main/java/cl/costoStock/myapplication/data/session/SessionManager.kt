package cl.costoStock.myapplication.data.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sesión del usuario (SharedPreferences): el id y el token firmado que entrega ms-usuarios.
 * Nunca se guarda la contraseña. El token se adjunta a cada llamada en ApiClient.
 */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(
        prefs.getLong(KEY_USER_ID, NO_USER).takeIf { it != NO_USER }
    )
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    @Volatile
    var token: String? = prefs.getString(KEY_TOKEN, null)
        private set

    fun login(userId: Long, token: String) {
        prefs.edit().putLong(KEY_USER_ID, userId).putString(KEY_TOKEN, token).apply()
        this.token = token
        _currentUserId.value = userId
    }

    fun logout() {
        prefs.edit().remove(KEY_USER_ID).remove(KEY_TOKEN).apply()
        token = null
        _currentUserId.value = null
    }

    private companion object {
        const val PREFS_NAME = "costostock_session"
        const val KEY_USER_ID = "user_id"
        const val KEY_TOKEN = "token"
        const val NO_USER = -1L
    }
}
