package cl.costoStock.proveedor.ui.publish

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.costoStock.proveedor.data.model.ProductCondition
import cl.costoStock.proveedor.domain.catalog.Catalog
import cl.costoStock.proveedor.domain.pricing.PriceBreakdown
import cl.costoStock.proveedor.domain.pricing.PriceCalculator
import cl.costoStock.proveedor.domain.validation.Validators
import cl.costoStock.proveedor.nativeapi.rememberCameraAction
import cl.costoStock.proveedor.nativeapi.rememberHaptics
import cl.costoStock.proveedor.ui.AppViewModelProvider
import cl.costoStock.proveedor.ui.components.AppDropdownField
import cl.costoStock.proveedor.ui.components.AppTextField
import cl.costoStock.proveedor.ui.components.FieldErrorText
import cl.costoStock.proveedor.ui.components.FormErrorBanner
import cl.costoStock.proveedor.ui.components.ProductPhoto
import cl.costoStock.proveedor.ui.components.shakeOnTrigger
import cl.costoStock.proveedor.ui.util.formatClp

/**
 * Asistente de publicación en 4 pasos (inspirado en "Vender" de Mercado Libre):
 * Producto → Fotos → Precio → Revisar. Cada paso se valida antes de avanzar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    productId: Long,
    onMessage: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: PublishViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current
    var showDiscardDialog by remember { mutableStateOf(false) }
    val takePhoto = rememberCameraAction(onPhotoTaken = viewModel::addPhoto, onMessage = onMessage)

    LaunchedEffect(productId) { viewModel.start(productId) }
    LaunchedEffect(state.shakeCount) { if (state.shakeCount > 0) haptics.error() }
    LaunchedEffect(state.completed) { if (state.completed) haptics.success() }

    val requestClose = { if (state.isDirty) showDiscardDialog = true else onClose() }

    // El botón "atrás" del teléfono retrocede un paso; en el primero pide salir.
    BackHandler(enabled = !state.completed) {
        if (state.step > 0) viewModel.previous() else requestClose()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("¿Descartar publicación?") },
            text = { Text("Perderás los datos que ingresaste.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onClose()
                }) { Text("Descartar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Seguir editando") } }
        )
    }

    Crossfade(targetState = state.completed, label = "publishCompleted") { completed ->
        if (completed) {
            PublishSuccess(
                isEditing = state.isEditing,
                onSeePublications = onClose,
                onPublishAnother = viewModel::reset
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        title = { Text(if (state.isEditing) "Editar publicación" else "Nueva publicación") },
                        navigationIcon = {
                            IconButton(onClick = requestClose) {
                                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 3.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Column {
                            FormErrorBanner(
                                message = state.formError,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (state.step > 0) {
                                    OutlinedButton(
                                        onClick = viewModel::previous,
                                        enabled = !state.isSaving,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    ) { Text("Atrás") }
                                }
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.next()
                                    },
                                    enabled = !state.isSaving && !state.isLoadingProduct,
                                    modifier = Modifier
                                        .weight(if (state.step > 0) 1.5f else 1f)
                                        .height(52.dp)
                                ) {
                                    if (state.isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text(
                                            when {
                                                state.step < PublishSteps.last -> "Siguiente"
                                                state.isEditing -> "Guardar cambios"
                                                else -> "Publicar artículo"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    WizardProgress(step = state.step, labels = PublishSteps.labels)

                    if (state.isLoadingProduct) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AnimatedContent(
                            targetState = state.step,
                            modifier = Modifier
                                .weight(1f)
                                .shakeOnTrigger(state.shakeCount),
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))) togetherWith
                                        (slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(200)))
                                } else {
                                    (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))) togetherWith
                                        (slideOutHorizontally(tween(300)) { it } + fadeOut(tween(200)))
                                }
                            },
                            label = "wizardStep"
                        ) { step ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (step) {
                                    PublishSteps.PRODUCT -> ProductStep(state, viewModel)
                                    PublishSteps.PHOTOS -> PhotosStep(state, viewModel, takePhoto)
                                    PublishSteps.PRICE -> PriceStep(state, viewModel)
                                    else -> ReviewStep(state)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------ Barra de progreso ------------------------------ */

