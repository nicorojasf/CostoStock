package cl.costoStock.proveedor.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.proveedor.data.repository.AuthRepository
import cl.costoStock.proveedor.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pasos de "Olvidé mi contraseña". */
enum class ResetStep { EMAIL, CODE, DONE }

data class ForgotPasswordUiState(
    val step: ResetStep = ResetStep.EMAIL,
    val email: String = "",
    val emailError: String? = null,
    val code: String = "",
    val codeError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirm: String = "",
    val confirmError: String? = null,
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    /** Error del servidor o de red (banner rojo). */
    val formError: String? = null,
    /** Mensaje informativo (ej: "Te enviamos un código..."). */
    val info: String? = null,
    val shakeCount: Int = 0
) {
    val passwordStrength: Int get() = Validators.passwordStrength(password)
}

/**
 * Recuperación de contraseña en dos pasos contra ms-usuarios:
 * 1) se ingresa el correo y el servidor envía un código de 6 dígitos;
 * 2) se ingresa el código y la nueva contraseña.
 * Toda la validación vive aquí (Validators), no en la pantalla.
 */
class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, emailError = if (it.emailError != null) Validators.validateEmail(value) else null)
    }

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _state.update {
            it.copy(code = digits, codeError = if (it.codeError != null) Validators.validateResetCode(digits) else null)
        }
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(
            password = value,
            passwordError = if (it.passwordError != null) Validators.validatePassword(value) else null,
            confirmError = if (it.confirm.isNotEmpty()) Validators.validateConfirmPassword(value, it.confirm) else null
        )
    }

    fun onConfirmChange(value: String) = _state.update {
        it.copy(
            confirm = value,
            confirmError = if (it.confirmError != null) Validators.validateConfirmPassword(it.password, value) else null
        )
    }

    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    /** Paso 1: pide el código. */
    fun submitEmail() {
        val s = _state.value
        if (s.isLoading) return
        val error = Validators.validateEmail(s.email)
        if (error != null) {
            _state.update { it.copy(emailError = error, shakeCount = it.shakeCount + 1) }
            return
        }
        _state.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            val failure = authRepository.requestPasswordReset(s.email)
            _state.update {
                if (failure == null) {
                    it.copy(
                        isLoading = false,
                        step = ResetStep.CODE,
                        info = "Si ${s.email.trim()} está registrado, te enviamos un código de 6 dígitos. Vence en 10 minutos."
                    )
                } else {
                    it.copy(isLoading = false, formError = failure, shakeCount = it.shakeCount + 1)
                }
            }
        }
    }

    /** Paso 2: valida y cambia la contraseña. */
    fun submitReset() {
        val s = _state.value
        if (s.isLoading) return
        val codeError = Validators.validateResetCode(s.code)
        val passwordError = Validators.validatePassword(s.password)
        val confirmError = Validators.validateConfirmPassword(s.password, s.confirm)
        if (codeError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(
                    codeError = codeError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                    shakeCount = it.shakeCount + 1
                )
            }
            return
        }
        _state.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            val failure = authRepository.resetPassword(s.email, s.code, s.password)
            _state.update {
                if (failure == null) it.copy(isLoading = false, step = ResetStep.DONE, info = null)
                else it.copy(isLoading = false, formError = failure, shakeCount = it.shakeCount + 1)
            }
        }
    }

    /** Pide un código nuevo para el mismo correo. */
    fun resendCode() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            val failure = authRepository.requestPasswordReset(_state.value.email)
            _state.update {
                if (failure == null) it.copy(isLoading = false, code = "", info = "Te enviamos un código nuevo")
                else it.copy(isLoading = false, formError = failure)
            }
        }
    }

    /** Vuelve al paso 1 para corregir el correo. */
    fun changeEmail() = _state.update {
        it.copy(step = ResetStep.EMAIL, code = "", codeError = null, formError = null, info = null)
    }
}
