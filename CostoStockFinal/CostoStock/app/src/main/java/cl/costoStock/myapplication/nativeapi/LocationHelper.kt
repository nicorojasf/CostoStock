package cl.costoStock.myapplication.nativeapi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Recurso nativo: ubicación del dispositivo (GPS / red) con LocationManager del sistema.
 * Se pide permiso en tiempo de ejecución y solo se usa para sugerir la comuna; la ubicación
 * no se guarda ni se envía a los microservicios.
 */
object LocationHelper {

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasPermission(context: Context): Boolean = permissions.any { granted(context, it) }

    /** Última ubicación reciente o una lectura nueva (máximo 10 s). Null si no se pudo. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val enabled = manager.getProviders(true)
        val last = enabled
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < MAX_AGE_MS) return last

        val fine = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val provider = when {
            fine && LocationManager.GPS_PROVIDER in enabled -> LocationManager.GPS_PROVIDER
            LocationManager.NETWORK_PROVIDER in enabled -> LocationManager.NETWORK_PROVIDER
            else -> return last
        }
        val fresh = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine<Location?> { cont ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val signal = CancellationSignal()
                        cont.invokeOnCancellation { signal.cancel() }
                        manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                            if (cont.isActive) cont.resume(location)
                        }
                    } else {
                        val listener = object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                if (cont.isActive) cont.resume(location)
                            }
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                            @Deprecated("Requerido en Android 10 o anterior")
                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        }
                        cont.invokeOnCancellation { manager.removeUpdates(listener) }
                        @Suppress("DEPRECATION")
                        manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                    }
                } catch (e: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        return fresh ?: last
    }

    private const val MAX_AGE_MS = 10 * 60_000L
    private const val TIMEOUT_MS = 10_000L
}

/** Estado del botón "Usar mi ubicación". */
@Stable
class LocationRequester {
    var isLocating by mutableStateOf(false)
        internal set
    internal var start: () -> Unit = {}

    fun request() = start()
}

/**
 * Pide el permiso si hace falta, obtiene la ubicación y la entrega en [onLocation].
 * Si el usuario niega el permiso o no hay señal, llama a [onError] con un mensaje claro.
 */
@Composable
fun rememberLocationRequester(
    onLocation: (lat: Double, lon: Double) -> Unit,
    onError: (String) -> Unit
): LocationRequester {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val requester = remember { LocationRequester() }
    val currentOnLocation by rememberUpdatedState(onLocation)
    val currentOnError by rememberUpdatedState(onError)

    val fetch: () -> Unit = {
        scope.launch {
            requester.isLocating = true
            val location = LocationHelper.currentLocation(context)
            requester.isLocating = false
            if (location != null) {
                currentOnLocation(location.latitude, location.longitude)
            } else {
                currentOnError("No pudimos obtener tu ubicación. Activa la ubicación del teléfono e inténtalo de nuevo")
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) fetch()
        else currentOnError("Sin permiso de ubicación no podemos detectar tu comuna. Puedes elegirla manualmente")
    }
    requester.start = {
        if (LocationHelper.hasPermission(context)) fetch() else launcher.launch(LocationHelper.permissions)
    }
    return requester
}
