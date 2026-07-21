package com.toppis.app.ui.sobres

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.foundation.shape.RoundedCornerShape
import com.toppis.app.data.db.entities.TipoMovimiento
import com.toppis.app.data.models.MovimientoSobre
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.Sobre
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SobresScreen(
    viewModel: SobreViewModel,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sobres by viewModel.sobres.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showHistorialDialog by remember { mutableStateOf(false) }
    var selectedSobre by remember { mutableStateOf<Sobre?>(null) }

    // Recargar al abrir (refleja cambios hechos fuera de la app).
    LaunchedEffect(Unit) { viewModel.recargar() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is SobreUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as SobreUiState.Error).message)
            }
            is SobreUiState.Success -> { }
            SobreUiState.Loading -> { }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (cargandoInicial && sobres.isEmpty()) {
            com.toppis.app.ui.components.SkeletonList()
        } else if (sobres.isEmpty()) {
            com.toppis.app.ui.components.EmptyState(
                icon = Icons.Filled.AccountBalance,
                titulo = "Sin sobres",
                subtitulo = "Creá tu primer sobre con el botón + para organizar el dinero."
            )
        } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val money = DecimalFormat("$#,##0")
                val real = sobres.filter { it.tipo == com.toppis.app.data.db.entities.TipoSobre.CUENTA }.sumOf { it.saldo }
                val provis = sobres.filter { it.tipo == com.toppis.app.data.db.entities.TipoSobre.FONDO }.sumOf { it.saldo }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Dinero real (cuentas)", style = MaterialTheme.typography.labelSmall)
                            Text(money.format(real), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Provisiones (fondos)", style = MaterialTheme.typography.labelSmall)
                            Text(money.format(provis), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            items(sobres) { sobre ->
                SobreCard(
                    sobre = sobre,
                    onTransferirClick = {
                        selectedSobre = sobre
                        showTransferDialog = true
                    },
                    onHistorialClick = {
                        selectedSobre = sobre
                        viewModel.cargarMovimientos(sobre.id)
                        showHistorialDialog = true
                    },
                    onEditarClick = {
                        selectedSobre = sobre
                        showEditDialog = true
                    },
                    onEliminarClick = {
                        selectedSobre = sobre
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Crear Sobre")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
        )
    }

    if (showCreateDialog) {
        CreateSobreDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { nombre, desc, tipo ->
                viewModel.crearSobre(nombre, desc, tipo)
                showCreateDialog = false
            }
        )
    }

    if (showTransferDialog && selectedSobre != null) {
        val otrosSobres = sobres.filter { it.id != selectedSobre?.id }
        TransferDialog(
            origen = selectedSobre!!,
            destinos = otrosSobres,
            onDismiss = { showTransferDialog = false },
            onConfirm = { destinoId, monto, desc ->
                viewModel.transferir(
                    origenId = selectedSobre!!.id.toLong(),
                    destinoId = destinoId,
                    monto = monto,
                    descripcion = desc,
                    usuarioId = null   // null hasta que exista login (Módulo 6)
                )
                showTransferDialog = false
            }
        )
    }

    if (showEditDialog && selectedSobre != null) {
        EditarSobreDialog(
            sobre = selectedSobre!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { nombre, descripcion, tipo ->
                viewModel.editarSobre(
                    selectedSobre!!.copy(nombre = nombre, descripcion = descripcion, tipo = tipo)
                )
                showEditDialog = false
            }
        )
    }

    if (showHistorialDialog && selectedSobre != null) {
        val movimientos by viewModel.movimientos.collectAsState()
        val cargandoMov by viewModel.cargandoMovimientos.collectAsState()
        HistorialDialog(
            sobre = selectedSobre!!,
            movimientos = movimientos,
            cargando = cargandoMov,
            sobresById = sobres.associateBy { it.id },
            onDismiss = { showHistorialDialog = false; viewModel.limpiarMovimientos() }
        )
    }

    if (showDeleteConfirmDialog && selectedSobre != null) {
        val conSaldo = selectedSobre!!.saldo > 0
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = selectedSobre!!.nombre,
            titulo = "Eliminar sobre",
            mensaje = if (conSaldo)
                "No se puede eliminar un sobre con saldo (${DecimalFormat("$#,##0 CLP").format(selectedSobre!!.saldo)})."
            else
                "¿Eliminar el sobre \"${selectedSobre!!.nombre}\"? Esta acción no se puede deshacer.",
            confirmarHabilitado = !conSaldo,
            onConfirm = {
                viewModel.eliminarSobre(selectedSobre!!)
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

@Composable
fun SobreCard(
    sobre: Sobre,
    onTransferirClick: () -> Unit,
    onHistorialClick: () -> Unit,
    onEditarClick: () -> Unit,
    onEliminarClick: () -> Unit
) {
    val esCuenta = sobre.tipo == com.toppis.app.data.db.entities.TipoSobre.CUENTA
    val acento = if (esCuenta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = sobre.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    if (sobre.descripcion.isNotBlank()) {
                        Text(text = sobre.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(shape = RoundedCornerShape(8.dp), color = acento.copy(alpha = 0.15f)) {
                    Text(
                        text = if (esCuenta) "Cuenta" else "Fondo",
                        style = MaterialTheme.typography.labelMedium, color = acento,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val formatter = DecimalFormat("$#,##0 CLP")
            Text(text = "Saldo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = formatter.format(sobre.saldo),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = acento
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Button(onClick = onTransferirClick, modifier = Modifier.weight(1f)) { Text("Transferir") }
                OutlinedButton(onClick = onHistorialClick) {
                    Icon(Icons.Filled.History, contentDescription = "Historial", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Historial")
                }
                IconButton(onClick = onEditarClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEliminarClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun HistorialDialog(
    sobre: Sobre,
    movimientos: List<MovimientoSobre>,
    cargando: Boolean,
    sobresById: Map<Int, Sobre>,
    onDismiss: () -> Unit
) {
    val money = DecimalFormat("$#,##0")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial · ${sobre.nombre}") },
        text = {
            when {
                cargando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
                movimientos.isEmpty() -> Text("Sin movimientos registrados en este sobre.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(movimientos) { m ->
                        val entra = m.destinoId == sobre.id
                        val signo = if (entra) "+" else "−"
                        val color = if (entra) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        val contraparteId = if (entra) m.origenId else m.destinoId
                        val contraparte = contraparteId?.let { sobresById[it]?.nombre }
                        val fechaTxt = (m.createdAt ?: m.fecha ?: "").take(10)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(m.descripcion.ifBlank { m.tipo.name }, style = MaterialTheme.typography.bodyMedium)
                                val sub = buildString {
                                    append(fechaTxt)
                                    if (contraparte != null) append(if (entra) "  ·  desde $contraparte" else "  ·  hacia $contraparte")
                                }
                                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$signo${money.format(m.monto)}", style = MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = color)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSobreDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, com.toppis.app.data.db.entities.TipoSobre) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(com.toppis.app.data.db.entities.TipoSobre.CUENTA) }
    var exp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Sobre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") })
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                    OutlinedTextField(
                        value = tipo.label, onValueChange = {}, readOnly = true,
                        label = { Text("Tipo") },
                        supportingText = { Text("Cuenta = dinero real. Fondo = provisión (IVA, sueldos...).") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        com.toppis.app.data.db.entities.TipoSobre.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { tipo = t; exp = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre, descripcion, tipo) },
                enabled = nombre.isNotBlank() && descripcion.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    origen: Sobre,
    destinos: List<Sobre>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, String) -> Unit
) {
    if (destinos.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Atención") },
            text = { Text("No hay otros sobres disponibles para transferir.") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        )
        return
    }

    var selectedDestino by remember { mutableStateOf(destinos.first()) }
    var monto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transferir desde ${origen.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedDestino.nombre,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destino") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        destinos.forEach { destino ->
                            DropdownMenuItem(
                                text = { Text(destino.nombre) },
                                onClick = {
                                    selectedDestino = destino
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val m = monto.toDoubleOrNull()
                    if (m != null && m > 0) {
                        onConfirm(selectedDestino.id.toLong(), m, descripcion)
                    }
                },
                enabled = monto.isNotBlank() && descripcion.isNotBlank() && monto.toDoubleOrNull() != null
            ) {
                Text("Transferir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarSobreDialog(
    sobre: Sobre,
    onDismiss: () -> Unit,
    onConfirm: (String, String, com.toppis.app.data.db.entities.TipoSobre) -> Unit
) {
    var nombre by remember { mutableStateOf(sobre.nombre) }
    var descripcion by remember { mutableStateOf(sobre.descripcion) }
    var tipo by remember { mutableStateOf(sobre.tipo) }
    var exp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Sobre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") })
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                    OutlinedTextField(
                        value = tipo.label, onValueChange = {}, readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        com.toppis.app.data.db.entities.TipoSobre.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { tipo = t; exp = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre, descripcion, tipo) },
                enabled = nombre.isNotBlank() && descripcion.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
