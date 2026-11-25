package cl.costoStock.myapplication.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.myapplication.data.model.Order
import cl.costoStock.myapplication.data.model.OrderStatus
import cl.costoStock.myapplication.data.repository.OrderRepository
import cl.costoStock.myapplication.data.repository.Outcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Pedido que se está cancelando o eliminando (para mostrar progreso en su tarjeta). */
    val busyOrderId: Long? = null
) {
    val totalSpent: Int get() = orders.filter { it.status == OrderStatus.CONFIRMED }.sumOf { it.total }
}

/** Pantalla "Mis pedidos": lista, cancela y elimina compras en ms-pedidos. */
class OrdersViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.myOrders()) {
                is Outcome.Ok -> _state.update { it.copy(orders = result.value, isLoading = false) }
                is Outcome.Fail -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun cancel(order: Order) {
        if (_state.value.busyOrderId != null) return
        _state.update { it.copy(busyOrderId = order.id) }
        viewModelScope.launch {
            when (val result = repository.cancel(order.id)) {
                is Outcome.Ok -> {
                    val updated = result.value
                    _state.update { s -> s.copy(orders = s.orders.map { if (it.id == updated.id) updated else it }) }
                    _messages.tryEmit("Pedido #${order.id} cancelado. El stock fue devuelto")
                }
                is Outcome.Fail -> _messages.tryEmit(result.message)
            }
            _state.update { it.copy(busyOrderId = null) }
        }
    }

    fun delete(order: Order) {
        if (_state.value.busyOrderId != null) return
        _state.update { it.copy(busyOrderId = order.id) }
        viewModelScope.launch {
            when (val result = repository.delete(order.id)) {
                is Outcome.Ok -> {
                    _state.update { s -> s.copy(orders = s.orders.filterNot { it.id == order.id }) }
                    _messages.tryEmit("Pedido #${order.id} eliminado del historial")
                }
                is Outcome.Fail -> _messages.tryEmit(result.message)
            }
            _state.update { it.copy(busyOrderId = null) }
        }
    }
}
