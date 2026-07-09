package com.toppis.app.ui.costos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.db.entities.Periodicidad
import com.toppis.app.data.models.CostoFijo
import com.toppis.app.domain.costos.CostosCalculos
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val money = DecimalFormat("$#,##0")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostosFijosScreen(
    viewModel: CostoFijoViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val costos by viewModel.costos.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrear by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<CostoFijo?>(null) }
    var aEliminar by remember { mutableStateOf<CostoFijo?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.refrescar() }
    LaunchedEffect(uiState) {
        if (uiState is CostoFijoUiState.Error) { errorMsg = (uiState as CostoFijoUiState.Error).message; viewModel.resetState() }
    }
    errorMsg?.let { msg -> com.toppis.app.ui.components.ToppisErrorDialog(mensaje = msg, onDismiss = { errorMsg = null }) }

    val totalSemanal = CostosCalculos.totalFijosSemanales(costos)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Costos fijos", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrear = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo costo fijo")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Fijos por semana (prorrateados)", style = MaterialTheme.typography.labelMedium)
                    Text(money.format(totalSemanal), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }

            when {
                cargandoInicial && costos.isEmpty() -> com.toppis.app.ui.components.SkeletonList()
                costos.isEmpty() -> com.toppis.app.ui.components.EmptyState(
                    icon = Icons.Filled.Receipt,
                    titulo = "Sin costos fijos",
                    subtitulo = "Agregá arriendo, luz, gas, internet… con el botón +."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(costos, key = { it.id }) { c ->
                        val semanal = CostosCalculos.prorrateoSemanal(c.monto, c.periodicidad)
                        Card(onClick = { enEdicion = c }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.nombre, style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (c.activo) FontWeight.SemiBold else FontWeight.Normal)
                                    Text("${c.categoria.label} · ${c.periodicidad.label} ${money.format(c.monto)}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Text("≈ ${money.format(semanal)} / semana" + if (!c.activo) " · inactivo" else "",
                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { aEliminar = c }) {
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
        CostoFijoDialog(
            onDismiss = { showCrear = false },
            onConfirm = { nombre, cat, monto, per, _ -> viewModel.crear(nombre, cat, monto, per); showCrear = false }
        )
    }
    enEdicion?.let { c ->
        CostoFijoDialog(
            inicial = c,
            onDismiss = { enEdicion = null },
            onConfirm = { nombre, cat, monto, per, activo ->
                viewModel.actualizar(c.copy(nombre = nombre, categoria = cat, monto = monto, periodicidad = per, activo = activo))
                enEdicion = null
            }
        )
    }
    aEliminar?.let { c ->
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = c.nombre,
            titulo = "Eliminar costo fijo",
            onConfirm = { viewModel.eliminar(c.id); aEliminar = null },
            onDismiss = { aEliminar = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CostoFijoDialog(
    inicial: CostoFijo? = null,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, categoria: CategoriaGasto, monto: Double, periodicidad: Periodicidad, activo: Boolean) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var categoria by remember { mutableStateOf(inicial?.categoria ?: CategoriaGasto.ARRIENDO) }
    var periodicidad by remember { mutableStateOf(inicial?.periodicidad ?: Periodicidad.MENSUAL) }
    var montoTxt by remember { mutableStateOf(inicial?.monto?.let { if (it == 0.0) "" else it.toLong().toString() } ?: "") }
    var activo by remember { mutableStateOf(inicial?.activo ?: true) }
    var expCat by remember { mutableStateOf(false) }
    var expPer by remember { mutableStateOf(false) }

    val monto = montoTxt.replace(",", ".").toDoubleOrNull()
    val valido = nombre.isNotBlank() && monto != null && monto >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial != null) "Editar costo fijo" else "Nuevo costo fijo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Luz, Arriendo)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expCat, onExpandedChange = { expCat = !expCat }) {
                    OutlinedTextField(value = categoria.label, onValueChange = {}, readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCat) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = expCat, onDismissRequest = { expCat = false }) {
                        CategoriaGasto.entries.forEach { c ->
                            DropdownMenuItem(text = { Text(c.label) }, onClick = { categoria = c; expCat = false })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = expPer, onExpandedChange = { expPer = !expPer }) {
                    OutlinedTextField(value = periodicidad.label, onValueChange = {}, readOnly = true,
                        label = { Text("Periodicidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expPer) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = expPer, onDismissRequest = { expPer = false }) {
                        Periodicidad.entries.forEach { p ->
                            DropdownMenuItem(text = { Text(p.label) }, onClick = { periodicidad = p; expPer = false })
                        }
                    }
                }
                OutlinedTextField(value = montoTxt, onValueChange = { montoTxt = it },
                    label = { Text("Monto (CLP, con IVA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                if (inicial != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Activo", modifier = Modifier.weight(1f))
                        Switch(checked = activo, onCheckedChange = { activo = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nombre, categoria, monto ?: return@TextButton, periodicidad, activo) }, enabled = valido) {
                Text(if (inicial != null) "Guardar" else "Crear")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
