package cl.costoStock.myapplication.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos local (SQLite).
 *
 * Desde la v3 los usuarios, productos y pedidos viven en los microservicios. Aquí solo se guarda:
 * - `users`: copia local del cliente con sesión (para la foto de perfil, que no sale del teléfono).
 * - `cart_items`: el carrito, que es un dato local del dispositivo.
 */
class CostoStockDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                photo_path TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_CART (
                user_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                PRIMARY KEY (user_id, product_id),
                FOREIGN KEY (user_id) REFERENCES $TABLE_USERS(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    // v3: los datos pasan a los microservicios. Los ids locales antiguos no coinciden con los del
    // servidor, así que se recrea la base (el usuario vuelve a iniciar sesión).
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CART")
        db.execSQL("DROP TABLE IF EXISTS products")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    companion object {
        const val DB_NAME = "costostock.db"
        const val DB_VERSION = 3
        const val TABLE_USERS = "users"
        const val TABLE_CART = "cart_items"
    }
}
