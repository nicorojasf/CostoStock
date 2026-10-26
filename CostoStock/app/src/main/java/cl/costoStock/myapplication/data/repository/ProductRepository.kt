package cl.costoStock.myapplication.data.repository

import cl.costoStock.myapplication.data.DataChangeNotifier
import cl.costoStock.myapplication.data.local.ProductDao
import cl.costoStock.myapplication.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val productDao: ProductDao,
    private val changes: DataChangeNotifier
) {
    fun observeProducts(): Flow<List<Product>> =
        changes.version.map { productDao.getAll() }.flowOn(Dispatchers.IO)
}
