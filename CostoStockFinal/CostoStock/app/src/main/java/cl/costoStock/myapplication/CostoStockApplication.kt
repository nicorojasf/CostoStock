package cl.costoStock.myapplication

import android.app.Application
import android.content.Context
import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.local.CartDao
import cl.costoStock.myapplication.data.local.CostoStockDatabase
import cl.costoStock.myapplication.data.local.UserDao
import cl.costoStock.myapplication.data.remote.ApiClient
import cl.costoStock.myapplication.data.repository.AuthRepository
import cl.costoStock.myapplication.data.repository.CartRepository
import cl.costoStock.myapplication.data.repository.IndicatorsRepository
import cl.costoStock.myapplication.data.repository.OrderRepository
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
    private val cartDao = CartDao(database, changes)

    val session = SessionManager(context)

    // Microservicios propios
    val authRepository = AuthRepository(ApiClient.usuarios, userDao, session, PhotoStorage(context), changes)
    val productRepository = ProductRepository(ApiClient.productos)
    val cartRepository = CartRepository(cartDao, productRepository, ApiClient.pedidos, session, changes)
    val orderRepository = OrderRepository(ApiClient.pedidos, session, productRepository)

    // API externa
    val indicatorsRepository = IndicatorsRepository(ApiClient.indicadores)

    init {
        ApiClient.tokenProvider = { session.token }
        authRepository.ensureValidSession()
    }
}
