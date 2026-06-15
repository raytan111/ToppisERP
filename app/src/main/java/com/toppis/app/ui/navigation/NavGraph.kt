package com.toppis.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.toppis.app.data.db.entities.Rol
import com.toppis.app.ui.auth.AuthViewModel
import com.toppis.app.ui.auth.LoginScreen
import com.toppis.app.ui.auth.UsuariosScreen
import com.toppis.app.ui.comprobantes.ComprobantesScreen
import com.toppis.app.ui.comprobantes.ComprobantesViewModel
import com.toppis.app.ui.comprobantes.ComprobantesViewModelFactory
import com.toppis.app.ui.components.MainScaffold
import com.toppis.app.ui.contabilidad.ContabilidadScreen
import com.toppis.app.ui.contabilidad.ContabilidadViewModel
import com.toppis.app.ui.contabilidad.ContabilidadViewModelFactory
import com.toppis.app.ui.exportacion.ExportacionScreen
import com.toppis.app.ui.exportacion.ExportacionViewModel
import com.toppis.app.ui.exportacion.ExportacionViewModelFactory
import com.toppis.app.ui.flujo.FlujoCajaScreen
import com.toppis.app.ui.flujo.FlujoCajaViewModel
import com.toppis.app.ui.flujo.FlujoCajaViewModelFactory
import com.toppis.app.ui.gastos.GastoViewModel
import com.toppis.app.ui.gastos.GastoViewModelFactory
import com.toppis.app.ui.gastos.GastosScreen
import com.toppis.app.ui.inventario.InventarioScreen
import com.toppis.app.ui.inventario.InventarioViewModel
import com.toppis.app.ui.inventario.InventarioViewModelFactory
import com.toppis.app.ui.pos.PosScreen
import com.toppis.app.ui.pos.PosViewModel
import com.toppis.app.ui.pos.PosViewModelFactory
import com.toppis.app.ui.reportes.ReporteViewModel
import com.toppis.app.ui.reportes.ReporteViewModelFactory
import com.toppis.app.ui.reportes.ReportesScreen
import com.toppis.app.ui.dashboard.DashboardScreen
import com.toppis.app.ui.dashboard.DashboardViewModel
import com.toppis.app.ui.dashboard.DashboardViewModelFactory
import com.toppis.app.ui.menu.MenuConfigScreen
import com.toppis.app.ui.menu.MenuConfigViewModel
import com.toppis.app.ui.menu.MenuConfigViewModelFactory
import com.toppis.app.ui.sobres.SobreViewModel
import com.toppis.app.ui.sobres.SobreViewModelFactory
import com.toppis.app.ui.sobres.SobresScreen

