package cl.costoStock.proveedor.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.costoStock.proveedor.data.model.Seller
import cl.costoStock.proveedor.domain.validation.Validators
import cl.costoStock.proveedor.nativeapi.rememberCameraAction
import cl.costoStock.proveedor.nativeapi.rememberHaptics
import cl.costoStock.proveedor.ui.AppViewModelProvider
import cl.costoStock.proveedor.ui.components.AppTextField
import cl.costoStock.proveedor.ui.components.rememberPhotoBitmap
import cl.costoStock.proveedor.ui.util.formatMonthYear

@Composable
fun ProfileScreen(
    onMessage: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val edit by viewModel.edit.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    var visible by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val takePhoto = rememberCameraAction(onPhotoTaken = viewModel::onPhotoCaptured, onMessage = onMessage)

    LaunchedEffect(Unit) {
        visible = true
        viewModel.messages.collect(onMessage)
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Tus publicaciones se mantienen guardadas en este dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") } }
        )
    }

    if (edit.visible) {
        EditProfileDialog(edit = edit, viewModel = viewModel)
    }

    val seller = state.seller
    if (seller == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 8 }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Avatar(seller = seller, onChangePhoto = {
                haptics.tick()
                takePhoto()
            })
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    seller.name,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Proveedor desde ${formatMonthYear(seller.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Publicaciones", state.totalProducts.toString(), Modifier.weight(1f))
                StatCard("Activas", state.activeProducts.toString(), Modifier.weight(1f))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Datos de contacto", style = MaterialTheme.typography.titleMedium)
                    InfoRow(Icons.Filled.Email, "Correo", seller.email)
                    InfoRow(Icons.Filled.Badge, "RUT", Validators.formatRut(seller.rut))
                    InfoRow(Icons.Filled.Phone, "Teléfono", Validators.formatPhone(seller.phone))
                }
            }

            OutlinedButton(
                onClick = viewModel::openEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Editar datos")
            }
            TextButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Avatar(seller: Seller, onChangePhoto: () -> Unit) {
    val bitmap = rememberPhotoBitmap(seller.photoPath, maxSize = 400)
    Box(modifier = Modifier.size(120.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onChangePhoto),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Filled.Storefront,
                    contentDescription = "Sin foto de perfil",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onChangePhoto),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = "Cambiar foto de perfil",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun EditProfileDialog(edit: EditProfileState, viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { if (!edit.isSaving) viewModel.closeEdit() },
        title = { Text("Editar datos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = edit.name,
                    onValueChange = viewModel::onEditNameChange,
                    label = "Nombre del comercio o vendedor",
                    leadingIcon = Icons.Filled.Storefront,
                    error = edit.nameError,
                    enabled = !edit.isSaving,
                    maxLength = Validators.NAME_MAX,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    onFocusLost = viewModel::onEditNameBlur
                )
                AppTextField(
                    value = edit.phone,
                    onValueChange = viewModel::onEditPhoneChange,
                    label = "Teléfono celular",
                    leadingIcon = Icons.Filled.Phone,
                    error = edit.phoneError,
                    enabled = !edit.isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    onFocusLost = viewModel::onEditPhoneBlur
                )
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::saveEdit, enabled = !edit.isSaving) {
                if (edit.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::closeEdit, enabled = !edit.isSaving) { Text("Cancelar") }
        }
    )
}
