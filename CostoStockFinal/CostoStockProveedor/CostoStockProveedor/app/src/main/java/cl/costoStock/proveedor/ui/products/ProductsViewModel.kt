package cl.costoStock.proveedor.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.proveedor.data.model.Product
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.model.EconomicIndicators
import cl.costoStock.proveedor.data.remote.ApiErrors
import cl.costoStock.proveedor.data.repository.IndicatorsRepository
import cl.costoStock.proveedor.data.repository.ProductRepository
import cl.costoStock.proveedor.data.repository.SyncState
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
    val isLoading: Boolean = true,
    /** Error al conectar con ms-productos. */
    val error: String? = null,
    /** Dólar y UF del día (API externa mindicador.cl) para mostrar el inventario en US$/UF. */
    val indicators: EconomicIndicators? = null
)

class ProductsViewModel(
    private val productRepository: ProductRepository,
    private val indicatorsRepository: IndicatorsRepository
) : ViewModel() {

    private val filter = MutableStateFlow(ProductFilter.ALL)
    private val query = MutableStateFlow("")

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val state: StateFlow<ProductsUiState> = combine(
        productRepository.observeMyProducts(),
        filter,
        query,
        productRepository.sync,
        indicatorsRepository.indicators
    ) { products, currentFilter, currentQuery, sync, indicators ->
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
            isLoading = sync is SyncState.Loading && products.isEmpty(),
            error = (sync as? SyncState.Error)?.message,
            indicators = indicators
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductsUiState())

    init {
        viewModelScope.launch { indicatorsRepository.refresh() }
    }

    fun onFilterChange(value: ProductFilter) = filter.update { value }

    /** Botón "Reintentar" cuando no hay conexión con ms-productos. */
    fun retry() {
        viewModelScope.launch {
            if (productRepository.refresh()) _messages.tryEmit("Publicaciones actualizadas")
            indicatorsRepository.refresh(force = true)
        }
    }
    fun onQueryChange(value: String) = query.update { value }

    fun togglePause(product: Product) {
        viewModelScope.launch {
            val newStatus =
                if (product.status == ProductStatus.ACTIVE) ProductStatus.PAUSED else ProductStatus.ACTIVE
            val ok = productRepository.setStatus(product.id, newStatus)
            _messages.tryEmit(
                when {
                    !ok -> productRepository.lastError ?: ApiErrors.UNEXPECTED
                    newStatus == ProductStatus.PAUSED -> "Publicación pausada"
                    else -> "Publicación activada"
                }
            )
        }
    }

    fun delete(product: Product) {
        viewModelScope.launch {
            val ok = productRepository.delete(product.id)
            _messages.tryEmit(
                if (ok) "Publicación eliminada" else productRepository.lastError ?: ApiErrors.UNEXPECTED
            )
        }
    }

    private fun matches(product: Product, filter: ProductFilter): Boolean = when (filter) {
        ProductFilter.ALL -> true
        ProductFilter.ACTIVE -> product.status == ProductStatus.ACTIVE && !product.isOutOfStock
        ProductFilter.PAUSED -> product.status == ProductStatus.PAUSED
        ProductFilter.OUT_OF_STOCK -> product.isOutOfStock
    }
}
