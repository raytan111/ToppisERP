package com.toppis.app.ui.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.ui.components.ToppisTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    viewModel: ClientesViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val clientes by viewModel.clientes.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()

    var query by remember { mutableStateOf("") }
    var editar by remember { mutableStateOf<ClienteResumen?>(null) }

    LaunchedEffect(Unit) { viewModel.cargar() }

    val filtrados = remember(clientes, query) {
        if (query.isBlank()) clientes
        else clientes.filter {
            it.cliente.telefono3.contains(query.trim()) ||
                (it.cliente.nombre?.contains(query.trim(), ignoreCase = true) ?: false)
        }
    }

    Scaffold(
        topBar = { ToppisTopBar(titulo = "Clientes", onBack = onNavigateBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                cargandoInicial && clientes.isEmpty() -> com.toppis.app.ui.components.SkeletonList()
                clientes.isEmpty() -> com.toppis.app.ui.components.EmptyState(
                    icon = Icons.Filled.People,
                    titulo = "Sin clientes",
                    subtitulo = "Los clientes se crean al registrar pedidos en el POS."
                )
                else -> {
                    com.toppis.app.ui.components.SearchField(
                        value = query, onValueChange = { query = it },
                        placeholder = "Buscar por 3 dígitos o nombre…",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    if (filtrados.isEmpty()) {
                        com.toppis.app.ui.components.EmptyState(
                            icon = Icons.Filled.SearchOff, titulo = "Sin resultados",
                            subtitulo = "No hay clientes que coincidan."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filtrados, key = { it.cliente.id }) { r ->
                                ClienteCard(r) { editar = r }
                            }
                        }
                    }
                }
            }
        }
    }

    editar?.let { r ->
        EditarClienteDialog(
            resumen = r,
            onDismiss = { editar = null },
            onGuardar = { nombre, sellos ->
                if (nombre != (r.cliente.nombre ?: "")) viewModel.actualizarNombre(r.cliente.id, nombre)
                if (sellos != r.cliente.sellosHamburguesa) viewModel.fijarSellos(r.cliente.id, sellos)
                editar = null
            }
        )
    }
}

@Composable
private fun ClienteCard(r: ClienteResumen, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (r.deuda) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.cliente.etiqueta, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (r.deuda) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)) {
                        Text("Con deuda", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
            Text("Pedidos: ${r.pedidos}  ·  Sellos: ${r.cliente.sellosHamburguesa}/6",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (r.cliente.sellosHamburguesa >= 6) {
                Text("🎁 Tiene cupón disponible", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EditarClienteDialog(
    resumen: ClienteResumen,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String, sellos: Int) -> Unit
) {
    var nombre by remember { mutableStateOf(resumen.cliente.nombre ?: "") }
    var sellosText by remember { mutableStateOf(resumen.cliente.sellosHamburguesa.toString()) }
    val sellosValido = sellosText.toIntOrNull()?.let { it >= 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cliente ${resumen.cliente.telefono3}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sellosText, onValueChange = { sellosText = it.filter { c -> c.isDigit() } },
                    label = { Text("Sellos de cuponera") },
                    supportingText = { Text("Cargá acá los cupones que el cliente ya tiene. 6 = una Cheese gratis.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(nombre.trim(), sellosText.toIntOrNull() ?: 0) },
                enabled = sellosValido
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
