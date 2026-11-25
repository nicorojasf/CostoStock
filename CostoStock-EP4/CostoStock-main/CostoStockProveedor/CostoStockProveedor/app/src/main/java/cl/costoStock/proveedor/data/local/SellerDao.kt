package cl.costoStock.proveedor.data.local

import android.content.ContentValues
import android.database.Cursor
import cl.costoStock.proveedor.data.DataChangeNotifier
import cl.costoStock.proveedor.data.model.Seller

/** Copia local del proveedor con sesión (el registro real está en ms-usuarios). */
class SellerDao(
    private val helper: ProveedorDatabase,
    private val changes: DataChangeNotifier
) {
    /** Inserta o actualiza los datos del proveedor conservando la foto local. */
    fun upsert(seller: Seller) {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("name", seller.name)
            put("rut", seller.rut)
            put("email", seller.email)
            put("phone", seller.phone)
            put("created_at", seller.createdAt)
        }
        val updated = db.update(ProveedorDatabase.TABLE_SELLERS, values, "id = ?", arrayOf(seller.id.toString()))
        if (updated == 0) {
            values.put("id", seller.id)
            db.insert(ProveedorDatabase.TABLE_SELLERS, null, values)
        }
        changes.notifyChanged()
    }

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
