package cl.costoStock.myapplication.data.remote

import cl.costoStock.myapplication.data.model.EconomicIndicators
import cl.costoStock.myapplication.data.model.Order
import cl.costoStock.myapplication.data.model.OrderLine
import cl.costoStock.myapplication.data.model.OrderStatus
import cl.costoStock.myapplication.data.model.Product
import cl.costoStock.myapplication.data.model.User
import cl.costoStock.myapplication.data.remote.dto.ClienteDto
import cl.costoStock.myapplication.data.remote.dto.IndicadoresDto
import cl.costoStock.myapplication.data.remote.dto.PedidoDto
import cl.costoStock.myapplication.data.remote.dto.ProductoDto

// Convierte los DTO de red en modelos de la app. Devuelven null si falta un dato esencial.

fun ClienteDto.toUser(): User? {
    val id = id ?: return null
    return User(id = id, name = nombre.orEmpty(), email = email.orEmpty(), photoPath = null)
}

fun ProductoDto.toProduct(): Product? {
    val id = id ?: return null
    return Product(
        id = id,
        name = nombre.orEmpty(),
        category = categoria ?: "Otros",
        description = descripcion.orEmpty(),
        price = precio ?: 0,
        stock = stock ?: 0,
        comuna = comuna.orEmpty(),
        emoji = emoji ?: "🛒"
    )
}

/** El comprador solo ve publicaciones activas (las pausadas por el proveedor se ocultan). */
val ProductoDto.isVisibleForBuyers: Boolean get() = estado == null || estado == "ACTIVE"

fun PedidoDto.toOrder(): Order? {
    val id = id ?: return null
    val lines = items.orEmpty().mapNotNull { item ->
        val productId = item.productoId ?: return@mapNotNull null
        OrderLine(
            productId = productId,
            name = item.nombreProducto.orEmpty(),
            emoji = item.emoji ?: "🛒",
            unitPrice = item.precioUnitario ?: 0,
            quantity = item.cantidad ?: 0
        )
    }
    return Order(
        id = id,
        createdAt = creadoEn ?: 0L,
        net = neto ?: 0,
        iva = iva ?: 0,
        total = total ?: 0,
        status = if (estado == "CANCELADO") OrderStatus.CANCELLED else OrderStatus.CONFIRMED,
        itemCount = cantidadTotal ?: lines.sumOf { it.quantity },
        lines = lines
    )
}

fun IndicadoresDto.toIndicators(): EconomicIndicators? {
    val dolarValue = dolar?.valor ?: return null
    val ufValue = uf?.valor ?: return null
    return EconomicIndicators(
        dolar = dolarValue,
        euro = euro?.valor,
        uf = ufValue,
        utm = utm?.valor,
        date = dolar?.fecha ?: fecha.orEmpty()
    )
}
