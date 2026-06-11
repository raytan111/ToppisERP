package com.toppis.app.ui.menu

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
import com.toppis.app.data.models.Ingrediente
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Insumo
import com.toppis.app.data.models.Salsa
import com.toppis.app.data.db.entities.TipoComponente
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
    val salsas by viewModel.salsas.collectAsState()
    val ingredientes by viewModel.ingredientes.collectAsState()
    val insumos by viewModel.insumos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    var showCrearItemDialog by remember { mutableStateOf(false) }
    var showCrearSalsaDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ItemMenu?>(null) }
    var componentesReceta by remember { mutableStateOf<List<ComponenteReceta>>(emptyList()) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is MenuConfigUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as MenuConfigUiState.Error).message)
                viewModel.resetState()
            }
            MenuConfigUiState.Success -> {
                snackbarHostState.showSnackbar("Guardado con éxito")
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ToppisTopBar(
                titulo = "Configurar Menú",
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showCrearItemDialog = true
                    else showCrearSalsaDialog = true
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Crear")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Items Menú (${itemsMenu.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Salsas (${salsas.size})") }
                )
            }

            when (selectedTab) {
                0 -> ItemsMenuTab(
                    items = itemsMenu,
                    onVerReceta = { item ->
                        selectedItem = item
                        componentesReceta = emptyList()
                        viewModel.loadComponentesReceta(item.id) { componentesReceta = it }
                    },
                    onEliminar = { viewModel.eliminarItemMenu(it) }
                )
                1 -> SalsasTab(
                    salsas = salsas,
                    onEliminar = { viewModel.eliminarSalsa(it) }
                )
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────────

    if (showCrearItemDialog) {
        CrearItemMenuDialog(
            onDismiss = { showCrearItemDialog = false },
            onConfirm = { nombre, desc, precio ->
                viewModel.crearItemMenu(nombre, desc, precio)
                showCrearItemDialog = false
            }
        )
    }

    if (showCrearSalsaDialog) {
        CrearSalsaDialog(
            onDismiss = { showCrearSalsaDialog = false },
            onConfirm = { nombre, desc ->
                viewModel.crearSalsa(nombre, desc)
                showCrearSalsaDialog = false
            }
        )
    }

    if (selectedItem != null) {
        RecetaMenuDialog(
            itemMenu = selectedItem!!,
            componentes = componentesReceta,
            ingredientes = ingredientes,
            insumos = insumos,
            salsas = salsas,
            onDismiss = {
                selectedItem = null
                componentesReceta = emptyList()
            },
            onAgregarComponente = { tipo, componenteId, cantidad ->
                viewModel.agregarComponente(selectedItem!!.id, tipo, componenteId, cantidad)
                viewModel.loadComponentesReceta(selectedItem!!.id) { componentesReceta = it }
            }
        )
    }
}

// ── Tab Items Menú ───────────────────────────────────────────────────────────────

