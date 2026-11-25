package cl.costoStock.myapplication.data.model

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val photoPath: String?
)

data class Product(
    val id: Long,
    val name: String,
    val category: String,
    val description: String,
    /** Precio final en CLP (IVA incluido). */
    val price: Int,
    val stock: Int,
    val comuna: String,
    val emoji: String
)

data class CartLine(
    val product: Product,
    val quantity: Int
) {
    val subtotal: Int get() = product.price * quantity
}

enum class OrderStatus(val label: String) {
    CONFIRMED("Confirmado"),
    CANCELLED("Cancelado")
}

data class OrderLine(
    val productId: Long,
    val name: String,
    val emoji: String,
    val unitPrice: Int,
    val quantity: Int
) {
    val subtotal: Int get() = unitPrice * quantity
}

/** Compra guardada en ms-pedidos. */
data class Order(
    val id: Long,
    val createdAt: Long,
    val net: Int,
    val iva: Int,
    val total: Int,
    val status: OrderStatus,
    val itemCount: Int,
    val lines: List<OrderLine>
)

/** Indicadores económicos del día obtenidos de la API externa mindicador.cl. */
data class EconomicIndicators(
    /** Valor del dólar observado en CLP. */
    val dolar: Double,
    val euro: Double?,
    /** Valor de la UF en CLP. */
    val uf: Double,
    val utm: Double?,
    /** Fecha ISO del dato, ej: 2026-11-20T03:00:00.000Z */
    val date: String
)
