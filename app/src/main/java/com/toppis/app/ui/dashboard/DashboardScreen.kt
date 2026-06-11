package com.toppis.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toppis.app.data.repository.DashboardKpi
import com.toppis.app.data.repository.DatoSerie
import com.toppis.app.data.repository.DistribucionEgreso
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val moneyFmt = DecimalFormat("$#,##0")

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val kpi by viewModel.kpi.collectAsState()
    val serie by viewModel.serieTiempo.collectAsState()
    val distribucion by viewModel.distribucion.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            if (kpi == null) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                KpiCards(kpi = kpi!!)
                if (serie.isNotEmpty()) {
                    Text("Flujo de Caja", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AreaChart(data = serie, modifier = Modifier.fillMaxWidth().height(220.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        LegendItem(Color(0xFF4CAF50), "Ingresos")
                        Spacer(Modifier.width(16.dp))
                        LegendItem(Color(0xFFE53935), "Egresos")
                    }
                }
                if (distribucion.isNotEmpty()) {
                    Text("Distribución de Egresos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    DonutChart(data = distribucion, modifier = Modifier.fillMaxWidth().height(260.dp))
                }
            }
        Spacer(Modifier.height(16.dp))
    }
}

// ── KPI Cards ────────────────────────────────────────────────────────────────────

@Composable
private fun KpiCards(kpi: DashboardKpi) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                title = "Ganancia Neta",
                value = moneyFmt.format(kpi.gananciaNeta),
                previous = kpi.gananciaNetaAnterior,
                current = kpi.gananciaNeta,
                color = if (kpi.gananciaNeta >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Flujo de Caja",
                value = moneyFmt.format(kpi.flujoCajaTotal),
                previous = null,
                current = null,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(
                title = "Ingresos",
                value = moneyFmt.format(kpi.ingresos),
                previous = kpi.ingresosAnterior,
                current = kpi.ingresos,
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Egresos",
                value = moneyFmt.format(kpi.egresos),
                previous = kpi.egresosAnterior,
                current = kpi.egresos,
                color = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
        }
        KpiCard(
            title = "Margen de Ganancia",
            value = "${"%.1f".format(kpi.margenPorcentaje)}%",
            previous = null,
            current = null,
            color = if (kpi.margenPorcentaje >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    previous: Double?,
    current: Double?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            if (previous != null && current != null) {
                val diff = current - previous
                val isUp = diff >= 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUp) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (isUp) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${if (isUp) "+" else ""}${moneyFmt.format(diff)} vs anterior",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUp) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                }
            }
        }
    }
}

// ── Area Chart ───────────────────────────────────────────────────────────────────

@Composable
private fun AreaChart(data: List<DatoSerie>, modifier: Modifier = Modifier) {
    var tooltipIdx by remember { mutableIntStateOf(-1) }

    Card(modifier = modifier) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            if (data.isEmpty()) return@detectTapGestures
                            val stepX = size.width.toFloat() / (data.size - 1).coerceAtLeast(1)
                            val idx = ((offset.x / stepX) + 0.5f).toInt().coerceIn(0, data.lastIndex)
                            tooltipIdx = if (tooltipIdx == idx) -1 else idx
                        }
                    }
            ) {
                if (data.isEmpty()) return@Canvas
                val n = data.size
                val maxVal = data.maxOf { maxOf(it.ingresos, it.egresos) }.coerceAtLeast(1.0)
                val chartH = size.height * 0.85f
                val baseY = size.height * 0.9f
                val stepX = if (n > 1) size.width / (n - 1).toFloat() else size.width

                val colorIng = Color(0xFF4CAF50)
                val colorEgr = Color(0xFFE53935)

                fun yFor(v: Double) = baseY - ((v / maxVal) * chartH).toFloat()

                // Area ingresos
                val pathIng = Path().apply {
                    moveTo(0f, baseY)
                    data.forEachIndexed { i, d -> lineTo(i * stepX, yFor(d.ingresos)) }
                    lineTo((n - 1) * stepX, baseY)
                    close()
                }
                drawPath(pathIng, colorIng.copy(alpha = 0.2f), style = Fill)
                // Line ingresos
                data.forEachIndexed { i, d ->
                    if (i > 0) {
                        drawLine(colorIng, Offset((i - 1) * stepX, yFor(data[i - 1].ingresos)), Offset(i * stepX, yFor(d.ingresos)), strokeWidth = 3f)
                    }
                }

                // Area egresos
                val pathEgr = Path().apply {
                    moveTo(0f, baseY)
                    data.forEachIndexed { i, d -> lineTo(i * stepX, yFor(d.egresos)) }
                    lineTo((n - 1) * stepX, baseY)
                    close()
                }
                drawPath(pathEgr, colorEgr.copy(alpha = 0.2f), style = Fill)
                // Line egresos
                data.forEachIndexed { i, d ->
                    if (i > 0) {
                        drawLine(colorEgr, Offset((i - 1) * stepX, yFor(data[i - 1].egresos)), Offset(i * stepX, yFor(d.egresos)), strokeWidth = 3f)
                    }
                }

                // Dots
                data.forEachIndexed { i, d ->
                    drawCircle(colorIng, 4f, Offset(i * stepX, yFor(d.ingresos)))
                    drawCircle(colorEgr, 4f, Offset(i * stepX, yFor(d.egresos)))
                }

                // Tooltip
                if (tooltipIdx in data.indices) {
                    val d = data[tooltipIdx]
                    val x = tooltipIdx * stepX
                    drawLine(Color.Gray, Offset(x, 0f), Offset(x, baseY), strokeWidth = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                    val paint = android.graphics.Paint().apply {
                        textSize = 28f; color = android.graphics.Color.DKGRAY; isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${d.label}: Ing ${moneyFmt.format(d.ingresos)} | Egr ${moneyFmt.format(d.egresos)}",
                        8f, 24f, paint
                    )
                }

                // Base line
                drawLine(Color(0xFFBDBDBD), Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1f)
            }
        }
    }
}

// ── Donut Chart ──────────────────────────────────────────────────────────────────

private val donutColors = listOf(
    Color(0xFF4CAF50), Color(0xFFE53935), Color(0xFF2196F3),
    Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF00BCD4),
    Color(0xFFFF5722), Color(0xFF795548)
)

@Composable
private fun DonutChart(data: List<DistribucionEgreso>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.monto }
    if (total <= 0) return

    Card(modifier = modifier) {
        Row(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(160.dp)) {
                val strokeW = 36f
                val radius = (size.minDimension - strokeW) / 2
                val center = Offset(size.width / 2, size.height / 2)
                var startAngle = -90f

                data.forEachIndexed { idx, item ->
                    val sweep = (item.monto / total * 360f).toFloat()
                    drawArc(
                        color = donutColors[idx % donutColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeW)
                    )
                    startAngle += sweep
                }
            }

            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                data.forEachIndexed { idx, item ->
                    val pct = (item.monto / total * 100).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(donutColors[idx % donutColors.size], MaterialTheme.shapes.extraSmall)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${item.categoria} (${pct}%)",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ── Legend item ───────────────────────────────────────────────────────────────────

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color, MaterialTheme.shapes.extraSmall))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
