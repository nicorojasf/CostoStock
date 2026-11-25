package cl.costoStock.proveedor.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.proveedor.data.repository.AuthRepository
import cl.costoStock.proveedor.data.repository.AuthResult
import cl.costoStock.proveedor.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val rut: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false,
    val nameError: String? = null,
    val rutError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val termsError: String? = null,
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val formError: String? = null,
    /** Se incrementa en cada intento fallido para disparar animación y vibración. */
    val shakeCount: Int = 0,
    val registered: Boolean = false
) {
    val passwordStrength: Int get() = Validators.passwordStrength(password)
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update {
        it.copy(
            name = value,
            formError = null,
            nameError = if (it.nameError != null) Validators.validateName(value) else null
        )
    }

    fun onRutChange(value: String) = _state.update {
        it.copy(
            rut = value,
            formError = null,
            rutError = if (it.rutError != null) Validators.validateRut(value) else null
        )
    }

    fun onPhoneChange(value: String) = _state.update {
        it.copy(
            phone = value,
            formError = null,
            phoneError = if (it.phoneError != null) Validators.validatePhone(value) else null
        )
    }

    fun onEmailChange(value: String) = _state.update {
        it.copy(
            email = value,
            formError = null,
            emailError = if (it.emailError != null) Validators.validateEmail(value) else null
        )
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(
            password = value,
            formError = null,
            passwordError = if (it.passwordError != null) Validators.validatePassword(value) else null,
            // Si ya había escrito la confirmación, se revalida contra la nueva contraseña.
            confirmError = if (it.confirmPassword.isNotEmpty()) {
                Validators.validateConfirmPassword(value, it.confirmPassword)
            } else null
        )
    }

    fun onConfirmChange(value: String) = _state.update {
        it.copy(
            confirmPassword = value,
            formError = null,
            confirmError = if (it.confirmError != null) {
                Validators.validateConfirmPassword(it.password, value)
            } else null
        )
    }

    fun onTermsChange(value: Boolean) = _state.update {
        it.copy(acceptedTerms = value, termsError = null, formError = null)
    }

    fun onNameBlur() = _state.update { it.copy(nameError = Validators.validateName(it.name)) }

    /** Al salir del campo, si el RUT es válido se muestra con formato (12.345.678-5). */
    fun onRutBlur() = _state.update {
        val error = Validators.validateRut(it.rut)
        it.copy(
            rutError = error,
            rut = if (error == null) Validators.formatRut(Validators.normalizeRut(it.rut)) else it.rut
        )
    }

    fun onPhoneBlur() = _state.update {
        val error = Validators.validatePhone(it.phone)
        it.copy(
            phoneError = error,
            phone = if (error == null) Validators.formatPhone(Validators.normalizePhone(it.phone)) else it.phone
        )
    }

    fun onEmailBlur() = _state.update { it.copy(emailError = Validators.validateEmail(it.email)) }
    fun onPasswordBlur() = _state.update { it.copy(passwordError = Validators.validatePassword(it.password)) }
    fun onConfirmBlur() = _state.update {
        it.copy(confirmError = Validators.validateConfirmPassword(it.password, it.confirmPassword))
    }

    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit() {
        val s = _state.value
        if (s.isLoading) return

        val nameError = Validators.validateName(s.name)
        val rutError = Validators.validateRut(s.rut)
        val phoneError = Validators.validatePhone(s.phone)
        val emailError = Validators.validateEmail(s.email)
        val passwordError = Validators.validatePassword(s.password)
        val confirmError = Validators.validateConfirmPassword(s.password, s.confirmPassword)
        val termsError = Validators.validateTerms(s.acceptedTerms)

        val errors = listOf(nameError, rutError, phoneError, emailError, passwordError, confirmError, termsError)
        if (errors.any { it != null }) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    rutError = rutError,
                    phoneError = phoneError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                    termsError = termsError,
                    shakeCount = it.shakeCount + 1
                )
            }
            return
        }

        _state.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            when (authRepository.register(s.name, s.rut, s.email, s.phone, s.password)) {
                AuthResult.Success -> _state.update { it.copy(isLoading = false, registered = true) }
                AuthResult.EmailTaken -> _state.update {
                    it.copy(
                        isLoading = false,
                        emailError = "Este correo ya está registrado",
                        shakeCount = it.shakeCount + 1
                    )
                }
                AuthResult.RutTaken -> _state.update {
                    it.copy(
                        isLoading = false,
                        rutError = "Este RUT ya está registrado",
                        shakeCount = it.shakeCount + 1
                    )
                }
                AuthResult.InvalidCredentials -> _state.update {
                    it.copy(
                        isLoading = false,
                        formError = "No se pudo crear la cuenta. Inténtalo nuevamente",
                        shakeCount = it.shakeCount + 1
                    )
                }
            }
        }
    }
}
