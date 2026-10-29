package cl.costoStock.proveedor.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodifica una foto reduciéndola (para no gastar memoria) y corrigiendo su rotación. */
fun decodeSampledBitmap(path: String, maxSize: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSize || bounds.outHeight / (sample * 2) >= maxSize) {
        sample *= 2
    }
    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null

    val rotation = try {
        when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (e: Exception) {
        0f
    }
    if (rotation == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/** Carga la foto en un hilo de fondo y la entrega lista para dibujar (null mientras carga). */
@Composable
fun rememberPhotoBitmap(path: String?, maxSize: Int = 600): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, path, maxSize) {
        value = if (path.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            decodeSampledBitmap(path, maxSize)?.asImageBitmap()
        }
    }
    return state.value
}

/** Foto del producto con aparición suave; si no hay foto muestra el emoji de la categoría. */
@Composable
fun ProductPhoto(
    path: String?,
    emoji: String,
    modifier: Modifier = Modifier,
    maxSize: Int = 600,
    emojiSize: TextUnit = 40.sp
) {
    val bitmap = rememberPhotoBitmap(path, maxSize)
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = bitmap, label = "photoFade") { bmp ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "Foto del producto",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(emoji, fontSize = emojiSize)
                }
            }
        }
    }
}
