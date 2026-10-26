@file:OptIn(ExperimentalMaterial3Api::class)

package cl.costoStock.myapplication.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.costoStock.myapplication.data.model.Product
import cl.costoStock.myapplication.nativeapi.rememberHaptics
import cl.costoStock.myapplication.ui.AppViewModelProvider
import cl.costoStock.myapplication.ui.components.CategoryAvatar
import cl.costoStock.myapplication.ui.components.QuantityStepper
import cl.costoStock.myapplication.ui.components.Wordmark
import cl.costoStock.myapplication.ui.theme.BrandDark
import cl.costoStock.myapplication.ui.util.categoryEmoji
import cl.costoStock.myapplication.ui.util.formatClp
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onMessage: (String) -> Unit,
    onGoToCart: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    var comunaMenuOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect(onMessage) }

    val addToCart: (Product) -> Unit = { product ->
        haptics.tick()
        viewModel.addToCart(product)
    }
    val changeQuantity: (Product, Int) -> Unit = { product, quantity ->
        haptics.tick()
        viewModel.changeQuantity(product, quantity)
    }

    Column(Modifier.fillMaxSize()) {
        HomeHeader(
            query = filters.query,
            onQueryChange = viewModel::onQueryChange,
            comuna = filters.comuna,
            comunas = state.comunas,
            menuOpen = comunaMenuOpen,
            onMenuChange = { comunaMenuOpen = it },
            onComunaSelected = viewModel::onComunaSelected
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.featured.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FeaturedBanner(
                        products = state.featured,
                        onOpen = { detail = it },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                QuickActions(
                    lowStockActive = filters.onlyLowStock,
                    cheapestActive = filters.order == SortOrder.PRICE_ASC,
                    onLowStock = viewModel::toggleLowStock,
                    onCheapest = {
                        viewModel.onOrderSelected(
                            if (filters.order == SortOrder.PRICE_ASC) SortOrder.RELEVANCE
                            else SortOrder.PRICE_ASC
                        )
                    },
                    onChooseComuna = { comunaMenuOpen = true },
                    onGoToCart = onGoToCart
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filters.category == null,
                            onClick = { viewModel.onCategorySelected(null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(state.categories) { category ->
                        FilterChip(
                            selected = filters.category == category,
                            onClick = { viewModel.onCategorySelected(category) },
                            label = { Text("${categoryEmoji(category)} $category") }
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ResultsHeader(
                    count = state.items.size,
                    filters = filters,
                    onOrderSelected = viewModel::onOrderSelected,
                    onClear = viewModel::clearFilters
                )
            }

            if (state.items.isEmpty() && !state.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyResults() }
            }

            itemsIndexed(state.items, key = { _, item -> item.product.id }) { index, item ->
                val isLeft = index % 2 == 0
                ProductCard(
                    item = item,
                    onClick = { detail = item.product },
                    onAdd = { addToCart(item.product) },
                    onQuantityChange = { q -> changeQuantity(item.product, q) },
                    modifier = Modifier.padding(
                        start = if (isLeft) 16.dp else 6.dp,
                        end = if (isLeft) 6.dp else 16.dp
                    )
                )
            }
        }
    }

    detail?.let { selected ->
        val live = state.items.firstOrNull { it.product.id == selected.id }?.product ?: selected
        ProductDetailSheet(
            product = live,
            quantity = state.inCart[live.id] ?: 0,
            onDismiss = { detail = null },
            onAdd = { addToCart(live) },
            onQuantityChange = { q -> changeQuantity(live, q) }
        )
    }
}

/* ---------------------------------- Header ---------------------------------- */

@Composable
private fun HomeHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    comuna: String?,
    comunas: List<String>,
    menuOpen: Boolean,
    onMenuChange: (Boolean) -> Unit,
    onComunaSelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Wordmark(fontSize = 24.sp, costoColor = BrandDark, stockColor = Color.White)
            Spacer(Modifier.weight(1f))
            Box {
                Row(
                    modifier = Modifier
                        .clickable { onMenuChange(true) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = comuna ?: "Todas las comunas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Elegir comuna",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuChange(false) }) {
                    DropdownMenuItem(
                        text = { Text("Todas las comunas") },
                        onClick = {
                            onComunaSelected(null)
                            onMenuChange(false)
                        }
                    )
                    comunas.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c) },
                            onClick = {
                                onComunaSelected(c)
                                onMenuChange(false)
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar productos...") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Borrar búsqueda", tint = BrandDark)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = BrandDark,
                unfocusedTextColor = BrandDark,
                focusedPlaceholderColor = Color(0xFF85746B),
                unfocusedPlaceholderColor = Color(0xFF85746B),
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/* ---------------------------------- Banner ---------------------------------- */

@Composable
private fun FeaturedBanner(
    products: List<Product>,
    onOpen: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState { products.size }

    LaunchedEffect(products.size) {
        while (true) {
            delay(4_000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % products.size)
        }
    }

    Column(modifier) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            BannerSlide(product = products[page], onOpen = { onOpen(products[page]) })
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(products.size) { i ->
                val selected = pagerState.currentPage == i
                val width by animateDpAsState(if (selected) 24.dp else 12.dp, label = "dotWidth")
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    label = "dotColor"
                )
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .width(width)
                        .height(4.dp)
                        .background(color, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun BannerSlide(product: Product, onOpen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.primary
                    )
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        Surface(
            modifier = Modifier.padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Destacado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(14.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 44.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { Text(product.emoji, fontSize = 56.sp) }
            Spacer(Modifier.width(12.dp))
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandDark,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatClp(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = onOpen,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) { Text("Ver producto") }
                }
            }
        }
    }
}

/* ------------------------------ Accesos rápidos ------------------------------ */

@Composable
private fun QuickActions(
    lowStockActive: Boolean,
    cheapestActive: Boolean,
    onLowStock: () -> Unit,
    onCheapest: () -> Unit,
    onChooseComuna: () -> Unit,
    onGoToCart: () -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(top = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickCard(
                icon = Icons.Filled.LocalFireDepartment,
                title = "Últimas unidades",
                subtitle = "Aprovecha antes de que se agoten",
                action = if (lowStockActive) "Quitar filtro" else "Ver productos",
                active = lowStockActive,
                onClick = onLowStock
            )
        }
        item {
            QuickCard(
                icon = Icons.Filled.Sell,
                title = "Menor precio",
                subtitle = "Ordena por los más económicos",
                action = if (cheapestActive) "Quitar orden" else "Ordenar",
                active = cheapestActive,
                onClick = onCheapest
            )
        }
        item {
            QuickCard(
                icon = Icons.Filled.LocationOn,
                title = "Cerca de ti",
                subtitle = "Elige tu comuna y compra local",
                action = "Elegir comuna",
                active = false,
                onClick = onChooseComuna
            )
        }
        item {
            QuickCard(
                icon = Icons.Filled.ShoppingCart,
                title = "Mi carrito",
                subtitle = "Revisa tus productos y paga",
                action = "Ir al carrito",
                active = false,
                onClick = onGoToCart
            )
        }
    }
}

@Composable
private fun QuickCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        label = "quickCardColor"
    )
    Card(
        onClick = onClick,
        modifier = Modifier.width(158.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2
            )
            Spacer(Modifier.height(8.dp))
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/* ------------------------------ Resultados ------------------------------ */

@Composable
private fun ResultsHeader(
    count: Int,
    filters: HomeFilters,
    onOrderSelected: (SortOrder) -> Unit,
    onClear: () -> Unit
) {
    var sortOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (count == 1) "1 producto" else "$count productos",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (filters.isActive) {
            TextButton(onClick = onClear) { Text("Limpiar") }
        }
        Box {
            TextButton(onClick = { sortOpen = true }) {
                Text(filters.order.label)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Ordenar")
            }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.label) },
                        onClick = {
                            onOrderSelected(order)
                            sortOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    item: ProductUi,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val product = item.product
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Text(product.emoji, fontSize = 60.sp)
            StockBadge(stock = product.stock, modifier = Modifier.align(Alignment.TopStart))
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatClp(product.price),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "IVA incluido",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    product.comuna,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            CartAction(
                quantity = item.inCart,
                stock = product.stock,
                onAdd = onAdd,
                onQuantityChange = onQuantityChange
            )
        }
    }
}