@Composable
private fun WizardProgress(step: Int, labels: List<String>) {
    val progress by animateFloatAsState((step + 1f) / labels.size, tween(350), label = "wizardProgress")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val reached = index <= step
                val circleColor by animateColorAsState(
                    if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    tween(300),
                    label = "stepColor"
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(circleColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < step) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Paso completado",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (reached) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == step) FontWeight.Bold else FontWeight.Normal,
                        color = if (reached) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ------------------------------ Paso 1: producto ------------------------------ */

@Composable
private fun ProductStep(state: PublishUiState, viewModel: PublishViewModel) {
    val focusManager = LocalFocusManager.current
    StepHeader("¿Qué vas a vender?", "Cuéntanos lo básico de tu artículo.")

    AppTextField(
        value = state.name,
        onValueChange = viewModel::onNameChange,
        label = "Título de la publicación",
        leadingIcon = Icons.Filled.Title,
        error = state.nameError,
        maxLength = Validators.TITLE_MAX,
        helperText = "Incluye marca, modelo y características",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        onFocusLost = viewModel::onNameBlur
    )
    AppDropdownField(
        value = state.category,
        options = Catalog.categories,
        onSelected = viewModel::onCategoryChange,
        label = "Categoría",
        leadingIcon = Icons.Filled.Category,
        error = state.categoryError
    )
    ConditionSelector(
        selected = state.condition,
        error = state.conditionError,
        onSelected = viewModel::onConditionChange
    )
    AppTextField(
        value = state.description,
        onValueChange = viewModel::onDescriptionChange,
        label = "Descripción",
        leadingIcon = Icons.Filled.Description,
        error = state.descriptionError,
        singleLine = false,
        minLines = 4,
        maxLength = Validators.DESCRIPTION_MAX,
        helperText = "Estado, medidas, motivo de venta, detalles importantes",
        onFocusLost = viewModel::onDescriptionBlur
    )
}

@Composable
private fun ConditionSelector(
    selected: ProductCondition?,
    error: String?,
    onSelected: (ProductCondition) -> Unit
) {
    Column {
        Text(
            "Condición",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductCondition.entries.forEach { condition ->
                val isSelected = selected == condition
                val checkIcon: (@Composable () -> Unit)? = if (isSelected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(condition) },
                    label = { Text(condition.label) },
                    leadingIcon = checkIcon
                )
            }
        }
        FieldErrorText(error)
    }
}

/* ------------------------------ Paso 2: fotos ------------------------------ */

@Composable
private fun PhotosStep(state: PublishUiState, viewModel: PublishViewModel, onTakePhoto: () -> Unit) {
    StepHeader(
        "Agrega fotos",
        "Las buenas fotos venden más. Puedes subir hasta ${Validators.MAX_PHOTOS}; la primera es la portada."
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(Validators.MAX_PHOTOS) { index ->
            PhotoSlot(
                path = state.photos.getOrNull(index),
                isCover = index == 0,
                canAdd = index == state.photos.size,
                onAdd = onTakePhoto,
                onRemove = { viewModel.removePhoto(index) },
                onMakeCover = { viewModel.makeCover(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    OutlinedButton(
        onClick = onTakePhoto,
        enabled = state.photos.size < Validators.MAX_PHOTOS,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            if (state.photos.isEmpty()) "Tomar foto"
            else "Tomar otra foto (${state.photos.size}/${Validators.MAX_PHOTOS})"
        )
    }
    FieldErrorText(state.photosError)
    if (state.photos.size > 1) {
        Text(
            "Toca una foto para usarla como portada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PhotoSlot(
    path: String?,
    isCover: Boolean,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onMakeCover: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    when {
        path != null -> Box(modifier.aspectRatio(1f).clip(shape)) {
            ProductPhoto(
                path = path,
                emoji = "📷",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = !isCover, onClick = onMakeCover),
                maxSize = 400
            )
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .clickable(onClick = onRemove)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Quitar foto",
                    tint = Color.White,
                    modifier = Modifier.padding(5.dp)
                )
            }
            if (isCover) {
                Surface(
                    shape = RoundedCornerShape(topEnd = 10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "Portada",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        canAdd -> Column(
            modifier = modifier
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                .clickable(onClick = onAdd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.AddAPhoto,
                contentDescription = "Agregar foto",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Agregar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        else -> Box(
            modifier
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
    }
}

/* ------------------------------ Paso 3: precio y ubicación ------------------------------ */

@Composable
private fun PriceStep(state: PublishUiState, viewModel: PublishViewModel) {
    val focusManager = LocalFocusManager.current
    StepHeader("Precio y disponibilidad", "Define cuánto cuesta y dónde está tu artículo.")

    AppTextField(
        value = state.price,
        onValueChange = viewModel::onPriceChange,
        label = "Precio (IVA incluido)",
        leadingIcon = Icons.Filled.AttachMoney,
        error = state.priceError,
        prefixText = "$",
        helperText = "Solo números, sin puntos. Ej: 12990",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        onFocusLost = viewModel::onPriceBlur
    )

    // Desglose de IVA (misma regla que el carrito de la app de cliente).
    val breakdown = state.priceValue
        ?.takeIf { state.priceError == null && it >= Validators.MIN_PRICE }
        ?.let { PriceCalculator.breakdown(it) }
    var lastBreakdown by remember { mutableStateOf<PriceBreakdown?>(null) }
    if (breakdown != null) lastBreakdown = breakdown
    AnimatedVisibility(visible = breakdown != null) {
        lastBreakdown?.let { BreakdownCard(it) }
    }

    AppTextField(
        value = state.stock,
        onValueChange = viewModel::onStockChange,
        label = "Unidades disponibles",
        leadingIcon = Icons.Filled.Inventory2,
        error = state.stockError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        onFocusLost = viewModel::onStockBlur
    )
    AppDropdownField(
        value = state.comuna,
        options = Catalog.comunas,
        onSelected = viewModel::onComunaChange,
        label = "Comuna del producto",
        leadingIcon = Icons.Filled.Place,
        error = state.comunaError
    )
    Text(
        "Los compradores verán esta comuna para coordinar la entrega.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun BreakdownCard(breakdown: PriceBreakdown) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BreakdownRow("Valor neto", formatClp(breakdown.net))
            BreakdownRow("IVA (${PriceCalculator.IVA_PERCENT}%)", formatClp(breakdown.iva))
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            BreakdownRow("Precio que verá el comprador", formatClp(breakdown.total), bold = true)
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/* ------------------------------ Paso 4: revisar ------------------------------ */

@Composable
private fun ReviewStep(state: PublishUiState) {
    StepHeader("Revisa tu publicación", "Así verán tu artículo los compradores.")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            ProductPhoto(
                path = state.photos.firstOrNull(),
                emoji = Catalog.emoji(state.category.orEmpty()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                maxSize = 900,
                emojiSize = 80.sp
            )
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val stock = state.stockValue ?: 0
                Text(
                    "${state.condition?.label.orEmpty()} · ${if (stock == 1) "1 disponible" else "$stock disponibles"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(state.name.trim(), style = MaterialTheme.typography.titleLarge)
                Text(
                    formatClp(state.priceValue ?: 0),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        " ${state.comuna.orEmpty()}  ·  ${Catalog.emoji(state.category.orEmpty())} ${state.category.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text("Descripción", style = MaterialTheme.typography.titleSmall)
                Text(state.description.trim(), style = MaterialTheme.typography.bodyMedium)

                if (state.photos.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.photos.drop(1).forEach { path ->
                            ProductPhoto(
                                path = path,
                                emoji = "📷",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                maxSize = 200
                            )
                        }
                    }
                }
            }
        }
    }
    Text(
        "Tu publicación quedará activa. Podrás editarla, pausarla o eliminarla desde \"Mis publicaciones\".",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/* ------------------------------ Éxito ------------------------------ */

@Composable
private fun PublishSuccess(
    isEditing: Boolean,
    onSeePublications: () -> Unit,
    onPublishAnother: () -> Unit
) {
    // El check "rebota" al aparecer.
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Operación exitosa",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (isEditing) "¡Cambios guardados!" else "¡Publicación creada!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isEditing) "Tu publicación ya está actualizada."
            else "Tu artículo ya está activo y listo para venderse.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onSeePublications,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) { Text("Ver mis publicaciones") }
        if (!isEditing) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPublishAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Publicar otro artículo") }
        }
    }
}
