package cl.costoStock.myapplication.data.local

import android.content.ContentValues
import android.database.Cursor
import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.model.User

/** Copia local del cliente que inició sesión (el registro real está en ms-usuarios). */
class UserDao(
    private val helper: CostoStockDatabase,
    private val changes: DataChangeNotifier
) {
    /** Inserta o actualiza nombre y correo conservando la foto local. */
    fun upsert(id: Long, name: String, email: String) {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("email", email)
        }
        val updated = db.update(CostoStockDatabase.TABLE_USERS, values, "id = ?", arrayOf(id.toString()))
        if (updated == 0) {
            values.put("id", id)
            db.insert(CostoStockDatabase.TABLE_USERS, null, values)
        }
        changes.notifyChanged()
    }

    fun findById(id: Long): User? =
        helper.readableDatabase.query(
            CostoStockDatabase.TABLE_USERS, null, "id = ?", arrayOf(id.toString()),
            null, null, null, "1"
        ).use { c -> if (c.moveToFirst()) c.toUser() else null }

    fun updatePhoto(id: Long, path: String) {
        val values = ContentValues().apply { put("photo_path", path) }
        helper.writableDatabase.update(
            CostoStockDatabase.TABLE_USERS, values, "id = ?", arrayOf(id.toString())
        )
        changes.notifyChanged()
    }

    fun delete(id: Long) {
        helper.writableDatabase.delete(CostoStockDatabase.TABLE_USERS, "id = ?", arrayOf(id.toString()))
        changes.notifyChanged()
    }

    private fun Cursor.toUser(): User {
        val photoIdx = getColumnIndexOrThrow("photo_path")
        return User(
            id = getLong(getColumnIndexOrThrow("id")),
            name = getString(getColumnIndexOrThrow("name")),
            email = getString(getColumnIndexOrThrow("email")),
            photoPath = if (isNull(photoIdx)) null else getString(photoIdx)
        )
    }
}
