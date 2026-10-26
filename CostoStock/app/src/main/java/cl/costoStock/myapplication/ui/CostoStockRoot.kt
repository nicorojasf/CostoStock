package cl.costoStock.myapplication.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.costoStock.myapplication.CostoStockApplication
import cl.costoStock.myapplication.ui.auth.LoginScreen
import cl.costoStock.myapplication.ui.auth.RegisterScreen
import cl.costoStock.myapplication.ui.cart.CartScreen
import cl.costoStock.myapplication.ui.home.HomeScreen
import cl.costoStock.myapplication.ui.navigation.Routes
import cl.costoStock.myapplication.ui.profile.ProfileScreen
import kotlinx.coroutines.launch

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.CATALOG, "Catálogo", Icons.Filled.Storefront),
    BottomItem(Routes.CART, "Carrito", Icons.Filled.ShoppingCart),
    BottomItem(Routes.PROFILE, "Perfil", Icons.Filled.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostoStockRoot(
    mainViewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val session = (LocalContext.current.applicationContext as CostoStockApplication)
        .container.session
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cartCount by mainViewModel.cartCount.collectAsStateWithLifecycle()

    // Si ya hay sesión guardada se entra directo al catálogo.
    val startDestination = remember {
        if (session.currentUserId.value != null) Routes.CATALOG else Routes.LOGIN
    }
    val currentRoute = backStackEntry?.destination?.route ?: startDestination
    val isMainRoute = currentRoute in Routes.mainRoutes

    val showMessage: (String) -> Unit = { message ->
        snackbarHostState.currentSnackbarData?.dismiss()
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isMainRoute && currentRoute != Routes.CATALOG) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    title = {
                        Text(
                            when (currentRoute) {
                                Routes.CART -> "Mi carrito"
                                Routes.PROFILE -> "Mi perfil"
                                else -> "CostoStock"
                            }
                        )
                    }
                )
            }
        },
        bottomBar = {
            if (isMainRoute) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navController.navigateToTab(item.route) },
                            icon = {
                                if (item.route == Routes.CART) {
                                    BadgedBox(badge = {
                                        if (cartCount > 0) Badge { Text(cartCount.toString()) }
                                    }) { Icon(item.icon, contentDescription = item.label) }
                                } else {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Las pantallas de acceso dibujan bajo la barra de estado (degradado), por eso sin padding.
        val contentModifier = when {
            // El catálogo dibuja su propio encabezado naranja bajo la barra de estado.
            currentRoute == Routes.CATALOG -> {
                val bottomOnly = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                Modifier
                    .padding(bottomOnly)
                    .consumeWindowInsets(bottomOnly)
            }
            isMainRoute -> Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
            else -> Modifier
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = contentModifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 8 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(250)) { it / 8 } }
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { navController.navigateToHomeClearingStack() },
                    onGoToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = { navController.navigateToHomeClearingStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CATALOG) {
                HomeScreen(
                    onMessage = showMessage,
                    onGoToCart = { navController.navigateToTab(Routes.CART) }
                )
            }
            composable(Routes.CART) {
                CartScreen(
                    onMessage = showMessage,
                    onGoToCatalog = { navController.navigateToTab(Routes.CATALOG) }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onMessage = showMessage,
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.CATALOG) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToHomeClearingStack() {
    navigate(Routes.CATALOG) {
        popUpTo(graph.id) { inclusive = true }
    }
}
