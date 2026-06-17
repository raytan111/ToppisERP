package com.toppis.app.ui.proveedores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.Proveedor
import com.toppis.app.ui.components.ToppisTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedoresScreen(
    viewModel: ProveedorViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val proveedores by viewModel.proveedores.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrear by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Proveedor?>(null) }
    var aEliminar by remember { mutableStateOf<Proveedor?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is ProveedorUiState.Error) {
            snackbarHostState.showSnackbar((uiState as ProveedorUiState.Error).message)
            viewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Proveedores", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrear = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo proveedor")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (proveedores.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin proveedores. Usá + para agregar.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(proveedores) { p ->
                        Card(modifier = Modifier.fillMaxWidth(), onClick = { enEdicion = p }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.nombre, style = MaterialTheme.typography.titleMedium)
                                    if (p.contacto.isNotBlank() || p.telefono.isNotBlank()) {
                                        Text(listOf(p.contacto, p.telefono).filter { it.isNotBlank() }.joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    if (p.email.isNotBlank()) Text(p.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { aEliminar = p }) {
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
        ProveedorDialog(
            onDismiss = { showCrear = false },
            onConfirm = { n, c, t, e, nota -> viewModel.crear(n, c, t, e, nota); showCrear = false }
        )
    }

    enEdicion?.let { p ->
        ProveedorDialog(
            inicial = p,
            onDismiss = { enEdicion = null },
            onConfirm = { n, c, t, e, nota ->
                viewModel.actualizar(p.copy(nombre = n, contacto = c, telefono = t, email = e, nota = nota))
                enEdicion = null
            }
        )
    }

    aEliminar?.let { p ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("Eliminar proveedor") },
            text = { Text("¿Eliminar a \"${p.nombre}\"?") },
            confirmButton = { TextButton(onClick = { viewModel.eliminar(p.id); aEliminar = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ProveedorDialog(
    inicial: Proveedor? = null,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, contacto: String, telefono: String, email: String, nota: String) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var contacto by remember { mutableStateOf(inicial?.contacto ?: "") }
    var telefono by remember { mutableStateOf(inicial?.telefono ?: "") }
    var email by remember { mutableStateOf(inicial?.email ?: "") }
    var nota by remember { mutableStateOf(inicial?.nota ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial != null) "Editar proveedor" else "Nuevo proveedor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contacto, onValueChange = { contacto = it }, label = { Text("Contacto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Nota") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nombre, contacto, telefono, email, nota) }, enabled = nombre.isNotBlank()) {
                Text(if (inicial != null) "Guardar" else "Crear")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