@Composable
private fun StockBadge(stock: Int, modifier: Modifier = Modifier) {
    val (text, container, content) = when {
        stock == 0 -> Triple(
            "Agotado",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        stock <= LOW_STOCK_THRESHOLD -> Triple(
            "¡Últimas unidades!",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> return
    }
    Surface(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        color = container,
        contentColor = content
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CartAction(
    quantity: Int,
    stock: Int,
    onAdd: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    if (stock == 0) {
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Sin stock")
        }
        return
    }
    AnimatedContent(
        targetState = quantity > 0,
        modifier = Modifier.fillMaxWidth(),
        label = "cartAction"
    ) { inCart ->
        if (!inCart) {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Agregar")
            }
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                QuantityStepper(
                    quantity = quantity,
                    canIncrement = quantity < stock,
                    onDecrement = { onQuantityChange(quantity - 1) },
                    onIncrement = { onQuantityChange(quantity + 1) }
                )
            }
        }
    }
}

@Composable
private fun EmptyResults() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(8.dp))
        Text("Sin resultados", style = MaterialTheme.typography.titleMedium)
        Text(
            "Prueba con otra búsqueda, comuna o categoría",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/* ------------------------------ Detalle del producto ------------------------------ */

@Composable
private fun ProductDetailSheet(
    product: Product,
    quantity: Int,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(product.emoji, fontSize = 88.sp)
                StockBadge(stock = product.stock, modifier = Modifier.align(Alignment.TopStart))
            }
            Text(product.name, style = MaterialTheme.typography.titleLarge)
            Text(
                formatClp(product.price),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "IVA incluido",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(product.emoji, size = 28)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${product.category} · ${product.comuna} · ${product.stock} disponibles",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            CartAction(
                quantity = quantity,
                stock = product.stock,
                onAdd = onAdd,
                onQuantityChange = onQuantityChange
            )
        }
    }
}
