package com.toppis.app.ui.kpis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val semana by viewModel.semana.collectAsState()
    val delivery by viewModel.delivery.collectAsState()
    val resultado by viewModel.resultado.collectAsState()

    Scaffold(
        topBar = { ToppisTopBar(titulo = "KPIs Semanales", onBack = onNavigateBack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selector de semana (controla toda la pantalla).
            SelectorSemana(
                etiqueta = semana.etiqueta,
                onPrev = { viewModel.cambiarSemana(-1) },
                onNext = { viewModel.cambiarSemana(1) }
            )

            if (cargando) {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // ── Ventas ────────────────────────────────────────────────────────
            SeccionTitulo("Ventas")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Ventas de la semana", money.format(k.ventasSemana), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                if (k.semanaContieneHoy) {
                    KpiCard("Ventas hoy", money.format(k.ventasHoy), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                } else {
                    KpiCard("Nº ventas", k.totalVentasSemana.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Ticket promedio", money.format(k.ticketPromedio), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                if (k.semanaContieneHoy) {
                    KpiCard("Nº ventas semana", k.totalVentasSemana.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            KpiCard("🍔 Hamburguesas vendidas", k.hamburguesasVendidas.toString(),
                MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth(),
                subtitle = "Unidades en la semana (incluye promos)")

            // ── Costos ──────────────────────────────────────────────────────────
            SeccionTitulo("Costos de la semana")
            val primeColor = when {
                k.ventasSemana == 0.0 -> MaterialTheme.colorScheme.outline
                k.primeCostPct <= 65 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
            KpiCard("Prime Cost", "${pct.format(k.primeCostPct)}% de ventas", primeColor, Modifier.fillMaxWidth(),
                subtitle = "Comida + mano de obra · Meta ≤ 60–65%")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val fcColor = when {
                    k.foodCostPct == 0.0 -> MaterialTheme.colorScheme.outline
                    k.foodCostPct <= 32 -> MaterialTheme.colorScheme.primary
                    k.foodCostPct <= 40 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                KpiCard("Food cost", "${pct.format(k.foodCostPct)}%", fcColor, Modifier.weight(1f), subtitle = "Meta 28–35%")
                KpiCard("Labor cost", "${pct.format(k.laborCostPct)}%", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f),
                    subtitle = "Sueldo fijo prorrateado")
            }

            // ── Resultado total de la semana ──────────────────────────────────────
            SeccionTitulo("Resultado de la semana")
            ResultadoCard(resultado)

            // ── Alertas ──────────────────────────────────────────────────────────
            SeccionTitulo("Alertas")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val mColor = if (k.mermaCostoSemana > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                KpiCard("Merma de la semana", money.format(k.mermaCostoSemana), mColor, Modifier.weight(1f))
                val parColor = if (k.articulosBajoPar > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                KpiCard("Bajo stock mín.", "${k.articulosBajoPar} art.", parColor, Modifier.weight(1f))
            }
            if (k.lotesProxVencer > 0) {
                KpiCard("Lotes por vencer (<7 días)", "${k.lotesProxVencer}", MaterialTheme.colorScheme.error, Modifier.fillMaxWidth())
            }

            // ── Delivery de la semana ─────────────────────────────────────────────
            SeccionTitulo("🛵 Delivery de la semana")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Delivery de la semana", money.format(delivery.total), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                KpiCard("Pedidos con envío", delivery.pedidosConEnvio.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }
            if (delivery.porDia.isEmpty()) {
                Text("Sin delivery esta semana.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            } else {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        delivery.porDia.forEachIndexed { i, d ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(d.dia, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("${d.pedidos} ${if (d.pedidos == 1) "pedido" else "pedidos"}",
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Text(money.format(d.monto), style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                            }
                            if (i < delivery.porDia.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SelectorSemana(etiqueta: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Semana anterior") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Semana", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text(etiqueta, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = "Semana siguiente") }
        }
    }
}

@Composable
private fun SeccionTitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ResultadoCard(r: com.toppis.app.domain.costos.ResultadoSemanal?) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (r == null) {
                Text("Calculando…", color = MaterialTheme.colorScheme.outline)
                return@Column
            }
            FilaKpi("Ventas cobradas", money.format(r.ventasCobradas))
            FilaKpi("(−) Costos variables (insumos/packaging/bencina)", money.format(r.costoVariable))
            FilaKpi("(−) Mano de obra", money.format(r.manoObraPagada))
            FilaKpi("(−) Costos fijos (prorrateados)", money.format(r.fijosProrrateados))
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            val resColor = when {
                r.ventasCobradas == 0.0 -> MaterialTheme.colorScheme.outline
                r.resultado >= 0 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
            FilaKpi(if (r.resultado >= 0) "RESULTADO (queda)" else "RESULTADO (pérdida)",
                money.format(r.resultado), resColor, destacado = true)
            Text("Los costos fijos y variables completos se administran en Costos.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun FilaKpi(label: String, valor: String, color: Color = MaterialTheme.colorScheme.onSurface, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
        Text(valor, style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}

@Composable
private fun KpiCard(
    label: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
        }
    }
}
