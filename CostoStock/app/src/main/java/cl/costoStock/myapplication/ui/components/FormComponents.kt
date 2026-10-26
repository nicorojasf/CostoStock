package cl.costoStock.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Campo de texto con ícono inicial, ícono de error y mensaje visual por campo.
 * [onFocusLost] se invoca cuando el usuario sale del campo (validación al perder foco).
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onFocusLost: () -> Unit = {}
) {
    var hadFocus by remember { mutableStateOf(false) }

    val errorIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = "Campo con error",
            tint = MaterialTheme.colorScheme.error
        )
    }
    val trailing: (@Composable () -> Unit)? =
        trailingContent ?: if (error != null) errorIcon else null
    val supporting: (@Composable () -> Unit)? = if (error != null) {
        { Text(error) }
    } else null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focus ->
                if (hadFocus && !focus.isFocused) onFocusLost()
                hadFocus = focus.isFocused
            },
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = trailing,
        supportingText = supporting,
        isError = error != null,
        singleLine = true,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp)
    )
}

/** Encabezado de las pantallas de acceso (logo, título y subtítulo). */
@Composable
fun AuthHeader(
    title: String?,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (onBack != null) 40.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Wordmark(fontSize = 40.sp, showSlogan = true)
            Spacer(Modifier.height(12.dp))
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/** Banner animado para errores generales del formulario. */
@Composable
fun FormErrorBanner(message: String?, modifier: Modifier = Modifier) {
    var lastMessage by remember { mutableStateOf("") }
    if (message != null && message != lastMessage) lastMessage = message

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(200)) + expandVertically(tween(250)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Error, contentDescription = null)
                Text(lastMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Barra animada con la fortaleza de la contraseña (0..4). */
@Composable
fun PasswordStrengthBar(strength: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = (strength + 1) / 5f,
        animationSpec = tween(300),
        label = "strengthProgress"
    )
    val targetColor = when {
        strength <= 1 -> MaterialTheme.colorScheme.error
        strength == 2 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val color by animateColorAsState(targetColor, tween(300), label = "strengthColor")
    val label = listOf("Muy débil", "Débil", "Aceptable", "Buena", "Fuerte")[strength.coerceIn(0, 4)]

    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Seguridad: $label",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/** Sacude horizontalmente el contenido cada vez que [trigger] aumenta (error de formulario). */
@Composable
fun Modifier.shakeOnTrigger(trigger: Int): Modifier {
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            repeat(3) {
                shakeX.animateTo(12f, tween(40))
                shakeX.animateTo(-12f, tween(40))
            }
            shakeX.animateTo(0f, tween(40))
        }
    }
    return this.offset { IntOffset(shakeX.value.roundToInt(), 0) }
}
