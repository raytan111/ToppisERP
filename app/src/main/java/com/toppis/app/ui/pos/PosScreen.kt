package com.toppis.app.ui.pos

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.db.entities.MetodoPago
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.db.entities.ZonaEnvio
import com.toppis.app.ui.sobres.SobreViewModel
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    posViewModel: PosViewModel,
    sobreViewModel: SobreViewModel,
    usuarioId: String? = null,
    modifier: Modifier = Modifier
) {
    val itemsMenu by posViewModel.itemsMenu.collectAsState()
    val salsasDisponibles by posViewModel.salsasDisponibles.collectAsState()
    val promociones by posViewModel.promociones.collectAsState()
    val articulosPos by posViewModel.articulosPos.collectAsState()
    val carrito by posViewModel.carrito.collectAsState()
    val total by posViewModel.totalCarrito.collectAsState()
    val uiState by posViewModel.uiState.collectAsState()
    val sobres by sobreViewModel.sobres.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var itemSeleccionado by remember { mutableStateOf<ItemMenu?>(null) }
    var modificadoresItem by remember { mutableStateOf<List<com.toppis.app.data.repository.ModificadorConCosto>>(emptyList()) }
    var showPromosDialog by remember { mutableStateOf(false) }
    var lineaEditandoIndex by remember { mutableStateOf<Int?>(null) }
    var recetaLinea by remember { mutableStateOf<List<com.toppis.app.data.repository.ComponenteReceta>>(emptyList()) }
    var showPostVentaDialog by remember { mutableStateOf<PosUiState.VentaExitosa?>(null) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PosUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as PosUiState.Error).message)
                posViewModel.resetState()
            }
            is PosUiState.VentaExitosa -> {
                showPostVentaDialog = uiState as PosUiState.VentaExitosa
            }
            else -> {}
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Izquierda: Items del Menú
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Text("Menú", style = MaterialTheme.typography.titleMedium)
                if (promociones.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(onClick = { showPromosDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("🏷️ Promociones (${promociones.size})")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (itemsMenu.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Sin items en el menú.\nConfiguralos desde el Dashboard.",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(itemsMenu) { item ->
                            ItemMenuCard(item) {
                                modificadoresItem = emptyList()
                                posViewModel.cargarModificadores(item.id) { modificadoresItem = it }
                                itemSeleccionado = item
                            }
                        }
                    }
                }
            }

            VerticalDivider(modifier = Modifier.fillMaxHeight())

            // Derecha: Carrito
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Text("Carrito", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (carrito.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Carrito vacío",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(carrito.size) { index ->
                            val item = carrito[index]
                            ItemCarritoMenuCard(
                                item = item,
                                onCantidadChange = { qty -> posViewModel.cambiarCantidad(index, qty) },
                                onDelete = { posViewModel.quitarDelCarrito(index) },
                                onEdit = {
                                    recetaLinea = emptyList()
                                    posViewModel.cargarReceta(item.itemMenu.id) { recetaLinea = it }
                                    lineaEditandoIndex = index
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                val formatter = DecimalFormat("$#,##0")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatter.format(total),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showCheckoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = carrito.isNotEmpty()
                ) {
                    Text("COBRAR")
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { posViewModel.limpiarCarrito() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("LIMPIAR")
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ── Diálogo selector de salsas + modificadores ───────────────────────────────
    if (itemSeleccionado != null) {
        ItemOpcionesDialog(
            itemMenu = itemSeleccionado!!,
            salsas = salsasDisponibles,
            modificadores = modificadoresItem,
            onDismiss = { itemSeleccionado = null },
            onConfirm = { salsasSeleccionadas, modsSeleccionados ->
                posViewModel.agregarAlCarrito(itemSeleccionado!!, salsasSeleccionadas, modsSeleccionados)
                itemSeleccionado = null
            }
        )
    }

    // ── Diálogo editar receta de la línea (quitar / cambiar) ─────────────────────
    val idxEdit = lineaEditandoIndex
    if (idxEdit != null && idxEdit in carrito.indices) {
        EditarRecetaLineaDialog(
            item = carrito[idxEdit],
            receta = recetaLinea,
            articulos = articulosPos,
            onDismiss = { lineaEditandoIndex = null },
            onConfirm = { ajustes ->
                posViewModel.aplicarAjustes(idxEdit, ajustes)
                lineaEditandoIndex = null
            }
        )
    }

    // ── Diálogo selector de promociones ──────────────────────────────────────────
    if (showPromosDialog) {
        AlertDialog(
            onDismissRequest = { showPromosDialog = false },
            title = { Text("Promociones") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(promociones) { promo ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                posViewModel.agregarPromoAlCarrito(promo)
                                showPromosDialog = false
                            }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(promo.nombre, style = MaterialTheme.typography.titleSmall)
                                val detalle = when (promo.tipo) {
                                    com.toppis.app.data.db.entities.TipoPromocion.COMBO ->
                                        "Combo ${DecimalFormat("$#,##0").format(promo.precio)}"
                                    com.toppis.app.data.db.entities.TipoPromocion.DESCUENTO_PORCENTAJE ->
                                        "Descuento ${DecimalFormat("0.#").format(promo.descuentoPct)}%"
                                }
                                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPromosDialog = false }) { Text("Cerrar") } }
        )
    }

    // ── Diálogo de checkout ──────────────────────────────────────────────────────
    if (showCheckoutDialog) {
        CheckoutDialog(
            sobres = sobres,
            total = total,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { metodoPago, sobreId, zonaEnvio ->
                posViewModel.procesarVenta(
                    metodoPago = metodoPago,
                    sobreId = sobreId,
                    usuarioId = usuarioId,
                    zonaEnvio = zonaEnvio
                )
                showCheckoutDialog = false
            }
        )
    }

    // ── Diálogo post-venta (comanda + WhatsApp) ──────────────────────────────────
    if (showPostVentaDialog != null) {
        PostVentaDialog(
            ventaExitosa = showPostVentaDialog!!,
            posViewModel = posViewModel,
            usuarioId = usuarioId,
            onDismiss = {
                showPostVentaDialog = null
                posViewModel.limpiarComprobante()
                posViewModel.resetState()
            }
        )
    }
}

// ── Card de ItemMenu ─────────────────────────────────────────────────────────────

@Composable
private fun ItemMenuCard(item: ItemMenu, onAdd: () -> Unit) {
    val formatter = DecimalFormat("$#,##0")
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAdd
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatter.format(item.precio),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            if (item.descripcion.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    item.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2
                )
            }
        }
    }
}

