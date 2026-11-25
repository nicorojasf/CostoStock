package cl.costoStock.myapplication.testutil

import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.data.model.EconomicIndicators
import cl.costoStock.myapplication.data.model.Order
import cl.costoStock.myapplication.data.model.OrderLine
import cl.costoStock.myapplication.data.model.OrderStatus
import cl.costoStock.myapplication.data.model.Product
import cl.costoStock.myapplication.data.remote.dto.ProductoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Reemplaza Dispatchers.Main (que no existe en la JVM) por un dispatcher de pruebas. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

fun product(id: Long, price: Int = 1000, stock: Int = 10, category: String = "Hogar", comuna: String = "Santiago") =
    Product(id, "Producto $id", category, "Descripción $id", price, stock, comuna, "🏠")

fun productoDto(id: Long?, estado: String? = "ACTIVE", precio: Int? = 1000, stock: Int? = 5) = ProductoDto(
    id = id, proveedorId = 1L, nombre = "Producto $id", categoria = "Hogar", descripcion = "desc",
    precio = precio, stock = stock, comuna = "Santiago", emoji = "🏠", condicion = "NEW",
    imagenes = emptyList(), estado = estado
)

fun line(id: Long, quantity: Int, price: Int = 1000, stock: Int = 10) =
    CartLine(product(id, price, stock), quantity)

fun order(id: Long, status: OrderStatus = OrderStatus.CONFIRMED, total: Int = 2000) = Order(
    id = id, createdAt = 0L, net = 1681, iva = 319, total = total, status = status, itemCount = 2,
    lines = listOf(OrderLine(1L, "Lámpara", "💡", 1000, 2))
)

val indicators = EconomicIndicators(dolar = 1000.0, euro = 1100.0, uf = 40000.0, utm = 68000.0, date = "2026-11-20T03:00:00.000Z")

fun jsonBody(json: String): ResponseBody = json.toResponseBody("application/json".toMediaType())
