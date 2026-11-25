package cl.costoStock.proveedor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Notifica a los repositorios que algo cambió en SQLite, para que los Flow
 * vuelvan a consultar la base y la UI se actualice sola.
 */
class DataChangeNotifier {
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun notifyChanged() = _version.update { it + 1 }
}
