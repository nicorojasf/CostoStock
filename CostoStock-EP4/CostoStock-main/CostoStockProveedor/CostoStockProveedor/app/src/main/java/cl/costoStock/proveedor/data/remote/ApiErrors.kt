package cl.costoStock.proveedor.data.remote

import cl.costoStock.proveedor.data.remote.dto.ErrorDto
import com.google.gson.Gson
import okhttp3.ResponseBody

/** Lectura de los errores JSON que devuelven los microservicios. */
object ApiErrors {
    const val NETWORK =
        "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo nuevamente"
    const val UNEXPECTED = "Ocurrió un error inesperado. Inténtalo nuevamente"

    private val gson = Gson()

    /** Devuelve el error del cuerpo o null si no es un JSON válido. Lee el cuerpo una sola vez. */
    fun parse(body: ResponseBody?): ErrorDto? = try {
        body?.string()?.takeIf { it.isNotBlank() }?.let { gson.fromJson(it, ErrorDto::class.java) }
    } catch (e: Exception) {
        null
    }
}

/** Error de negocio o de red con un mensaje listo para mostrar al usuario. */
class ApiException(message: String) : Exception(message)
