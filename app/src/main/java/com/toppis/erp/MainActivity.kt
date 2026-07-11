package com.toppis.erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.toppis.erp.ui.theme.ToppisERPTheme
import com.toppis.app.data.repository.AuthRepository
import com.toppis.app.data.repository.ComandaRepository
import com.toppis.app.data.repository.ComprobanteRepository
import com.toppis.app.data.repository.ExportacionRepository
import com.toppis.app.data.repository.FlujoCajaRepository
import com.toppis.app.data.repository.GastoRepository
import com.toppis.app.data.repository.ArticuloRepository
import com.toppis.app.data.repository.MenuRepository
import com.toppis.app.data.repository.ReporteRepository
import com.toppis.app.data.repository.SobreRepository
import com.toppis.app.data.repository.VentaRepository
import com.toppis.app.ui.auth.AuthViewModelFactory
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
        // Splash de arranque (debe ir ANTES de super.onCreate).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        com.toppis.app.data.repository.LocalSession.init(applicationContext)
        com.toppis.erp.ui.theme.ThemeManager.init(applicationContext)

        // Manual DI (todos los repositorios usan Supabase)
        val inventarioRepo = ArticuloRepository()
        val sobreRepo = SobreRepository()
        val ventaRepo = VentaRepository()

        // ── Rediseño POS: pedidos + clientes ──
        val pedidoRepo = com.toppis.app.data.repository.PedidoRepository()
        val clienteRepo = com.toppis.app.data.repository.ClienteRepository()
        val pedidosFactory = com.toppis.app.ui.pos.PedidosViewModelFactory(pedidoRepo, clienteRepo)
        val comandasFactory = com.toppis.app.ui.pos.ComandasViewModelFactory(pedidoRepo, clienteRepo)
        val clientesFactory = com.toppis.app.ui.pos.ClientesViewModelFactory(clienteRepo, pedidoRepo)

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

        val foodCostFactory = com.toppis.app.ui.foodcost.FoodCostViewModelFactory(menuRepo)

        // Carrito del POS (necesita menú, modificadores y promociones)
        val carritoFactory = com.toppis.app.ui.pos.CarritoViewModelFactory(pedidoRepo, menuRepo, modificadorRepo, promocionRepo, sobreRepo, clienteRepo)

        val mermaRepo = com.toppis.app.data.repository.MermaRepository()
        val mermaFactory = com.toppis.app.ui.mermas.MermaViewModelFactory(mermaRepo)

        val conteoRepo = com.toppis.app.data.repository.ConteoRepository()
        val conteoFactory = com.toppis.app.ui.conteos.ConteoViewModelFactory(conteoRepo)

        val compraSugeridaFactory = com.toppis.app.ui.compras.CompraSugeridaViewModelFactory(inventarioRepo)

        val varianceFactory = com.toppis.app.ui.variance.VarianceViewModelFactory(
            com.toppis.app.data.repository.VarianceRepository()
        )

        // ── Fase 6: Proveedores / Compras ──
        val proveedorFactory = com.toppis.app.ui.proveedores.ProveedorViewModelFactory(
            com.toppis.app.data.repository.ProveedorRepository()
        )
        val compraFactory = com.toppis.app.ui.compras.CompraViewModelFactory(
            com.toppis.app.data.repository.CompraRepository()
        )

        // ── Fase 7: Arqueo de caja ──
        val arqueoFactory = com.toppis.app.ui.arqueo.ArqueoViewModelFactory(
            com.toppis.app.data.repository.ArqueoRepository()
        )

        // ── Fase 8: Empleados / Mano de obra ──
        val empleadoFactory = com.toppis.app.ui.empleados.EmpleadoViewModelFactory(
            com.toppis.app.data.repository.EmpleadoRepository()
        )
        val manoObraFactory = com.toppis.app.ui.manoobra.ManoObraViewModelFactory(
            com.toppis.app.data.repository.ManoObraRepository()
        )

        // ── Fase 9: Locales / multi-local ──
        val localFactory = com.toppis.app.ui.locales.LocalViewModelFactory(
            com.toppis.app.data.repository.LocalRepository()
        )

        // ── Control de Costos ──
        val configCostosRepo = com.toppis.app.data.repository.ConfigCostosRepository()
        val costoFijoRepo = com.toppis.app.data.repository.CostoFijoRepository()
        val cierreSemanalRepo = com.toppis.app.data.repository.CierreSemanalRepository()
        val resultadoSemanalRepo = com.toppis.app.data.repository.ResultadoSemanalRepository(
            configCostosRepo, costoFijoRepo, cierreSemanalRepo
        )
        val costoFijoFactory = com.toppis.app.ui.costos.CostoFijoViewModelFactory(costoFijoRepo)
        val cierreSemanalFactory = com.toppis.app.ui.costos.CierreSemanalViewModelFactory(
            resultadoSemanalRepo, cierreSemanalRepo
        )
        val objetivosFactory = com.toppis.app.ui.costos.ObjetivosViewModelFactory(configCostosRepo)
        val rutinaSemanalRepo = com.toppis.app.data.repository.RutinaSemanalRepository()
        val rutinaSemanalFactory = com.toppis.app.ui.costos.RutinaSemanalViewModelFactory(
            rutinaSemanalRepo, costoFijoRepo, sobreRepo
        )

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

        enableEdgeToEdge()
        setContent {
            ToppisERPTheme {
                NavGraph(
                        sobreViewModelFactory = sobreFactory,
                        posViewModelFactory = posFactory,
                        inventarioViewModelFactory = inventarioFactory,
                        gastoViewModelFactory = gastoFactory,
                        reporteViewModelFactory = reporteFactory,
                        exportacionViewModelFactory = exportacionFactory,
                        flujoCajaViewModelFactory = flujoCajaFactory,
                        menuConfigViewModelFactory = menuConfigFactory,
                        comprobantesViewModelFactory = comprobantesFactory,
                        contabilidadViewModelFactory = contabilidadFactory,
                        preparacionViewModelFactory = preparacionFactory,
                        modificadorViewModelFactory = modificadorFactory,
                        promocionViewModelFactory = promocionFactory,
                        foodCostViewModelFactory = foodCostFactory,
                        mermaViewModelFactory = mermaFactory,
                        conteoViewModelFactory = conteoFactory,
                        compraSugeridaViewModelFactory = compraSugeridaFactory,
                        varianceViewModelFactory = varianceFactory,
                        proveedorViewModelFactory = proveedorFactory,
                        compraViewModelFactory = compraFactory,
                        arqueoViewModelFactory = arqueoFactory,
                        empleadoViewModelFactory = empleadoFactory,
                        manoObraViewModelFactory = manoObraFactory,
                        localViewModelFactory = localFactory,
                        costoFijoViewModelFactory = costoFijoFactory,
                        cierreSemanalViewModelFactory = cierreSemanalFactory,
                        objetivosViewModelFactory = objetivosFactory,
                        rutinaSemanalViewModelFactory = rutinaSemanalFactory,
                        pedidosViewModelFactory = pedidosFactory,
                        carritoViewModelFactory = carritoFactory,
                        comandasViewModelFactory = comandasFactory,
                        clientesViewModelFactory = clientesFactory,
                        authViewModel = authViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
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