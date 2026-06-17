package com.toppis.app.ui.variance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VarianceScreen(
    viewModel: VarianceViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val items by viewModel.items.collectAsState()
    val periodo by viewModel.periodo.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val money = DecimalFormat("$#,##0")
    val num = DecimalFormat("#,##0.##")

    val totalTeorico = items.sumOf { it.costoTeorico }
    val totalMerma = items.sumOf { it.mermaCosto }

    Scaffold(
        topBar = { ToppisTopBar(titulo = "Análisis de Inventario", onBack = onNavigateBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Selector de período
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.mesAnterior() }) { Icon(Icons.Filled.ChevronLeft, "Mes anterior") }
                Text("${periodo.monthValue.toString().padStart(2, '0')}/${periodo.year}",
                    style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { viewModel.mesSiguiente() }) { Icon(Icons.Filled.ChevronRight, "Mes siguiente") }
            }

            // KPIs
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Consumo teórico", style = MaterialTheme.typography.labelSmall)
                        Text(money.format(totalTeorico), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Merma registrada", style = MaterialTheme.typography.labelSmall)
                        Text(money.format(totalMerma), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Text(
                "Teórico = lo que las recetas dicen que se consumió según ventas. El variance real completo (vs compras) llega en Fase 6.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin datos en el período.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items) { it2 ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(it2.nombre, style = MaterialTheme.typography.titleSmall)
                                Text("Consumo teórico: ${num.format(it2.consumoTeorico)} ${it2.unidad} · ${money.format(it2.costoTeorico)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                if (it2.mermaCosto > 0) {
                                    Text("Merma registrada: ${num.format(it2.mermaCantidad)} ${it2.unidad} · ${money.format(it2.mermaCosto)}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
