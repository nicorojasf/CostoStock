package cl.costoStock.myapplication.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password"
    const val CATALOG = "catalog"
    const val CART = "cart"
    const val PROFILE = "profile"
    const val ORDERS = "orders"

    /** Rutas que muestran barra superior e inferior. */
    val mainRoutes = setOf(CATALOG, CART, PROFILE)
}
