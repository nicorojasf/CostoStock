package cl.costoStock.myapplication.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Base de datos local (SQLite): usuarios, productos y carrito. */
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
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                photo_path TEXT
            )
            """.trimIndent()
        )
        createCatalogTables(db)
        seedProducts(db)
    }

    // v2: el catálogo ahora incluye comuna e ícono. Se recrean productos y carrito;
    // la tabla de usuarios se conserva.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CART")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
            createCatalogTables(db)
            seedProducts(db)
        }
    }

    private fun createCatalogTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PRODUCTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT NOT NULL,
                price INTEGER NOT NULL CHECK (price >= 0),
                stock INTEGER NOT NULL CHECK (stock >= 0),
                comuna TEXT NOT NULL,
                emoji TEXT NOT NULL
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
                FOREIGN KEY (user_id) REFERENCES $TABLE_USERS(id) ON DELETE CASCADE,
                FOREIGN KEY (product_id) REFERENCES $TABLE_PRODUCTS(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun seedProducts(db: SQLiteDatabase) {
        val seed = listOf(
            Seed("Sombrero de paja con cinta animal print", "Moda", "Sombrero de ala ancha, ideal para el verano", 19037, 8, "Providencia", "👒"),
            Seed("Chaqueta de mezclilla talla M", "Moda", "Corte clásico, excelente estado", 24990, 5, "Ñuñoa", "🧥"),
            Seed("Zapatillas urbanas blancas N°40", "Moda", "Suela antideslizante, poco uso", 29990, 3, "Santiago", "👟"),
            Seed("Botella de aluminio blanca 750 ml", "Hogar", "Ideal para sublimar, tapa con mosquetón", 4990, 60, "Maipú", "🍶"),
            Seed("Lámpara de escritorio LED", "Hogar", "3 niveles de luz, brazo flexible", 12990, 14, "La Florida", "💡"),
            Seed("Set de 6 vasos de vidrio", "Hogar", "Vidrio templado de 350 ml", 6990, 25, "Puente Alto", "🥛"),
            Seed("Conejo de peluche 40 cm", "Bebés y niños", "Suave y lavable, hipoalergénico", 8990, 22, "Las Condes", "🐰"),
            Seed("Silla de auto reclinable 0-36 kg", "Bebés y niños", "Giratoria 360°, con arnés de 5 puntos", 89990, 4, "Providencia", "💺"),
            Seed("Mordedor de silicona y madera", "Bebés y niños", "Libre de BPA, para dentición", 5490, 40, "Ñuñoa", "🧸"),
            Seed("Audífonos bluetooth", "Tecnología", "Batería de 20 horas, con micrófono", 19990, 15, "Santiago", "🎧"),
            Seed("Cargador rápido USB-C 20 W", "Tecnología", "Compatible con celulares y tablets", 9990, 30, "Quilicura", "🔌"),
            Seed("Mouse inalámbrico", "Tecnología", "Receptor USB, silencioso", 7990, 0, "Maipú", "🖱️"),
            Seed("Cojín para gato color chocolate", "Mascotas", "Relleno suave, funda removible", 14990, 12, "La Florida", "🐱"),
            Seed("Rascador de cartón", "Mascotas", "Incluye hierba gatera", 7490, 18, "Las Condes", "🐾")
        )
        seed.forEach { item ->
            val values = ContentValues().apply {
                put("name", item.name)
                put("category", item.category)
                put("description", item.description)
                put("price", item.price)
                put("stock", item.stock)
                put("comuna", item.comuna)
                put("emoji", item.emoji)
            }
            db.insert(TABLE_PRODUCTS, null, values)
        }
    }

    private data class Seed(
        val name: String,
        val category: String,
        val description: String,
        val price: Int,
        val stock: Int,
        val comuna: String,
        val emoji: String
    )

    companion object {
        const val DB_NAME = "costostock.db"
        const val DB_VERSION = 2
        const val TABLE_USERS = "users"
        const val TABLE_PRODUCTS = "products"
        const val TABLE_CART = "cart_items"
    }
}