@Composable
private fun ItemsMenuTab(
    items: List<ItemMenu>,
    onVerReceta: (ItemMenu) -> Unit,
    onEliminar: (ItemMenu) -> Unit
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Sin items en el menú.\nUsá el botón + para agregar.",
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }
    val formatter = DecimalFormat("$#,##0")
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(items) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatter.format(item.precio),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.descripcion.isNotBlank()) {
                            Text(
                                item.descripcion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    OutlinedButton(onClick = { onVerReceta(item) }) {
                        Text("Receta")
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onEliminar(item) }) {
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

// ── Tab Salsas ───────────────────────────────────────────────────────────────────

@Composable
private fun SalsasTab(
    salsas: List<Salsa>,
    onEliminar: (Salsa) -> Unit
) {
    if (salsas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Sin salsas configuradas.\nUsá el botón + para agregar.",
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(salsas) { salsa ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(salsa.nombre, style = MaterialTheme.typography.titleMedium)
                        if (salsa.descripcion.isNotBlank()) {
                            Text(
                                salsa.descripcion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    IconButton(onClick = { onEliminar(salsa) }) {
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

// ── Dialogs ──────────────────────────────────────────────────────────────────────

@Composable
private fun CrearItemMenuDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, descripcion: String, precio: Double) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precioText by remember { mutableStateOf("") }

    val precioValido = precioText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Item del Menú") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Hamburguesa)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precioText,
                    onValueChange = { precioText = it },
                    label = { Text("Precio (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(nombre, descripcion, precioText.replace(",", ".").toDouble())
                },
                enabled = nombre.isNotBlank() && precioValido
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CrearSalsaDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, descripcion: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Salsa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Mayonesa)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre, descripcion) },
                enabled = nombre.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecetaMenuDialog(
    itemMenu: ItemMenu,
    componentes: List<ComponenteReceta>,
    ingredientes: List<Ingrediente>,
    insumos: List<Insumo>,
    salsas: List<Salsa>,
    onDismiss: () -> Unit,
    onAgregarComponente: (tipo: TipoComponente, componenteId: Int, cantidad: Double) -> Unit
) {
    var tipoSeleccionado by remember { mutableStateOf(TipoComponente.INGREDIENTE) }
    var selectedIngrediente by remember { mutableStateOf(ingredientes.firstOrNull()) }
    var selectedInsumo by remember { mutableStateOf(insumos.firstOrNull()) }
    var selectedSalsa by remember { mutableStateOf(salsas.firstOrNull()) }
    var cantidadText by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val cantidadValida = cantidadText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false

    // Actualizar selección cuando cambian las listas
    LaunchedEffect(ingredientes) { if (selectedIngrediente == null) selectedIngrediente = ingredientes.firstOrNull() }
    LaunchedEffect(insumos) { if (selectedInsumo == null) selectedInsumo = insumos.firstOrNull() }
    LaunchedEffect(salsas) { if (selectedSalsa == null) selectedSalsa = salsas.firstOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receta: ${itemMenu.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // ── Lista actual de componentes ──────────────────────────
                if (componentes.isEmpty()) {
                    Text(
                        "Sin componentes. Agrega ingredientes y/o insumos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text("Componentes:", style = MaterialTheme.typography.labelLarge)
                    componentes.forEach { comp ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val etiqueta = when (comp.recetaMenu.tipoComponente) {
                                TipoComponente.INGREDIENTE -> "[Ing]"
                                TipoComponente.INSUMO -> "[Ins]"
                                TipoComponente.SALSA -> "[Sal]"
                            }
                            Text(
                                "$etiqueta ${comp.nombre}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${comp.recetaMenu.cantidad} ${comp.unidad}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ── Tipo de componente ───────────────────────────────────
                Text("Agregar componente:", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = tipoSeleccionado == TipoComponente.INGREDIENTE,
                        onClick = {
                            tipoSeleccionado = TipoComponente.INGREDIENTE
                            expandedDropdown = false
                        }
                    )
                    Text("Ingrediente", style = MaterialTheme.typography.bodySmall)
                    RadioButton(
                        selected = tipoSeleccionado == TipoComponente.INSUMO,
                        onClick = {
                            tipoSeleccionado = TipoComponente.INSUMO
                            expandedDropdown = false
                        }
                    )
                    Text("Insumo", style = MaterialTheme.typography.bodySmall)
                    RadioButton(
                        selected = tipoSeleccionado == TipoComponente.SALSA,
                        onClick = {
                            tipoSeleccionado = TipoComponente.SALSA
                            expandedDropdown = false
                        }
                    )
                    Text("Salsa", style = MaterialTheme.typography.bodySmall)
                }

                // ── Selector ────────────────────────────────────────────
                when (tipoSeleccionado) {
                    TipoComponente.INGREDIENTE -> {
                        if (ingredientes.isEmpty()) {
                            Text(
                                "No hay ingredientes. Crealos en Inventario.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = expandedDropdown,
                                onExpandedChange = { expandedDropdown = !expandedDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedIngrediente?.nombre ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Ingrediente") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    ingredientes.forEach { ing ->
                                        DropdownMenuItem(
                                            text = { Text("${ing.nombre} (${ing.unidadMedida})") },
                                            onClick = {
                                                selectedIngrediente = ing
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    TipoComponente.INSUMO -> {
                        if (insumos.isEmpty()) {
                            Text(
                                "No hay insumos. Crealos en Inventario.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = expandedDropdown,
                                onExpandedChange = { expandedDropdown = !expandedDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedInsumo?.nombre ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Insumo") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    insumos.forEach { ins ->
                                        DropdownMenuItem(
                                            text = { Text("${ins.nombre} (${ins.unidadMedida})") },
                                            onClick = {
                                                selectedInsumo = ins
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    TipoComponente.SALSA -> {
                        if (salsas.isEmpty()) {
                            Text(
                                "No hay salsas. Crealas en la pestaña Salsas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = expandedDropdown,
                                onExpandedChange = { expandedDropdown = !expandedDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedSalsa?.nombre ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Salsa") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    salsas.forEach { sal ->
                                        DropdownMenuItem(
                                            text = { Text(sal.nombre) },
                                            onClick = {
                                                selectedSalsa = sal
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Cantidad ────────────────────────────────────────────
                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = { cantidadText = it },
                    label = { Text("Cantidad en gramos (por unidad vendida)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val compId = when (tipoSeleccionado) {
                            TipoComponente.INGREDIENTE -> selectedIngrediente?.id ?: return@Button
                            TipoComponente.INSUMO -> selectedInsumo?.id ?: return@Button
                            TipoComponente.SALSA -> selectedSalsa?.id ?: return@Button
                        }
                        onAgregarComponente(
                            tipoSeleccionado,
                            compId,
                            cantidadText.replace(",", ".").toDouble()
                        )
                        cantidadText = ""
                    },
                    enabled = cantidadValida && when (tipoSeleccionado) {
                        TipoComponente.INGREDIENTE -> selectedIngrediente != null
                        TipoComponente.INSUMO -> selectedInsumo != null
                        TipoComponente.SALSA -> selectedSalsa != null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Agregar a receta")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
