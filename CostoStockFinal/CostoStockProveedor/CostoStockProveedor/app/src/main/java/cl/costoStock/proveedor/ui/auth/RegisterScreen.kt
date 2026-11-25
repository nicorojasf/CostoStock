package cl.costoStock.proveedor.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.costoStock.proveedor.nativeapi.rememberHaptics
import cl.costoStock.proveedor.ui.AppViewModelProvider
import cl.costoStock.proveedor.ui.components.AppTextField
import cl.costoStock.proveedor.ui.components.AuthHeader
import cl.costoStock.proveedor.ui.components.FormErrorBanner
import cl.costoStock.proveedor.ui.components.PasswordStrengthBar
import cl.costoStock.proveedor.ui.components.shakeOnTrigger

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(state.registered) { if (state.registered) onRegistered() }
    LaunchedEffect(state.shakeCount) { if (state.shakeCount > 0) haptics.error() }

    val passwordToggle: @Composable () -> Unit = {
        IconButton(onClick = viewModel::togglePasswordVisibility) {
            Icon(
                imageVector = if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (state.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
            )
        }
    }
    val passwordTransformation =
        if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val goNext = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
    ) {
        AuthHeader(
            title = "Crear cuenta de proveedor",
            subtitle = "Regístrate para publicar y vender tus artículos",
            onBack = onBack
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 6 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .shakeOnTrigger(state.shakeCount),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FormErrorBanner(state.formError)

                AppTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = "Nombre del comercio o vendedor",
                    leadingIcon = Icons.Filled.Storefront,
                    error = state.nameError,
                    enabled = !state.isLoading,
                    maxLength = 60,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = goNext,
                    onFocusLost = viewModel::onNameBlur
                )
                AppTextField(
                    value = state.rut,
                    onValueChange = viewModel::onRutChange,
                    label = "RUT",
                    leadingIcon = Icons.Filled.Badge,
                    error = state.rutError,
                    helperText = "Ej: 12.345.678-5",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = goNext,
                    onFocusLost = viewModel::onRutBlur
                )
                AppTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = "Teléfono celular",
                    leadingIcon = Icons.Filled.Phone,
                    error = state.phoneError,
                    helperText = "Ej: +56 9 1234 5678",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = goNext,
                    onFocusLost = viewModel::onPhoneBlur
                )
                AppTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Correo electrónico",
                    leadingIcon = Icons.Filled.Email,
                    error = state.emailError,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = goNext,
                    onFocusLost = viewModel::onEmailBlur
                )
                AppTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Contraseña",
                    leadingIcon = Icons.Filled.Lock,
                    error = state.passwordError,
                    enabled = !state.isLoading,
                    visualTransformation = passwordTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = goNext,
                    trailingContent = passwordToggle,
                    onFocusLost = viewModel::onPasswordBlur
                )
                AnimatedVisibility(visible = state.password.isNotEmpty()) {
                    PasswordStrengthBar(strength = state.passwordStrength)
                }
                AppTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmChange,
                    label = "Repetir contraseña",
                    leadingIcon = Icons.Filled.VerifiedUser,
                    error = state.confirmError,
                    enabled = !state.isLoading,
                    visualTransformation = passwordTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    onFocusLost = viewModel::onConfirmBlur
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = state.acceptedTerms,
                            enabled = !state.isLoading,
                            role = Role.Checkbox,
                            onValueChange = viewModel::onTermsChange
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = state.acceptedTerms, onCheckedChange = null)
                    Text(
                        text = "  Acepto los términos y condiciones para proveedores",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                AnimatedVisibility(visible = state.termsError != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "  ${state.termsError.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submit()
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Crear cuenta")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Ya tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onBack, enabled = !state.isLoading) { Text("Inicia sesión") }
                }
            }
        }
    }
}
