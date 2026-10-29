package cl.costoStock.myapplication.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.costoStock.myapplication.CostoStockApplication
import cl.costoStock.myapplication.ui.auth.LoginViewModel
import cl.costoStock.myapplication.ui.auth.RegisterViewModel
import cl.costoStock.myapplication.ui.cart.CartViewModel
import cl.costoStock.myapplication.ui.home.HomeViewModel
import cl.costoStock.myapplication.ui.profile.ProfileViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { LoginViewModel(app().container.authRepository) }
        initializer { RegisterViewModel(app().container.authRepository) }
        initializer {
            val c = app().container
            HomeViewModel(c.productRepository, c.cartRepository)
        }
        initializer { CartViewModel(app().container.cartRepository) }
        initializer {
            val c = app().container
            ProfileViewModel(c.authRepository, c.cartRepository)
        }
        initializer { MainViewModel(app().container.cartRepository) }
    }
}

private fun CreationExtras.app(): CostoStockApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CostoStockApplication
