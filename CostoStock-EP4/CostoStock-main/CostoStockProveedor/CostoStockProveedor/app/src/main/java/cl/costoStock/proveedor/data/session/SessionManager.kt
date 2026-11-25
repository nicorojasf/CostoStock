package cl.costoStock.proveedor.data.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda solo el id del proveedor con sesión activa (nunca la contraseña). */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentSellerId = MutableStateFlow(
        prefs.getLong(KEY_SELLER_ID, NO_SELLER).takeIf { it != NO_SELLER }
    )
    val currentSellerId: StateFlow<Long?> = _currentSellerId.asStateFlow()

    fun login(sellerId: Long) {
        prefs.edit().putLong(KEY_SELLER_ID, sellerId).apply()
        _currentSellerId.value = sellerId
    }

    fun logout() {
        prefs.edit().remove(KEY_SELLER_ID).apply()
        _currentSellerId.value = null
    }

    private companion object {
        const val PREFS_NAME = "costostock_proveedor_session"
        const val KEY_SELLER_ID = "seller_id"
        const val NO_SELLER = -1L
    }
}
