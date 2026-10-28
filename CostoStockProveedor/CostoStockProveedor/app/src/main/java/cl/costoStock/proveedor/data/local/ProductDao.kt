package cl.costoStock.proveedor.data.local

import android.content.ContentValues
import android.database.Cursor
import cl.costoStock.proveedor.data.DataChangeNotifier
import cl.costoStock.proveedor.data.model.Product
import cl.costoStock.proveedor.data.model.ProductCondition
import cl.costoStock.proveedor.data.model.ProductDraft
import cl.costoStock.proveedor.data.model.ProductStatus

class ProductDao(
    private val helper: ProveedorDatabase,
    private val changes: DataChangeNotifier
) {
    fun getBySeller(sellerId: Long): List<Product> =
        helper.readableDatabase.query(
            ProveedorDatabase.TABLE_PRODUCTS, null, "seller_id = ?", arrayOf(sellerId.toString()),
            null, null, "created_at DESC, id DESC"
        ).use { c -> buildList { while (c.moveToNext()) add(c.toProduct()) } }

    fun findById(id: Long, sellerId: Long): Product? =
        helper.readableDatabase.query(
            ProveedorDatabase.TABLE_PRODUCTS, null, "id = ? AND seller_id = ?",
            arrayOf(id.toString(), sellerId.toString()), null, null, null, "1"
        ).use { c -> if (c.moveToFirst()) c.toProduct() else null }

    fun insert(sellerId: Long, draft: ProductDraft, emoji: String, imagePaths: List<String>): Long {
        val now = System.currentTimeMillis()
        val values = draft.toValues(emoji, imagePaths).apply {
            put("seller_id", sellerId)
            put("status", ProductStatus.ACTIVE.name)
            put("created_at", now)
            put("updated_at", now)
        }
        val id = helper.writableDatabase.insert(ProveedorDatabase.TABLE_PRODUCTS, null, values)
        if (id != -1L) changes.notifyChanged()
        return id
    }

    fun update(id: Long, sellerId: Long, draft: ProductDraft, emoji: String, imagePaths: List<String>): Boolean {
        val values = draft.toValues(emoji, imagePaths).apply {
            put("updated_at", System.currentTimeMillis())
        }
        val rows = helper.writableDatabase.update(
            ProveedorDatabase.TABLE_PRODUCTS, values, "id = ? AND seller_id = ?",
            arrayOf(id.toString(), sellerId.toString())
        )
        if (rows > 0) changes.notifyChanged()
        return rows > 0
    }

    fun setStatus(id: Long, sellerId: Long, status: ProductStatus) {
        val values = ContentValues().apply {
            put("status", status.name)
            put("updated_at", System.currentTimeMillis())
        }
        helper.writableDatabase.update(
            ProveedorDatabase.TABLE_PRODUCTS, values, "id = ? AND seller_id = ?",
            arrayOf(id.toString(), sellerId.toString())
        )
        changes.notifyChanged()
    }

    fun delete(id: Long, sellerId: Long) {
        helper.writableDatabase.delete(
            ProveedorDatabase.TABLE_PRODUCTS, "id = ? AND seller_id = ?",
            arrayOf(id.toString(), sellerId.toString())
        )
        changes.notifyChanged()
    }

    private fun ProductDraft.toValues(emoji: String, imagePaths: List<String>) = ContentValues().apply {
        put("name", name)
        put("category", category)
        put("description", description)
        put("price", price)
        put("stock", stock)
        put("comuna", comuna)
        put("emoji", emoji)
        put("item_condition", condition.name)
        put("image_paths", imagePaths.joinToString(PATH_SEPARATOR))
    }

    private fun Cursor.toProduct(): Product {
        val rawPaths = getString(getColumnIndexOrThrow("image_paths"))
        return Product(
            id = getLong(getColumnIndexOrThrow("id")),
            sellerId = getLong(getColumnIndexOrThrow("seller_id")),
            name = getString(getColumnIndexOrThrow("name")),
            category = getString(getColumnIndexOrThrow("category")),
            description = getString(getColumnIndexOrThrow("description")),
            price = getInt(getColumnIndexOrThrow("price")),
            stock = getInt(getColumnIndexOrThrow("stock")),
            comuna = getString(getColumnIndexOrThrow("comuna")),
            emoji = getString(getColumnIndexOrThrow("emoji")),
            condition = runCatching { ProductCondition.valueOf(getString(getColumnIndexOrThrow("item_condition"))) }
                .getOrDefault(ProductCondition.NEW),
            imagePaths = if (rawPaths.isEmpty()) emptyList() else rawPaths.split(PATH_SEPARATOR),
            status = runCatching { ProductStatus.valueOf(getString(getColumnIndexOrThrow("status"))) }
                .getOrDefault(ProductStatus.ACTIVE),
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
            updatedAt = getLong(getColumnIndexOrThrow("updated_at"))
        )
    }

    private companion object {
        /** Las rutas de las fotos se guardan en una sola columna separadas por "|". */
        const val PATH_SEPARATOR = "|"
    }
}