@Composable
fun NavGraph(
    sobreViewModelFactory: SobreViewModelFactory,
    posViewModelFactory: PosViewModelFactory,
    inventarioViewModelFactory: InventarioViewModelFactory,
    gastoViewModelFactory: GastoViewModelFactory,
    reporteViewModelFactory: ReporteViewModelFactory,
    exportacionViewModelFactory: ExportacionViewModelFactory,
    flujoCajaViewModelFactory: FlujoCajaViewModelFactory,
    dashboardViewModelFactory: DashboardViewModelFactory,
    menuConfigViewModelFactory: MenuConfigViewModelFactory,
    comprobantesViewModelFactory: ComprobantesViewModelFactory,
    contabilidadViewModelFactory: ContabilidadViewModelFactory,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val usuarioActual by authViewModel.usuarioActual.collectAsState()
    val isAdmin = usuarioActual?.rol == Rol.ADMIN

    // Guard global: redirige a login si el usuario cierra sesión desde cualquier pantalla
    LaunchedEffect(usuarioActual) {
        if (usuarioActual == null) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != null && currentRoute != "login") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Obtener la ruta actual para MainScaffold
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Determinar si la ruta actual debe usar MainScaffold
    val mainRoutes = listOf("dashboard", "sobres", "pos", "inventario", "gastos", "reportes")
    val useMainScaffold = currentRoute in mainRoutes

    if (useMainScaffold) {
        MainScaffold(
            currentRoute = currentRoute,
            isAdmin = isAdmin,
            navController = navController,
            onLogout = { authViewModel.logout() }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "login",
                modifier = modifier.padding(padding)
            ) {
                composable("login") {
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("dashboard") {
                    val vm: DashboardViewModel = viewModel(factory = dashboardViewModelFactory)
                    DashboardScreen(viewModel = vm)
                }

                composable("sobres") {
                    val vm: SobreViewModel = viewModel(factory = sobreViewModelFactory)
                    SobresScreen(viewModel = vm, isAdmin = isAdmin)
                }

                composable("pos") {
                    val posVm: PosViewModel = viewModel(factory = posViewModelFactory)
                    val sobreVm: SobreViewModel = viewModel(factory = sobreViewModelFactory)
                    PosScreen(
                        posViewModel = posVm,
                        sobreViewModel = sobreVm,
                        usuarioId = usuarioActual?.id
                    )
                }

                composable("inventario") {
                    val vm: InventarioViewModel = viewModel(factory = inventarioViewModelFactory)
                    InventarioScreen(viewModel = vm, isAdmin = isAdmin)
                }

                composable("gastos") {
                    val vm: GastoViewModel = viewModel(factory = gastoViewModelFactory)
                    GastosScreen(viewModel = vm, usuarioId = usuarioActual?.id, isAdmin = isAdmin)
                }

                composable("reportes") {
                    val vm: ReporteViewModel = viewModel(factory = reporteViewModelFactory)
                    ReportesScreen(viewModel = vm, isAdmin = isAdmin)
                }

                composable("usuarios") {
                    if (!isAdmin) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        return@composable
                    }
                    UsuariosScreen(
                        viewModel = authViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("exportacion") {
                    if (!isAdmin) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        return@composable
                    }
                    val vm: ExportacionViewModel = viewModel(factory = exportacionViewModelFactory)
                    ExportacionScreen(
                        viewModel = vm,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("flujo_caja") {
                    if (!isAdmin) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        return@composable
                    }
                    val vm: FlujoCajaViewModel = viewModel(factory = flujoCajaViewModelFactory)
                    FlujoCajaScreen(
                        viewModel = vm,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("menu_config") {
                    if (!isAdmin) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        return@composable
                    }
                    val vm: MenuConfigViewModel = viewModel(factory = menuConfigViewModelFactory)
                    MenuConfigScreen(
                        viewModel = vm,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("comprobantes") {
                    val vm: ComprobantesViewModel = viewModel(factory = comprobantesViewModelFactory)
                    ComprobantesScreen(
                        viewModel = vm,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("contabilidad") {
                    if (!isAdmin) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                        return@composable
                    }
                    val vm: ContabilidadViewModel = viewModel(factory = contabilidadViewModelFactory)
                    ContabilidadScreen(
                        viewModel = vm,
                        usuarioId = usuarioActual?.id,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = modifier
        ) {
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("dashboard") {
                val vm: DashboardViewModel = viewModel(factory = dashboardViewModelFactory)
                DashboardScreen(viewModel = vm)
            }

            composable("sobres") {
                val vm: SobreViewModel = viewModel(factory = sobreViewModelFactory)
                SobresScreen(viewModel = vm, isAdmin = isAdmin)
            }

            composable("pos") {
                val posVm: PosViewModel = viewModel(factory = posViewModelFactory)
                val sobreVm: SobreViewModel = viewModel(factory = sobreViewModelFactory)
                PosScreen(
                    posViewModel = posVm,
                    sobreViewModel = sobreVm,
                    usuarioId = usuarioActual?.id
                )
            }

            composable("inventario") {
                val vm: InventarioViewModel = viewModel(factory = inventarioViewModelFactory)
                InventarioScreen(viewModel = vm, isAdmin = isAdmin)
            }

            composable("gastos") {
                val vm: GastoViewModel = viewModel(factory = gastoViewModelFactory)
                GastosScreen(viewModel = vm, usuarioId = usuarioActual?.id, isAdmin = isAdmin)
            }

            composable("reportes") {
                val vm: ReporteViewModel = viewModel(factory = reporteViewModelFactory)
                ReportesScreen(viewModel = vm, isAdmin = isAdmin)
            }

            composable("usuarios") {
                if (!isAdmin) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                UsuariosScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("exportacion") {
                if (!isAdmin) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                val vm: ExportacionViewModel = viewModel(factory = exportacionViewModelFactory)
                ExportacionScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("flujo_caja") {
                if (!isAdmin) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                val vm: FlujoCajaViewModel = viewModel(factory = flujoCajaViewModelFactory)
                FlujoCajaScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("menu_config") {
                if (!isAdmin) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                val vm: MenuConfigViewModel = viewModel(factory = menuConfigViewModelFactory)
                MenuConfigScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("comprobantes") {
                val vm: ComprobantesViewModel = viewModel(factory = comprobantesViewModelFactory)
                ComprobantesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("contabilidad") {
                if (!isAdmin) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                val vm: ContabilidadViewModel = viewModel(factory = contabilidadViewModelFactory)
                ContabilidadScreen(
                    viewModel = vm,
                    usuarioId = usuarioActual?.id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
