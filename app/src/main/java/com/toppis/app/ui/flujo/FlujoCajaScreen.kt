package com.toppis.app.ui.flujo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.repository.PresupuestoVsReal
import com.toppis.app.data.repository.ProyeccionMes
import com.toppis.app.data.repository.ResumenFlujoCaja
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Locale

private val fmt = DecimalFormat("#,##0")

// ── Screen principal ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlujoCajaScreen(
    viewModel: FlujoCajaViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val resumen          by viewModel.resumen.collectAsState()
    val proyeccion       by viewModel.proyeccion.collectAsState()
    val presupuestoVsReal by viewModel.presupuestoVsReal.collectAsState()
    val saldoTotal       by viewModel.saldoTotal.collectAsState()
    val periodo          by viewModel.periodoSeleccionado.collectAsState()

    var selectedTab          by remember { mutableIntStateOf(0) }
    var showPresupuestoDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { ToppisTopBar("📊 Flujo de Caja", onBack = onNavigateBack) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showPresupuestoDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar presupuesto")
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Selector de período ──────────────────────────────────────────
            Text("Período de análisis", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PeriodoFlujo.entries) { p ->
                    FilterChip(
                        selected = periodo == p,
                        onClick  = { viewModel.cambiarPeriodo(p) },
                        label    = { Text(p.label) }
                    )
                }
            }

            // ── Card de resumen ──────────────────────────────────────────────
            if (resumen != null) {
                ResumenCard(resumen = resumen!!, saldoTotal = saldoTotal)

                // ── Gráfico de barras ────────────────────────────────────────
                val todasFechas = (resumen!!.ventasPorDia.keys + resumen!!.gastosPorDia.keys)
                    .distinct().sorted()
                if (todasFechas.isNotEmpty()) {
                    BarChart(
                        ingresosPorDia = resumen!!.ventasPorDia,
                        egresosPorDia  = resumen!!.gastosPorDia,
                        modifier       = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LeyendaItem(color = Color(0xFF4CAF50), label = "Ingresos")
                        Spacer(Modifier.width(16.dp))
                        LeyendaItem(color = Color(0xFFE53935), label = "Egresos")
                    }
                }

                // ── Donut chart de gastos por categoría ───────────────
                if (resumen!!.gastosPorCategoria.isNotEmpty()) {
                    Text("Distribución de Egresos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    DonutChartFlujo(
                        data = resumen!!.gastosPorCategoria,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }

            // ── Tabs ─────────────────────────────────────────────────────────
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    text     = { Text("Presupuesto vs Real") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    text     = { Text("Proyección") }
                )
            }

            Spacer(Modifier.height(4.dp))

            when (selectedTab) {
                0 -> PresupuestoVsRealContent(items = presupuestoVsReal)
                1 -> ProyeccionContent(items = proyeccion)
            }

            // Espacio inferior para el FAB
            Spacer(Modifier.height(80.dp))
        }
    }

    // ── Diálogo agregar presupuesto ───────────────────────────────────────────
    if (showPresupuestoDialog) {
        PresupuestoDialog(
            onDismiss = { showPresupuestoDialog = false },
            onConfirm = { categoria, monto, mes, anio ->
                viewModel.guardarPresupuesto(categoria, monto, mes, anio)
                showPresupuestoDialog = false
            }
        )
    }
}

// ── Card de resumen ───────────────────────────────────────────────────────────

