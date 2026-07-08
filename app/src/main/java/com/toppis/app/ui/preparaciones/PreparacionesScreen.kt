package com.toppis.app.ui.preparaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.DimensionUnidad
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Preparacion
import com.toppis.app.data.repository.ComponentePreparacion
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreparacionesScreen(
    viewModel: PreparacionViewModel,
    puedeBorrar: Boolean = true,
    onNavigateBack: () -> Unit = {}
) {
    val preparaciones by viewModel.preparaciones.collectAsState()
    val articulos by viewModel.articulos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrearDialog by remember { mutableStateOf(false) }
    var selectedPrep by remember { mutableStateOf<Preparacion?>(null) }
    var prepAEliminar by remember { mutableStateOf<Preparacion?>(null) }
    var prepAEditar by remember { mutableStateOf<Preparacion?>(null) }
    var componentes by remember { mutableStateOf<List<ComponentePreparacion>>(emptyList()) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PreparacionUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as PreparacionUiState.Error).message)
                viewModel.resetState()
            }
            PreparacionUiState.Success -> viewModel.resetState()
            else -> {}
        }
    }

    // Mantener la preparación seleccionada sincronizada con la lista actualizada
    LaunchedEffect(preparaciones) {
        selectedPrep?.let { sel ->
            selectedPrep = preparaciones.firstOrNull { it.id == sel.id } ?: sel
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Preparaciones", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrearDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear preparación")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cargandoInicial && preparaciones.isEmpty()) {
                com.toppis.app.ui.components.SkeletonList()
            } else
            PreparacionesList(
                preparaciones = preparaciones,
                puedeBorrar = puedeBorrar,
                onVerReceta = { prep ->
                    selectedPrep = prep
                    componentes = emptyList()
                    viewModel.loadComponentes(prep.id) { componentes = it }
                },
                onEditar = { prepAEditar = it },
                onEliminar = { prepAEliminar = it }
            )
        }
    }

    if (showCrearDialog) {
        CrearPreparacionDialog(
            onDismiss = { showCrearDialog = false },
            onConfirm = { nombre, dimension, rendimiento, seleccionable ->
                viewModel.crearPreparacion(nombre, dimension, rendimiento, seleccionable)
                showCrearDialog = false
            }
        )
    }

    selectedPrep?.let { prep ->
        RecetaPreparacionDialog(
            preparacion = prep,
            componentes = componentes,
            articulos = articulos,
            onDismiss = {
                selectedPrep = null
                componentes = emptyList()
            },
            onAgregarComponente = { tipo, componenteId, cantidad ->
                viewModel.agregarComponente(prep.id, tipo, componenteId, cantidad) {
                    viewModel.loadComponentes(prep.id) { componentes = it }
                }
            },
            onEliminarComponente = { comp ->
                viewModel.eliminarComponente(comp.componente) {
                    viewModel.loadComponentes(prep.id) { componentes = it }
                }
            }
        )
    }

    prepAEliminar?.let { prep ->
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = prep.nombre,
            titulo = "Eliminar preparación",
            onConfirm = { viewModel.eliminarPreparacion(prep.id); prepAEliminar = null },
            onDismiss = { prepAEliminar = null }
        )
    }

    prepAEditar?.let { prep ->
        EditarPreparacionDialog(
            preparacion = prep,
            onDismiss = { prepAEditar = null },
            onConfirm = { nombre, dimension, rendimiento, seleccionable ->
                viewModel.actualizarPreparacion(
                    prep.copy(
                        nombre = nombre,
                        dimension = dimension,
                        unidadBase = dimension.unidadBase,
                        rendimientoLote = rendimiento,
                        seleccionableEnPos = seleccionable
                    )
                )
                prepAEditar = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarPreparacionDialog(
    preparacion: Preparacion,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, dimension: DimensionUnidad, rendimientoLote: Double, seleccionableEnPos: Boolean) -> Unit
) {
    var nombre by remember { mutableStateOf(preparacion.nombre) }
    var dimension by remember { mutableStateOf(preparacion.dimension) }
    var rendimientoText by remember { mutableStateOf(if (preparacion.rendimientoLote == 0.0) "" else cantidadFormat.format(preparacion.rendimientoLote)) }
    var seleccionable by remember { mutableStateOf(preparacion.seleccionableEnPos) }
    var expandedDimension by remember { mutableStateOf(false) }

    val rendimientoValido = rendimientoText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Preparación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expandedDimension,
                    onExpandedChange = { expandedDimension = !expandedDimension }
                ) {
                    OutlinedTextField(
                        value = dimension.label, onValueChange = {}, readOnly = true,
                        label = { Text("Dimensión") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDimension) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedDimension, onDismissRequest = { expandedDimension = false }) {
                        DimensionUnidad.entries.forEach { d ->
                            DropdownMenuItem(text = { Text(d.label) }, onClick = {
                                dimension = d; expandedDimension = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = rendimientoText, onValueChange = { rendimientoText = it },
                    label = { Text("Rendimiento del lote (${dimension.unidadBase})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = seleccionable, onCheckedChange = { seleccionable = it })
                    Text("Seleccionable en POS", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Los ingredientes de la receta se editan con el botón \"Receta\".",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(nombre, dimension, rendimientoText.replace(",", ".").toDoubleOrNull() ?: return@TextButton, seleccionable)
                },
                enabled = nombre.isNotBlank() && rendimientoValido
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private val costoFormat = DecimalFormat("$#,##0.######")
private val cantidadFormat = DecimalFormat("#,##0.######")

@Composable
private fun PreparacionesList(
    preparaciones: List<Preparacion>,
    puedeBorrar: Boolean = true,
    onVerReceta: (Preparacion) -> Unit,
    onEditar: (Preparacion) -> Unit,
    onEliminar: (Preparacion) -> Unit
) {
    if (preparaciones.isEmpty()) {
        com.toppis.app.ui.components.EmptyState(
            icon = Icons.Filled.Blender,
            titulo = "Sin preparaciones",
            subtitulo = "Usá el botón + para crear tu primera preparación."
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(preparaciones) { prep ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prep.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Costo: ${costoFormat.format(prep.costoBase)}/${prep.unidadBase}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Rinde ${cantidadFormat.format(prep.rendimientoLote)} ${prep.unidadBase} por lote",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (prep.seleccionableEnPos) {
                            Spacer(Modifier.height(4.dp))
                            AssistChip(onClick = {}, label = { Text("Seleccionable en POS") })
                        }
                    }
                    IconButton(onClick = { onVerReceta(prep) }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Receta", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = { onEditar(prep) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (puedeBorrar) {
                        IconButton(onClick = { onEliminar(prep) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearPreparacionDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, dimension: DimensionUnidad, rendimientoLote: Double, seleccionableEnPos: Boolean) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var dimension by remember { mutableStateOf(DimensionUnidad.MASA) }
    var rendimientoText by remember { mutableStateOf("") }
    var seleccionable by remember { mutableStateOf(false) }
    var expandedDimension by remember { mutableStateOf(false) }

    val rendimientoValido = rendimientoText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Preparación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Salsa Bechamel)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expandedDimension,
                    onExpandedChange = { expandedDimension = !expandedDimension }
                ) {
                    OutlinedTextField(
                        value = dimension.label, onValueChange = {}, readOnly = true,
                        label = { Text("Dimensión") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDimension) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedDimension, onDismissRequest = { expandedDimension = false }) {
                        DimensionUnidad.entries.forEach { d ->
                            DropdownMenuItem(text = { Text(d.label) }, onClick = {
                                dimension = d; expandedDimension = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = rendimientoText, onValueChange = { rendimientoText = it },
                    label = { Text("Rendimiento del lote (${dimension.unidadBase})") },
                    supportingText = { Text("Cuánto produce UNA tanda. Si te salen 2 litros de bechamel, poné 2000 (ml).") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = seleccionable, onCheckedChange = { seleccionable = it })
                    Text("Seleccionable en POS", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(nombre, dimension, rendimientoText.replace(",", ".").toDoubleOrNull() ?: return@TextButton, seleccionable)
                },
                enabled = nombre.isNotBlank() && rendimientoValido
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecetaPreparacionDialog(
    preparacion: Preparacion,
    componentes: List<ComponentePreparacion>,
    articulos: List<Articulo>,
    onDismiss: () -> Unit,
    onAgregarComponente: (tipo: TipoComponente, componenteId: Int, cantidad: Double) -> Unit,
    onEliminarComponente: (ComponentePreparacion) -> Unit
) {
    var selectedArticulo by remember { mutableStateOf(articulos.firstOrNull()) }
    var cantidadText by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val cantidadValida = cantidadText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false
    val costoLote = componentes.sumOf { it.costoLinea }
    val costoUnidad = if (preparacion.rendimientoLote > 0) costoLote / preparacion.rendimientoLote else 0.0

    LaunchedEffect(articulos) { if (selectedArticulo == null) selectedArticulo = articulos.firstOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receta: ${preparacion.nombre}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (componentes.isEmpty()) {
                    item {
                        Text(
                            "Sin componentes. Agregá artículos a la receta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    item { Text("Componentes:", style = MaterialTheme.typography.labelLarge) }
                    items(componentes) { comp ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val etiqueta = when (comp.componente.tipoComponente) {
                                TipoComponente.ARTICULO -> "[Art]"
                                TipoComponente.PREPARACION -> "[Prep]"
                            }
                            Column(Modifier.weight(1f)) {
                                Text("$etiqueta ${comp.nombre}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${cantidadFormat.format(comp.componente.cantidadBase)} ${comp.unidad} · ${costoFormat.format(comp.costoLinea)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onEliminarComponente(comp) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    item {
                        Column {
                            Text(
                                "Costo del lote: ${costoFormat.format(costoLote)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "Costo por ${preparacion.unidadBase}: ${costoFormat.format(costoUnidad)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                item { Text("Agregar componente:", style = MaterialTheme.typography.labelLarge) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = true, onClick = {})
                        Text("Artículo", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    val unidadActual = selectedArticulo?.unidadBase ?: preparacion.unidadBase
                    if (articulos.isEmpty()) {
                        Text(
                            "No hay artículos. Crealos en Inventario.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = expandedDropdown,
                            onExpandedChange = { expandedDropdown = !expandedDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedArticulo?.nombre ?: "", onValueChange = {}, readOnly = true,
                                label = { Text("Artículo") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                                articulos.forEach { a ->
                                    DropdownMenuItem(text = { Text("${a.nombre} (${a.unidadBase})") }, onClick = {
                                        selectedArticulo = a; expandedDropdown = false
                                    })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cantidadText, onValueChange = { cantidadText = it },
                        label = { Text("Cantidad en $unidadActual") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            val compId = selectedArticulo?.id ?: return@Button
                            onAgregarComponente(TipoComponente.ARTICULO, compId, cantidadText.replace(",", ".").toDoubleOrNull() ?: return@Button)
                            cantidadText = ""
                        },
                        enabled = cantidadValida && selectedArticulo != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Agregar a receta") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
