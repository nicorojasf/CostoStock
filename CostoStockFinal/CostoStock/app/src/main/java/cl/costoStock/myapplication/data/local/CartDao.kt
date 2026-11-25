package cl.costoStock.myapplication.data.local

import android.content.ContentValues
import cl.costoStock.myapplication.data.DataChangeNotifier

/** Una línea del carrito guardada en el teléfono: solo id del producto y cantidad. */
data class CartItem(val productId: Long, val quantity: Int)

class CartDao(
    private val helper: CostoStockDatabase,
    private val changes: DataChangeNotifier
) {
    fun getItems(userId: Long): List<CartItem> =
        helper.readableDatabase.rawQuery(
            "SELECT product_id, quantity FROM ${CostoStockDatabase.TABLE_CART} WHERE user_id = ? ORDER BY rowid",
            arrayOf(userId.toString())
        ).use { c ->
            buildList { while (c.moveToNext()) add(CartItem(c.getLong(0), c.getInt(1))) }
        }

    fun quantityOf(userId: Long, productId: Long): Int =
        helper.readableDatabase.rawQuery(
            "SELECT quantity FROM ${CostoStockDatabase.TABLE_CART} WHERE user_id = ? AND product_id = ?",
            arrayOf(userId.toString(), productId.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    /** Crea o actualiza la línea. Si [quantity] <= 0 la elimina. */
    fun setQuantity(userId: Long, productId: Long, quantity: Int) {
        if (quantity <= 0) {
            remove(userId, productId)
            return
        }
        val db = helper.writableDatabase
        val values = ContentValues().apply { put("quantity", quantity) }
        val where = "user_id = ? AND product_id = ?"
        val args = arrayOf(userId.toString(), productId.toString())
        val updated = db.update(CostoStockDatabase.TABLE_CART, values, where, args)
        if (updated == 0) {
            values.put("user_id", userId)
            values.put("product_id", productId)
            db.insert(CostoStockDatabase.TABLE_CART, null, values)
        }
        changes.notifyChanged()
    }

    fun remove(userId: Long, productId: Long) {
        helper.writableDatabase.delete(
            CostoStockDatabase.TABLE_CART, "user_id = ? AND product_id = ?",
            arrayOf(userId.toString(), productId.toString())
        )
        changes.notifyChanged()
    }

    fun clear(userId: Long) {
        helper.writableDatabase.delete(
            CostoStockDatabase.TABLE_CART, "user_id = ?", arrayOf(userId.toString())
        )
        changes.notifyChanged()
    }
}
