package com.toppis.app.ui.mermas

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
import com.toppis.app.data.db.entities.MotivoMerma
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Preparacion
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MermasScreen(
    viewModel: MermaViewModel,
    usuarioId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val mermas by viewModel.mermas.collectAsState()
    val articulos by viewModel.articulos.collectAsState()
    val preparaciones by viewModel.preparaciones.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrear by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Int?>(null) }
    val money = DecimalFormat("$#,##0")
    val num = DecimalFormat("#,##0.##")

    LaunchedEffect(uiState) {
        when (uiState) {
            is MermaUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as MermaUiState.Error).message)
                viewModel.resetState()
            }
            MermaUiState.Success -> { snackbarHostState.showSnackbar("Merma registrada"); viewModel.resetState() }
            else -> {}
        }
    }

    val totalMerma = mermas.sumOf { it.merma.costo }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Mermas", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrear = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar merma")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Costo total de merma (histórico)", style = MaterialTheme.typography.labelMedium)
                    Text(money.format(totalMerma), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
                }
            }

            if (mermas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin mermas registradas.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(mermas) { m ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(m.nombre, style = MaterialTheme.typography.titleSmall)
                                    Text("${num.format(m.merma.cantidadBase)} · ${m.merma.motivo.label}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Text("Costo: ${money.format(m.merma.costo)}",
                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                    if (m.merma.nota.isNotBlank()) {
                                        Text(m.merma.nota, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                IconButton(onClick = { aEliminar = m.merma.id }) {
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
        RegistrarMermaDialog(
            articulos = articulos,
            preparaciones = preparaciones,
            onDismiss = { showCrear = false },
            onConfirm = { tipo, compId, cantidad, motivo, nota ->
                viewModel.registrarMerma(tipo, compId, cantidad, motivo, nota, usuarioId)
                showCrear = false
            }
        )
    }

    aEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("Eliminar merma") },
            text = { Text("¿Eliminar este registro? (no devuelve el stock)") },
            confirmButton = { TextButton(onClick = { viewModel.eliminarMerma(id); aEliminar = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrarMermaDialog(
    articulos: List<Articulo>,
    preparaciones: List<Preparacion>,
    onDismiss: () -> Unit,
    onConfirm: (tipo: TipoComponente, compId: Int, cantidad: Double, motivo: MotivoMerma, nota: String) -> Unit
) {
    var tipo by remember { mutableStateOf(TipoComponente.ARTICULO) }
    var selectedArticulo by remember { mutableStateOf(articulos.firstOrNull()) }
    var selectedPrep by remember { mutableStateOf(preparaciones.firstOrNull()) }
    var motivo by remember { mutableStateOf(MotivoMerma.ESTROPEADO) }
    var cantidadText by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var expCompo by remember { mutableStateOf(false) }
    var expMotivo by remember { mutableStateOf(false) }

    LaunchedEffect(articulos) { if (selectedArticulo == null) selectedArticulo = articulos.firstOrNull() }
    LaunchedEffect(preparaciones) { if (selectedPrep == null) selectedPrep = preparaciones.firstOrNull() }

    val cantidadValida = cantidadText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false
    val unidad = when (tipo) {
        TipoComponente.ARTICULO -> selectedArticulo?.unidadBase ?: "g"
        TipoComponente.PREPARACION -> selectedPrep?.unidadBase ?: "g"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar merma") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tipo == TipoComponente.ARTICULO, onClick = { tipo = TipoComponente.ARTICULO; expCompo = false })
                    Text("Artículo", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = tipo == TipoComponente.PREPARACION, onClick = { tipo = TipoComponente.PREPARACION; expCompo = false })
                    Text("Preparación", style = MaterialTheme.typography.bodySmall)
                }
                // Selector de componente
                val listaVacia = (tipo == TipoComponente.ARTICULO && articulos.isEmpty()) ||
                    (tipo == TipoComponente.PREPARACION && preparaciones.isEmpty())
                if (listaVacia) {
                    Text("No hay ${if (tipo == TipoComponente.ARTICULO) "artículos" else "preparaciones"}.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    ExposedDropdownMenuBox(expanded = expCompo, onExpandedChange = { expCompo = !expCompo }) {
                        val valor = when (tipo) {
                            TipoComponente.ARTICULO -> selectedArticulo?.nombre ?: ""
                            TipoComponente.PREPARACION -> selectedPrep?.nombre ?: ""
                        }
                        OutlinedTextField(
                            value = valor, onValueChange = {}, readOnly = true,
                            label = { Text(if (tipo == TipoComponente.ARTICULO) "Artículo" else "Preparación") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCompo) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expCompo, onDismissRequest = { expCompo = false }) {
                            if (tipo == TipoComponente.ARTICULO) {
                                articulos.forEach { a -> DropdownMenuItem(text = { Text("${a.nombre} (${a.unidadBase})") }, onClick = { selectedArticulo = a; expCompo = false }) }
                            } else {
                                preparaciones.forEach { p -> DropdownMenuItem(text = { Text("${p.nombre} (${p.unidadBase})") }, onClick = { selectedPrep = p; expCompo = false }) }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = cantidadText, onValueChange = { cantidadText = it },
                    label = { Text("Cantidad ($unidad)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                // Motivo
                ExposedDropdownMenuBox(expanded = expMotivo, onExpandedChange = { expMotivo = !expMotivo }) {
                    OutlinedTextField(
                        value = motivo.label, onValueChange = {}, readOnly = true,
                        label = { Text("Motivo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expMotivo) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expMotivo, onDismissRequest = { expMotivo = false }) {
                        MotivoMerma.entries.forEach { mo -> DropdownMenuItem(text = { Text(mo.label) }, onClick = { motivo = mo; expMotivo = false }) }
                    }
                }
                OutlinedTextField(
                    value = nota, onValueChange = { nota = it },
                    label = { Text("Nota (opcional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val compId = when (tipo) {
                        TipoComponente.ARTICULO -> selectedArticulo?.id ?: return@TextButton
                        TipoComponente.PREPARACION -> selectedPrep?.id ?: return@TextButton
                    }
                    onConfirm(tipo, compId, cantidadText.replace(",", ".").toDouble(), motivo, nota)
                },
                enabled = cantidadValida && when (tipo) {
                    TipoComponente.ARTICULO -> selectedArticulo != null
                    TipoComponente.PREPARACION -> selectedPrep != null
                }
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
