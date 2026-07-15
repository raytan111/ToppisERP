package com.toppis.app.ui.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.EstadoPedido
import com.toppis.app.data.models.Pedido
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val money = DecimalFormat("$#,##0")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosPedidosScreen(
    viewModel: PedidosViewModel,
    onAbrirPedido: (Int) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val pedidos by viewModel.pedidos.collectAsState()
    val clientes by viewModel.clientesById.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showNuevo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(uiState) {
        (uiState as? PedidosUiState.Error)?.let { errorMsg = it.message; viewModel.resetState() }
    }
    errorMsg?.let { msg ->
        com.toppis.app.ui.components.ToppisErrorDialog(mensaje = msg, onDismiss = { errorMsg = null })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Pedidos", onBack = onNavigateBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNuevo = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo pedido") }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                cargandoInicial && pedidos.isEmpty() -> com.toppis.app.ui.components.SkeletonList()
                pedidos.isEmpty() -> com.toppis.app.ui.components.EmptyState(
                    icon = Icons.Filled.PointOfSale,
                    titulo = "Sin pedidos activos",
                    subtitulo = "Tocá \"Nuevo pedido\" para empezar una venta."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                ) {
                    items(pedidos, key = { it.id }) { pedido ->
                        PedidoCard(
                            pedido = pedido,
                            clienteEtiqueta = viewModel.clienteDe(pedido)?.etiqueta ?: "Cliente",
                            onClick = { onAbrirPedido(pedido.id) }
                        )
                    }
                }
            }
        }
    }

    if (showNuevo) {
        val clientesList = remember(clientes) { clientes.values.sortedBy { it.nombre ?: it.telefono3 } }
        NuevoPedidoDialog(
            clientes = clientesList,
            onDismiss = { showNuevo = false },
            onCrearNuevo = { tel, nombre ->
                showNuevo = false
                viewModel.crearPedido(tel, nombre) { id -> onAbrirPedido(id) }
            },
            onSeleccionar = { cliente ->
                showNuevo = false
                viewModel.crearPedidoParaCliente(cliente.id) { id -> onAbrirPedido(id) }
            }
        )
    }
}

@Composable
private fun PedidoCard(pedido: Pedido, clienteEtiqueta: String, onClick: () -> Unit) {
    val deuda = pedido.tieneDeuda
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (deuda) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(clienteEtiqueta, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(money.format(pedido.total), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                EstadoChip(
                    texto = if (pedido.estado == EstadoPedido.ABIERTO) "Abierto" else "Cerrado",
                    color = if (pedido.estado == EstadoPedido.ABIERTO) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.secondary
                )
                EstadoChip(
                    texto = if (pedido.pagado) "Pagado" else "No pagado",
                    color = if (pedido.pagado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                if (pedido.entregado) EstadoChip("Entregado", MaterialTheme.colorScheme.primary)
            }
            if (deuda) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp))
                    Text("Entregado sin pagar", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EstadoChip(texto: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun NuevoPedidoDialog(
    clientes: List<com.toppis.app.data.models.Cliente>,
    onDismiss: () -> Unit,
    onCrearNuevo: (telefono: String, nombre: String?) -> Unit,
    onSeleccionar: (com.toppis.app.data.models.Cliente) -> Unit
) {
    var tel by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    val telValido = tel.length >= 3 && tel.all { it.isDigit() }

    // Coincidencias con clientes existentes mientras se escribe (teléfono o nombre).
    val coincidencias = remember(tel, nombre, clientes) {
        val q = tel.trim()
        val qn = nombre.trim()
        if (q.isBlank() && qn.length < 2) emptyList()
        else clientes.filter { c ->
            (q.isNotBlank() && c.telefono3.contains(q)) ||
                (qn.length >= 2 && (c.nombre?.contains(qn, ignoreCase = true) ?: false))
        }.take(6)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo pedido") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tel, onValueChange = { tel = it.filter { c -> c.isDigit() } },
                    label = { Text("WhatsApp (últimos dígitos o completo)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre (opcional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (coincidencias.isNotEmpty()) {
                    Text("Clientes existentes (tocá para usar):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    coincidencias.forEach { c ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(c) }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.etiqueta, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Sellos: ${c.sellosHamburguesa}/6", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Usar", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCrearNuevo(tel, nombre.ifBlank { null }) },
                enabled = telValido
            ) { Text("Crear nuevo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
