package cl.costoStock.myapplication.data.storage

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/** Guarda las fotos de perfil en el almacenamiento interno privado de la app. */
class PhotoStorage(private val context: Context) {

    fun save(userId: Long, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val file = File(dir, "avatar_${userId}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        return file.absolutePath
    }

    fun delete(path: String?) {
        if (path != null) File(path).delete()
    }
}
