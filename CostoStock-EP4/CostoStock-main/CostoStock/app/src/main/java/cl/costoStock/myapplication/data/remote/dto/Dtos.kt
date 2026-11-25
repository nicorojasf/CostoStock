package cl.costoStock.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

/*
 * Objetos que viajan por la red (JSON). Los campos de respuesta son nullables porque Gson no
 * garantiza que vengan; el mapeo a los modelos de la app se hace en Mappers.kt.
 */

// ------------------------------- ms-usuarios (puerto 8081) -------------------------------

data class ClienteRegistroRequest(val nombre: String, val email: String, val password: String)

data class LoginRequest(val email: String, val password: String)

data class ClienteUpdateRequest(val nombre: String)

data class ClienteDto(
    val id: Long?,
    val nombre: String?,
    val email: String?,
    val creadoEn: Long?
)

// ------------------------------- ms-productos (puerto 8082) -------------------------------

data class ProductoDto(
    val id: Long?,
    val proveedorId: Long?,
    val nombre: String?,
    val categoria: String?,
    val descripcion: String?,
    val precio: Int?,
    val stock: Int?,
    val comuna: String?,
    val emoji: String?,
    val condicion: String?,
    val imagenes: List<String>?,
    val estado: String?
)

// ------------------------------- ms-pedidos (puerto 8083) -------------------------------

data class PedidoItemRequest(val productoId: Long, val cantidad: Int)

data class PedidoRequest(val clienteId: Long, val items: List<PedidoItemRequest>)

data class PedidoItemDto(
    val productoId: Long?,
    val nombreProducto: String?,
    val emoji: String?,
    val precioUnitario: Int?,
    val cantidad: Int?,
    val subtotal: Int?
)

data class PedidoDto(
    val id: Long?,
    val clienteId: Long?,
    val creadoEn: Long?,
    val neto: Int?,
    val iva: Int?,
    val total: Int?,
    val estado: String?,
    val cantidadTotal: Int?,
    val items: List<PedidoItemDto>?
)

/** Formato de error común de los tres microservicios. */
data class ErrorDto(
    val status: Int?,
    val mensaje: String?,
    val campos: Map<String, String>?,
    val detalles: List<String>?
)

// ------------------------------- API externa: mindicador.cl -------------------------------

data class IndicadoresDto(
    val fecha: String?,
    val uf: IndicadorDto?,
    val dolar: IndicadorDto?,
    val euro: IndicadorDto?,
    val utm: IndicadorDto?
)

data class IndicadorDto(
    val codigo: String?,
    val nombre: String?,
    @SerializedName("unidad_medida") val unidadMedida: String?,
    val fecha: String?,
    val valor: Double?
)
