package cl.costoStock.proveedor.data.remote.dto

import com.google.gson.annotations.SerializedName

/*
 * Objetos que viajan por la red (JSON). Los campos de respuesta son nullables porque Gson no
 * garantiza que vengan; el mapeo a los modelos de la app se hace en Mappers.kt.
 */

// ------------------------------- ms-usuarios (puerto 8081) -------------------------------

data class ProveedorRegistroRequest(
    val nombre: String,
    val rut: String,
    val email: String,
    val telefono: String,
    val password: String
)

data class LoginRequest(val email: String, val password: String)

data class ProveedorUpdateRequest(val nombre: String, val telefono: String)

data class ProveedorDto(
    val id: Long?,
    val nombre: String?,
    val rut: String?,
    val email: String?,
    val telefono: String?,
    val creadoEn: Long?,
    /** Solo en login/registro: token firmado que se envía en Authorization. */
    val token: String? = null,
    /** Solo en login/registro: PROVEEDOR. */
    val rol: String? = null
)

/** Recuperación de contraseña: tipo = PROVEEDOR (esta app) o CLIENTE. */
data class SolicitarCodigoRequest(val email: String, val tipo: String)

data class RestablecerRequest(
    val email: String,
    val tipo: String,
    val codigo: String,
    val nuevaPassword: String
)

data class MensajeDto(val mensaje: String?)

// ------------------------------- ms-productos (puerto 8082) -------------------------------

data class ProductoRequest(
    val proveedorId: Long,
    val nombre: String,
    val categoria: String,
    val descripcion: String,
    val precio: Int,
    val stock: Int,
    val comuna: String,
    val emoji: String,
    val condicion: String,
    val imagenes: List<String>
)

data class EstadoRequest(val estado: String)

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
    val estado: String?,
    val creadoEn: Long?,
    val actualizadoEn: Long?
)

/** Formato de error común de los microservicios. */
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