@Composable
private fun ResumenCard(resumen: ResumenFlujoCaja, saldoTotal: Double) {
    val resultadoPositivo = resumen.resultadoOperacional >= 0
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Resumen del período", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ResumenItem(emoji = "💚", label = "Ingresos", valor = resumen.totalIngresos, color = Color(0xFF2E7D32))
                ResumenItem(emoji = "🔴", label = "Egresos", valor = resumen.totalEgresos, color = Color(0xFFC62828))
            }

            HorizontalDivider()

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Resultado operacional", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text  = "${if (resultadoPositivo) "+" else ""}$${fmt.format(resumen.resultadoOperacional)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (resultadoPositivo) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Saldo en sobres", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text  = "$${fmt.format(saldoTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (resumen.totalIngresos > 0) {
                Text(
                    text  = "Margen: ${"%.1f".format(resumen.margenPorcentaje)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ResumenItem(emoji: String, label: String, valor: Double, color: Color) {
    Column {
        Text(emoji, fontSize = 20.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$${fmt.format(valor)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Gráfico de área con Canvas ───────────────────────────────────────────────

@Composable
private fun BarChart(
    ingresosPorDia: Map<String, Double>,
    egresosPorDia: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val todasFechas = (ingresosPorDia.keys + egresosPorDia.keys).distinct().sorted()
    val maxValor = maxOf(
        ingresosPorDia.values.maxOrNull() ?: 0.0,
        egresosPorDia.values.maxOrNull() ?: 0.0
    ).coerceAtLeast(1.0)

    val colorIngreso = Color(0xFF4CAF50)
    val colorEgreso  = Color(0xFFE53935)
    val colorBase    = Color(0xFFBDBDBD)

    Card(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (todasFechas.isEmpty()) return@Canvas

            val n         = todasFechas.size
            val alturaMax = size.height * 0.85f
            val baseY     = size.height * 0.9f
            val stepX     = if (n > 1) size.width / (n - 1).toFloat() else size.width

            fun yFor(v: Double) = baseY - ((v / maxValor) * alturaMax).toFloat()

            // Area ingresos
            val pathIng = Path().apply {
                moveTo(0f, baseY)
                todasFechas.forEachIndexed { i, f ->
                    lineTo(i * stepX, yFor(ingresosPorDia[f] ?: 0.0))
                }
                lineTo((n - 1) * stepX, baseY)
                close()
            }
            drawPath(pathIng, colorIngreso.copy(alpha = 0.2f))
            // Line ingresos
            todasFechas.forEachIndexed { i, f ->
                if (i > 0) {
                    val prev = todasFechas[i - 1]
                    drawLine(
                        colorIngreso,
                        Offset((i - 1) * stepX, yFor(ingresosPorDia[prev] ?: 0.0)),
                        Offset(i * stepX, yFor(ingresosPorDia[f] ?: 0.0)),
                        strokeWidth = 3f
                    )
                }
                drawCircle(colorIngreso, 4f, Offset(i * stepX, yFor(ingresosPorDia[f] ?: 0.0)))
            }

            // Area egresos
            val pathEgr = Path().apply {
                moveTo(0f, baseY)
                todasFechas.forEachIndexed { i, f ->
                    lineTo(i * stepX, yFor(egresosPorDia[f] ?: 0.0))
                }
                lineTo((n - 1) * stepX, baseY)
                close()
            }
            drawPath(pathEgr, colorEgreso.copy(alpha = 0.2f))
            // Line egresos
            todasFechas.forEachIndexed { i, f ->
                if (i > 0) {
                    val prev = todasFechas[i - 1]
                    drawLine(
                        colorEgreso,
                        Offset((i - 1) * stepX, yFor(egresosPorDia[prev] ?: 0.0)),
                        Offset(i * stepX, yFor(egresosPorDia[f] ?: 0.0)),
                        strokeWidth = 3f
                    )
                }
                drawCircle(colorEgreso, 4f, Offset(i * stepX, yFor(egresosPorDia[f] ?: 0.0)))
            }

            // Línea base
            drawLine(color = colorBase, start = Offset(0f, baseY), end = Offset(size.width, baseY), strokeWidth = 1.5f)
        }
    }
}

@Composable
private fun LeyendaItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = MaterialTheme.shapes.extraSmall)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// ── Tab: Presupuesto vs Real ──────────────────────────────────────────────────

@Composable
private fun PresupuestoVsRealContent(items: List<PresupuestoVsReal>) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sin presupuestos para este mes.\nPresioná + para agregar uno.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            PresupuestoVsRealCard(item = item)
        }
    }
}

@Composable
private fun PresupuestoVsRealCard(item: PresupuestoVsReal) {
    val porcentaje = item.porcentajeUsado.coerceIn(0.0, 100.0).toFloat() / 100f
    val colorBarra = when {
        item.porcentajeUsado > 100.0 -> Color(0xFFE53935)   // rojo: excedido
        item.porcentajeUsado >= 80.0 -> Color(0xFFFF9800)   // amarillo: cerca del límite
        else                         -> Color(0xFF4CAF50)   // verde: ok
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.categoria.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${"%.0f".format(item.porcentajeUsado)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorBarra,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress   = { porcentaje },
                modifier   = Modifier.fillMaxWidth().height(8.dp),
                color      = colorBarra,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Gastado: $${fmt.format(item.gastadoReal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Presupuesto: $${fmt.format(item.presupuestado)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.diferencia < 0) {
                Text(
                    "⚠️ Excedido por $${fmt.format(-item.diferencia)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE53935)
                )
            }
        }
    }
}

// ── Tab: Proyección ───────────────────────────────────────────────────────────

private val nombreMes = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
                                 "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

@Composable
private fun ProyeccionContent(items: List<ProyeccionMes>) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text     = "ℹ️ Estimación basada en el promedio de los últimos 3 meses reales.",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }

        items.forEach { proyeccion ->
            ProyeccionCard(proyeccion = proyeccion)
        }
    }
}

@Composable
private fun ProyeccionCard(proyeccion: ProyeccionMes) {
    val resultadoEst  = proyeccion.ingresoEstimado - proyeccion.egresoEstimado
    val resultadoPos  = resultadoEst >= 0
    val mesNombre     = nombreMes.getOrElse(proyeccion.mes - 1) { "?" }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text       = "$mesNombre ${proyeccion.anio}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = "Ingresos est.: $${fmt.format(proyeccion.ingresoEstimado)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text  = "Egresos est.: $${fmt.format(proyeccion.egresoEstimado)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = "${if (resultadoPos) "+" else ""}$${fmt.format(resultadoEst)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (resultadoPos) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Text(
                    text  = "resultado est.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Diálogo: agregar presupuesto ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresupuestoDialog(
    onDismiss: () -> Unit,
    onConfirm: (CategoriaGasto, Double, Int, Int) -> Unit
) {
    val cal = Calendar.getInstance()
    var selectedCategoria by remember { mutableStateOf(CategoriaGasto.INSUMOS) }
    var montoText         by remember { mutableStateOf("") }
    var mes               by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
    var anio              by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var catExpanded       by remember { mutableStateOf(false) }

    val formValido = montoText.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Categoría
                ExposedDropdownMenuBox(
                    expanded        = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value       = selectedCategoria.label,
                        onValueChange = {},
                        readOnly    = true,
                        label       = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier    = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded        = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        CategoriaGasto.entries.forEach { cat ->
                            DropdownMenuItem(
                                text    = { Text(cat.label) },
                                onClick = { selectedCategoria = cat; catExpanded = false }
                            )
                        }
                    }
                }

                // Monto
                OutlinedTextField(
                    value         = montoText,
                    onValueChange = { montoText = it },
                    label         = { Text("Monto presupuestado") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix        = { Text("$") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                // Mes / Año
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value         = mes.toString(),
                        onValueChange = { mes = it.toIntOrNull()?.coerceIn(1, 12) ?: mes },
                        label         = { Text("Mes (1-12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine    = true,
                        modifier      = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value         = anio.toString(),
                        onValueChange = { anio = it.toIntOrNull() ?: anio },
                        label         = { Text("Año") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine    = true,
                        modifier      = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { onConfirm(selectedCategoria, montoText.toDouble(), mes, anio) },
                enabled  = formValido
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Donut Chart para distribución de egresos ─────────────────────────────────

private val donutColorsFlujo = listOf(
    Color(0xFF4CAF50), Color(0xFFE53935), Color(0xFF2196F3),
    Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF00BCD4),
    Color(0xFFFF5722), Color(0xFF795548)
)

@Composable
private fun DonutChartFlujo(
    data: Map<CategoriaGasto, Double>,
    modifier: Modifier = Modifier
) {
    val entries = data.entries.sortedByDescending { it.value }
    val total = entries.sumOf { it.value }
    if (total <= 0) return

    Card(modifier = modifier) {
        Row(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(120.dp)) {
                val strokeW = 28f
                val radius = (size.minDimension - strokeW) / 2
                val center = Offset(size.width / 2, size.height / 2)
                var startAngle = -90f

                entries.forEachIndexed { idx, (_, monto) ->
                    val sweep = (monto / total * 360.0).toFloat()
                    drawArc(
                        color = donutColorsFlujo[idx % donutColorsFlujo.size],
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

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                entries.forEachIndexed { idx, (cat, monto) ->
                    val pct = (monto / total * 100).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(donutColorsFlujo[idx % donutColorsFlujo.size], MaterialTheme.shapes.extraSmall)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("${cat.label} (${pct}%)", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}
