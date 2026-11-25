package cl.costoStock.proveedor.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.costoStock.proveedor.nativeapi.rememberHaptics
import cl.costoStock.proveedor.ui.AppViewModelProvider
import cl.costoStock.proveedor.ui.components.AppTextField
import cl.costoStock.proveedor.ui.components.AuthHeader
import cl.costoStock.proveedor.ui.components.FormErrorBanner
import cl.costoStock.proveedor.ui.components.shakeOnTrigger

/** "¿Olvidaste tu contraseña?": correo → código + nueva contraseña → listo. */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.shakeCount) { if (state.shakeCount > 0) haptics.error() }
    LaunchedEffect(state.step) { if (state.step == ResetStep.DONE) haptics.success() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
    ) {
        AuthHeader(
            title = "Recuperar contraseña",
            subtitle = "Te ayudamos a volver a tu cuenta",
            onBack = onBack
        )

        LinearProgressIndicator(
            progress = { (state.step.ordinal + 1) / 3f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300)))
                    .togetherWith(slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(200)))
            },
            label = "pasoRecuperacion"
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .shakeOnTrigger(state.shakeCount),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormErrorBanner(state.formError)
                when (step) {
                    ResetStep.EMAIL -> {
                        Text("Paso 1 de 2 · Tu correo", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Ingresa el correo de tu cuenta y te enviaremos un código de 6 dígitos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AppTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            label = "Correo electrónico",
                            leadingIcon = Icons.Filled.Email,
                            error = state.emailError,
                            enabled = !state.isLoading,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                viewModel.submitEmail()
                            })
                        )
                        PrimaryButton("Enviar código", state.isLoading) {
                            focusManager.clearFocus()
                            viewModel.submitEmail()
                        }
                    }

                    ResetStep.CODE -> {
                        Text("Paso 2 de 2 · Código y nueva contraseña", style = MaterialTheme.typography.titleMedium)
                        state.info?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        AppTextField(
                            value = state.code,
                            onValueChange = viewModel::onCodeChange,
                            label = "Código de 6 dígitos",
                            leadingIcon = Icons.Filled.Pin,
                            error = state.codeError,
                            enabled = !state.isLoading,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        AppTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = "Nueva contraseña",
                            leadingIcon = Icons.Filled.Lock,
                            error = state.passwordError,
                            enabled = !state.isLoading,
                            visualTransformation = if (state.passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            trailingContent = {
                                IconButton(onClick = viewModel::togglePasswordVisibility) {
                                    Icon(
                                        if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (state.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            }
                        )
                        if (state.password.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = { state.passwordStrength / 4f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AppTextField(
                            value = state.confirm,
                            onValueChange = viewModel::onConfirmChange,
                            label = "Repite la nueva contraseña",
                            leadingIcon = Icons.Filled.LockReset,
                            error = state.confirmError,
                            enabled = !state.isLoading,
                            visualTransformation = if (state.passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                viewModel.submitReset()
                            })
                        )
                        PrimaryButton("Cambiar contraseña", state.isLoading) {
                            focusManager.clearFocus()
                            viewModel.submitReset()
                        }
                        TextButton(onClick = viewModel::resendCode, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                            Text("Reenviar código")
                        }
                        TextButton(onClick = viewModel::changeEmail, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                            Text("Usar otro correo")
                        }
                    }

                    ResetStep.DONE -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp)
                            )
                            Text("¡Contraseña actualizada!", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Ya puedes iniciar sesión con tu nueva contraseña.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PrimaryButton("Volver a iniciar sesión", isLoading = false, onClick = onDone)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text)
        }
    }
}
