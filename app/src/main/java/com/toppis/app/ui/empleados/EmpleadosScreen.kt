package com.toppis.app.ui.empleados

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpleadosScreen(
    viewModel: EmpleadoViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val empleados by viewModel.empleados.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()
    val money = DecimalFormat("$#,##0")

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrear by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Empleado?>(null) }
    var aEliminar by remember { mutableStateOf<Empleado?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is EmpleadoUiState.Error) {
            snackbarHostState.showSnackbar((uiState as EmpleadoUiState.Error).message)
            viewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Empleados", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrear = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo empleado")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cargandoInicial && empleados.isEmpty()) {
                com.toppis.app.ui.components.SkeletonList()
            } else if (empleados.isEmpty()) {
                com.toppis.app.ui.components.EmptyState(
                    icon = Icons.Filled.Badge,
                    titulo = "Sin empleados",
                    subtitulo = "Usá el botón + para agregar tu primer empleado."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(empleados) { e ->
                        Card(modifier = Modifier.fillMaxWidth(), onClick = { enEdicion = e }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.nombre, style = MaterialTheme.typography.titleMedium)
                                    if (e.cargo.isNotBlank()) Text(e.cargo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    val detalle = when (e.tipoPago) {
                                        TipoPago.SUELDO_FIJO -> "${e.tipoPago.label}: ${money.format(e.monto)}/mes"
                                        TipoPago.POR_TURNO -> "${e.tipoPago.label}: ${money.format(e.monto)}/turno"
                                        TipoPago.POR_HORA -> "${e.tipoPago.label}: ${money.format(e.monto)}/hora"
                                    }
                                    Text(detalle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { aEliminar = e }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCrear) {
        EmpleadoDialog(onDismiss = { showCrear = false }, onConfirm = { n, c, t, m ->
            viewModel.crear(n, c, t, m); showCrear = false
        })
    }
    enEdicion?.let { e ->
        EmpleadoDialog(inicial = e, onDismiss = { enEdicion = null }, onConfirm = { n, c, t, m ->
            viewModel.actualizar(e.copy(nombre = n, cargo = c, tipoPago = t, monto = m)); enEdicion = null
        })
    }
    aEliminar?.let { e ->
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = e.nombre,
            titulo = "Eliminar empleado",
            onConfirm = { viewModel.eliminar(e.id); aEliminar = null },
            onDismiss = { aEliminar = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmpleadoDialog(
    inicial: Empleado? = null,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, cargo: String, tipoPago: TipoPago, monto: Double) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var cargo by remember { mutableStateOf(inicial?.cargo ?: "") }
    var tipo by remember { mutableStateOf(inicial?.tipoPago ?: TipoPago.POR_TURNO) }
    var exp by remember { mutableStateOf(false) }
    var montoText by remember { mutableStateOf(inicial?.monto?.let { if (it == 0.0) "" else it.toLong().toString() } ?: "") }

    val monto = montoText.replace(",", ".").toDoubleOrNull()
    val labelMonto = when (tipo) {
        TipoPago.SUELDO_FIJO -> "Sueldo mensual (CLP)"
        TipoPago.POR_TURNO -> "Valor por turno (CLP)"
        TipoPago.POR_HORA -> "Valor por hora (CLP)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial != null) "Editar empleado" else "Nuevo empleado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cargo, onValueChange = { cargo = it }, label = { Text("Cargo (ej: Cocina, Cajero)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                    OutlinedTextField(
                        value = tipo.label, onValueChange = {}, readOnly = true,
                        label = { Text("Forma de pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        TipoPago.entries.forEach { t -> DropdownMenuItem(text = { Text(t.label) }, onClick = { tipo = t; exp = false }) }
                    }
                }
                OutlinedTextField(
                    value = montoText, onValueChange = { montoText = it },
                    label = { Text(labelMonto) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nombre, cargo, tipo, monto ?: 0.0) },
                enabled = nombre.isNotBlank() && monto != null && monto >= 0) { Text(if (inicial != null) "Guardar" else "Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
