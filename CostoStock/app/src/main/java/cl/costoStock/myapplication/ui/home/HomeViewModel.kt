package cl.costoStock.myapplication.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.myapplication.data.model.Product
import cl.costoStock.myapplication.data.repository.CartRepository
import cl.costoStock.myapplication.data.repository.CartUpdateResult
import cl.costoStock.myapplication.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Con este stock o menos (y mayor a 0) se muestra "¡Últimas unidades!". */
const val LOW_STOCK_THRESHOLD = 10

enum class SortOrder(val label: String) {
    RELEVANCE("Relevantes"),
    PRICE_ASC("Menor precio"),
    PRICE_DESC("Mayor precio")
}

data class HomeFilters(
    val query: String = "",
    val category: String? = null,
    val comuna: String? = null,
    val order: SortOrder = SortOrder.RELEVANCE,
    val onlyLowStock: Boolean = false
) {
    val isActive: Boolean
        get() = query.isNotBlank() || category != null || comuna != null ||
            order != SortOrder.RELEVANCE || onlyLowStock
}

data class ProductUi(val product: Product, val inCart: Int)

data class HomeUiState(
    val categories: List<String> = emptyList(),
    val comunas: List<String> = emptyList(),
    /** Productos destacados para el carrusel principal. */
    val featured: List<Product> = emptyList(),
    val items: List<ProductUi> = emptyList(),
    /** Cantidad en el carrito por id de producto. */
    val inCart: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _filters = MutableStateFlow(HomeFilters())
    val filters: StateFlow<HomeFilters> = _filters.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val state: StateFlow<HomeUiState> = combine(
        productRepository.observeProducts(),
        cartRepository.observeCart(),
        _filters
    ) { products, lines, f ->
        val inCart = lines.associate { it.product.id to it.quantity }
        val text = f.query.trim()
        val filtered = products.filter { p ->
            (f.category == null || p.category == f.category) &&
                (f.comuna == null || p.comuna == f.comuna) &&
                (!f.onlyLowStock || p.stock in 1..LOW_STOCK_THRESHOLD) &&
                (text.isEmpty() ||
                    p.name.contains(text, ignoreCase = true) ||
                    p.description.contains(text, ignoreCase = true) ||
                    p.category.contains(text, ignoreCase = true))
        }
        val sorted = when (f.order) {
            SortOrder.RELEVANCE -> filtered
            SortOrder.PRICE_ASC -> filtered.sortedBy { it.price }
            SortOrder.PRICE_DESC -> filtered.sortedByDescending { it.price }
        }
        HomeUiState(
            categories = products.map { it.category }.distinct(),
            comunas = products.map { it.comuna }.distinct().sorted(),
            featured = products.filter { it.stock > 0 }.take(5),
            items = sorted.map { ProductUi(it, inCart[it.id] ?: 0) },
            inCart = inCart,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) = _filters.update { it.copy(query = value) }
    fun onCategorySelected(category: String?) = _filters.update { it.copy(category = category) }
    fun onComunaSelected(comuna: String?) = _filters.update { it.copy(comuna = comuna) }
    fun onOrderSelected(order: SortOrder) = _filters.update { it.copy(order = order) }
    fun toggleLowStock() = _filters.update { it.copy(onlyLowStock = !it.onlyLowStock) }
    fun clearFilters() {
        _filters.value = HomeFilters()
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            when (val result = cartRepository.addProduct(product.id)) {
                CartUpdateResult.Ok -> _messages.tryEmit("«${product.name}» agregado al carrito")
                is CartUpdateResult.StockLimit ->
                    _messages.tryEmit("Stock máximo disponible: ${result.available} unidades")
                CartUpdateResult.NotAvailable ->
                    _messages.tryEmit("No se pudo agregar el producto")
            }
        }
    }

    fun changeQuantity(product: Product, newQuantity: Int) {
        viewModelScope.launch {
            val result = cartRepository.setQuantity(product.id, newQuantity)
            if (result is CartUpdateResult.StockLimit) {
                _messages.tryEmit("Stock máximo disponible: ${result.available} unidades")
            }
        }
    }
}
