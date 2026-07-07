package com.toppis.app.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.toppis.app.ui.components.ImagePickerField
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Preparacion
import com.toppis.app.data.repository.ComponenteReceta
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuConfigScreen(
    viewModel: MenuConfigViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val itemsMenu by viewModel.itemsMenu.collectAsState()
    val articulos by viewModel.articulos.collectAsState()
    val preparaciones by viewModel.preparaciones.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrearItemDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ItemMenu?>(null) }
    var fotoDe by remember { mutableStateOf<ItemMenu?>(null) }
    var componentesReceta by remember { mutableStateOf<List<ComponenteReceta>>(emptyList()) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is MenuConfigUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as MenuConfigUiState.Error).message)
                viewModel.resetState()
            }
            MenuConfigUiState.Success -> viewModel.resetState()
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Configurar Menú", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrearItemDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear item")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ItemsMenuTab(
                items = itemsMenu,
                onVerReceta = { item ->
                    selectedItem = item
                    componentesReceta = emptyList()
                    viewModel.loadComponentesReceta(item.id) { componentesReceta = it }
                },
                onFoto = { fotoDe = it },
                onEliminar = { viewModel.eliminarItemMenu(it) }
            )
        }
    }

    if (showCrearItemDialog) {
        CrearItemMenuDialog(
            onDismiss = { showCrearItemDialog = false },
            onConfirm = { nombre, desc, precio, categoria, imagenUrl ->
                viewModel.crearItemMenu(nombre, desc, precio, categoria, imagenUrl)
                showCrearItemDialog = false
            }
        )
    }

    // Diálogo para agregar/cambiar la foto de un item existente
    fotoDe?.let { item ->
        AlertDialog(
            onDismissRequest = { fotoDe = null },
            title = { Text("Foto de ${item.nombre}") },
            text = {
                com.toppis.app.ui.components.ImagePickerField(
                    imagenUrl = item.imagenUrl,
                    carpeta = "items",
                    onImagenSubida = { url ->
                        viewModel.actualizarItemMenu(item.copy(imagenUrl = url))
                        fotoDe = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { fotoDe = null }) { Text("Cerrar") } }
        )
    }

    if (selectedItem != null) {
        RecetaMenuDialog(
            itemMenu = selectedItem!!,
            componentes = componentesReceta,
            articulos = articulos,
            preparaciones = preparaciones,
            onDismiss = {
                selectedItem = null
                componentesReceta = emptyList()
            },
            onAgregarComponente = { tipo, componenteId, cantidad ->
                viewModel.agregarComponente(selectedItem!!.id, tipo, componenteId, cantidad) {
                    viewModel.loadComponentesReceta(selectedItem!!.id) { componentesReceta = it }
                }
            },
            onEliminarComponente = { comp ->
                viewModel.eliminarComponente(comp.recetaMenu) {
                    viewModel.loadComponentesReceta(selectedItem!!.id) { componentesReceta = it }
                }
            }
        )
    }
}

