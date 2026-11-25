package cl.costoStock.myapplication.data.remote

import cl.costoStock.myapplication.data.remote.dto.ClienteDto
import cl.costoStock.myapplication.data.remote.dto.ClienteRegistroRequest
import cl.costoStock.myapplication.data.remote.dto.ClienteUpdateRequest
import cl.costoStock.myapplication.data.remote.dto.IndicadoresDto
import cl.costoStock.myapplication.data.remote.dto.LoginRequest
import cl.costoStock.myapplication.data.remote.dto.PedidoDto
import cl.costoStock.myapplication.data.remote.dto.PedidoRequest
import cl.costoStock.myapplication.data.remote.dto.ProductoDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Microservicio ms-usuarios (clientes). */
interface UsuariosApi {
    @POST("api/clientes/registro")
    suspend fun registrar(@Body body: ClienteRegistroRequest): Response<ClienteDto>

    @POST("api/clientes/login")
    suspend fun login(@Body body: LoginRequest): Response<ClienteDto>

    @GET("api/clientes/{id}")
    suspend fun obtener(@Path("id") id: Long): Response<ClienteDto>

    @PUT("api/clientes/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body body: ClienteUpdateRequest): Response<ClienteDto>

    @DELETE("api/clientes/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}

/** Microservicio ms-productos (catálogo). */
interface ProductosApi {
    @GET("api/productos")
    suspend fun listar(@Query("soloDisponibles") soloDisponibles: Boolean): List<ProductoDto>
}

/** Microservicio ms-pedidos (compras). */
interface PedidosApi {
    @POST("api/pedidos")
    suspend fun crear(@Body body: PedidoRequest): Response<PedidoDto>

    @GET("api/pedidos")
    suspend fun listar(@Query("clienteId") clienteId: Long): List<PedidoDto>

    @PATCH("api/pedidos/{id}/cancelar")
    suspend fun cancelar(@Path("id") id: Long): Response<PedidoDto>

    @DELETE("api/pedidos/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}

/** API externa pública: https://mindicador.cl/api (indicadores económicos de Chile). */
interface IndicadoresApi {
    @GET("api")
    suspend fun obtener(): IndicadoresDto
}
