package cl.costoStock.myapplication.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Selector de cantidad con animación del número (sube/baja según el cambio). */
@Composable
fun QuantityStepper(
    quantity: Int,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Quitar una unidad")
            }
            AnimatedContent(
                targetState = quantity,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { it } + fadeIn()) togetherWith
                            (slideOutVertically { -it } + fadeOut())
                    } else {
                        (slideInVertically { -it } + fadeIn()) togetherWith
                            (slideOutVertically { it } + fadeOut())
                    }
                },
                label = "quantityAnimation"
            ) { value ->
                Text(
                    text = value.toString(),
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onIncrement,
                enabled = canIncrement,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar una unidad")
            }
        }
    }
}

/** Círculo con el emoji de la categoría del producto. */
@Composable
fun CategoryAvatar(emoji: String, modifier: Modifier = Modifier, size: Int = 52) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size * 0.46f).sp)
    }
}
