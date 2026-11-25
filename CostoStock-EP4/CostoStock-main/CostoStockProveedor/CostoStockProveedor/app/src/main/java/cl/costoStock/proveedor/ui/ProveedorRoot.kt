package cl.costoStock.proveedor.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.costoStock.proveedor.ProveedorApplication
import cl.costoStock.proveedor.nativeapi.rememberHaptics
import cl.costoStock.proveedor.ui.auth.LoginScreen
import cl.costoStock.proveedor.ui.auth.RegisterScreen
import cl.costoStock.proveedor.ui.navigation.Routes
import cl.costoStock.proveedor.ui.products.ProductsScreen
import cl.costoStock.proveedor.ui.profile.ProfileScreen
import cl.costoStock.proveedor.ui.publish.PublishScreen
import kotlinx.coroutines.launch

private data class TabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    TabItem(Routes.PRODUCTS, "Publicaciones", Icons.Filled.Inventory2),
    TabItem(Routes.PROFILE, "Perfil", Icons.Filled.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedorRoot() {
    val context = LocalContext.current
    val container = (context.applicationContext as ProveedorApplication).container
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showMainChrome = currentRoute in Routes.mainRoutes

    val sellerId by container.session.currentSellerId.collectAsStateWithLifecycle()
    val startDestination = remember {
        if (container.session.currentSellerId.value != null) Routes.PRODUCTS else Routes.LOGIN
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // Si se cierra la sesión estando dentro de la app, se vuelve al login.
    var hadSession by remember { mutableStateOf(sellerId != null) }
    LaunchedEffect(sellerId) {
        if (sellerId != null) {
            hadSession = true
        } else if (hadSession) {
            hadSession = false
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    Scaffold(
        topBar = {
            if (showMainChrome) {
                TopAppBar(
                    title = { Text(if (currentRoute == Routes.PROFILE) "Mi perfil" else "Mis publicaciones") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        bottomBar = {
            if (showMainChrome) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.PRODUCTS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.PRODUCTS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptics.tick()
                        navController.navigate(Routes.publish())
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Publicar") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            // En login, registro y publicar cada pantalla maneja sus propios márgenes del sistema.
            modifier = if (showMainChrome) Modifier.padding(innerPadding) else Modifier,
            enterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 6 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it / 6 } }
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.PRODUCTS) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onGoToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = {
                        navController.navigate(Routes.PRODUCTS) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PRODUCTS) {
                ProductsScreen(
                    onMessage = showMessage,
                    onPublish = { navController.navigate(Routes.publish()) },
                    onEdit = { id -> navController.navigate(Routes.publish(id)) }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(onMessage = showMessage)
            }
            composable(
                route = Routes.PUBLISH_PATTERN,
                arguments = listOf(
                    navArgument(Routes.PRODUCT_ID_ARG) {
                        type = NavType.LongType
                        defaultValue = Routes.NO_PRODUCT
                    }
                ),
                // El asistente de publicación sube desde abajo, como una hoja a pantalla completa.
                enterTransition = { slideInVertically(tween(350)) { it } + fadeIn(tween(350)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition = { slideOutVertically(tween(300)) { it } + fadeOut(tween(250)) }
            ) { entry ->
                PublishScreen(
                    productId = entry.arguments?.getLong(Routes.PRODUCT_ID_ARG) ?: Routes.NO_PRODUCT,
                    onMessage = showMessage,
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
