package com.toppis.app.ui.modificadores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.AccionModificador
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.db.entities.TipoModificador
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Modificador
import com.toppis.app.data.models.ModificadorComponente
import com.toppis.app.data.models.Preparacion
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModificadoresScreen(
    viewModel: ModificadorViewModel,
    puedeBorrar: Boolean = true,
    onNavigateBack: () -> Unit = {}
) {
    val modificadores by viewModel.modificadores.collectAsState()
    val articulos by viewModel.articulos.collectAsState()
    val preparaciones by viewModel.preparaciones.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrearDialog by remember { mutableStateOf(false) }
    var modAEliminar by remember { mutableStateOf<Modificador?>(null) }
    var modAEditar by remember { mutableStateOf<Modificador?>(null) }
    var modSeleccionado by remember { mutableStateOf<Modificador?>(null) }
    var componentes by remember { mutableStateOf<List<ModificadorComponente>>(emptyList()) }

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
            if (cargandoInicial && modificadores.isEmpty()) {
                com.toppis.app.ui.components.SkeletonList()
            } else if (modificadores.isEmpty()) {
                com.toppis.app.ui.components.EmptyState(
                    icon = Icons.Filled.Tune,
                    titulo = "Sin modificadores",
                    subtitulo = "Usá el botón + para crear tu primer modificador."
                )
            } else {
                val money = DecimalFormat("$#,##0")
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(modificadores) { mod ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mod.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(mod.tipo.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    val signo = if (mod.deltaPrecio >= 0) "+" else "-"
                                    Text("Precio: $signo${money.format(kotlin.math.abs(mod.deltaPrecio))}",
                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                OutlinedButton(onClick = {
                                    modSeleccionado = mod
                                    componentes = emptyList()
                                    viewModel.loadComponentes(mod.id) { componentes = it }
                                }) { Text("Receta") }
                                IconButton(onClick = { modAEditar = mod }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                }
                                if (puedeBorrar) {
                                    IconButton(onClick = { modAEliminar = mod }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                    }
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

    modAEditar?.let { mod ->
        EditarModificadorDialog(
            modificador = mod,
            onDismiss = { modAEditar = null },
            onConfirm = { nombre, tipo, delta ->
                viewModel.actualizarModificador(mod.copy(nombre = nombre, tipo = tipo, deltaPrecio = delta))
                modAEditar = null
            }
        )
    }

    modSeleccionado?.let { mod ->
        RecetaModificadorDialog(
            modificador = mod,
            componentes = componentes,
            articulos = articulos,
            preparaciones = preparaciones,
            nombreComponente = { tipo, id -> viewModel.nombreComponente(tipo, id) },
            onDismiss = { modSeleccionado = null; componentes = emptyList() },
            onAgregar = { accion, tipo, compId, cantidad ->
                viewModel.agregarComponente(mod.id, accion, tipo, compId, cantidad) {
                    viewModel.loadComponentes(mod.id) { componentes = it }
                }
            },
            onEliminar = { compId ->
                viewModel.eliminarComponente(compId) {
                    viewModel.loadComponentes(mod.id) { componentes = it }
                }
            }
        )
    }

    modAEliminar?.let { mod ->
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = mod.nombre,
            titulo = "Eliminar modificador",
            onConfirm = { viewModel.eliminarModificador(mod.id); modAEliminar = null },
            onDismiss = { modAEliminar = null }
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

    val ayudaTipo = when (tipoSeleccionado) {
        TipoModificador.DOBLE -> "Duplica un componente. Agregá en la Receta lo que se duplica (ej: +1 medallón)."
        TipoModificador.QUITAR -> "Saca un ingrediente. En la Receta poné QUITAR ese ingrediente (devuelve su stock)."
        TipoModificador.REEMPLAZAR -> "Cambia uno por otro. En la Receta: QUITAR el original y AGREGAR el nuevo."
        TipoModificador.EXTRA -> "Agregado extra. En la Receta poné AGREGAR el ingrediente y un precio +."
    }

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
                ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = !expandedTipo }) {
                    OutlinedTextField(
                        value = tipoSeleccionado.label, onValueChange = {}, readOnly = true,
                        label = { Text("Tipo") },
                        supportingText = { Text(ayudaTipo) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                        TipoModificador.entries.forEach { tipo ->
                            DropdownMenuItem(text = { Text(tipo.label) }, onClick = { tipoSeleccionado = tipo; expandedTipo = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = deltaText, onValueChange = { deltaText = it },
                    label = { Text("Cambio de precio (CLP)") },
                    supportingText = { Text("Lo que suma (o resta con -) al precio del plato. Doble carne ≈ +1800.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text("Después de crear, tocá \"Receta\" para definir qué ingredientes agrega o quita (para descontar stock y calcular costo).",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre, tipoSeleccionado, deltaText.replace(",", ".").toDoubleOrNull() ?: return@TextButton) },
                enabled = nombre.isNotBlank() && deltaValido
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarModificadorDialog(
    modificador: Modificador,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, tipo: TipoModificador, deltaPrecio: Double) -> Unit
) {
    var nombre by remember { mutableStateOf(modificador.nombre) }
    var tipoSeleccionado by remember { mutableStateOf(modificador.tipo) }
    var expandedTipo by remember { mutableStateOf(false) }
    var deltaText by remember { mutableStateOf(if (modificador.deltaPrecio == 0.0) "" else modificador.deltaPrecio.toLong().toString()) }

    val deltaValido = deltaText.replace(",", ".").toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Modificador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = !expandedTipo }) {
                    OutlinedTextField(
                        value = tipoSeleccionado.label, onValueChange = {}, readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                        TipoModificador.entries.forEach { tipo ->
                            DropdownMenuItem(text = { Text(tipo.label) }, onClick = { tipoSeleccionado = tipo; expandedTipo = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = deltaText, onValueChange = { deltaText = it },
                    label = { Text("Cambio de precio (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text("La receta (ingredientes que agrega/quita) se edita con el botón \"Receta\".",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre, tipoSeleccionado, deltaText.replace(",", ".").toDoubleOrNull() ?: return@TextButton) },
                enabled = nombre.isNotBlank() && deltaValido
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecetaModificadorDialog(
    modificador: Modificador,
    componentes: List<ModificadorComponente>,
    articulos: List<Articulo>,
    preparaciones: List<Preparacion>,
    nombreComponente: (TipoComponente, Int) -> String,
    onDismiss: () -> Unit,
    onAgregar: (accion: AccionModificador, tipo: TipoComponente, compId: Int, cantidad: Double) -> Unit,
    onEliminar: (compId: Int) -> Unit
) {
    var accion by remember { mutableStateOf(AccionModificador.AGREGAR) }
    var tipo by remember { mutableStateOf(TipoComponente.ARTICULO) }
    var selectedArticulo by remember { mutableStateOf(articulos.firstOrNull()) }
    var selectedPrep by remember { mutableStateOf(preparaciones.firstOrNull()) }
    var cantidadText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val cantidadValida = cantidadText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false

    LaunchedEffect(articulos) { if (selectedArticulo == null) selectedArticulo = articulos.firstOrNull() }
    LaunchedEffect(preparaciones) { if (selectedPrep == null) selectedPrep = preparaciones.firstOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receta: ${modificador.nombre}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (componentes.isEmpty()) {
                    item { Text("Sin componentes. Agregá qué ingrediente suma (AGREGAR) o saca (QUITAR).",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                } else {
                    item { Text("Componentes:", style = MaterialTheme.typography.labelLarge) }
                    items(componentes) { c ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            val etiqueta = if (c.accion == AccionModificador.AGREGAR) "➕" else "➖"
                            Text("$etiqueta ${nombreComponente(c.tipoComponente, c.componenteId)} · ${DecimalFormat("#,##0.##").format(c.cantidadBase)}",
                                modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onEliminar(c.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                item { Text("Agregar componente:", style = MaterialTheme.typography.labelLarge) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = accion == AccionModificador.AGREGAR, onClick = { accion = AccionModificador.AGREGAR })
                        Text("Agregar", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = accion == AccionModificador.QUITAR, onClick = { accion = AccionModificador.QUITAR })
                        Text("Quitar", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tipo == TipoComponente.ARTICULO, onClick = { tipo = TipoComponente.ARTICULO; expanded = false })
                        Text("Artículo", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = tipo == TipoComponente.PREPARACION, onClick = { tipo = TipoComponente.PREPARACION; expanded = false })
                        Text("Preparación", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    val unidad = when (tipo) {
                        TipoComponente.ARTICULO -> selectedArticulo?.unidadBase ?: "g"
                        TipoComponente.PREPARACION -> selectedPrep?.unidadBase ?: "g"
                    }
                    val listaVacia = (tipo == TipoComponente.ARTICULO && articulos.isEmpty()) ||
                        (tipo == TipoComponente.PREPARACION && preparaciones.isEmpty())
                    if (listaVacia) {
                        Text("No hay ${if (tipo == TipoComponente.ARTICULO) "artículos" else "preparaciones"}.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            val valor = when (tipo) {
                                TipoComponente.ARTICULO -> selectedArticulo?.nombre ?: ""
                                TipoComponente.PREPARACION -> selectedPrep?.nombre ?: ""
                            }
                            OutlinedTextField(
                                value = valor, onValueChange = {}, readOnly = true,
                                label = { Text(if (tipo == TipoComponente.ARTICULO) "Artículo" else "Preparación") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                if (tipo == TipoComponente.ARTICULO) {
                                    articulos.forEach { a -> DropdownMenuItem(text = { Text("${a.nombre} (${a.unidadBase})") }, onClick = { selectedArticulo = a; expanded = false }) }
                                } else {
                                    preparaciones.forEach { p -> DropdownMenuItem(text = { Text("${p.nombre} (${p.unidadBase})") }, onClick = { selectedPrep = p; expanded = false }) }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cantidadText, onValueChange = { cantidadText = it },
                            label = { Text("Cantidad en $unidad") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Button(
                        onClick = {
                            val compId = when (tipo) {
                                TipoComponente.ARTICULO -> selectedArticulo?.id ?: return@Button
                                TipoComponente.PREPARACION -> selectedPrep?.id ?: return@Button
                            }
                            onAgregar(accion, tipo, compId, cantidadText.replace(",", ".").toDoubleOrNull() ?: return@Button)
                            cantidadText = ""
                        },
                        enabled = cantidadValida && when (tipo) {
                            TipoComponente.ARTICULO -> selectedArticulo != null
                            TipoComponente.PREPARACION -> selectedPrep != null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Agregar a receta") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
