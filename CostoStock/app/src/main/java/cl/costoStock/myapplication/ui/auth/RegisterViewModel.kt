package cl.costoStock.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.myapplication.data.repository.AuthRepository
import cl.costoStock.myapplication.data.repository.AuthResult
import cl.costoStock.myapplication.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val termsError: String? = null,
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val formError: String? = null,
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
        val emailError = Validators.validateEmail(s.email)
        val passwordError = Validators.validatePassword(s.password)
        val confirmError = Validators.validateConfirmPassword(s.password, s.confirmPassword)
        val termsError = Validators.validateTerms(s.acceptedTerms)

        if (listOf(nameError, emailError, passwordError, confirmError, termsError).any { it != null }) {
            _state.update {
                it.copy(
                    nameError = nameError,
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
            when (authRepository.register(s.name, s.email, s.password)) {
                AuthResult.Success -> _state.update { it.copy(isLoading = false, registered = true) }
                AuthResult.EmailTaken -> _state.update {
                    it.copy(
                        isLoading = false,
                        emailError = "Este correo ya está registrado",
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
