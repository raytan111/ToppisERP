package com.toppis.app.ui.reportes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.models.Venta
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReportesScreen(
    viewModel: ReporteViewModel,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val periodo by viewModel.periodo.collectAsState()
    val totalVentas by viewModel.totalVentas.collectAsState()
    val totalGastos by viewModel.totalGastos.collectAsState()
    val balanceNeto by viewModel.balanceNeto.collectAsState()
    val ultimasVentas by viewModel.ultimasVentas.collectAsState()
    val gastosPorCategoria by viewModel.gastosPorCategoria.collectAsState()
    val sobres by viewModel.sobres.collectAsState()
    val ivaProvisionado by viewModel.ivaProvisionado.collectAsState()
    val locales by viewModel.locales.collectAsState()
    val localFiltro by viewModel.localFiltro.collectAsState()

    // Recargar al abrir (refleja cambios hechos fuera de la app).
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.recargarManual() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // ── Selector de período ──────────────────────────────────────────
            item {
                if (locales.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = localFiltro == null,
                            onClick = { viewModel.seleccionarLocal(null) },
                            label = { Text("Todos") }
                        )
                        Spacer(Modifier.width(8.dp))
                        locales.forEach { l ->
                            FilterChip(
                                selected = localFiltro == l.id,
                                onClick = { viewModel.seleccionarLocal(l.id) },
                                label = { Text(l.nombre) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                PeriodoSelector(
                    periodoActual = periodo,
                    onSeleccionar = { viewModel.seleccionarPeriodo(it) }
                )
            }

            // ── Cards de resumen ─────────────────────────────────────────────
            item {
                ResumenCards(
                    totalVentas = totalVentas,
                    totalGastos = totalGastos,
                    balanceNeto = balanceNeto
                )
            }

            // ── IVA provisionado ─────────────────────────────────────────────
            item {
                IvaProvisionadoCard(iva = ivaProvisionado)
            }

            // ── Gastos por categoría ─────────────────────────────────────────
            item {
                GastosPorCategoriaCard(mapa = gastosPorCategoria)
            }

            // ── Balance de sobres ────────────────────────────────────────────
            item {
                SobresBalanceCard(sobres = sobres)
            }

            // ── Últimas ventas ───────────────────────────────────────────────
            item {
                Text(
                    text = "Últimas ventas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (ultimasVentas.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay ventas en este período",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(ultimasVentas) { venta ->
                    VentaReporteCard(venta = venta)
                }
            }
    }
}

// ── Selector de período ────────────────────────────────────────────────────────

@Composable
private fun PeriodoSelector(
    periodoActual: PeriodoReporte,
    onSeleccionar: (PeriodoReporte) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodoReporte.entries.forEach { p ->
            FilterChip(
                selected = p == periodoActual,
                onClick = { onSeleccionar(p) },
                label = { Text(p.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Cards de resumen ───────────────────────────────────────────────────────────

@Composable
private fun ResumenCards(
    totalVentas: Double,
    totalGastos: Double,
    balanceNeto: Double
) {
    val formatter = DecimalFormat("$#,##0 CLP")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResumenCard(
                titulo = "Ventas",
                valor = formatter.format(totalVentas),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            ResumenCard(
                titulo = "Gastos",
                valor = formatter.format(totalGastos),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
        val isPositive = balanceNeto >= 0
        ResumenCard(
            titulo = "Balance Neto",
            valor = (if (isPositive) "+" else "") + formatter.format(balanceNeto),
            containerColor = if (isPositive)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.errorContainer,
            contentColor = if (isPositive)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResumenCard(
    titulo: String,
    valor: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
        }
    }
}

// ── Gastos por categoría ───────────────────────────────────────────────────────

@Composable
private fun GastosPorCategoriaCard(mapa: Map<CategoriaGasto, Double>) {
    val formatter = DecimalFormat("$#,##0 CLP")
    val total = mapa.values.sum()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gastos por categoría",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (mapa.isEmpty()) {
                Text(
                    text = "Sin gastos en este período",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Ordenar por monto descendente
                mapa.entries.sortedByDescending { it.value }.forEach { (cat, monto) ->
                    val porcentaje = if (total > 0) (monto / total * 100).toInt() else 0
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${formatter.format(monto)} ($porcentaje%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (total > 0) (monto / total).toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(top = 2.dp),
                            color = MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.errorContainer
                        )
                    }
                }
            }
        }
    }
}

// ── Sobres balance ─────────────────────────────────────────────────────────────

@Composable
private fun SobresBalanceCard(sobres: List<Sobre>) {
    val formatter = DecimalFormat("$#,##0 CLP")
    val totalSaldo = sobres.sumOf { it.saldo }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Balance de sobres",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatter.format(totalSaldo),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (sobres.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No hay sobres creados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                sobres.forEach { sobre ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sobre.nombre,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatter.format(sobre.saldo),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (sobre.saldo > 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─��� Card de venta individual ───────────────────────────────────────────────────

@Composable
private fun VentaReporteCard(venta: Venta) {
    val moneyFormatter = DecimalFormat("$#,##0 CLP")
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatFechaIsoReporte(venta.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = venta.metodoPago?.name ?: "—",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = moneyFormatter.format(venta.total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}




/** Formatea un timestamp ISO (de Supabase) a "dd/MM/yyyy HH:mm" en hora local. */
private fun formatFechaIsoReporte(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        OffsetDateTime.parse(iso)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    } catch (e: Exception) {
        iso.take(16).replace("T", " ")
    }
}

@Composable
private fun IvaProvisionadoCard(iva: Double) {
    val fmt = DecimalFormat("$#,##0")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "IVA provisionado (19%)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = fmt.format(iva),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "Acumulado de comprobantes del período. Reserva estimada para impuestos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
