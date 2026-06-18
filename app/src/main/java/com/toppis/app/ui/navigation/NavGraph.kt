package com.toppis.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.toppis.app.ui.preparaciones.PreparacionViewModel
import com.toppis.app.ui.preparaciones.PreparacionViewModelFactory
import com.toppis.app.ui.preparaciones.PreparacionesScreen
import com.toppis.app.ui.modificadores.ModificadorViewModel
import com.toppis.app.ui.modificadores.ModificadorViewModelFactory
import com.toppis.app.ui.modificadores.ModificadoresScreen
import com.toppis.app.ui.promociones.PromocionViewModel
import com.toppis.app.ui.promociones.PromocionViewModelFactory
import com.toppis.app.ui.promociones.PromocionesScreen
import com.toppis.app.ui.papa.PapaRendimientoViewModel
import com.toppis.app.ui.papa.PapaRendimientoViewModelFactory
import com.toppis.app.ui.papa.PapaRendimientoScreen
import com.toppis.app.ui.foodcost.FoodCostViewModel
import com.toppis.app.ui.foodcost.FoodCostViewModelFactory
import com.toppis.app.ui.foodcost.FoodCostScreen
import com.toppis.app.ui.mermas.MermaViewModel
import com.toppis.app.ui.mermas.MermaViewModelFactory
import com.toppis.app.ui.mermas.MermasScreen
import com.toppis.app.ui.conteos.ConteoViewModel
import com.toppis.app.ui.conteos.ConteoViewModelFactory
import com.toppis.app.ui.conteos.ConteosScreen
import com.toppis.app.ui.compras.CompraSugeridaViewModel
import com.toppis.app.ui.compras.CompraSugeridaViewModelFactory
import com.toppis.app.ui.compras.CompraSugeridaScreen
import com.toppis.app.ui.variance.VarianceViewModel
import com.toppis.app.ui.variance.VarianceViewModelFactory
import com.toppis.app.ui.variance.VarianceScreen
import com.toppis.app.ui.proveedores.ProveedorViewModel
import com.toppis.app.ui.proveedores.ProveedorViewModelFactory
import com.toppis.app.ui.proveedores.ProveedoresScreen
import com.toppis.app.ui.compras.CompraViewModel
import com.toppis.app.ui.compras.CompraViewModelFactory
import com.toppis.app.ui.compras.ComprasScreen
import com.toppis.app.ui.arqueo.ArqueoViewModel
import com.toppis.app.ui.arqueo.ArqueoViewModelFactory
import com.toppis.app.ui.arqueo.ArqueoScreen
import com.toppis.app.ui.empleados.EmpleadoViewModel
import com.toppis.app.ui.empleados.EmpleadoViewModelFactory
import com.toppis.app.ui.empleados.EmpleadosScreen
import com.toppis.app.ui.manoobra.ManoObraViewModel
import com.toppis.app.ui.manoobra.ManoObraViewModelFactory
import com.toppis.app.ui.manoobra.ManoObraScreen
import com.toppis.app.ui.locales.LocalViewModel
import com.toppis.app.ui.locales.LocalViewModelFactory
import com.toppis.app.ui.locales.LocalesScreen
import com.toppis.app.ui.locales.AsignacionesScreen
import com.toppis.app.ui.kpis.KpisViewModel
import com.toppis.app.ui.kpis.KpisScreen
import com.toppis.app.ui.ventas.VentasHistorialScreen
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
    preparacionViewModelFactory: PreparacionViewModelFactory,
    modificadorViewModelFactory: ModificadorViewModelFactory,
    promocionViewModelFactory: PromocionViewModelFactory,
    papaViewModelFactory: PapaRendimientoViewModelFactory,
    foodCostViewModelFactory: FoodCostViewModelFactory,
    mermaViewModelFactory: MermaViewModelFactory,
    conteoViewModelFactory: ConteoViewModelFactory,
    compraSugeridaViewModelFactory: CompraSugeridaViewModelFactory,
    varianceViewModelFactory: VarianceViewModelFactory,
    proveedorViewModelFactory: ProveedorViewModelFactory,
    compraViewModelFactory: CompraViewModelFactory,
    arqueoViewModelFactory: ArqueoViewModelFactory,
    empleadoViewModelFactory: EmpleadoViewModelFactory,
    manoObraViewModelFactory: ManoObraViewModelFactory,
    localViewModelFactory: LocalViewModelFactory,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val usuarioActual by authViewModel.usuarioActual.collectAsState()
    val isAdmin = usuarioActual?.rol == Rol.ADMIN

    // Performance / cache en memoria: scopeamos los ViewModels al Activity (no al
    // NavBackStackEntry). Así sobreviven a la navegación entre pantallas: la data
    // ya cargada en sus StateFlow se conserva y no se vuelve a consultar a la base
    // de datos cada vez que se abre una pantalla. Realtime los mantiene frescos.
    val activityOwner = LocalContext.current as ComponentActivity

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
                    val vm: DashboardViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = dashboardViewModelFactory)
                    DashboardScreen(viewModel = vm)
                }

                composable("sobres") {
                    val vm: SobreViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = sobreViewModelFactory)
                    SobresScreen(viewModel = vm, isAdmin = isAdmin)
                }

                composable("pos") {
                    val posVm: PosViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = posViewModelFactory)
                    val sobreVm: SobreViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = sobreViewModelFactory)
                    PosScreen(
                        posViewModel = posVm,
                        sobreViewModel = sobreVm,
                        usuarioId = usuarioActual?.id
                    )
                }

                composable("inventario") {
                    val vm: InventarioViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = inventarioViewModelFactory)
                    InventarioScreen(viewModel = vm, isAdmin = isAdmin)
                }

                composable("gastos") {
                    val vm: GastoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = gastoViewModelFactory)
                    GastosScreen(viewModel = vm, usuarioId = usuarioActual?.id, isAdmin = isAdmin)
                }

                composable("reportes") {
                    val vm: ReporteViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = reporteViewModelFactory)
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
                    val vm: ExportacionViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = exportacionViewModelFactory)
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
                    val vm: FlujoCajaViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = flujoCajaViewModelFactory)
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
                    val vm: MenuConfigViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = menuConfigViewModelFactory)
                    MenuConfigScreen(
                        viewModel = vm,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("comprobantes") {
                    val vm: ComprobantesViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = comprobantesViewModelFactory)
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
                    val vm: ContabilidadViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = contabilidadViewModelFactory)
                    ContabilidadScreen(
                        viewModel = vm,
                        usuarioId = usuarioActual?.id,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("preparaciones") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: PreparacionViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = preparacionViewModelFactory)
                    PreparacionesScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("modificadores") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: ModificadorViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = modificadorViewModelFactory)
                    ModificadoresScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("promociones") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: PromocionViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = promocionViewModelFactory)
                    PromocionesScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("papa_rendimientos") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: PapaRendimientoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = papaViewModelFactory)
                    PapaRendimientoScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("food_cost") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: FoodCostViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = foodCostViewModelFactory)
                    FoodCostScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("mermas") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: MermaViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = mermaViewModelFactory)
                    MermasScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
                }
                composable("conteos") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: ConteoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = conteoViewModelFactory)
                    ConteosScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
                }
                composable("compra_sugerida") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: CompraSugeridaViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = compraSugeridaViewModelFactory)
                    CompraSugeridaScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("variance") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: VarianceViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = varianceViewModelFactory)
                    VarianceScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("proveedores") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: ProveedorViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = proveedorViewModelFactory)
                    ProveedoresScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("compras") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: CompraViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = compraViewModelFactory)
                    ComprasScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
                }
                composable("arqueo") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: ArqueoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = arqueoViewModelFactory)
                    ArqueoScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
                }
                composable("empleados") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: EmpleadoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = empleadoViewModelFactory)
                    EmpleadosScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("mano_obra") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: ManoObraViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = manoObraViewModelFactory)
                    ManoObraScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
                }
                composable("locales") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: LocalViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = localViewModelFactory)
                    LocalesScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
                }
                composable("ventas_historial") {
                    VentasHistorialScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("asignaciones_local") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    AsignacionesScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("kpis") {
                    if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                    val vm: KpisViewModel = viewModel(viewModelStoreOwner = activityOwner)
                    KpisScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
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
                val vm: DashboardViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = dashboardViewModelFactory)
                DashboardScreen(viewModel = vm)
            }

            composable("sobres") {
                val vm: SobreViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = sobreViewModelFactory)
                SobresScreen(viewModel = vm, isAdmin = isAdmin)
            }

            composable("pos") {
                val posVm: PosViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = posViewModelFactory)
                val sobreVm: SobreViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = sobreViewModelFactory)
                PosScreen(
                    posViewModel = posVm,
                    sobreViewModel = sobreVm,
                    usuarioId = usuarioActual?.id
                )
            }

            composable("inventario") {
                val vm: InventarioViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = inventarioViewModelFactory)
                InventarioScreen(viewModel = vm, isAdmin = isAdmin)
            }

            composable("gastos") {
                val vm: GastoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = gastoViewModelFactory)
                GastosScreen(viewModel = vm, usuarioId = usuarioActual?.id, isAdmin = isAdmin)
            }

            composable("reportes") {
                val vm: ReporteViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = reporteViewModelFactory)
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
                val vm: ExportacionViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = exportacionViewModelFactory)
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
                val vm: FlujoCajaViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = flujoCajaViewModelFactory)
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
                val vm: MenuConfigViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = menuConfigViewModelFactory)
                MenuConfigScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("comprobantes") {
                val vm: ComprobantesViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = comprobantesViewModelFactory)
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
                val vm: ContabilidadViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = contabilidadViewModelFactory)
                ContabilidadScreen(
                    viewModel = vm,
                    usuarioId = usuarioActual?.id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("preparaciones") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: PreparacionViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = preparacionViewModelFactory)
                PreparacionesScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("modificadores") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: ModificadorViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = modificadorViewModelFactory)
                ModificadoresScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("promociones") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: PromocionViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = promocionViewModelFactory)
                PromocionesScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("papa_rendimientos") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: PapaRendimientoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = papaViewModelFactory)
                PapaRendimientoScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("food_cost") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: FoodCostViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = foodCostViewModelFactory)
                FoodCostScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("mermas") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: MermaViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = mermaViewModelFactory)
                MermasScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
            }
            composable("conteos") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: ConteoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = conteoViewModelFactory)
                ConteosScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
            }
            composable("compra_sugerida") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: CompraSugeridaViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = compraSugeridaViewModelFactory)
                CompraSugeridaScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("variance") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: VarianceViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = varianceViewModelFactory)
                VarianceScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("proveedores") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: ProveedorViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = proveedorViewModelFactory)
                ProveedoresScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("compras") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: CompraViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = compraViewModelFactory)
                ComprasScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
            }
            composable("arqueo") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: ArqueoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = arqueoViewModelFactory)
                ArqueoScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
            }
            composable("empleados") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: EmpleadoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = empleadoViewModelFactory)
                EmpleadosScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("mano_obra") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: ManoObraViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = manoObraViewModelFactory)
                ManoObraScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
            }
            composable("locales") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: LocalViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = localViewModelFactory)
                LocalesScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("ventas_historial") {
                VentasHistorialScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("asignaciones_local") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                AsignacionesScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("kpis") {
                if (!isAdmin) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
                val vm: KpisViewModel = viewModel(viewModelStoreOwner = activityOwner)
                KpisScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
