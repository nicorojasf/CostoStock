package cl.costoStock.myapplication.data.local

import android.database.Cursor
import cl.costoStock.myapplication.data.model.Product

class ProductDao(private val helper: CostoStockDatabase) {

    fun getAll(): List<Product> =
        helper.readableDatabase.query(
            CostoStockDatabase.TABLE_PRODUCTS, null, null, null,
            null, null, "id ASC"
        ).use { c -> buildList { while (c.moveToNext()) add(c.toProduct()) } }

    fun findById(id: Long): Product? =
        helper.readableDatabase.query(
            CostoStockDatabase.TABLE_PRODUCTS, null, "id = ?", arrayOf(id.toString()),
            null, null, null, "1"
        ).use { c -> if (c.moveToFirst()) c.toProduct() else null }
}

internal fun Cursor.toProduct(): Product = Product(
    id = getLong(getColumnIndexOrThrow("id")),
    name = getString(getColumnIndexOrThrow("name")),
    category = getString(getColumnIndexOrThrow("category")),
    description = getString(getColumnIndexOrThrow("description")),
    price = getInt(getColumnIndexOrThrow("price")),
    stock = getInt(getColumnIndexOrThrow("stock")),
    comuna = getString(getColumnIndexOrThrow("comuna")),
    emoji = getString(getColumnIndexOrThrow("emoji"))
)
