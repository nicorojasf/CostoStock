package cl.costoStock.proveedor.data.local

import android.content.ContentValues
import android.database.Cursor
import cl.costoStock.proveedor.data.DataChangeNotifier
import cl.costoStock.proveedor.data.model.Seller

data class SellerCredentials(val seller: Seller, val passwordHash: String, val salt: String)

class SellerDao(
    private val helper: ProveedorDatabase,
    private val changes: DataChangeNotifier
) {
    /** Devuelve el id creado o -1 si el correo o el RUT ya existen. */
    fun insert(
        name: String,
        rut: String,
        email: String,
        phone: String,
        passwordHash: String,
        salt: String
    ): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("rut", rut)
            put("email", email)
            put("phone", phone)
            put("password_hash", passwordHash)
            put("salt", salt)
            put("created_at", System.currentTimeMillis())
        }
        val id = helper.writableDatabase.insert(ProveedorDatabase.TABLE_SELLERS, null, values)
        if (id != -1L) changes.notifyChanged()
        return id
    }

    fun findCredentialsByEmail(email: String): SellerCredentials? =
        helper.readableDatabase.query(
            ProveedorDatabase.TABLE_SELLERS, null, "email = ?", arrayOf(email),
            null, null, null, "1"
        ).use { c ->
            if (c.moveToFirst()) {
                SellerCredentials(
                    seller = c.toSeller(),
                    passwordHash = c.getString(c.getColumnIndexOrThrow("password_hash")),
                    salt = c.getString(c.getColumnIndexOrThrow("salt"))
                )
            } else null
        }

    fun existsByRut(rut: String): Boolean =
        helper.readableDatabase.query(
            ProveedorDatabase.TABLE_SELLERS, arrayOf("id"), "rut = ?", arrayOf(rut),
            null, null, null, "1"
        ).use { it.moveToFirst() }

    fun findById(id: Long): Seller? =
        helper.readableDatabase.query(
            ProveedorDatabase.TABLE_SELLERS, null, "id = ?", arrayOf(id.toString()),
            null, null, null, "1"
        ).use { c -> if (c.moveToFirst()) c.toSeller() else null }

    fun updatePhoto(id: Long, path: String) {
        val values = ContentValues().apply { put("photo_path", path) }
        helper.writableDatabase.update(
            ProveedorDatabase.TABLE_SELLERS, values, "id = ?", arrayOf(id.toString())
        )
        changes.notifyChanged()
    }

    fun updateProfile(id: Long, name: String, phone: String) {
        val values = ContentValues().apply {
            put("name", name)
            put("phone", phone)
        }
        helper.writableDatabase.update(
            ProveedorDatabase.TABLE_SELLERS, values, "id = ?", arrayOf(id.toString())
        )
        changes.notifyChanged()
    }

    private fun Cursor.toSeller(): Seller {
        val photoIdx = getColumnIndexOrThrow("photo_path")
        return Seller(
            id = getLong(getColumnIndexOrThrow("id")),
            name = getString(getColumnIndexOrThrow("name")),
            rut = getString(getColumnIndexOrThrow("rut")),
            email = getString(getColumnIndexOrThrow("email")),
            phone = getString(getColumnIndexOrThrow("phone")),
            photoPath = if (isNull(photoIdx)) null else getString(photoIdx),
            createdAt = getLong(getColumnIndexOrThrow("created_at"))
        )
    }
}