// ── Card de carrito menú (versión compacta) ──────────────────────────────────────

@Composable
private fun ItemCarritoMenuCard(
    item: ItemCarritoMenu,
    onCantidadChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val formatter = DecimalFormat("$#,##0")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.itemMenu.nombre,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Text(
                        formatter.format(item.subtotal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.promoNombre != null) {
                        Text(
                            "🏷️ ${item.promoNombre}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                    if (item.modificadoresTexto.isNotBlank()) {
                        Text(
                            item.modificadoresTexto,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 2
                        )
                    }
                    if (item.salsas.isNotEmpty()) {
                        Text(
                            item.salsasTexto,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                }

                if (item.promocionId == null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            "Editar receta",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { onCantidadChange(item.cantidad - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("-", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "${item.cantidad}",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                FilledTonalIconButton(
                    onClick = { onCantidadChange(item.cantidad + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

// ── Fila de carrito menú (legacy, mantener por compatibilidad) ────────────────────

@Composable
private fun ItemCarritoMenuRow(
    item: ItemCarritoMenu,
    onCantidadChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val formatter = DecimalFormat("$#,##0")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.itemMenu.nombre, style = MaterialTheme.typography.bodyLarge)
            Text(
                formatter.format(item.subtotal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (item.salsas.isNotEmpty()) {
                Text(
                    "Salsas: ${item.salsasTexto}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onCantidadChange(item.cantidad - 1) }) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            Text("${item.cantidad}", modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onCantidadChange(item.cantidad + 1) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Selector de salsas ───────────────────────────────────────────────────────────

@Composable
private fun ItemOpcionesDialog(
    itemMenu: ItemMenu,
    salsas: List<com.toppis.app.data.repository.OpcionPos>,
    modificadores: List<com.toppis.app.data.repository.ModificadorConCosto>,
    onDismiss: () -> Unit,
    onConfirm: (salsas: List<com.toppis.app.data.repository.OpcionPos>, modificadores: List<com.toppis.app.data.repository.ModificadorConCosto>) -> Unit
) {
    val maxSalsas = 5
    val salsasSeleccionadas = remember { mutableStateListOf<com.toppis.app.data.repository.OpcionPos>() }
    val modsSeleccionados = remember { mutableStateListOf<com.toppis.app.data.repository.ModificadorConCosto>() }
    val money = java.text.DecimalFormat("$#,##0")

    val precioFinal = itemMenu.precio + modsSeleccionados.sumOf { it.deltaPrecio }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(itemMenu.nombre) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn {
                if (modificadores.isNotEmpty()) {
                    item {
                        Text("Modificadores", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(modificadores) { mod ->
                        val isSelected = mod in modsSeleccionados
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (isSelected) modsSeleccionados.remove(mod) else modsSeleccionados.add(mod)
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(mod.modificador.nombre)
                                val signo = if (mod.deltaPrecio >= 0) "+" else "-"
                                Text(
                                    "${mod.modificador.tipo.label} · $signo${money.format(kotlin.math.abs(mod.deltaPrecio))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp)) }
                }

                if (salsas.isNotEmpty()) {
                    item {
                        Text("Salsas / agregados (${salsasSeleccionadas.size}/$maxSalsas)", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(salsas) { salsa ->
                        val isSelected = salsa in salsasSeleccionadas
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (isSelected) salsasSeleccionadas.remove(salsa)
                                    else if (salsasSeleccionadas.size < maxSalsas) salsasSeleccionadas.add(salsa)
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(salsa.nombre)
                                if (salsa.cantidad > 0) {
                                    Text(
                                        "${java.text.DecimalFormat("#,##0.##").format(salsa.cantidad)} ${salsa.unidad}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }

                if (salsas.isEmpty() && modificadores.isEmpty()) {
                    item { Text("Sin opciones. Se agrega directo.", color = MaterialTheme.colorScheme.outline) }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Precio: ${money.format(precioFinal)}", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(salsasSeleccionadas.toList(), modsSeleccionados.toList()) }) {
                Text("Agregar al carrito")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Checkout Dialog ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutDialog(
    sobres: List<Sobre>,
    total: Double,
    onDismiss: () -> Unit,
    onConfirm: (MetodoPago, Int, ZonaEnvio) -> Unit
) {
    var selectedMetodo by remember { mutableStateOf(MetodoPago.EFECTIVO) }
    var selectedZona by remember { mutableStateOf(ZonaEnvio.SIN_ENVIO) }
    var expanded by remember { mutableStateOf(false) }

    val sobreAutomatico = when (selectedMetodo) {
        MetodoPago.DEBITO -> sobres.firstOrNull { it.nombre.lowercase().contains("tarjeta") || it.nombre.lowercase().contains("débito") }
        MetodoPago.EFECTIVO -> sobres.firstOrNull { it.nombre.lowercase().contains("efectivo") }
    }
    var selectedSobre by remember { mutableStateOf(sobreAutomatico ?: sobres.firstOrNull()) }

    LaunchedEffect(selectedMetodo) {
        selectedSobre = sobreAutomatico ?: sobres.firstOrNull()
    }

    val totalConEnvio = total + selectedZona.precio
    val formatter = DecimalFormat("$#,##0")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cobrar Venta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Total
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total a cobrar:", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatter.format(totalConEnvio),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Método de Pago
                Text("Método de Pago:", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedMetodo == MetodoPago.EFECTIVO,
                        onClick = { selectedMetodo = MetodoPago.EFECTIVO }
                    )
                    Text("EFECTIVO", modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = selectedMetodo == MetodoPago.DEBITO,
                        onClick = { selectedMetodo = MetodoPago.DEBITO }
                    )
                    Text("DÉBITO")
                }

                // Zona de envío
                Text("Envío:", style = MaterialTheme.typography.labelMedium)
                ZonaEnvio.entries.forEach { zona ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedZona == zona,
                            onClick = { selectedZona = zona }
                        )
                        Text(
                            if (zona == ZonaEnvio.SIN_ENVIO) zona.label
                            else "${zona.label} — ${formatter.format(zona.precio)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Selector de sobre
                if (sobres.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "No hay sobres configurados. Crea uno en Sobres antes de cobrar.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSobre?.nombre ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sobre Destino") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            sobres.forEach { sobre ->
                                DropdownMenuItem(
                                    text = { Text(sobre.nombre) },
                                    onClick = {
                                        selectedSobre = sobre
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSobre != null) {
                        onConfirm(selectedMetodo, selectedSobre!!.id, selectedZona)
                    }
                },
                enabled = selectedSobre != null && sobres.isNotEmpty()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Post-venta Dialog (Comanda + WhatsApp) ───────────────────────────────────────

@Composable
private fun PostVentaDialog(
    ventaExitosa: PosUiState.VentaExitosa,
    posViewModel: PosViewModel,
    usuarioId: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val comprobante by posViewModel.comprobante.collectAsState()
    val comprobanteError by posViewModel.comprobanteError.collectAsState()
    val moneyFmt = DecimalFormat("$#,##0")

    val comandaTexto = ventaExitosa.comandaTexto

    // Texto a compartir: comanda + comprobante (si fue emitido)
    val textoCompartir = buildString {
        append(ventaExitosa.whatsappTexto)
        comprobante?.let { c ->
            append("\n\n--- Comprobante interno #${c.folio} ---")
            append("\nNeto: ${moneyFmt.format(c.neto)}")
            append("\nIVA (19%): ${moneyFmt.format(c.iva)}")
            append("\nTotal: ${moneyFmt.format(c.total)}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Venta #${ventaExitosa.ventaId} completada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = comandaTexto,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val c = comprobante
                if (c == null) {
                    OutlinedButton(
                        onClick = { posViewModel.emitirComprobante(ventaExitosa.ventaId, usuarioId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Emitir comprobante")
                    }
                    comprobanteError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Comprobante interno #${c.folio}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text("Neto: ${moneyFmt.format(c.neto)}", style = MaterialTheme.typography.bodySmall)
                            Text("IVA (19%): ${moneyFmt.format(c.iva)}", style = MaterialTheme.typography.bodySmall)
                            Text("Total: ${moneyFmt.format(c.total)}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Documento de control interno · no tributario",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, textoCompartir)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Compartir pedido")
                    )
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("WhatsApp")
                }
                comprobante?.let { c ->
                    OutlinedButton(onClick = {
                        val uri = com.toppis.app.util.ComprobantePdfUtil.generarPdf(
                            context, c, ventaExitosa.lineas
                        )
                        com.toppis.app.util.ComprobantePdfUtil.compartirPdf(context, uri)
                    }) {
                        Text("PDF")
                    }
                }
                Button(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    )
}

// ── Editar receta de la línea (quitar / cambiar ingredientes) ─────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarRecetaLineaDialog(
    item: ItemCarritoMenu,
    receta: List<com.toppis.app.data.repository.ComponenteReceta>,
    articulos: List<com.toppis.app.data.models.Articulo>,
    onDismiss: () -> Unit,
    onConfirm: (List<com.toppis.app.ui.pos.AjusteReceta>) -> Unit
) {
    // Estado por componente: null = sin cambio, QUITAR, o CAMBIAR(reemplazo)
    val ajustes = remember { mutableStateMapOf<Int, com.toppis.app.ui.pos.AjusteReceta>() }

    // Prefill con los ajustes actuales de la línea
    LaunchedEffect(receta) {
        ajustes.clear()
        item.ajustes.forEach { aj ->
            ajustes[aj.original.recetaMenu.id] = aj
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar: ${item.itemMenu.nombre}") },
        text = {
            if (receta.isEmpty()) {
                Text("Este plato no tiene receta cargada.", color = MaterialTheme.colorScheme.outline)
            } else {
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("Tocá un ingrediente para quitarlo o cambiarlo:", style = MaterialTheme.typography.labelMedium) }
                    items(receta) { comp ->
                        ComponenteEditableRow(
                            comp = comp,
                            articulos = articulos,
                            ajusteActual = ajustes[comp.recetaMenu.id],
                            onQuitarToggle = {
                                val key = comp.recetaMenu.id
                                if (ajustes[key]?.tipo == com.toppis.app.ui.pos.TipoAjuste.QUITAR) {
                                    ajustes.remove(key)
                                } else {
                                    ajustes[key] = com.toppis.app.ui.pos.AjusteReceta(
                                        tipo = com.toppis.app.ui.pos.TipoAjuste.QUITAR,
                                        original = comp
                                    )
                                }
                            },
                            onCambiar = { articuloNuevo ->
                                val key = comp.recetaMenu.id
                                ajustes[key] = com.toppis.app.ui.pos.AjusteReceta(
                                    tipo = com.toppis.app.ui.pos.TipoAjuste.CAMBIAR,
                                    original = comp,
                                    reemplazo = articuloNuevo,
                                    cantidadReemplazo = comp.recetaMenu.cantidadBase
                                )
                            },
                            onLimpiar = { ajustes.remove(comp.recetaMenu.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(ajustes.values.toList()) }) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComponenteEditableRow(
    comp: com.toppis.app.data.repository.ComponenteReceta,
    articulos: List<com.toppis.app.data.models.Articulo>,
    ajusteActual: com.toppis.app.ui.pos.AjusteReceta?,
    onQuitarToggle: () -> Unit,
    onCambiar: (com.toppis.app.data.models.Articulo) -> Unit,
    onLimpiar: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val num = DecimalFormat("#,##0.##")

    val estado = when (ajusteActual?.tipo) {
        com.toppis.app.ui.pos.TipoAjuste.QUITAR -> "Sin ${comp.nombre}"
        com.toppis.app.ui.pos.TipoAjuste.CAMBIAR -> "→ ${ajusteActual.reemplazo?.nombre}"
        null -> null
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${comp.nombre} · ${num.format(comp.recetaMenu.cantidadBase)} ${comp.unidad}",
                        style = MaterialTheme.typography.bodyMedium)
                    if (estado != null) {
                        Text(estado, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                if (ajusteActual != null) {
                    TextButton(onClick = onLimpiar) { Text("Deshacer") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quitado = ajusteActual?.tipo == com.toppis.app.ui.pos.TipoAjuste.QUITAR
                OutlinedButton(onClick = onQuitarToggle) {
                    Text(if (quitado) "Quitado ✓" else "Quitar")
                }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text("Cambiar por…") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        articulos.forEach { a ->
                            DropdownMenuItem(
                                text = { Text("${a.nombre} (${a.unidadBase})") },
                                onClick = { onCambiar(a); expanded = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
