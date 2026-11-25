package cl.costoStock.proveedor.testutil

import cl.costoStock.proveedor.data.model.EconomicIndicators
import cl.costoStock.proveedor.data.model.Product
import cl.costoStock.proveedor.data.model.ProductCondition
import cl.costoStock.proveedor.data.model.ProductDraft
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.remote.dto.ProductoDto
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

fun product(
    id: Long,
    price: Int = 10_000,
    stock: Int = 5,
    status: ProductStatus = ProductStatus.ACTIVE,
    name: String = "Producto número $id",
    imagePaths: List<String> = listOf("/fotos/$id.jpg")
) = Product(
    id = id, sellerId = 1L, name = name, category = "Hogar", description = "Descripción del producto $id",
    price = price, stock = stock, comuna = "Santiago", emoji = "🏠", condition = ProductCondition.NEW,
    imagePaths = imagePaths, status = status, createdAt = 0L, updatedAt = 0L
)

fun productoDto(id: Long?, estado: String = "ACTIVE", condicion: String = "NEW", proveedorId: Long = 1L) = ProductoDto(
    id = id, proveedorId = proveedorId, nombre = "Producto $id", categoria = "Hogar", descripcion = "desc",
    precio = 10_000, stock = 5, comuna = "Santiago", emoji = "🏠", condicion = condicion,
    imagenes = listOf("/fotos/$id.jpg"), estado = estado, creadoEn = 1L, actualizadoEn = 2L
)

fun draft(paths: List<String> = listOf("/cache/tmp.jpg")) = ProductDraft(
    name = "Lámpara de escritorio LED",
    category = "Hogar",
    description = "Lámpara con tres niveles de luz y brazo flexible",
    price = 12_990,
    stock = 4,
    comuna = "Maipú",
    condition = ProductCondition.USED,
    imagePaths = paths
)

val indicators = EconomicIndicators(dolar = 1000.0, euro = 1100.0, uf = 40000.0, utm = null, date = "2026-11-20T03:00:00.000Z")

fun jsonBody(json: String): ResponseBody = json.toResponseBody("application/json".toMediaType())
