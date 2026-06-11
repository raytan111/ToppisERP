package com.toppis.erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.toppis.erp.ui.theme.ToppisERPTheme
import com.toppis.app.data.repository.AuthRepository
import com.toppis.app.data.repository.ComandaRepository
import com.toppis.app.data.repository.ComprobanteRepository
import com.toppis.app.data.repository.DashboardRepository
import com.toppis.app.data.repository.ExportacionRepository
import com.toppis.app.data.repository.FlujoCajaRepository
import com.toppis.app.data.repository.GastoRepository
import com.toppis.app.data.repository.InventarioRepository
import com.toppis.app.data.repository.MenuRepository
import com.toppis.app.data.repository.ReporteRepository
import com.toppis.app.data.repository.SobreRepository
import com.toppis.app.data.repository.VentaRepository
import com.toppis.app.ui.auth.AuthViewModelFactory
import com.toppis.app.ui.dashboard.DashboardViewModelFactory
import com.toppis.app.ui.exportacion.ExportacionViewModelFactory
import com.toppis.app.ui.flujo.FlujoCajaViewModelFactory
import com.toppis.app.ui.gastos.GastoViewModelFactory
import com.toppis.app.ui.inventario.InventarioViewModelFactory
import com.toppis.app.ui.menu.MenuConfigViewModelFactory
import com.toppis.app.ui.navigation.NavGraph
import com.toppis.app.ui.pos.PosViewModelFactory
import com.toppis.app.ui.reportes.ReporteViewModelFactory
import com.toppis.app.ui.sobres.SobreViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manual DI (todos los repositorios usan Supabase)
        val inventarioRepo = InventarioRepository()
        val sobreRepo = SobreRepository()
        val ventaRepo = VentaRepository()

        // ── Menú Interactivo ────────────────────────────────────────────────
        val menuRepo = MenuRepository()
        val menuConfigFactory = MenuConfigViewModelFactory(menuRepo)

        val comandaRepo = ComandaRepository()
        val comprobanteRepo = ComprobanteRepository()
        val comprobantesFactory = com.toppis.app.ui.comprobantes.ComprobantesViewModelFactory(comprobanteRepo)

        val sobreFactory = SobreViewModelFactory(sobreRepo)
        val posFactory = PosViewModelFactory(ventaRepo, sobreRepo, menuRepo, comandaRepo, comprobanteRepo)
        val inventarioFactory = InventarioViewModelFactory(inventarioRepo)
        val gastoRepo = GastoRepository()
        val gastoFactory = GastoViewModelFactory(gastoRepo, sobreRepo)

        val reporteRepo = ReporteRepository()
        val reporteFactory = ReporteViewModelFactory(reporteRepo)

        // ── Auth (Supabase) ───────────────────────────────────────────────────
        val authRepo = AuthRepository()
        val authFactory = AuthViewModelFactory(authRepo)
        // Se instancia aquí para que el mismo VM sobreviva las recomposiciones
        val authViewModel = androidx.lifecycle.ViewModelProvider(this, authFactory)
            .get(com.toppis.app.ui.auth.AuthViewModel::class.java)

        // ── Exportación (Módulo 7) ────────────────────────────────────────────
        val exportacionRepo = ExportacionRepository()
        val exportacionFactory = ExportacionViewModelFactory(exportacionRepo)

        // ── Flujo de Caja (Módulo 8) ──────────────────────────────────────────
        val flujoCajaRepo = FlujoCajaRepository()
        val flujoCajaFactory = FlujoCajaViewModelFactory(flujoCajaRepo)

        // ── Dashboard ───────────────────────────────────────────────────────
        val dashboardRepo = DashboardRepository()
        val dashboardFactory = DashboardViewModelFactory(dashboardRepo)

        enableEdgeToEdge()
        setContent {
            ToppisERPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavGraph(
                        sobreViewModelFactory = sobreFactory,
                        posViewModelFactory = posFactory,
                        inventarioViewModelFactory = inventarioFactory,
                        gastoViewModelFactory = gastoFactory,
                        reporteViewModelFactory = reporteFactory,
                        exportacionViewModelFactory = exportacionFactory,
                        flujoCajaViewModelFactory = flujoCajaFactory,
                        dashboardViewModelFactory = dashboardFactory,
                        menuConfigViewModelFactory = menuConfigFactory,
                        comprobantesViewModelFactory = comprobantesFactory,
                        authViewModel = authViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ToppisERPTheme {
        Greeting("Android")
    }
}