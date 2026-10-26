package cl.costoStock.myapplication

import android.app.Application
import android.content.Context
import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.local.CartDao
import cl.costoStock.myapplication.data.local.CostoStockDatabase
import cl.costoStock.myapplication.data.local.ProductDao
import cl.costoStock.myapplication.data.local.UserDao
import cl.costoStock.myapplication.data.repository.AuthRepository
import cl.costoStock.myapplication.data.repository.CartRepository
import cl.costoStock.myapplication.data.repository.ProductRepository
import cl.costoStock.myapplication.data.session.SessionManager
import cl.costoStock.myapplication.data.storage.PhotoStorage

class CostoStockApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Contenedor de dependencias manual (suficiente para este tamaño de proyecto). */
class AppContainer(context: Context) {
    private val changes = DataChangeNotifier()
    private val database = CostoStockDatabase(context)
    private val userDao = UserDao(database, changes)
    private val productDao = ProductDao(database)
    private val cartDao = CartDao(database, changes)

    val session = SessionManager(context)

    val authRepository = AuthRepository(userDao, session, PhotoStorage(context), changes)
    val productRepository = ProductRepository(productDao, changes)
    val cartRepository = CartRepository(cartDao, productDao, session, changes)
}