@Composable
private fun ItemsMenuTab(
    items: List<ItemMenu>,
    onVerReceta: (ItemMenu) -> Unit,
    onFoto: (ItemMenu) -> Unit,
    onEliminar: (ItemMenu) -> Unit
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin items en el menú.\nUsá el botón + para agregar.", color = MaterialTheme.colorScheme.outline)
        }
        return
    }
    val money = DecimalFormat("$#,##0")
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(items) { item ->
            val foodCostPct = if (item.precio > 0) item.costoTeorico / item.precio * 100.0 else 0.0
            val fcColor = when {
                foodCostPct == 0.0 -> MaterialTheme.colorScheme.outline
                foodCostPct <= 32 -> MaterialTheme.colorScheme.primary
                foodCostPct <= 40 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    MenuThumb(item.imagenUrl, onClick = { onFoto(item) })
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Precio ${money.format(item.precio)} · Costo ${money.format(item.costoTeorico)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (foodCostPct == 0.0) "Food cost: sin receta"
                            else "Food cost: ${DecimalFormat("0.#").format(foodCostPct)}%  ·  Margen ${money.format(item.precio - item.costoTeorico)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = fcColor
                        )
                        if (item.categoria.isNotBlank()) {
                            Text(item.categoria, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    OutlinedButton(onClick = { onVerReceta(item) }) { Text("Receta") }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onEliminar(item) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuThumb(url: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(56.dp)
    ) {
        if (url != null) {
            AsyncImage(
                model = url, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Agregar foto", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CrearItemMenuDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, descripcion: String, precio: Double, categoria: String, imagenUrl: String?) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var precioText by remember { mutableStateOf("") }
    var imagenUrl by remember { mutableStateOf<String?>(null) }

    val precioValido = precioText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Item del Menú") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ImagePickerField(
                    imagenUrl = imagenUrl,
                    carpeta = "items",
                    onImagenSubida = { imagenUrl = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Hamburguesa)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoria, onValueChange = { categoria = it },
                    label = { Text("Categoría (ej: Hamburguesas)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion, onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precioText, onValueChange = { precioText = it },
                    label = { Text("Precio (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre, descripcion, precioText.replace(",", ".").toDoubleOrNull() ?: return@TextButton, categoria, imagenUrl) },
                enabled = nombre.isNotBlank() && precioValido
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecetaMenuDialog(
    itemMenu: ItemMenu,
    componentes: List<ComponenteReceta>,
    articulos: List<Articulo>,
    preparaciones: List<Preparacion>,
    onDismiss: () -> Unit,
    onAgregarComponente: (tipo: TipoComponente, componenteId: Int, cantidad: Double) -> Unit,
    onEliminarComponente: (ComponenteReceta) -> Unit
) {
    var tipoSeleccionado by remember { mutableStateOf(TipoComponente.ARTICULO) }
    var selectedArticulo by remember { mutableStateOf(articulos.firstOrNull()) }
    var selectedPrep by remember { mutableStateOf(preparaciones.firstOrNull()) }
    var cantidadText by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val cantidadValida = cantidadText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false
    val money = DecimalFormat("$#,##0")
    val costoTotal = componentes.sumOf { it.costoLinea }

    LaunchedEffect(articulos) { if (selectedArticulo == null) selectedArticulo = articulos.firstOrNull() }
    LaunchedEffect(preparaciones) { if (selectedPrep == null) selectedPrep = preparaciones.firstOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receta: ${itemMenu.nombre}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (componentes.isEmpty()) {
                    item {
                        Text("Sin componentes. Agregá artículos o preparaciones.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    item { Text("Componentes:", style = MaterialTheme.typography.labelLarge) }
                    items(componentes) { comp ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val etiqueta = when (comp.recetaMenu.tipoComponente) {
                                TipoComponente.ARTICULO -> "[Art]"
                                TipoComponente.PREPARACION -> "[Prep]"
                            }
                            Column(Modifier.weight(1f)) {
                                Text("$etiqueta ${comp.nombre}", style = MaterialTheme.typography.bodyMedium)
                                Text("${DecimalFormat("#,##0.##").format(comp.recetaMenu.cantidadBase)} ${comp.unidad} · ${money.format(comp.costoLinea)}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onEliminarComponente(comp) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    item {
                        val fc = if (itemMenu.precio > 0) costoTotal / itemMenu.precio * 100.0 else 0.0
                        Text("Costo receta: ${money.format(costoTotal)} · Food cost ${DecimalFormat("0.#").format(fc)}%",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                item { Text("Agregar componente:", style = MaterialTheme.typography.labelLarge) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tipoSeleccionado == TipoComponente.ARTICULO, onClick = {
                            tipoSeleccionado = TipoComponente.ARTICULO; expandedDropdown = false
                        })
                        Text("Artículo", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = tipoSeleccionado == TipoComponente.PREPARACION, onClick = {
                            tipoSeleccionado = TipoComponente.PREPARACION; expandedDropdown = false
                        })
                        Text("Preparación", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    val unidadActual = when (tipoSeleccionado) {
                        TipoComponente.ARTICULO -> selectedArticulo?.unidadBase ?: "g"
                        TipoComponente.PREPARACION -> selectedPrep?.unidadBase ?: "g"
                    }
                    when (tipoSeleccionado) {
                        TipoComponente.ARTICULO -> {
                            if (articulos.isEmpty()) {
                                Text("No hay artículos. Crealos en Inventario.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            } else {
                                ExposedDropdownMenuBox(expanded = expandedDropdown, onExpandedChange = { expandedDropdown = !expandedDropdown }) {
                                    OutlinedTextField(
                                        value = selectedArticulo?.nombre ?: "", onValueChange = {}, readOnly = true,
                                        label = { Text("Artículo") },
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
                        }
                        TipoComponente.PREPARACION -> {
                            if (preparaciones.isEmpty()) {
                                Text("No hay preparaciones. Crealas en Preparaciones.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            } else {
                                ExposedDropdownMenuBox(expanded = expandedDropdown, onExpandedChange = { expandedDropdown = !expandedDropdown }) {
                                    OutlinedTextField(
                                        value = selectedPrep?.nombre ?: "", onValueChange = {}, readOnly = true,
                                        label = { Text("Preparación") },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                                        preparaciones.forEach { p ->
                                            DropdownMenuItem(text = { Text("${p.nombre} (${p.unidadBase})") }, onClick = {
                                                selectedPrep = p; expandedDropdown = false
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cantidadText, onValueChange = { cantidadText = it },
                        label = { Text("Cantidad en $unidadActual (por unidad vendida)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            val compId = when (tipoSeleccionado) {
                                TipoComponente.ARTICULO -> selectedArticulo?.id ?: return@Button
                                TipoComponente.PREPARACION -> selectedPrep?.id ?: return@Button
                            }
                            onAgregarComponente(tipoSeleccionado, compId, cantidadText.replace(",", ".").toDoubleOrNull() ?: return@Button)
                            cantidadText = ""
                        },
                        enabled = cantidadValida && when (tipoSeleccionado) {
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
