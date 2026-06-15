package com.toppis.app.ui.contabilidad

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toppis.app.data.repository.LineaLibroCompra
import com.toppis.app.data.repository.LineaLibroVenta
import com.toppis.app.data.repository.ResumenContable
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)
private val money = DecimalFormat("$#,##0")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContabilidadScreen(
    viewModel: ContabilidadViewModel,
    usuarioId: String?,
    onNavigateBack: () -> Unit = {}
) {
    val mes by viewModel.mes.collectAsState()
    val anio by viewModel.anio.collectAsState()
    val resumen by viewModel.resumen.collectAsState()
    val libroVentas by viewModel.libroVentas.collectAsState()
    val libroCompras by viewModel.libroCompras.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }
    var mesExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(mensaje) {
        mensaje?.let { snackbar.showSnackbar(it); viewModel.limpiarMensaje() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { ToppisTopBar(titulo = "Contabilidad", onBack = onNavigateBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Selector de período
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = mesExpanded,
                    onExpandedChange = { mesExpanded = !mesExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "${MESES[mes - 1]} $anio",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Período") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mesExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = mesExpanded, onDismissRequest = { mesExpanded = false }) {
                        MESES.forEachIndexed { idx, nombre ->
                            DropdownMenuItem(
                                text = { Text("$nombre $anio") },
                                onClick = {
                                    viewModel.cambiarPeriodo(idx + 1, anio)
                                    mesExpanded = false
                                }
                            )
                        }
                    }
                }
                Button(onClick = { viewModel.cerrarMes(usuarioId) }) {
                    Text("Cerrar mes")
                }
            }

            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Resumen IVA") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Ventas") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Compras") })
            }

            when (tab) {
                0 -> ResumenTab(resumen)
                1 -> LibroVentasTab(libroVentas)
                2 -> LibroComprasTab(libroCompras)
            }
        }
    }
}

@Composable
private fun ResumenTab(resumen: ResumenContable?) {
    if (resumen == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // IVA
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Resumen de IVA (base F29)", style = MaterialTheme.typography.titleMedium)
                Linea("IVA débito (ventas)", resumen.ivaDebito)
                Linea("IVA crédito (compras)", resumen.ivaCredito)
                HorizontalDivider()
                val pagar = resumen.ivaAPagar
                Linea(
                    if (pagar >= 0) "IVA a pagar" else "IVA a favor",
                    kotlin.math.abs(pagar),
                    bold = true
                )
            }
        }
        // Resultado
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Estado de resultados", style = MaterialTheme.typography.titleMedium)
                Linea("Ventas netas", resumen.ventasNetas)
                Linea("Compras/gastos netos", resumen.comprasNetas)
                HorizontalDivider()
                Linea("Resultado operacional", resumen.resultado, bold = true)
            }
        }
        // Totales brutos
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Totales (con IVA)", style = MaterialTheme.typography.titleMedium)
                Linea("Total ventas", resumen.totalVentas)
                Linea("Total gastos", resumen.totalGastos)
            }
        }
    }
}

@Composable
private fun Linea(label: String, valor: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(money.format(valor), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun LibroVentasTab(items: List<LineaLibroVenta>) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin ventas en el período", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
        items(items) { l ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Venta #${l.ventaId}" + (l.folioComprobante?.let { " · Comp #$it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium)
                        Text(money.format(l.total), fontWeight = FontWeight.Bold)
                    }
                    Text("Neto ${money.format(l.neto)} · IVA ${money.format(l.iva)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LibroComprasTab(items: List<LineaLibroCompra>) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin compras/gastos en el período", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
        items(items) { l ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(l.descripcion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(money.format(l.total), fontWeight = FontWeight.Bold)
                    }
                    Text("Neto ${money.format(l.neto)} · IVA ${money.format(l.iva)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
