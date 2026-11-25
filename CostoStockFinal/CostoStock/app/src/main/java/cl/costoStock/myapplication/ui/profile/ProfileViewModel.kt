package cl.costoStock.myapplication.ui.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.myapplication.data.model.User
import cl.costoStock.myapplication.data.repository.AuthRepository
import cl.costoStock.myapplication.data.repository.CartRepository
import cl.costoStock.myapplication.domain.validation.Validators
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val cartItems: Int = 0
)

/** Diálogo "Editar nombre" (actualiza el cliente en ms-usuarios). */
data class EditNameState(
    val visible: Boolean = false,
    val name: String = "",
    val error: String? = null,
    val isSaving: Boolean = false
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

    private val _edit = MutableStateFlow(EditNameState())
    val edit: StateFlow<EditNameState> = _edit.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    /** Se pone en true cuando la cuenta se eliminó, para volver al login. */
    private val _accountDeleted = MutableStateFlow(false)
    val accountDeleted: StateFlow<Boolean> = _accountDeleted.asStateFlow()

    fun onPhotoCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            authRepository.updatePhoto(bitmap)
            _messages.tryEmit("Foto de perfil actualizada")
        }
    }

    fun openEdit() {
        _edit.value = EditNameState(visible = true, name = state.value.user?.name.orEmpty())
    }

    fun closeEdit() = _edit.update { it.copy(visible = false) }

    fun onEditNameChange(value: String) = _edit.update {
        it.copy(name = value, error = if (it.error != null) Validators.validateName(value) else null)
    }

    fun saveEdit() {
        val e = _edit.value
        if (e.isSaving) return
        val error = Validators.validateName(e.name)
        if (error != null) {
            _edit.update { it.copy(error = error) }
            return
        }
        _edit.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val failure = authRepository.updateName(e.name)
            if (failure == null) {
                _edit.value = EditNameState()
                _messages.tryEmit("Nombre actualizado")
            } else {
                _edit.update { it.copy(isSaving = false, error = failure) }
            }
        }
    }

    fun deleteAccount() {
        if (_isDeleting.value) return
        _isDeleting.value = true
        viewModelScope.launch {
            val failure = authRepository.deleteAccount()
            _isDeleting.value = false
            if (failure == null) _accountDeleted.value = true else _messages.tryEmit(failure)
        }
    }

    fun logout() = authRepository.logout()
}
