package com.toppis.app.ui.kpis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val money = DecimalFormat("$#,##0")
private val pct = DecimalFormat("0.#")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpisScreen(
    viewModel: KpisViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val k by viewModel.kpis.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    Scaffold(
        topBar = { ToppisTopBar(titulo = "KPIs Ejecutivos", onBack = onNavigateBack) }
    ) { padding ->
        if (cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Mes actual", style = MaterialTheme.typography.titleMedium)

                // Ventas
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard("Ventas mes", money.format(k.ventasMes), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    KpiCard("Ventas hoy", money.format(k.ventasHoy), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard("Ticket promedio", money.format(k.ticketPromedio), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    KpiCard("Nº ventas mes", k.totalVentasMes.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }

                HorizontalDivider()

                // Costos
                val primeColor = when {
                    k.ventasMes == 0.0 -> MaterialTheme.colorScheme.outline
                    k.primeCostPct <= 65 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
                KpiCard("Prime Cost", "${pct.format(k.primeCostPct)}% de ventas", primeColor, Modifier.fillMaxWidth(),
                    subtitle = "Meta ≤ 60–65%")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fcColor = when {
                        k.foodCostPct == 0.0 -> MaterialTheme.colorScheme.outline
                        k.foodCostPct <= 32 -> MaterialTheme.colorScheme.primary
                        k.foodCostPct <= 40 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    KpiCard("Food cost", "${pct.format(k.foodCostPct)}%", fcColor, Modifier.weight(1f), subtitle = "Meta 28–35%")
                    KpiCard("Labor cost", "${pct.format(k.laborCostPct)}%", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }

                HorizontalDivider()

                // Alertas
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val mColor = if (k.mermaCostoMes > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    KpiCard("Merma del mes", money.format(k.mermaCostoMes), mColor, Modifier.weight(1f))
                    val parColor = if (k.articulosBajoPar > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    KpiCard("Bajo stock mín.", "${k.articulosBajoPar} art.", parColor, Modifier.weight(1f))
                }
                if (k.lotesProxVencer > 0) {
                    KpiCard("Lotes por vencer (<7 días)", "${k.lotesProxVencer}", MaterialTheme.colorScheme.error, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    valor: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(valor, style = MaterialTheme.typography.titleLarge, color = color)
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
        }
    }
}
