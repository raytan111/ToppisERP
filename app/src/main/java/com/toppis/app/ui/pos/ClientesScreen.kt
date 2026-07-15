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
    puedeBorrar: Boolean = true,
    onNavigateBack: () -> Unit = {}
) {
    val clientes by viewModel.clientes.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()

    var query by remember { mutableStateOf("") }
    var editar by remember { mutableStateOf<ClienteResumen?>(null) }
    var aEliminar by remember { mutableStateOf<ClienteResumen?>(null) }

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
            puedeBorrar = puedeBorrar,
            onDismiss = { editar = null },
            onGuardar = { nombre, telefono3, sellos ->
                if (telefono3 != r.cliente.telefono3) viewModel.actualizarTelefono3(r.cliente.id, telefono3)
                if (nombre != (r.cliente.nombre ?: "")) viewModel.actualizarNombre(r.cliente.id, nombre)
                if (sellos != r.cliente.sellosHamburguesa) viewModel.fijarSellos(r.cliente.id, sellos)
                editar = null
            },
            onEliminar = { editar = null; aEliminar = r }
        )
    }

    aEliminar?.let { r ->
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = r.cliente.etiqueta,
            titulo = "Eliminar cliente",
            onConfirm = { viewModel.eliminar(r.cliente.id); aEliminar = null },
            onDismiss = { aEliminar = null }
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
    puedeBorrar: Boolean,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String, telefono3: String, sellos: Int) -> Unit,
    onEliminar: () -> Unit
) {
    var nombre by remember { mutableStateOf(resumen.cliente.nombre ?: "") }
    var telefonoText by remember { mutableStateOf(resumen.cliente.telefono3) }
    var sellosText by remember { mutableStateOf(resumen.cliente.sellosHamburguesa.toString()) }
    val telefonoValido = telefonoText.isNotBlank()
    val sellosValido = sellosText.toIntOrNull()?.let { it >= 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = telefonoText, onValueChange = { telefonoText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("3 dígitos WhatsApp") },
                    supportingText = { Text("Los 3 últimos dígitos del número del cliente.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
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
                if (puedeBorrar) {
                    TextButton(
                        onClick = onEliminar,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar cliente") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(nombre.trim(), telefonoText.trim(), sellosText.toIntOrNull() ?: 0) },
                enabled = telefonoValido && sellosValido
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
