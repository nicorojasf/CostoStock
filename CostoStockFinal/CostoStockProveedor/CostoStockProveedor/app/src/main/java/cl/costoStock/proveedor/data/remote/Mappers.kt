package cl.costoStock.proveedor.data.remote

import cl.costoStock.proveedor.data.model.EconomicIndicators
import cl.costoStock.proveedor.data.model.Product
import cl.costoStock.proveedor.data.model.ProductCondition
import cl.costoStock.proveedor.data.model.ProductDraft
import cl.costoStock.proveedor.data.model.ProductStatus
import cl.costoStock.proveedor.data.model.Seller
import cl.costoStock.proveedor.data.remote.dto.IndicadoresDto
import cl.costoStock.proveedor.data.remote.dto.ProductoDto
import cl.costoStock.proveedor.data.remote.dto.ProductoRequest
import cl.costoStock.proveedor.data.remote.dto.ProveedorDto

// Convierte entre los DTO de red y los modelos de la app. Devuelven null si falta un dato esencial.

fun ProveedorDto.toSeller(): Seller? {
    val id = id ?: return null
    return Seller(
        id = id,
        name = nombre.orEmpty(),
        rut = rut.orEmpty(),
        email = email.orEmpty(),
        phone = telefono.orEmpty(),
        photoPath = null,
        createdAt = creadoEn ?: 0L
    )
}

fun ProductoDto.toProduct(): Product? {
    val id = id ?: return null
    return Product(
        id = id,
        sellerId = proveedorId ?: 0L,
        name = nombre.orEmpty(),
        category = categoria ?: "Otros",
        description = descripcion.orEmpty(),
        price = precio ?: 0,
        stock = stock ?: 0,
        comuna = comuna.orEmpty(),
        emoji = emoji ?: "🛒",
        condition = if (condicion == ProductCondition.USED.name) ProductCondition.USED else ProductCondition.NEW,
        imagePaths = imagenes.orEmpty(),
        status = if (estado == ProductStatus.PAUSED.name) ProductStatus.PAUSED else ProductStatus.ACTIVE,
        createdAt = creadoEn ?: 0L,
        updatedAt = actualizadoEn ?: 0L
    )
}

fun ProductDraft.toRequest(sellerId: Long, emoji: String, imagePaths: List<String>) = ProductoRequest(
    proveedorId = sellerId,
    nombre = name,
    categoria = category,
    descripcion = description,
    precio = price,
    stock = stock,
    comuna = comuna,
    emoji = emoji,
    condicion = condition.name,
    imagenes = imagePaths
)

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
