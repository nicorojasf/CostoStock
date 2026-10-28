package cl.costoStock.proveedor.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.costoStock.proveedor.ProveedorApplication
import cl.costoStock.proveedor.ui.auth.LoginViewModel
import cl.costoStock.proveedor.ui.auth.RegisterViewModel
import cl.costoStock.proveedor.ui.products.ProductsViewModel
import cl.costoStock.proveedor.ui.profile.ProfileViewModel
import cl.costoStock.proveedor.ui.publish.PublishViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { LoginViewModel(app().container.authRepository) }
        initializer { RegisterViewModel(app().container.authRepository) }
        initializer { ProductsViewModel(app().container.productRepository) }
        initializer { PublishViewModel(app().container.productRepository) }
        initializer {
            val c = app().container
            ProfileViewModel(c.authRepository, c.productRepository)
        }
    }
}

private fun CreationExtras.app(): ProveedorApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ProveedorApplication
