package cl.costoStock.myapplication.data.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda solo el id del usuario con sesión activa (nunca la contraseña). */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(
        prefs.getLong(KEY_USER_ID, NO_USER).takeIf { it != NO_USER }
    )
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    fun login(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
        _currentUserId.value = userId
    }

    fun logout() {
        prefs.edit().remove(KEY_USER_ID).apply()
        _currentUserId.value = null
    }

    private companion object {
        const val PREFS_NAME = "costostock_session"
        const val KEY_USER_ID = "user_id"
        const val NO_USER = -1L
    }
}
