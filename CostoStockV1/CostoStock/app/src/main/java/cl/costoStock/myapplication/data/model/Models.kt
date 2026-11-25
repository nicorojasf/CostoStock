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
