package cl.costoStock.proveedor.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PRODUCTS = "products"
    const val PROFILE = "profile"

    const val PRODUCT_ID_ARG = "productId"
    const val PUBLISH = "publish"
    const val PUBLISH_PATTERN = "$PUBLISH?$PRODUCT_ID_ARG={$PRODUCT_ID_ARG}"
    const val NO_PRODUCT = -1L

    /** Sin id crea una publicación nueva; con id edita la existente. */
    fun publish(productId: Long? = null) =
        if (productId == null) PUBLISH else "$PUBLISH?$PRODUCT_ID_ARG=$productId"

    /** Rutas que muestran barra superior e inferior. */
    val mainRoutes = setOf(PRODUCTS, PROFILE)
}
