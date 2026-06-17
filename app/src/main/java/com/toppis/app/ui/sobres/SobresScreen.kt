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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
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

    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedSobre by remember { mutableStateOf<Sobre?>(null) }

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

    if (showDeleteConfirmDialog && selectedSobre != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar sobre") },
            text = {
                if (selectedSobre!!.saldo > 0) {
                    Text("No se puede eliminar un sobre con saldo (${DecimalFormat("$#,##0 CLP").format(selectedSobre!!.saldo)})")
                } else {
                    Text("¿Estás seguro de que deseas eliminar el sobre \"${selectedSobre!!.nombre}\"?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarSobre(selectedSobre!!)
                        showDeleteConfirmDialog = false
                    },
                    enabled = selectedSobre!!.saldo == 0.0
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun SobreCard(
    sobre: Sobre,
    onTransferirClick: () -> Unit,
    onEditarClick: () -> Unit,
    onEliminarClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = sobre.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(text = sobre.descripcion, style = MaterialTheme.typography.bodyMedium)
                    val esCuenta = sobre.tipo == com.toppis.app.data.db.entities.TipoSobre.CUENTA
                    Text(
                        text = if (esCuenta) "Cuenta (dinero real)" else "Fondo (provisión)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (esCuenta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                }
                Row {
                    IconButton(onClick = onEditarClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onEliminarClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val formatter = DecimalFormat("$#,##0 CLP")
            Text(
                text = "Saldo: ${formatter.format(sobre.saldo)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onTransferirClick, modifier = Modifier.fillMaxWidth()) {
                Text("Transferir")
            }
        }
    }
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
                        modifier = Modifier.menuAnchor()
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
