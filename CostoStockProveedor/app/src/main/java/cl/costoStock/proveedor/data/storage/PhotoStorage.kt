package cl.costoStock.proveedor.data.storage

import android.content.Context
import java.io.File

/**
 * Manejo de fotos. La cámara guarda primero en la caché (carpeta `captures`) y al confirmar
 * la acción (publicar / cambiar foto) se copian al almacenamiento interno privado de la app.
 */
class PhotoStorage(private val context: Context) {

    private val avatarsDir: File get() = File(context.filesDir, "avatars").apply { mkdirs() }
    private val productsDir: File get() = File(context.filesDir, "products").apply { mkdirs() }

    fun persistAvatar(sellerId: Long, tempPath: String): String {
        val target = File(avatarsDir, "avatar_${sellerId}_${System.currentTimeMillis()}.jpg")
        return moveInto(tempPath, target)
    }

    /** Si la foto ya está guardada de forma permanente la deja igual; si es temporal la copia. */
    fun persistProductPhoto(path: String): String {
        val source = File(path)
        if (source.parentFile?.canonicalPath == productsDir.canonicalPath) return path
        val target = File(productsDir, "product_${System.currentTimeMillis()}_${source.name}")
        return moveInto(path, target)
    }

    fun delete(path: String?) {
        if (path != null) File(path).delete()
    }

    fun deleteAll(paths: List<String>) = paths.forEach { delete(it) }

    private fun moveInto(sourcePath: String, target: File): String {
        val source = File(sourcePath)
        source.copyTo(target, overwrite = true)
        source.delete()
        return target.absolutePath
    }

    companion object {
        /** Archivo temporal donde la app de cámara deja la foto recién tomada. */
        fun newCaptureFile(context: Context): File {
            val dir = File(context.cacheDir, "captures").apply { mkdirs() }
            return File(dir, "capture_${System.currentTimeMillis()}.jpg")
        }
    }
}
