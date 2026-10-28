package cl.costoStock.proveedor.ui.products

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.costoStock.proveedor.data.model.Product
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.ui.AppViewModelProvider
import cl.costoStock.proveedor.ui.components.AppTextField
import cl.costoStock.proveedor.ui.components.EmptyState
import cl.costoStock.proveedor.ui.components.ProductPhoto
import cl.costoStock.proveedor.ui.components.StatusChip
import cl.costoStock.proveedor.ui.util.formatClp

@Composable
fun ProductsScreen(
    onMessage: (String) -> Unit,
    onPublish: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ProductsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect(onMessage) }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Eliminar publicación") },
            text = { Text("¿Quieres eliminar \"${product.name}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(product)
                    productToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { productToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (state.totalCount == 0 && !state.isLoading) {
        EmptyState(
            emoji = "📦",
            title = "Aún no tienes publicaciones",
            message = "Publica tu primer artículo y empieza a vender en CostoStock.",
            actionLabel = "Publicar artículo",
            onAction = onPublish
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // El espacio inferior deja libre el botón flotante "Publicar".
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") { SummaryCard(state.summary) }

        item(key = "search") {
            AppTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = "Buscar en mis publicaciones",
                leadingIcon = Icons.Filled.Search,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }

        item(key = "filters") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProductFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = { Text("${filter.label} (${state.filterCounts[filter] ?: 0})") }
                    )
                }
            }
        }

        if (state.products.isEmpty()) {
            item(key = "no-results") {
                Text(
                    text = "No hay publicaciones que coincidan con tu búsqueda.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onEdit = { onEdit(product.id) },
                onTogglePause = { viewModel.togglePause(product) },
                onDelete = { productToDelete = product },
                modifier = Modifier.animateItem()
            )
        }
    }
}

/* ------------------------------ Resumen ------------------------------ */

@Composable
private fun SummaryCard(summary: ProductsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Resumen de tus ventas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Activas", animatedInt(summary.active))
                Metric("Pausadas", animatedInt(summary.paused))
                Metric("Unidades", animatedInt(summary.units))
            }
            Metric("Valor en inventario", animatedClp(summary.inventoryValue), large = true)
        }
    }
}

/** Número que "sube" hasta su valor cuando cambia. */
@Composable
private fun animatedInt(target: Int): String {
    val value by animateIntAsState(target, tween(700), label = "metricInt")
    return value.toString()
}

@Composable
private fun animatedClp(target: Long): String {
    val value by animateFloatAsState(target.toFloat(), tween(700), label = "metricClp")
    // Al terminar la animación se muestra el valor exacto (un Float pierde precisión en montos grandes).
    return formatClp(if (value == target.toFloat()) target else value.toLong())
}

@Composable
private fun Metric(label: String, value: String, large: Boolean = false) {
    Column {
        Text(
            value,
            style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
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

/* ------------------------------ Tarjeta de publicación ------------------------------ */

@Composable
private fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paused = product.status == ProductStatus.PAUSED
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                ProductPhoto(
                    path = product.imagePaths.firstOrNull(),
                    emoji = product.emoji,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    maxSize = 300,
                    emojiSize = 44.sp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatClp(product.price),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${product.condition.label} · ${stockText(product)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            product.comuna,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusChip(product)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Editar")
                }
                TextButton(onClick = onTogglePause) {
                    Icon(
                        if (paused) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (paused) "Activar" else "Pausar")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar publicación",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun stockText(product: Product): String = when {
    product.isOutOfStock -> "Sin stock"
    product.stock == 1 -> "1 disponible"
    else -> "${product.stock} disponibles"
}
