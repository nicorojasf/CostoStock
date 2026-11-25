package cl.costoStock.myapplication.data.remote

import cl.costoStock.myapplication.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Crea los clientes Retrofit. Las URLs vienen de BuildConfig (ver app/build.gradle.kts). */
object ApiClient {

    /** Entrega el token de la sesión actual (lo configura AppContainer con SessionManager). */
    @Volatile
    var tokenProvider: () -> String? = { null }

    /** Agrega "Authorization: Bearer <token>" a las llamadas a los microservicios propios. */
    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider()
        val request = if (token == null) chain.request()
        else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        chain.proceed(request)
    }

    private fun baseClient(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            }
        )

    /** Microservicios propios: con token. */
    private val http: OkHttpClient by lazy { baseClient().addInterceptor(authInterceptor).build() }

    /** API externa: sin token (nunca se envían credenciales a terceros). */
    private val publicHttp: OkHttpClient by lazy { baseClient().build() }

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val usuarios: UsuariosApi by lazy { retrofit(BuildConfig.USUARIOS_URL, http).create(UsuariosApi::class.java) }
    val productos: ProductosApi by lazy { retrofit(BuildConfig.PRODUCTOS_URL, http).create(ProductosApi::class.java) }
    val pedidos: PedidosApi by lazy { retrofit(BuildConfig.PEDIDOS_URL, http).create(PedidosApi::class.java) }
    val indicadores: IndicadoresApi by lazy {
        retrofit(BuildConfig.INDICADORES_URL, publicHttp).create(IndicadoresApi::class.java)
    }
}
