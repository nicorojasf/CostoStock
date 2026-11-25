package cl.costoStock.proveedor.data.remote

import cl.costoStock.proveedor.data.remote.dto.EstadoRequest
import cl.costoStock.proveedor.data.remote.dto.IndicadoresDto
import cl.costoStock.proveedor.data.remote.dto.LoginRequest
import cl.costoStock.proveedor.data.remote.dto.MensajeDto
import cl.costoStock.proveedor.data.remote.dto.ProductoDto
import cl.costoStock.proveedor.data.remote.dto.ProductoRequest
import cl.costoStock.proveedor.data.remote.dto.ProveedorDto
import cl.costoStock.proveedor.data.remote.dto.ProveedorRegistroRequest
import cl.costoStock.proveedor.data.remote.dto.ProveedorUpdateRequest
import cl.costoStock.proveedor.data.remote.dto.RestablecerRequest
import cl.costoStock.proveedor.data.remote.dto.SolicitarCodigoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Microservicio ms-usuarios (proveedores). */
interface UsuariosApi {
    @POST("api/proveedores/registro")
    suspend fun registrar(@Body body: ProveedorRegistroRequest): Response<ProveedorDto>

    @POST("api/proveedores/login")
    suspend fun login(@Body body: LoginRequest): Response<ProveedorDto>

    @PUT("api/proveedores/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body body: ProveedorUpdateRequest): Response<ProveedorDto>

    /** Recuperar contraseña, paso 1: ms-usuarios genera un código de 6 dígitos y lo envía. */
    @POST("api/recuperacion/solicitar")
    suspend fun solicitarCodigo(@Body body: SolicitarCodigoRequest): Response<MensajeDto>

    /** Recuperar contraseña, paso 2: cambia la contraseña si el código es correcto. */
    @POST("api/recuperacion/restablecer")
    suspend fun restablecer(@Body body: RestablecerRequest): Response<MensajeDto>
}

/** Microservicio ms-productos: CRUD completo de las publicaciones del proveedor. */
interface ProductosApi {
    @GET("api/productos")
    suspend fun listarPorProveedor(@Query("proveedorId") proveedorId: Long): List<ProductoDto>

    @GET("api/productos/{id}")
    suspend fun obtener(@Path("id") id: Long): Response<ProductoDto>

    @POST("api/productos")
    suspend fun crear(@Body body: ProductoRequest): Response<ProductoDto>

    @PUT("api/productos/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body body: ProductoRequest): Response<ProductoDto>

    @PATCH("api/productos/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: Long, @Body body: EstadoRequest): Response<ProductoDto>

    @DELETE("api/productos/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}

/** API externa pública: https://mindicador.cl/api (indicadores económicos de Chile). */
interface IndicadoresApi {
    @GET("api")
    suspend fun obtener(): IndicadoresDto
}
