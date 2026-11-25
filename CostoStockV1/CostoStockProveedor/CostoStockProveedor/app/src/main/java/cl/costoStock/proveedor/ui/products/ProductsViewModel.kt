package cl.costoStock.proveedor.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.proveedor.data.model.Product
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProductFilter(val label: String) {
    ALL("Todas"),
    ACTIVE("Activas"),
    PAUSED("Pausadas"),
    OUT_OF_STOCK("Sin stock")
}

data class ProductsSummary(
    val active: Int = 0,
    val paused: Int = 0,
    val units: Int = 0,
    /** Valor de lo que tienes en stock (precio × unidades). */
    val inventoryValue: Long = 0
)

data class ProductsUiState(
    /** Publicaciones ya filtradas por estado y búsqueda. */
    val products: List<Product> = emptyList(),
    val totalCount: Int = 0,
    val filterCounts: Map<ProductFilter, Int> = emptyMap(),
    val summary: ProductsSummary = ProductsSummary(),
    val filter: ProductFilter = ProductFilter.ALL,
    val query: String = "",
    val isLoading: Boolean = true
)

class ProductsViewModel(private val productRepository: ProductRepository) : ViewModel() {

    private val filter = MutableStateFlow(ProductFilter.ALL)
    private val query = MutableStateFlow("")

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val state: StateFlow<ProductsUiState> = combine(
        productRepository.observeMyProducts(), filter, query
    ) { products, currentFilter, currentQuery ->
        val counts = ProductFilter.entries.associateWith { f -> products.count { matches(it, f) } }
        val visible = products
            .filter { matches(it, currentFilter) }
            .filter { currentQuery.isBlank() || it.name.contains(currentQuery.trim(), ignoreCase = true) }
        ProductsUiState(
            products = visible,
            totalCount = products.size,
            filterCounts = counts,
            summary = ProductsSummary(
                active = counts[ProductFilter.ACTIVE] ?: 0,
                paused = counts[ProductFilter.PAUSED] ?: 0,
                units = products.sumOf { it.stock },
                inventoryValue = products.sumOf { it.price.toLong() * it.stock }
            ),
            filter = currentFilter,
            query = currentQuery,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductsUiState())

    fun onFilterChange(value: ProductFilter) = filter.update { value }
    fun onQueryChange(value: String) = query.update { value }

    fun togglePause(product: Product) {
        viewModelScope.launch {
            val newStatus =
                if (product.status == ProductStatus.ACTIVE) ProductStatus.PAUSED else ProductStatus.ACTIVE
            productRepository.setStatus(product.id, newStatus)
            _messages.tryEmit(
                if (newStatus == ProductStatus.PAUSED) "Publicación pausada" else "Publicación activada"
            )
        }
    }

    fun delete(product: Product) {
        viewModelScope.launch {
            productRepository.delete(product.id)
            _messages.tryEmit("Publicación eliminada")
        }
    }

    private fun matches(product: Product, filter: ProductFilter): Boolean = when (filter) {
        ProductFilter.ALL -> true
        ProductFilter.ACTIVE -> product.status == ProductStatus.ACTIVE && !product.isOutOfStock
        ProductFilter.PAUSED -> product.status == ProductStatus.PAUSED
        ProductFilter.OUT_OF_STOCK -> product.isOutOfStock
    }
}
