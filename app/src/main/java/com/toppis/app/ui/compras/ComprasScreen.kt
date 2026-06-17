package com.toppis.app.ui.compras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Compra
import com.toppis.app.data.models.Proveedor
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.repository.LineaCompra
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val money = DecimalFormat("$#,##0")
private val num = DecimalFormat("#,##0.##")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprasScreen(
    viewModel: CompraViewModel,
    usuarioId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val compras by viewModel.compras.collectAsState()
    val articulos by viewModel.articulos.collectAsState()
    val proveedores by viewModel.proveedores.collectAsState()
    val sobres by viewModel.sobres.collectAsState()
    val porVencer by viewModel.porVencer.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var modoNuevo by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is CompraUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as CompraUiState.Error).message)
                viewModel.resetState()
            }
            CompraUiState.Success -> {
                snackbarHostState.showSnackbar("Compra registrada")
                modoNuevo = false
                viewModel.resetState()
            }
            else -> {}
        }
    }

    if (modoNuevo) {
        NuevaCompraView(
            articulos = articulos,
            proveedores = proveedores,
            sobres = sobres,
            snackbarHostState = snackbarHostState,
            onCancelar = { modoNuevo = false },
            onGuardar = { provId, tieneIva, nota, lineas, sobreId ->
                viewModel.registrarCompra(provId, tieneIva, nota, lineas, sobreId, usuarioId)
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Compras", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.refrescar(); modoNuevo = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva compra")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (porVencer.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Lotes por vencer", style = MaterialTheme.typography.titleSmall)
                            porVencer.take(5).forEach { lv ->
                                Text("${lv.nombreArticulo} — vence ${lv.detalle.vencimiento}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            if (compras.isEmpty()) {
                item { Text("Sin compras registradas. Usá + para registrar una.", color = MaterialTheme.colorScheme.outline) }
            } else {
                items(compras) { c -> CompraCard(c) }
            }
        }
    }
}

@Composable
private fun CompraCard(c: Compra) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Compra #${c.id}", style = MaterialTheme.typography.titleMedium)
                Text(money.format(c.total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(c.fecha?.take(16)?.replace("T", " ") ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            if (c.tieneIva) Text("Con factura (IVA)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            if (c.nota.isNotBlank()) Text(c.nota, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaCompraView(
    articulos: List<Articulo>,
    proveedores: List<Proveedor>,
    sobres: List<Sobre>,
    snackbarHostState: SnackbarHostState,
    onCancelar: () -> Unit,
    onGuardar: (proveedorId: Int?, tieneIva: Boolean, nota: String, lineas: List<LineaCompra>, sobreId: Int?) -> Unit
) {
    var proveedor by remember { mutableStateOf<Proveedor?>(null) }
    var expProv by remember { mutableStateOf(false) }
    var tieneIva by remember { mutableStateOf(false) }
    var nota by remember { mutableStateOf("") }
    var pagarSobre by remember { mutableStateOf(false) }
    var sobre by remember { mutableStateOf<Sobre?>(null) }
    var expSobre by remember { mutableStateOf(false) }
    val lineas = remember { mutableStateListOf<LineaCompra>() }
    var showAgregar by remember { mutableStateOf(false) }

    val total = lineas.sumOf { it.subtotal }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Nueva compra", onBack = onCancelar) },
        floatingActionButton = {
            if (articulos.isNotEmpty()) {
                FloatingActionButton(onClick = { showAgregar = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar artículo")
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total compra:", style = MaterialTheme.typography.titleMedium)
                        Text(money.format(total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = {
                            onGuardar(proveedor?.id, tieneIva, nota, lineas.toList(), if (pagarSobre) sobre?.id else null)
                        },
                        enabled = lineas.isNotEmpty() && (!pagarSobre || sobre != null),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("REGISTRAR COMPRA") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Proveedor
            item {
                ExposedDropdownMenuBox(expanded = expProv, onExpandedChange = { expProv = !expProv }) {
                    OutlinedTextField(
                        value = proveedor?.nombre ?: "Sin proveedor", onValueChange = {}, readOnly = true,
                        label = { Text("Proveedor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expProv) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expProv, onDismissRequest = { expProv = false }) {
                        DropdownMenuItem(text = { Text("Sin proveedor") }, onClick = { proveedor = null; expProv = false })
                        proveedores.forEach { p ->
                            DropdownMenuItem(text = { Text(p.nombre) }, onClick = { proveedor = p; expProv = false })
                        }
                    }
                }
            }
            item {
                OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Nota (opcional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = tieneIva, onCheckedChange = { tieneIva = it })
                    Text("Con factura (IVA 19% crédito)")
                }
            }
            // Pago desde sobre
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pagarSobre, onCheckedChange = { pagarSobre = it })
                    Text("Pagar desde un sobre (descuenta dinero)")
                }
            }
            if (pagarSobre) {
                item {
                    ExposedDropdownMenuBox(expanded = expSobre, onExpandedChange = { expSobre = !expSobre }) {
                        OutlinedTextField(
                            value = sobre?.nombre ?: "", onValueChange = {}, readOnly = true,
                            label = { Text("Sobre") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expSobre) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expSobre, onDismissRequest = { expSobre = false }) {
                            sobres.forEach { s ->
                                DropdownMenuItem(text = { Text("${s.nombre} (${money.format(s.saldo)})") }, onClick = { sobre = s; expSobre = false })
                            }
                        }
                    }
                }
            }
            item { HorizontalDivider() }
            item { Text("Artículos (${lineas.size})", style = MaterialTheme.typography.labelLarge) }
            if (lineas.isEmpty()) {
                item { Text("Tocá + para agregar artículos a la compra.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
            } else {
                itemsIndexed(lineas) { idx, l ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(l.articulo.nombre, style = MaterialTheme.typography.bodyMedium)
                                Text("${num.format(l.cantidadCompra)} ${l.articulo.unidadCompra} × ${money.format(l.costoUnitarioCompra)} = ${money.format(l.subtotal)}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                if (!l.vencimiento.isNullOrBlank()) Text("Vence: ${l.vencimiento}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                            IconButton(onClick = { lineas.removeAt(idx) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAgregar) {
        AgregarLineaCompraDialog(
            articulos = articulos,
            onDismiss = { showAgregar = false },
            onAgregar = { linea -> lineas.add(linea); showAgregar = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgregarLineaCompraDialog(
    articulos: List<Articulo>,
    onDismiss: () -> Unit,
    onAgregar: (LineaCompra) -> Unit
) {
    var articulo by remember { mutableStateOf(articulos.firstOrNull()) }
    var exp by remember { mutableStateOf(false) }
    var cantidadText by remember { mutableStateOf("") }
    var costoText by remember { mutableStateOf("") }
    var vencimiento by remember { mutableStateOf("") }

    val cantidad = cantidadText.replace(",", ".").toDoubleOrNull()
    val costo = costoText.replace(",", ".").toDoubleOrNull()
    val valido = articulo != null && cantidad != null && cantidad > 0 && costo != null && costo >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar artículo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                    OutlinedTextField(
                        value = articulo?.let { "${it.nombre} (${it.unidadCompra})" } ?: "", onValueChange = {}, readOnly = true,
                        label = { Text("Artículo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        articulos.forEach { a ->
                            DropdownMenuItem(text = { Text("${a.nombre} (${a.unidadCompra})") }, onClick = { articulo = a; exp = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = cantidadText, onValueChange = { cantidadText = it },
                    label = { Text("Cantidad (${articulo?.unidadCompra ?: ""})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costoText, onValueChange = { costoText = it },
                    label = { Text("Costo por ${articulo?.unidadCompra ?: "unidad"} (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vencimiento, onValueChange = { vencimiento = it },
                    label = { Text("Vencimiento (yyyy-MM-dd, opcional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAgregar(LineaCompra(articulo!!, cantidad!!, costo!!, vencimiento.ifBlank { null }))
                },
                enabled = valido
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
