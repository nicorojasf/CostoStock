package cl.costoStock.proveedor.data.model

/** Proveedor (vendedor) con sesión en la app. */
data class Seller(
    val id: Long,
    /** Nombre del comercio o del vendedor. */
    val name: String,
    /** RUT normalizado, ej: 12345678-5 */
    val rut: String,
    val email: String,
    /** Teléfono normalizado, ej: +56912345678 */
    val phone: String,
    val photoPath: String?,
    val createdAt: Long
)

enum class ProductStatus { ACTIVE, PAUSED }

enum class ProductCondition(val label: String) {
    NEW("Nuevo"),
    USED("Usado")
}

/**
 * Publicación de un proveedor. Incluye TODAS las columnas del `Product` de la app de cliente
 * (id, name, category, description, price, stock, comuna, emoji) más datos propios del
 * proveedor (sellerId, condition, imagePaths, status, fechas). Ver docs/contrato-datos.md.
 */
data class Product(
    val id: Long,
    val sellerId: Long,
    val name: String,
    val category: String,
    val description: String,
    /** Precio final en CLP (IVA incluido), igual que en la app de cliente. */
    val price: Int,
    val stock: Int,
    val comuna: String,
    val emoji: String,
    val condition: ProductCondition,
    /** Rutas de las fotos (la primera es la portada). */
    val imagePaths: List<String>,
    val status: ProductStatus,
    val createdAt: Long,
    val updatedAt: Long
) {
    val isOutOfStock: Boolean get() = stock == 0
}

/** Datos que el proveedor completa en el formulario de publicación. */
data class ProductDraft(
    val name: String,
    val category: String,
    val description: String,
    val price: Int,
    val stock: Int,
    val comuna: String,
    val condition: ProductCondition,
    val imagePaths: List<String>
)
