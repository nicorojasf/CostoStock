package cl.costoStock.proveedor

import android.app.Application
import android.content.Context
import cl.costoStock.proveedor.data.DataChangeNotifier
import cl.costoStock.proveedor.data.local.ProductDao
import cl.costoStock.proveedor.data.local.ProveedorDatabase
import cl.costoStock.proveedor.data.local.SellerDao
import cl.costoStock.proveedor.data.repository.AuthRepository
import cl.costoStock.proveedor.data.repository.ProductRepository
import cl.costoStock.proveedor.data.session.SessionManager
import cl.costoStock.proveedor.data.storage.PhotoStorage

class ProveedorApplication : Application() {
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
    private val database = ProveedorDatabase(context)
    private val sellerDao = SellerDao(database, changes)
    private val productDao = ProductDao(database, changes)
    private val photoStorage = PhotoStorage(context)

    val session = SessionManager(context)

    val authRepository = AuthRepository(sellerDao, session, photoStorage, changes)
    val productRepository = ProductRepository(productDao, session, photoStorage, changes)
}
