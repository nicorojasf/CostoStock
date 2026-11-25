package cl.costoStock.myapplication.data.remote

import cl.costoStock.myapplication.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Crea los clientes Retrofit. Las URLs vienen de BuildConfig (ver app/build.gradle.kts). */
object ApiClient {

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val usuarios: UsuariosApi by lazy { retrofit(BuildConfig.USUARIOS_URL).create(UsuariosApi::class.java) }
    val productos: ProductosApi by lazy { retrofit(BuildConfig.PRODUCTOS_URL).create(ProductosApi::class.java) }
    val pedidos: PedidosApi by lazy { retrofit(BuildConfig.PEDIDOS_URL).create(PedidosApi::class.java) }
    val indicadores: IndicadoresApi by lazy {
        retrofit(BuildConfig.INDICADORES_URL).create(IndicadoresApi::class.java)
    }
}
