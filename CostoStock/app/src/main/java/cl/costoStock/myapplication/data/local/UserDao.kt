package cl.costoStock.myapplication.data.local

import android.content.ContentValues
import android.database.Cursor
import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.model.User

data class UserCredentials(val user: User, val passwordHash: String, val salt: String)

class UserDao(
    private val helper: CostoStockDatabase,
    private val changes: DataChangeNotifier
) {
    /** Devuelve el id creado o -1 si el correo ya existe. */
    fun insert(name: String, email: String, passwordHash: String, salt: String): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("email", email)
            put("password_hash", passwordHash)
            put("salt", salt)
        }
        val id = helper.writableDatabase.insert(CostoStockDatabase.TABLE_USERS, null, values)
        if (id != -1L) changes.notifyChanged()
        return id
    }

    fun findCredentialsByEmail(email: String): UserCredentials? =
        helper.readableDatabase.query(
            CostoStockDatabase.TABLE_USERS, null, "email = ?", arrayOf(email),
            null, null, null, "1"
        ).use { c ->
            if (c.moveToFirst()) {
                UserCredentials(
                    user = c.toUser(),
                    passwordHash = c.getString(c.getColumnIndexOrThrow("password_hash")),
                    salt = c.getString(c.getColumnIndexOrThrow("salt"))
                )
            } else null
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
