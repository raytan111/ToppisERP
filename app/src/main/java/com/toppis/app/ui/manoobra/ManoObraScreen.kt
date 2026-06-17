package com.toppis.app.ui.manoobra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.TipoPago
import com.toppis.app.data.models.Empleado
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat
import java.time.LocalDate

private val money = DecimalFormat("$#,##0")
private val pct = DecimalFormat("0.#")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManoObraScreen(
    viewModel: ManoObraViewModel,
    usuarioId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val periodo by viewModel.periodo.collectAsState()
    val prime by viewModel.prime.collectAsState()
    val jornadas by viewModel.jornadas.collectAsState()
    val propinas by viewModel.propinas.collectAsState()
    val empleados by viewModel.empleados.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showJornada by remember { mutableStateOf(false) }
    var showPropina by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ManoObraUiState.Error -> { snackbarHostState.showSnackbar((uiState as ManoObraUiState.Error).message); viewModel.resetState() }
            ManoObraUiState.Success -> viewModel.resetState()
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Mano de Obra", onBack = onNavigateBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Selector de período
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.mesAnterior() }) { Icon(Icons.Filled.ChevronLeft, "Mes anterior") }
                    Text("${periodo.monthValue.toString().padStart(2, '0')}/${periodo.year}", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { viewModel.mesSiguiente() }) { Icon(Icons.Filled.ChevronRight, "Mes siguiente") }
                }
            }

            // Prime Cost
            item {
                val p = prime
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Prime Cost del mes", style = MaterialTheme.typography.titleSmall)
                        if (p == null) {
                            Text("Calculando...", color = MaterialTheme.colorScheme.outline)
                        } else {
                            Fila("Ventas", money.format(p.ventas))
                            Fila("Food cost (teórico)", "${money.format(p.foodCost)} · ${pct.format(p.foodPct)}%")
                            Fila("Mano de obra", "${money.format(p.laborCost)} · ${pct.format(p.laborPct)}%")
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            val primeColor = when {
                                p.ventas == 0.0 -> MaterialTheme.colorScheme.outline
                                p.primePct <= 65 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            }
                            Fila("PRIME COST", "${money.format(p.primeCost)} · ${pct.format(p.primePct)}%", primeColor)
                            Text("Meta saludable: ≤ 60–65%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            if (p.propinas > 0) Text("Propinas del mes: ${money.format(p.propinas)}",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            // Jornadas
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Turnos / horas", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { showJornada = true }, enabled = empleados.isNotEmpty()) { Text("+ Registrar") }
                }
            }
            if (jornadas.isEmpty()) {
                item { Text("Sin turnos registrados este mes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
            } else {
                items(jornadas) { j ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(j.nombre, style = MaterialTheme.typography.bodyMedium)
                                Text("${j.jornada.fecha} · ${DecimalFormat("#,##0.##").format(j.jornada.cantidad)} · ${money.format(j.jornada.costo)}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.eliminarJornada(j.jornada.id) }) {
                                Icon(Icons.Filled.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Propinas
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Propinas (total por día)", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { showPropina = true }) { Text("+ Registrar") }
                }
            }
            if (propinas.isEmpty()) {
                item { Text("Sin propinas registradas este mes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
            } else {
                items(propinas) { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.fecha ?: "", style = MaterialTheme.typography.bodyMedium)
                                Text(money.format(p.monto), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                            IconButton(onClick = { viewModel.eliminarPropina(p.id) }) {
                                Icon(Icons.Filled.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJornada) {
        JornadaDialog(
            empleados = empleados,
            onDismiss = { showJornada = false },
            onConfirm = { emp, fecha, cant, nota -> viewModel.registrarJornada(emp, fecha, cant, nota, usuarioId); showJornada = false }
        )
    }
    if (showPropina) {
        PropinaDialog(
            onDismiss = { showPropina = false },
            onConfirm = { fecha, monto, nota -> viewModel.registrarPropina(fecha, monto, nota); showPropina = false }
        )
    }
}

@Composable
private fun Fila(label: String, valor: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JornadaDialog(
    empleados: List<Empleado>,
    onDismiss: () -> Unit,
    onConfirm: (empleado: Empleado, fecha: String, cantidad: Double, nota: String) -> Unit
) {
    var empleado by remember { mutableStateOf(empleados.firstOrNull()) }
    var exp by remember { mutableStateOf(false) }
    var fecha by remember { mutableStateOf(LocalDate.now().toString()) }
    var cantidadText by remember { mutableStateOf("1") }
    var nota by remember { mutableStateOf("") }

    val cantidad = cantidadText.replace(",", ".").toDoubleOrNull()
    val esHora = empleado?.tipoPago == TipoPago.POR_HORA
    val costoEstimado = if (cantidad != null && empleado != null) cantidad * empleado!!.monto else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar turno / horas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                    OutlinedTextField(
                        value = empleado?.nombre ?: "", onValueChange = {}, readOnly = true,
                        label = { Text("Empleado") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        empleados.forEach { e -> DropdownMenuItem(text = { Text("${e.nombre} (${e.tipoPago.label})") }, onClick = { empleado = e; exp = false }) }
                    }
                }
                OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = cantidadText, onValueChange = { cantidadText = it },
                    label = { Text(if (esHora) "Horas trabajadas" else "Cantidad de turnos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (costoEstimado != null) {
                    Text("Costo: ${money.format(costoEstimado)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (empleado?.tipoPago == TipoPago.SUELDO_FIJO) {
                        Text("Ojo: este empleado es sueldo fijo; su sueldo ya entra al prime cost del mes. Registrar turnos lo sumaría doble.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(empleado!!, fecha, cantidad ?: 0.0, nota) },
                enabled = empleado != null && cantidad != null && cantidad > 0 && fecha.isNotBlank()) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun PropinaDialog(
    onDismiss: () -> Unit,
    onConfirm: (fecha: String, monto: Double, nota: String) -> Unit
) {
    var fecha by remember { mutableStateOf(LocalDate.now().toString()) }
    var montoText by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    val monto = montoText.replace(",", ".").toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar propina del día") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = montoText, onValueChange = { montoText = it },
                    label = { Text("Total propinas del día (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(fecha, monto ?: 0.0, nota) }, enabled = monto != null && monto >= 0 && fecha.isNotBlank()) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
