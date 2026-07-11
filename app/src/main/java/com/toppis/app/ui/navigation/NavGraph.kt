package com.toppis.app.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toppis.app.data.db.entities.Rol
import com.toppis.app.ui.auth.AuthViewModel
import com.toppis.app.ui.auth.LoginScreen
import com.toppis.app.ui.auth.UsuariosScreen
import com.toppis.app.ui.comprobantes.ComprobantesScreen
import com.toppis.app.ui.comprobantes.ComprobantesViewModel
import com.toppis.app.ui.comprobantes.ComprobantesViewModelFactory
import com.toppis.app.ui.components.BackScaffold
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
import com.toppis.app.ui.home.CategoriaMenuScreen
import com.toppis.app.ui.home.HomeScreen
import com.toppis.app.ui.home.categoriaPorId
import com.toppis.app.ui.inventario.InventarioScreen
import com.toppis.app.ui.inventario.InventarioViewModel
import com.toppis.app.ui.inventario.InventarioViewModelFactory
import com.toppis.app.ui.pos.PosScreen
import com.toppis.app.ui.pos.PosViewModel
import com.toppis.app.ui.pos.PosViewModelFactory
import com.toppis.app.ui.reportes.ReporteViewModel
import com.toppis.app.ui.reportes.ReporteViewModelFactory
import com.toppis.app.ui.reportes.ReportesScreen
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
    menuConfigViewModelFactory: MenuConfigViewModelFactory,
    comprobantesViewModelFactory: ComprobantesViewModelFactory,
    contabilidadViewModelFactory: ContabilidadViewModelFactory,
    preparacionViewModelFactory: PreparacionViewModelFactory,
    modificadorViewModelFactory: ModificadorViewModelFactory,
    promocionViewModelFactory: PromocionViewModelFactory,
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
    costoFijoViewModelFactory: com.toppis.app.ui.costos.CostoFijoViewModelFactory,
    cierreSemanalViewModelFactory: com.toppis.app.ui.costos.CierreSemanalViewModelFactory,
    objetivosViewModelFactory: com.toppis.app.ui.costos.ObjetivosViewModelFactory,
    rutinaSemanalViewModelFactory: com.toppis.app.ui.costos.RutinaSemanalViewModelFactory,
    pedidosViewModelFactory: com.toppis.app.ui.pos.PedidosViewModelFactory,
    carritoViewModelFactory: com.toppis.app.ui.pos.CarritoViewModelFactory,
    comandasViewModelFactory: com.toppis.app.ui.pos.ComandasViewModelFactory,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val usuarioActual by authViewModel.usuarioActual.collectAsState()
    val isAdmin = usuarioActual?.rol == Rol.ADMIN
    val permisos = com.toppis.app.ui.auth.Permisos.de(usuarioActual?.rol)

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

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier,
        // Transiciones suaves: deslizar + fundido al navegar entre pantallas.
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(320)
            ) + fadeIn(tween(320))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(320)
            ) + fadeOut(tween(320))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(320)
            ) + fadeIn(tween(320))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(320)
            ) + fadeOut(tween(320))
        }
    ) {
        // ── Login ──────────────────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // ── Home (menú principal, sin bottom bar) ────────────────────────────
        composable("home") {
            HomeScreen(
                permisos = permisos,
                onAbrirPos = { navController.navigate("pos") },
                onAbrirCategoria = { catId -> navController.navigate("categoria/$catId") },
                onAbrirSobres = { navController.navigate("sobres") },
                onLogout = { authViewModel.logout() }
            )
        }

        // ── Sub-menú de categoría ────────────────────────────────────────────
        composable(
            route = "categoria/{catId}",
            arguments = listOf(navArgument("catId") { type = NavType.StringType })
        ) { entry ->
            val cat = categoriaPorId(entry.arguments?.getString("catId"))
            if (cat == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            CategoriaMenuScreen(
                categoria = cat,
                permisos = permisos,
                onAbrirOpcion = { ruta -> navController.navigate(ruta) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Venta (POS) — lista de pedidos ───────────────────────────────────
        composable("pos") {
            if (!permisos.puedeAbrir("pos")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: com.toppis.app.ui.pos.PedidosViewModel =
                viewModel(viewModelStoreOwner = activityOwner, factory = pedidosViewModelFactory)
            com.toppis.app.ui.pos.PosPedidosScreen(
                viewModel = vm,
                onAbrirPedido = { id -> navController.navigate("pedido/$id") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Carrito de un pedido (se completa en Fase E) ─────────────────────
        composable(
            route = "pedido/{pedidoId}",
            arguments = listOf(navArgument("pedidoId") { type = NavType.IntType })
        ) { entry ->
            val pedidoId = entry.arguments?.getInt("pedidoId") ?: 0
            // VM scopeado al backstack entry (uno por pedido, no reutiliza estado).
            val vm: com.toppis.app.ui.pos.CarritoViewModel = viewModel(factory = carritoViewModelFactory)
            com.toppis.app.ui.pos.PedidoCarritoScreen(
                viewModel = vm,
                pedidoId = pedidoId,
                usuarioId = usuarioActual?.id,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Cocina: comandas (KDS) ───────────────────────────────────────────
        composable("comandas") {
            if (!permisos.puedeAbrir("comandas")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: com.toppis.app.ui.pos.ComandasViewModel =
                viewModel(viewModelStoreOwner = activityOwner, factory = comandasViewModelFactory)
            com.toppis.app.ui.pos.ComandasScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        // ── Fondos ───────────────────────────────────────────────────────────
        composable("sobres") {
            if (!permisos.puedeAbrir("sobres")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: SobreViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = sobreViewModelFactory)
            BackScaffold("Sobres", onNavigateBack = { navController.popBackStack() }) { padding ->
                SobresScreen(viewModel = vm, isAdmin = isAdmin, modifier = Modifier.padding(padding))
            }
        }

        composable("gastos") {
            if (!permisos.puedeAbrir("gastos")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: GastoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = gastoViewModelFactory)
            BackScaffold("Gastos", onNavigateBack = { navController.popBackStack() }) { padding ->
                GastosScreen(viewModel = vm, usuarioId = usuarioActual?.id, isAdmin = isAdmin, modifier = Modifier.padding(padding))
            }
        }

        composable("arqueo") {
            if (!permisos.puedeAbrir("arqueo")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: ArqueoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = arqueoViewModelFactory)
            ArqueoScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
        }

        composable("flujo_caja") {
            if (!permisos.puedeAbrir("flujo_caja")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: FlujoCajaViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = flujoCajaViewModelFactory)
            FlujoCajaScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("contabilidad") {
            if (!permisos.puedeAbrir("contabilidad")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: ContabilidadViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = contabilidadViewModelFactory)
            ContabilidadScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
        }

        composable("reportes") {
            if (!permisos.puedeAbrir("reportes")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: ReporteViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = reporteViewModelFactory)
            BackScaffold("Reportes", onNavigateBack = { navController.popBackStack() }) { padding ->
                ReportesScreen(viewModel = vm, isAdmin = isAdmin, modifier = Modifier.padding(padding))
            }
        }

        // ── Inventario ───────────────────────────────────────────────────────
        composable("inventario") {
            if (!permisos.puedeAbrir("inventario")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: InventarioViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = inventarioViewModelFactory)
            BackScaffold("Artículos", onNavigateBack = { navController.popBackStack() }) { padding ->
                InventarioScreen(viewModel = vm, puedeEditar = permisos.puedeEditar, puedeBorrar = permisos.puedeBorrar, modifier = Modifier.padding(padding))
            }
        }

        composable("mermas") {
            if (!permisos.puedeAbrir("mermas")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: MermaViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = mermaViewModelFactory)
            MermasScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
        }

        composable("conteos") {
            if (!permisos.puedeAbrir("conteos")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: ConteoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = conteoViewModelFactory)
            ConteosScreen(viewModel = vm, usuarioId = usuarioActual?.id, puedeBorrar = permisos.puedeBorrar, onNavigateBack = { navController.popBackStack() })
        }

        composable("compra_sugerida") {
            if (!permisos.puedeAbrir("compra_sugerida")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: CompraSugeridaViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = compraSugeridaViewModelFactory)
            CompraSugeridaScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("variance") {
            if (!permisos.puedeAbrir("variance")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: VarianceViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = varianceViewModelFactory)
            VarianceScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("compras") {
            if (!permisos.puedeAbrir("compras")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: CompraViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = compraViewModelFactory)
            ComprasScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
        }

        composable("proveedores") {
            if (!permisos.puedeAbrir("proveedores")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: ProveedorViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = proveedorViewModelFactory)
            ProveedoresScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        // ── Cocina ───────────────────────────────────────────────────────────
        composable("menu_config") {
            if (!permisos.puedeAbrir("menu_config")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: MenuConfigViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = menuConfigViewModelFactory)
            MenuConfigScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("preparaciones") {
            if (!permisos.puedeAbrir("preparaciones")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: PreparacionViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = preparacionViewModelFactory)
            PreparacionesScreen(viewModel = vm, puedeBorrar = permisos.puedeBorrar, onNavigateBack = { navController.popBackStack() })
        }

        composable("modificadores") {
            if (!permisos.puedeAbrir("modificadores")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: ModificadorViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = modificadorViewModelFactory)
            ModificadoresScreen(viewModel = vm, puedeBorrar = permisos.puedeBorrar, onNavigateBack = { navController.popBackStack() })
        }

        composable("food_cost") {
            if (!permisos.puedeAbrir("food_cost")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: FoodCostViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = foodCostViewModelFactory)
            FoodCostScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        // ── Personal ─────────────────────────────────────────────────────────
        composable("empleados") {
            if (!permisos.puedeAbrir("empleados")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: EmpleadoViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = empleadoViewModelFactory)
            EmpleadosScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("mano_obra") {
            if (!permisos.puedeAbrir("mano_obra")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: ManoObraViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = manoObraViewModelFactory)
            ManoObraScreen(viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() })
        }

        // ── Administración ───────────────────────────────────────────────────
        composable("usuarios") {
            if (!permisos.puedeAbrir("usuarios")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            UsuariosScreen(viewModel = authViewModel, onNavigateBack = { navController.popBackStack() })
        }

        composable("locales") {
            if (!permisos.puedeAbrir("locales")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: LocalViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = localViewModelFactory)
            LocalesScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("asignaciones_local") {
            if (!permisos.puedeAbrir("asignaciones_local")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            AsignacionesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("kpis") {
            if (!permisos.puedeAbrir("kpis")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: KpisViewModel = viewModel(viewModelStoreOwner = activityOwner)
            KpisScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("ventas_historial") {
            VentasHistorialScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("comprobantes") {
            val vm: ComprobantesViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = comprobantesViewModelFactory)
            ComprobantesScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("promociones") {
            if (!permisos.puedeAbrir("promociones")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: PromocionViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = promocionViewModelFactory)
            PromocionesScreen(viewModel = vm, puedeBorrar = permisos.puedeBorrar, onNavigateBack = { navController.popBackStack() })
        }

        composable("exportacion") {
            if (!permisos.puedeAbrir("exportacion")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: ExportacionViewModel = viewModel(viewModelStoreOwner = activityOwner, factory = exportacionViewModelFactory)
            ExportacionScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("config_color") {
            if (!permisos.puedeAbrir("config_color")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            com.toppis.app.ui.ajustes.ConfiguracionColorScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Control de Costos ────────────────────────────────────────────────
        composable("costos_fijos") {
            if (!permisos.puedeAbrir("costos_fijos")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: com.toppis.app.ui.costos.CostoFijoViewModel =
                viewModel(viewModelStoreOwner = activityOwner, factory = costoFijoViewModelFactory)
            com.toppis.app.ui.costos.CostosFijosScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("cierre_semanal") {
            if (!permisos.puedeAbrir("cierre_semanal")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: com.toppis.app.ui.costos.CierreSemanalViewModel =
                viewModel(viewModelStoreOwner = activityOwner, factory = cierreSemanalViewModelFactory)
            com.toppis.app.ui.costos.CierreSemanalScreen(
                viewModel = vm, usuarioId = usuarioActual?.id, onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("objetivos_costos") {
            if (!permisos.puedeAbrir("objetivos_costos")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: com.toppis.app.ui.costos.ObjetivosViewModel =
                viewModel(viewModelStoreOwner = activityOwner, factory = objetivosViewModelFactory)
            com.toppis.app.ui.costos.ObjetivosScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable("ayuda_costos") {
            com.toppis.app.ui.costos.AyudaCostosScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("rutina_semanal") {
            if (!permisos.puedeAbrir("rutina_semanal")) { LaunchedEffect(Unit) { navController.popBackStack() }; return@composable }
            val vm: com.toppis.app.ui.costos.RutinaSemanalViewModel =
                viewModel(viewModelStoreOwner = activityOwner, factory = rutinaSemanalViewModelFactory)
            com.toppis.app.ui.costos.RutinaSemanalScreen(
                viewModel = vm,
                usuarioId = usuarioActual?.id,
                onIrAConteo = { navController.navigate("conteos") },
                onIrAMermas = { navController.navigate("mermas") },
                onIrAResultado = { navController.navigate("cierre_semanal") },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
