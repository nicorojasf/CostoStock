package cl.costoStock.myapplication.nativeapi

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Recurso nativo: vibración del dispositivo.
 * Se usa como retroalimentación al agregar productos, al fallar un formulario
 * y al confirmar una compra. No requiere permiso en tiempo de ejecución.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = resolveVibrator(context)

    fun tick() = vibrate(longArrayOf(0, 20))
    fun error() = vibrate(longArrayOf(0, 40, 50, 40))
    fun success() = vibrate(longArrayOf(0, 30, 40, 30, 40, 60))

    @Suppress("DEPRECATION")
    private fun vibrate(timings: LongArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            v.vibrate(timings, -1)
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveVibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}

@Composable
fun rememberHaptics(): Haptics {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { Haptics(appContext) }
}
