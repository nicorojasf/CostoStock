package cl.costoStock.proveedor.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos local (SQLite): proveedores y sus publicaciones.
 *
 * La tabla `products` tiene las mismas columnas que la de la app de cliente
 * (id, name, category, description, price, stock, comuna, emoji) más las propias del
 * proveedor, para poder conectarlas a futuro. Ver docs/contrato-datos.md.
 */
class ProveedorDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SELLERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                rut TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL UNIQUE,
                phone TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                photo_path TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_PRODUCTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                seller_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT NOT NULL,
                price INTEGER NOT NULL CHECK (price >= 0),
                stock INTEGER NOT NULL CHECK (stock >= 0),
                comuna TEXT NOT NULL,
                emoji TEXT NOT NULL,
                item_condition TEXT NOT NULL DEFAULT 'NEW',
                image_paths TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (seller_id) REFERENCES $TABLE_SELLERS(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_products_seller ON $TABLE_PRODUCTS(seller_id)")
    }

    // v1: aún no hay migraciones. Al cambiar el esquema, subir DB_VERSION y migrar aquí.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    companion object {
        const val DB_NAME = "costostock_proveedor.db"
        const val DB_VERSION = 1
        const val TABLE_SELLERS = "sellers"
        const val TABLE_PRODUCTS = "products"
    }
}
