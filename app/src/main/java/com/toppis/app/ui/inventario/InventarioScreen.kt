package com.toppis.app.ui.inventario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.Ingrediente
import com.toppis.app.data.models.Insumo
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private const val STOCK_ALERTA = 10.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val insumos by viewModel.insumos.collectAsState()
    val ingredientes by viewModel.ingredientes.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    var showCrearInsumoDialog by remember { mutableStateOf(false) }
    var showCrearIngredienteDialog by remember { mutableStateOf(false) }
    var insumoEnEdicion by remember { mutableStateOf<Insumo?>(null) }
    var ingredienteEnEdicion by remember { mutableStateOf<Ingrediente?>(null) }
    var insumoAEliminar by remember { mutableStateOf<Insumo?>(null) }
    var ingredienteAEliminar by remember { mutableStateOf<Ingrediente?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is InventarioUiState.Error) {
            snackbarHostState.showSnackbar((uiState as InventarioUiState.Error).message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Insumos (${insumos.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Ingredientes (${ingredientes.size})") }
                )
            }

            when (selectedTab) {
                0 -> InsumosTab(
                    insumos = insumos,
                    onEditar = { insumoEnEdicion = it },
                    onEliminar = { insumoAEliminar = it }
                )
                1 -> IngredientesTab(
                    ingredientes = ingredientes,
                    onEditar = { ingredienteEnEdicion = it },
                    onEliminar = { ingredienteAEliminar = it }
                )
            }
        }

        FloatingActionButton(
            onClick = {
                if (selectedTab == 0) showCrearInsumoDialog = true
                else showCrearIngredienteDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Crear")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    // Crear insumo
    if (showCrearInsumoDialog) {
        CrearInsumoDialog(
            onDismiss = { showCrearInsumoDialog = false },
            onConfirm = { nombre, desc, precio, unidad, stock ->
                viewModel.crearInsumo(nombre, desc, precio, unidad, stock)
                showCrearInsumoDialog = false
            }
        )
    }

    // Editar insumo (mismo formulario)
    insumoEnEdicion?.let { insumo ->
        CrearInsumoDialog(
            insumoInicial = insumo,
            onDismiss = { insumoEnEdicion = null },
            onConfirm = { nombre, desc, precio, unidad, stock ->
                viewModel.editarInsumo(
                    insumo.copy(
                        nombre = nombre,
                        descripcion = desc,
                        precio = precio,
                        unidadMedida = unidad,
                        stock = stock
                    )
                )
                insumoEnEdicion = null
            }
        )
    }

    // Crear ingrediente
    if (showCrearIngredienteDialog) {
        CrearIngredienteConMermaDialog(
            onDismiss = { showCrearIngredienteDialog = false },
            onConfirm = { nombre, unidad, stock, costo, costoCompra, porcentajeMerma, unidadCompra, cantidadComprada, cantidadAprovechable, costoGramo ->
                viewModel.crearIngrediente(nombre, unidad, stock, costo, costoCompra, porcentajeMerma, unidadCompra, cantidadComprada, cantidadAprovechable, costoGramo)
                showCrearIngredienteDialog = false
            }
        )
    }

    // Editar ingrediente (mismo formulario)
    ingredienteEnEdicion?.let { ingrediente ->
        CrearIngredienteConMermaDialog(
            ingredienteInicial = ingrediente,
            onDismiss = { ingredienteEnEdicion = null },
            onConfirm = { nombre, unidad, stock, costo, costoCompra, porcentajeMerma, unidadCompra, cantidadComprada, cantidadAprovechable, costoGramo ->
                viewModel.editarIngrediente(
                    ingrediente.copy(
                        nombre = nombre,
                        unidadMedida = unidad,
                        stockActual = stock,
                        costoUnitario = costo,
                        costoCompra = costoCompra,
                        porcentajeMerma = porcentajeMerma,
                        unidadCompra = unidadCompra,
                        cantidadComprada = cantidadComprada,
                        cantidadAprovechable = cantidadAprovechable,
                        costoGramo = costoGramo
                    )
                )
                ingredienteEnEdicion = null
            }
        )
    }

    // Confirmar eliminar insumo
    insumoAEliminar?.let { insumo ->
        AlertDialog(
            onDismissRequest = { insumoAEliminar = null },
            title = { Text("Eliminar insumo") },
            text = { Text("¿Eliminar el insumo \"${insumo.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarInsumo(insumo.id)
                    insumoAEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { insumoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    // Confirmar eliminar ingrediente
    ingredienteAEliminar?.let { ingrediente ->
        AlertDialog(
            onDismissRequest = { ingredienteAEliminar = null },
            title = { Text("Eliminar ingrediente") },
            text = { Text("¿Eliminar el ingrediente \"${ingrediente.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarIngrediente(ingrediente.id)
                    ingredienteAEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { ingredienteAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

// ── Tab Insumos ───────────────────────────────────────────────────────────────

@Composable
private fun InsumosTab(
    insumos: List<Insumo>,
    onEditar: (Insumo) -> Unit,
    onEliminar: (Insumo) -> Unit
) {
    if (insumos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin insumos. Usa el boton + para agregar.", color = MaterialTheme.colorScheme.outline)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(insumos) { insumo ->
            InsumoCard(
                insumo = insumo,
                onEditar = { onEditar(insumo) },
                onEliminar = { onEliminar(insumo) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InsumoCard(
    insumo: Insumo,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val formatter = DecimalFormat("$#,##0")
    Card(modifier = Modifier.fillMaxWidth(), onClick = onEditar) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(insumo.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${formatter.format(insumo.precio)} · ${insumo.unidadMedida}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Stock: ${insumo.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Tab Ingredientes ──────────────────────────────────────────────────────────

@Composable
private fun IngredientesTab(
    ingredientes: List<Ingrediente>,
    onEditar: (Ingrediente) -> Unit,
    onEliminar: (Ingrediente) -> Unit
) {
    if (ingredientes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin ingredientes. Usá el botón + para agregar.", color = MaterialTheme.colorScheme.outline)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(ingredientes) { ingrediente ->
            IngredienteCard(
                ingrediente = ingrediente,
                onEditar = { onEditar(ingrediente) },
                onEliminar = { onEliminar(ingrediente) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredienteCard(
    ingrediente: Ingrediente,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val enAlerta = ingrediente.stockActual < STOCK_ALERTA
    val stockColor = if (enAlerta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val formatter = DecimalFormat("$#,##0")

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEditar,
        colors = CardDefaults.cardColors(
            containerColor = if (enAlerta)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ingrediente.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Stock: ${ingrediente.stockActual} ${ingrediente.unidadMedida}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = stockColor
                )
                Text(
                    text = "Costo unitario: ${formatter.format(ingrediente.costoUnitario)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (enAlerta) {
                    Text(
                        text = "⚠ Stock bajo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun CrearInsumoDialog(
    insumoInicial: Insumo? = null,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, descripcion: String, precio: Double, unidad: String, stockInicial: Int) -> Unit
) {
    val esEdicion = insumoInicial != null
    var nombre by remember { mutableStateOf(insumoInicial?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(insumoInicial?.descripcion ?: "") }
    var precioText by remember { mutableStateOf(insumoInicial?.precio?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var unidadMedida by remember { mutableStateOf(insumoInicial?.unidadMedida ?: "UN") }
    var stockText by remember { mutableStateOf(insumoInicial?.stock?.toString() ?: "0") }

    val precioValido = precioText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false
    val stockValido = stockText.toIntOrNull()?.let { it >= 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (esEdicion) "Editar Insumo" else "Nuevo Insumo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripcion (opcional)") },
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
                OutlinedTextField(
                    value = unidadMedida,
                    onValueChange = { unidadMedida = it },
                    label = { Text("Unidad (ej: unidad, kg, gr)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("Stock inicial") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        nombre,
                        descripcion,
                        precioText.replace(",", ".").toDouble(),
                        unidadMedida,
                        stockText.toInt()
                    )
                },
                enabled = nombre.isNotBlank() && unidadMedida.isNotBlank() && precioValido && stockValido
            ) { Text(if (esEdicion) "Guardar" else "Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}





