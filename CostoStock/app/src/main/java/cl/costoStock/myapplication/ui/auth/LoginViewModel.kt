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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val formError: String? = null,
    /** Se incrementa en cada intento fallido para disparar animación y vibración. */
    val shakeCount: Int = 0,
    val loggedIn: Boolean = false
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

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
            passwordError = if (it.passwordError != null) Validators.validateLoginPassword(value) else null
        )
    }

    fun onEmailBlur() = _state.update { it.copy(emailError = Validators.validateEmail(it.email)) }

    fun onPasswordBlur() = _state.update {
        it.copy(passwordError = Validators.validateLoginPassword(it.password))
    }

    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit() {
        val current = _state.value
        if (current.isLoading) return

        val emailError = Validators.validateEmail(current.email)
        val passwordError = Validators.validateLoginPassword(current.password)
        if (emailError != null || passwordError != null) {
            _state.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    shakeCount = it.shakeCount + 1
                )
            }
            return
        }

        _state.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            when (authRepository.login(current.email, current.password)) {
                AuthResult.Success -> _state.update { it.copy(isLoading = false, loggedIn = true) }
                else -> _state.update {
                    it.copy(
                        isLoading = false,
                        formError = "Correo o contraseña incorrectos",
                        shakeCount = it.shakeCount + 1
                    )
                }
            }
        }
    }
}
