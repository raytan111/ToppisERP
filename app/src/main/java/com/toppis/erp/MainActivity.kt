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
import com.toppis.app.data.repository.ArticuloRepository
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
        val inventarioRepo = ArticuloRepository()
        val sobreRepo = SobreRepository()
        val ventaRepo = VentaRepository()

        // ── Menú Interactivo ────────────────────────────────────────────────
        val menuRepo = MenuRepository()
        val menuConfigFactory = MenuConfigViewModelFactory(menuRepo)

        val comandaRepo = ComandaRepository()
        val comprobanteRepo = ComprobanteRepository()
        val modificadorRepo = com.toppis.app.data.repository.ModificadorRepository()
        val promocionRepo = com.toppis.app.data.repository.PromocionRepository()
        val comprobantesFactory = com.toppis.app.ui.comprobantes.ComprobantesViewModelFactory(comprobanteRepo)
        val contabilidadFactory = com.toppis.app.ui.contabilidad.ContabilidadViewModelFactory(
            com.toppis.app.data.repository.ContabilidadRepository()
        )

        val sobreFactory = SobreViewModelFactory(sobreRepo)
        val posFactory = PosViewModelFactory(ventaRepo, sobreRepo, menuRepo, comandaRepo, comprobanteRepo, modificadorRepo, promocionRepo)
        val inventarioFactory = InventarioViewModelFactory(inventarioRepo)
        val gastoRepo = GastoRepository()
        val gastoFactory = GastoViewModelFactory(gastoRepo, sobreRepo)

        val reporteRepo = ReporteRepository()
        val reporteFactory = ReporteViewModelFactory(reporteRepo)

        // ── Fase 4: Preparaciones, Modificadores, Promociones, Papa, Food Cost ──
        val preparacionRepo = com.toppis.app.data.repository.PreparacionRepository()
        val preparacionFactory = com.toppis.app.ui.preparaciones.PreparacionViewModelFactory(preparacionRepo)

        val modificadorFactory = com.toppis.app.ui.modificadores.ModificadorViewModelFactory(modificadorRepo)

        val promocionFactory = com.toppis.app.ui.promociones.PromocionViewModelFactory(promocionRepo)
        // promocionRepo y modificadorRepo se definen arriba (los usa también el POS)

        val papaRepo = com.toppis.app.data.repository.PapaRendimientoRepository()
        val papaFactory = com.toppis.app.ui.papa.PapaRendimientoViewModelFactory(papaRepo, inventarioRepo)

        val foodCostFactory = com.toppis.app.ui.foodcost.FoodCostViewModelFactory(menuRepo)

        val mermaRepo = com.toppis.app.data.repository.MermaRepository()
        val mermaFactory = com.toppis.app.ui.mermas.MermaViewModelFactory(mermaRepo)

        val conteoRepo = com.toppis.app.data.repository.ConteoRepository()
        val conteoFactory = com.toppis.app.ui.conteos.ConteoViewModelFactory(conteoRepo)

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
                        contabilidadViewModelFactory = contabilidadFactory,
                        preparacionViewModelFactory = preparacionFactory,
                        modificadorViewModelFactory = modificadorFactory,
                        promocionViewModelFactory = promocionFactory,
                        papaViewModelFactory = papaFactory,
                        foodCostViewModelFactory = foodCostFactory,
                        mermaViewModelFactory = mermaFactory,
                        conteoViewModelFactory = conteoFactory,
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