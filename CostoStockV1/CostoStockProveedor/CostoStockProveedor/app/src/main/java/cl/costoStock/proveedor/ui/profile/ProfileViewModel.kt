package cl.costoStock.proveedor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.model.Seller
import cl.costoStock.proveedor.data.repository.AuthRepository
import cl.costoStock.proveedor.data.repository.ProductRepository
import cl.costoStock.proveedor.domain.validation.Validators
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
    val seller: Seller? = null,
    val totalProducts: Int = 0,
    val activeProducts: Int = 0,
    val isLoading: Boolean = true
)

/** Estado del formulario "Editar datos" (se muestra en un diálogo). */
data class EditProfileState(
    val visible: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val isSaving: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    productRepository: ProductRepository
) : ViewModel() {

    val state: StateFlow<ProfileUiState> = combine(
        authRepository.observeCurrentSeller(),
        productRepository.observeMyProducts()
    ) { seller, products ->
        ProfileUiState(
            seller = seller,
            totalProducts = products.size,
            activeProducts = products.count { it.status == ProductStatus.ACTIVE && !it.isOutOfStock },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _edit = MutableStateFlow(EditProfileState())
    val edit: StateFlow<EditProfileState> = _edit.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** [path] es la foto recién tomada (archivo temporal). */
    fun onPhotoCaptured(path: String) {
        viewModelScope.launch {
            authRepository.updatePhoto(path)
            _messages.tryEmit("Foto de perfil actualizada")
        }
    }

    fun openEdit() {
        val seller = state.value.seller ?: return
        _edit.value = EditProfileState(
            visible = true,
            name = seller.name,
            phone = Validators.formatPhone(seller.phone)
        )
    }

    fun closeEdit() = _edit.update { it.copy(visible = false) }

    fun onEditNameChange(value: String) = _edit.update {
        it.copy(name = value, nameError = if (it.nameError != null) Validators.validateName(value) else null)
    }

    fun onEditPhoneChange(value: String) = _edit.update {
        it.copy(phone = value, phoneError = if (it.phoneError != null) Validators.validatePhone(value) else null)
    }

    fun onEditNameBlur() = _edit.update { it.copy(nameError = Validators.validateName(it.name)) }
    fun onEditPhoneBlur() = _edit.update { it.copy(phoneError = Validators.validatePhone(it.phone)) }

    fun saveEdit() {
        val e = _edit.value
        if (e.isSaving) return
        val nameError = Validators.validateName(e.name)
        val phoneError = Validators.validatePhone(e.phone)
        if (nameError != null || phoneError != null) {
            _edit.update { it.copy(nameError = nameError, phoneError = phoneError) }
            return
        }
        _edit.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            authRepository.updateProfile(e.name, e.phone)
            _edit.value = EditProfileState()
            _messages.tryEmit("Datos actualizados")
        }
    }

    fun logout() = authRepository.logout()
}
