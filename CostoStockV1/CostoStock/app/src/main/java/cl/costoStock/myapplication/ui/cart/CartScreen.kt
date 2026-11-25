package cl.costoStock.myapplication.ui.cart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.domain.cart.CartCalculator
import cl.costoStock.myapplication.domain.cart.CartSummary
import cl.costoStock.myapplication.nativeapi.rememberHaptics
import cl.costoStock.myapplication.ui.AppViewModelProvider
import cl.costoStock.myapplication.ui.components.CategoryAvatar
import cl.costoStock.myapplication.ui.components.QuantityStepper
import cl.costoStock.myapplication.ui.util.formatClp

@Composable
fun CartScreen(
    onMessage: (String) -> Unit,
    onGoToCatalog: () -> Unit,
    viewModel: CartViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.messages.collect(onMessage) }
    LaunchedEffect(state.confirmation) { if (state.confirmation != null) haptics.success() }

    state.confirmation?.let { summary ->
        PurchaseSuccessDialog(summary = summary, onDismiss = viewModel::dismissConfirmation)
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Vaciar carrito") },
            text = { Text("Se quitarán todos los productos del carrito.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearCart()
                }) { Text("Vaciar") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") } }
        )
    }

    if (state.lines.isEmpty()) {
        EmptyCart(onGoToCatalog)
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${state.summary.itemCount} unidades",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showClearDialog = true }) { Text("Vaciar") }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.lines, key = { it.product.id }) { line ->
                CartLineCard(
                    line = line,
                    onQuantityChange = { q ->
                        haptics.tick()
                        viewModel.changeQuantity(line, q)
                    },
                    onRemove = {
                        haptics.tick()
                        viewModel.remove(line)
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }

        CartSummaryCard(
            summary = state.summary,
            isProcessing = state.isProcessing,
            hasStockIssues = state.hasStockIssues,
            onCheckout = viewModel::checkout
        )
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val product = line.product
    val exceedsStock = line.quantity > product.stock
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryAvatar(product.emoji, size = 48)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatClp(product.price)} c/u (IVA incl.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exceedsStock) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            " Solo quedan ${product.stock}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                QuantityStepper(
                    quantity = line.quantity,
                    canIncrement = line.quantity < product.stock,
                    onDecrement = { onQuantityChange(line.quantity - 1) },
                    onIncrement = { onQuantityChange(line.quantity + 1) }
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar ${product.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    formatClp(line.subtotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CartSummaryCard(
    summary: CartSummary,
    isProcessing: Boolean,
    hasStockIssues: Boolean,
    onCheckout: () -> Unit
) {
    val animatedTotal by animateIntAsState(summary.total, tween(400), label = "total")
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            SummaryRow("Neto", formatClp(summary.net))
            SummaryRow("IVA (${CartCalculator.IVA_PERCENT}%) incluido", formatClp(summary.iva))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    formatClp(animatedTotal),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onCheckout,
                enabled = !isProcessing && !hasStockIssues,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Finalizar compra")
                }
            }
            if (hasStockIssues) {
                Text(
                    "Ajusta las cantidades que superan el stock disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyCart(onGoToCatalog: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "emptyPulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emptyScale"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.RemoveShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Tu carrito está vacío", style = MaterialTheme.typography.titleLarge)
        Text(
            "Agrega productos desde el catálogo para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onGoToCatalog) { Text("Ver catálogo") }
    }
}

@Composable
private fun PurchaseSuccessDialog(summary: CartSummary, onDismiss: () -> Unit) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale.value)
            )
        },
        title = { Text("¡Compra registrada!") },
        text = {
            Text(
                "Se descontó el stock de ${summary.itemCount} unidades.\n" +
                    "Total pagado: ${formatClp(summary.total)} (IVA incluido)."
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )
}
