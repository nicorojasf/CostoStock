package cl.costoStock.proveedor.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos local (SQLite).
 *
 * Desde la v2 los proveedores y sus publicaciones viven en los microservicios ms-usuarios y
 * ms-productos. Aquí solo queda `sellers`: copia local del proveedor con sesión, para guardar
 * su foto de perfil (que no sale del teléfono).
 */
class ProveedorDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SELLERS (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                rut TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                photo_path TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    // v2: los datos pasan a los microservicios. Se recrea la base local (el proveedor vuelve a
    // iniciar sesión con su cuenta del servidor).
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS products")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SELLERS")
        onCreate(db)
    }

    companion object {
        const val DB_NAME = "costostock_proveedor.db"
        const val DB_VERSION = 2
        const val TABLE_SELLERS = "sellers"
    }
}
