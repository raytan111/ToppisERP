package com.toppis.app.ui.locales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.Local
import com.toppis.app.ui.components.ToppisTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalesScreen(
    viewModel: LocalViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val locales by viewModel.locales.collectAsState()
    val activoId by viewModel.activoId.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrear by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Local?>(null) }
    var aEliminar by remember { mutableStateOf<Local?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is LocalUiState.Error) {
            snackbarHostState.showSnackbar((uiState as LocalUiState.Error).message)
            viewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Locales", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrear = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo local")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                "El local activo se usa para registrar nuevas ventas, gastos y compras.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(16.dp)
            )
            if (cargandoInicial && locales.isEmpty()) {
                com.toppis.app.ui.components.SkeletonList()
            } else if (locales.isEmpty()) {
                com.toppis.app.ui.components.EmptyState(
                    icon = Icons.Filled.Store,
                    titulo = "Sin locales",
                    subtitulo = "Usá el botón + para agregar tu primer local."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(locales) { l ->
                        val esActivo = l.id == activoId
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (esActivo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(l.nombre, style = MaterialTheme.typography.titleMedium)
                                    if (l.direccion.isNotBlank()) Text(l.direccion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    if (esActivo) Text("● Local activo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                if (!esActivo) {
                                    TextButton(onClick = { viewModel.marcarActivo(l) }) { Text("Activar") }
                                } else {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Activo", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { enEdicion = l }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Editar", tint = MaterialTheme.colorScheme.secondary)
                                }
                                IconButton(onClick = { aEliminar = l }) {
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
        LocalDialog(onDismiss = { showCrear = false }, onConfirm = { n, d -> viewModel.crear(n, d); showCrear = false })
    }
    enEdicion?.let { l ->
        LocalDialog(inicial = l, onDismiss = { enEdicion = null }, onConfirm = { n, d ->
            viewModel.actualizar(l.copy(nombre = n, direccion = d)); enEdicion = null
        })
    }
    aEliminar?.let { l ->
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = l.nombre,
            titulo = "Eliminar local",
            onConfirm = { viewModel.eliminar(l.id); aEliminar = null },
            onDismiss = { aEliminar = null }
        )
    }
}

@Composable
private fun LocalDialog(
    inicial: Local? = null,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, direccion: String) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var direccion by remember { mutableStateOf(inicial?.direccion ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial != null) "Editar local" else "Nuevo local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = direccion, onValueChange = { direccion = it }, label = { Text("Dirección") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(nombre, direccion) }, enabled = nombre.isNotBlank()) { Text(if (inicial != null) "Guardar" else "Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
