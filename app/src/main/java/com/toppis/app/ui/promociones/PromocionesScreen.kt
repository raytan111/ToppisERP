package com.toppis.app.ui.promociones

import androidx.compose.foundation.clickable
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
import com.toppis.app.data.db.entities.TipoPromocion
import com.toppis.app.data.models.AnalisisPromocion
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Promocion
import com.toppis.app.data.repository.PromocionItemDetalle
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val money = DecimalFormat("$#,##0")
private val pct = DecimalFormat("0.#")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromocionesScreen(
    viewModel: PromocionViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val promociones by viewModel.promociones.collectAsState()
    val itemsMenu by viewModel.itemsMenu.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrearDialog by remember { mutableStateOf(false) }
    var promoAEliminar by remember { mutableStateOf<Promocion?>(null) }

    var promoSeleccionada by remember { mutableStateOf<Promocion?>(null) }
    var detalleItems by remember { mutableStateOf<List<PromocionItemDetalle>>(emptyList()) }
    var analisis by remember { mutableStateOf<AnalisisPromocion?>(null) }

    fun recargarDetalle(promo: Promocion) {
        viewModel.cargarDetalle(promo) { items, an ->
            detalleItems = items
            analisis = an
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PromocionUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as PromocionUiState.Error).message)
                viewModel.resetState()
            }
            PromocionUiState.Success -> viewModel.resetState()
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Promociones", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrearDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear promoción")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (promociones.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin promociones.\nUsá el botón + para agregar.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(promociones) { promo ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                promoSeleccionada = promo
                                detalleItems = emptyList()
                                analisis = null
                                recargarDetalle(promo)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(promo.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        promo.tipo.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    val detalle = when (promo.tipo) {
                                        TipoPromocion.COMBO -> "Precio ${money.format(promo.precio)}"
                                        TipoPromocion.DESCUENTO_PORCENTAJE -> "Descuento ${pct.format(promo.descuentoPct)}%"
                                    }
                                    Text(
                                        detalle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { promoAEliminar = promo }) {
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
        }
    }

    if (showCrearDialog) {
        CrearPromocionDialog(
            onDismiss = { showCrearDialog = false },
            onConfirm = { nombre, tipo, precio, descuento ->
                viewModel.crearPromocion(nombre, tipo, precio, descuento)
                showCrearDialog = false
            }
        )
    }

    promoAEliminar?.let { promo ->
        AlertDialog(
            onDismissRequest = { promoAEliminar = null },
            title = { Text("Eliminar promoción") },
            text = { Text("¿Seguro que querés eliminar \"${promo.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarPromocion(promo.id)
                    promoAEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { promoAEliminar = null }) { Text("Cancelar") } }
        )
    }

    promoSeleccionada?.let { promo ->
        ArmarPromoDialog(
            promocion = promo,
            detalleItems = detalleItems,
            analisis = analisis,
            itemsMenu = itemsMenu,
            onDismiss = {
                promoSeleccionada = null
                detalleItems = emptyList()
                analisis = null
            },
            onAgregarItem = { itemMenuId, cantidad ->
                viewModel.agregarItem(promo.id, itemMenuId, cantidad) { recargarDetalle(promo) }
            },
            onEliminarItem = { detalle ->
                viewModel.eliminarItem(detalle.promocionItem.id) { recargarDetalle(promo) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearPromocionDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, tipo: TipoPromocion, precio: Double, descuentoPct: Double) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf(TipoPromocion.COMBO) }
    var expandedTipo by remember { mutableStateOf(false) }
    var precioText by remember { mutableStateOf("") }
    var descuentoText by remember { mutableStateOf("") }

    val precioValido = precioText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false
    val descuentoValido = descuentoText.replace(",", ".").toDoubleOrNull()?.let { it > 0 && it <= 100 } ?: false
    val datosValidos = nombre.isNotBlank() && when (tipoSeleccionado) {
        TipoPromocion.COMBO -> precioValido
        TipoPromocion.DESCUENTO_PORCENTAJE -> descuentoValido
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Promoción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Combo del día)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tipoSeleccionado.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        TipoPromocion.entries.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo.label) },
                                onClick = {
                                    tipoSeleccionado = tipo
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }
                when (tipoSeleccionado) {
                    TipoPromocion.COMBO -> {
                        OutlinedTextField(
                            value = precioText, onValueChange = { precioText = it },
                            label = { Text("Precio combo (CLP)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TipoPromocion.DESCUENTO_PORCENTAJE -> {
                        OutlinedTextField(
                            value = descuentoText, onValueChange = { descuentoText = it },
                            label = { Text("Descuento (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val precio = precioText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val descuento = descuentoText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    onConfirm(nombre, tipoSeleccionado, precio, descuento)
                },
                enabled = datosValidos
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArmarPromoDialog(
    promocion: Promocion,
    detalleItems: List<PromocionItemDetalle>,
    analisis: AnalisisPromocion?,
    itemsMenu: List<ItemMenu>,
    onDismiss: () -> Unit,
    onAgregarItem: (itemMenuId: Int, cantidad: Int) -> Unit,
    onEliminarItem: (PromocionItemDetalle) -> Unit
) {
    var selectedItem by remember { mutableStateOf(itemsMenu.firstOrNull()) }
    var expandedItem by remember { mutableStateOf(false) }
    var cantidadText by remember { mutableStateOf("1") }

    LaunchedEffect(itemsMenu) { if (selectedItem == null) selectedItem = itemsMenu.firstOrNull() }

    val cantidadValida = cantidadText.toIntOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Armar: ${promocion.nombre}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Items incluidos:", style = MaterialTheme.typography.labelLarge) }
                if (detalleItems.isEmpty()) {
                    item {
                        Text(
                            "Sin items. Agregá items del menú abajo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    items(detalleItems) { detalle ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${detalle.promocionItem.cantidad}x ${detalle.item.nombre}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Precio ${money.format(detalle.item.precio)} · Costo ${money.format(detalle.item.costoTeorico)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onEliminarItem(detalle) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Quitar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                item { Text("Agregar item:", style = MaterialTheme.typography.labelLarge) }
                item {
                    if (itemsMenu.isEmpty()) {
                        Text(
                            "No hay items en el menú. Crealos en Configurar Menú.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = expandedItem,
                            onExpandedChange = { expandedItem = !expandedItem }
                        ) {
                            OutlinedTextField(
                                value = selectedItem?.nombre ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Item del menú") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItem) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedItem,
                                onDismissRequest = { expandedItem = false }
                            ) {
                                itemsMenu.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text("${item.nombre} (${money.format(item.precio)})") },
                                        onClick = {
                                            selectedItem = item
                                            expandedItem = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cantidadText, onValueChange = { cantidadText = it },
                            label = { Text("Cantidad") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val item = selectedItem ?: return@Button
                                onAgregarItem(item.id, cantidadText.toInt())
                                cantidadText = "1"
                            },
                            enabled = selectedItem != null && cantidadValida,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("+ Agregar item") }
                    }
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                item { AnalisisPanel(analisis) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun AnalisisPanel(analisis: AnalisisPromocion?) {
    if (analisis == null) {
        Text(
            "Análisis no disponible.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        return
    }
    val fcColor = when {
        analisis.foodCostPct <= 0.0 -> MaterialTheme.colorScheme.outline
        analisis.foodCostPct <= 32 -> MaterialTheme.colorScheme.primary
        analisis.foodCostPct <= 40 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val gananciaColor = if (analisis.ganancia >= 0) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Análisis", style = MaterialTheme.typography.titleSmall)
            AnalisisFila("Costo total", money.format(analisis.costoPromo))
            AnalisisFila("Precio normal", money.format(analisis.precioNormal))
            AnalisisFila("Precio promo", money.format(analisis.precioPromo))
            AnalisisFila(
                "Ganancia",
                "${money.format(analisis.ganancia)} (${pct.format(analisis.gananciaPct)}%)",
                gananciaColor
            )
            AnalisisFila("Food cost", "${pct.format(analisis.foodCostPct)}%", fcColor)
            AnalisisFila("Ahorro cliente", money.format(analisis.ahorroCliente))
        }
    }
}

@Composable
private fun AnalisisFila(
    etiqueta: String,
    valor: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
