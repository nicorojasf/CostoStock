package cl.costoStock.myapplication.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.data.model.EconomicIndicators
import cl.costoStock.myapplication.data.repository.CartRepository
import cl.costoStock.myapplication.data.repository.CartUpdateResult
import cl.costoStock.myapplication.data.repository.IndicatorsRepository
import cl.costoStock.myapplication.data.repository.ProductRepository
import cl.costoStock.myapplication.domain.cart.CartCalculator
import cl.costoStock.myapplication.domain.cart.CartSummary
import cl.costoStock.myapplication.domain.cart.CheckoutResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartUiState(
    val lines: List<CartLine> = emptyList(),
    val summary: CartSummary = CartSummary.EMPTY,
    val isProcessing: Boolean = false,
    /** Resumen de la compra confirmada; mientras no sea null se muestra el diálogo. */
    val confirmation: CartSummary? = null,
    /** Para mostrar el total equivalente en dólares y UF. */
    val indicators: EconomicIndicators? = null
) {
    val hasStockIssues: Boolean get() = lines.any { it.quantity > it.product.stock }
}

class CartViewModel(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val indicatorsRepository: IndicatorsRepository
) : ViewModel() {

    private val processing = MutableStateFlow(false)
    private val confirmation = MutableStateFlow<CartSummary?>(null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val state: StateFlow<CartUiState> = combine(
        cartRepository.observeCart(), processing, confirmation, indicatorsRepository.indicators
    ) { lines, isProcessing, confirmed, indicators ->
        CartUiState(
            lines = lines,
            summary = CartCalculator.summarize(lines),
            isProcessing = isProcessing,
            confirmation = confirmed,
            indicators = indicators
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    init {
        // Stock actualizado desde ms-productos al abrir el carrito.
        viewModelScope.launch { productRepository.refresh() }
        viewModelScope.launch { indicatorsRepository.refresh() }
    }

    fun changeQuantity(line: CartLine, newQuantity: Int) {
        viewModelScope.launch {
            val result = cartRepository.setQuantity(line.product.id, newQuantity)
            if (result is CartUpdateResult.StockLimit) {
                _messages.tryEmit("Solo hay ${result.available} unidades disponibles")
            }
        }
    }

    fun remove(line: CartLine) {
        viewModelScope.launch {
            cartRepository.remove(line.product.id)
            _messages.tryEmit("«${line.product.name}» eliminado del carrito")
        }
    }

    fun clearCart() {
        viewModelScope.launch { cartRepository.clear() }
    }

    fun checkout() {
        if (processing.value) return
        processing.value = true
        viewModelScope.launch {
            when (val result = cartRepository.checkout()) {
                is CheckoutResult.Success -> confirmation.value = result.summary
                CheckoutResult.Empty -> _messages.tryEmit("Tu carrito está vacío")
                is CheckoutResult.OutOfStock -> _messages.tryEmit(
                    "Stock insuficiente: ${result.productNames.joinToString()}. Ajusta las cantidades."
                )
                is CheckoutResult.Failure -> _messages.tryEmit(result.message)
            }
            processing.value = false
        }
    }

    fun dismissConfirmation() {
        confirmation.value = null
    }
}
