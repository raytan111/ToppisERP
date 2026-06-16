package com.toppis.app.ui.modificadores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.TipoModificador
import com.toppis.app.data.models.Modificador
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModificadoresScreen(
    viewModel: ModificadorViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val modificadores by viewModel.modificadores.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrearDialog by remember { mutableStateOf(false) }
    var modAEliminar by remember { mutableStateOf<Modificador?>(null) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ModificadorUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as ModificadorUiState.Error).message)
                viewModel.resetState()
            }
            ModificadorUiState.Success -> viewModel.resetState()
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Modificadores", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrearDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear modificador")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (modificadores.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin modificadores.\nUsá el botón + para agregar.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                val money = DecimalFormat("$#,##0")
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(modificadores) { mod ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mod.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        mod.tipo.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    val signo = if (mod.deltaPrecio >= 0) "+" else "-"
                                    Text(
                                        "Delta precio: $signo${money.format(kotlin.math.abs(mod.deltaPrecio))}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { modAEliminar = mod }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCrearDialog) {
        CrearModificadorDialog(
            onDismiss = { showCrearDialog = false },
            onConfirm = { nombre, tipo, delta ->
                viewModel.crearModificador(nombre, tipo, delta)
                showCrearDialog = false
            }
        )
    }

    modAEliminar?.let { mod ->
        AlertDialog(
            onDismissRequest = { modAEliminar = null },
            title = { Text("Eliminar modificador") },
            text = { Text("¿Seguro que querés eliminar \"${mod.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarModificador(mod.id)
                    modAEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { modAEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearModificadorDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, tipo: TipoModificador, deltaPrecio: Double) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf(TipoModificador.EXTRA) }
    var expandedTipo by remember { mutableStateOf(false) }
    var deltaText by remember { mutableStateOf("") }

    val deltaValido = deltaText.replace(",", ".").toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Modificador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Doble carne)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tipoSeleccionado.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        TipoModificador.entries.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo.label) },
                                onClick = {
                                    tipoSeleccionado = tipo
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = deltaText, onValueChange = { deltaText = it },
                    label = { Text("Delta de precio (CLP, puede ser negativo)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(nombre, tipoSeleccionado, deltaText.replace(",", ".").toDouble())
                },
                enabled = nombre.isNotBlank() && deltaValido
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
