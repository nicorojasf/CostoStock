package cl.costoStock.myapplication.ui.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.myapplication.data.model.User
import cl.costoStock.myapplication.data.repository.AuthRepository
import cl.costoStock.myapplication.data.repository.CartRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val cartItems: Int = 0
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    cartRepository: CartRepository
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val state: StateFlow<ProfileUiState> = combine(
        authRepository.observeCurrentUser(), cartRepository.observeCart()
    ) { user, lines ->
        ProfileUiState(user = user, cartItems = lines.sumOf { it.quantity })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onPhotoCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            authRepository.updatePhoto(bitmap)
            _messages.tryEmit("Foto de perfil actualizada")
        }
    }

    fun logout() = authRepository.logout()
}
