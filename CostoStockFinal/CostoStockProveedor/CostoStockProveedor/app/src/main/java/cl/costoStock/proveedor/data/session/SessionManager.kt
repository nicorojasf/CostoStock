package cl.costoStock.proveedor.data.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sesión del proveedor (SharedPreferences): el id y el token firmado que entrega ms-usuarios.
 * Nunca se guarda la contraseña. El token se adjunta a cada llamada en ApiClient.
 */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentSellerId = MutableStateFlow(
        prefs.getLong(KEY_SELLER_ID, NO_SELLER).takeIf { it != NO_SELLER }
    )
    val currentSellerId: StateFlow<Long?> = _currentSellerId.asStateFlow()

    @Volatile
    var token: String? = prefs.getString(KEY_TOKEN, null)
        private set

    fun login(sellerId: Long, token: String) {
        prefs.edit().putLong(KEY_SELLER_ID, sellerId).putString(KEY_TOKEN, token).apply()
        this.token = token
        _currentSellerId.value = sellerId
    }

    fun logout() {
        prefs.edit().remove(KEY_SELLER_ID).remove(KEY_TOKEN).apply()
        token = null
        _currentSellerId.value = null
    }

    private companion object {
        const val PREFS_NAME = "costostock_proveedor_session"
        const val KEY_SELLER_ID = "seller_id"
        const val KEY_TOKEN = "token"
        const val NO_SELLER = -1L
    }
}
