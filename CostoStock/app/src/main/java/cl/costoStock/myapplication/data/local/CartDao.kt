package cl.costoStock.myapplication.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.model.CartLine
import cl.costoStock.myapplication.domain.cart.CartCalculator
import cl.costoStock.myapplication.domain.cart.CheckoutResult

class CartDao(
    private val helper: CostoStockDatabase,
    private val changes: DataChangeNotifier
) {
    fun getCart(userId: Long): List<CartLine> = readCart(helper.readableDatabase, userId)

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

    /**
     * Confirma la compra en una sola transacción: valida stock, lo descuenta
     * y vacía el carrito. Si algo falla no se modifica nada.
     */
    fun checkout(userId: Long): CheckoutResult {
        val db = helper.writableDatabase
        db.beginTransaction()
        val result = try {
            val lines = readCart(db, userId)
            val r: CheckoutResult = when {
                lines.isEmpty() -> CheckoutResult.Empty
                lines.any { it.quantity > it.product.stock } -> CheckoutResult.OutOfStock(
                    lines.filter { it.quantity > it.product.stock }.map { it.product.name }
                )
                else -> {
                    lines.forEach { line ->
                        db.execSQL(
                            "UPDATE ${CostoStockDatabase.TABLE_PRODUCTS} SET stock = stock - ? " +
                                "WHERE id = ? AND stock >= ?",
                            arrayOf<Any>(line.quantity, line.product.id, line.quantity)
                        )
                    }
                    db.delete(
                        CostoStockDatabase.TABLE_CART, "user_id = ?", arrayOf(userId.toString())
                    )
                    CheckoutResult.Success(CartCalculator.summarize(lines))
                }
            }
            if (r is CheckoutResult.Success) db.setTransactionSuccessful()
            r
        } finally {
            db.endTransaction()
        }
        if (result is CheckoutResult.Success) changes.notifyChanged()
        return result
    }

    private fun readCart(db: SQLiteDatabase, userId: Long): List<CartLine> =
        db.rawQuery(
            """
            SELECT p.id, p.name, p.category, p.description, p.price, p.stock, p.comuna, p.emoji, c.quantity
            FROM ${CostoStockDatabase.TABLE_CART} c
            INNER JOIN ${CostoStockDatabase.TABLE_PRODUCTS} p ON p.id = c.product_id
            WHERE c.user_id = ?
            ORDER BY c.rowid
            """.trimIndent(),
            arrayOf(userId.toString())
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(CartLine(c.toProduct(), c.getInt(c.getColumnIndexOrThrow("quantity"))))
                }
            }
        }
}
