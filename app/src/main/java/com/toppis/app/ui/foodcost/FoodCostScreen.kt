package com.toppis.app.ui.foodcost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCostScreen(
    viewModel: FoodCostViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val filas by viewModel.filas.collectAsState()
    val promedio by viewModel.foodCostPromedio.collectAsState()
    val money = DecimalFormat("$#,##0")
    val pct = DecimalFormat("0.#")

    Scaffold(
        topBar = { ToppisTopBar(titulo = "Food Cost & Menú", onBack = onNavigateBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // KPI promedio
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        promedio == 0.0 -> MaterialTheme.colorScheme.surfaceVariant
                        promedio <= 32 -> MaterialTheme.colorScheme.primaryContainer
                        promedio <= 40 -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Food cost promedio del menú", style = MaterialTheme.typography.labelMedium)
                    Text("${pct.format(promedio)}%", style = MaterialTheme.typography.headlineMedium)
                    Text("Meta saludable: 28–35% (QSR)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }

            if (filas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin items en el menú.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filas) { fila ->
                        val fc = fila.item.foodCostPct
                        val fcColor = when {
                            fila.item.costoTeorico <= 0.0 -> MaterialTheme.colorScheme.outline
                            fc <= 32 -> MaterialTheme.colorScheme.primary
                            fc <= 40 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(fila.item.item.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Precio ${money.format(fila.item.precio)} · Costo ${money.format(fila.item.costoTeorico)} · Margen ${money.format(fila.item.margen)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(fila.clase.label, style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary)
                                }
                                Text(
                                    if (fila.item.costoTeorico <= 0.0) "—" else "${pct.format(fc)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = fcColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
