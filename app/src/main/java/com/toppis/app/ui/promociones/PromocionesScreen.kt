package com.toppis.app.ui.promociones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.toppis.app.data.db.entities.CategoriaMenu
import com.toppis.app.data.db.entities.ModoEspacioPromo
import com.toppis.app.data.models.PromocionEspacio
import com.toppis.app.data.models.PromocionEspacioOpcion
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.toppis.app.data.db.entities.TipoPromocion
import com.toppis.app.data.models.AnalisisPromocion
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Promocion
import com.toppis.app.data.repository.PromocionItemDetalle
import com.toppis.app.ui.components.ImagePickerField
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@Composable
private fun PromoThumb(url: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(56.dp)
    ) {
        if (url != null) {
            AsyncImage(
                model = url, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Agregar foto", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val money = DecimalFormat("$#,##0")
private val pct = DecimalFormat("0.#")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromocionesScreen(
    viewModel: PromocionViewModel,
    puedeBorrar: Boolean = true,
    onNavigateBack: () -> Unit = {}
) {
    val promociones by viewModel.promociones.collectAsState()
    val itemsMenu by viewModel.itemsMenu.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrearDialog by remember { mutableStateOf(false) }
    var promoAEliminar by remember { mutableStateOf<Promocion?>(null) }
    var promoAEditar by remember { mutableStateOf<Promocion?>(null) }
    var fotoDe by remember { mutableStateOf<Promocion?>(null) }

    var promoSeleccionada by remember { mutableStateOf<Promocion?>(null) }
    var detalleItems by remember { mutableStateOf<List<PromocionItemDetalle>>(emptyList()) }
    var analisis by remember { mutableStateOf<AnalisisPromocion?>(null) }

    var espaciosDe by remember { mutableStateOf<Promocion?>(null) }
    var espacios by remember { mutableStateOf<List<PromocionEspacio>>(emptyList()) }
    var opcionesPorEspacio by remember { mutableStateOf<Map<Int, List<PromocionEspacioOpcion>>>(emptyMap()) }

    fun recargarDetalle(promo: Promocion) {
        viewModel.cargarDetalle(promo) { items, an ->
            detalleItems = items
            analisis = an
        }
    }

    fun recargarEspacios(promo: Promocion) {
        viewModel.cargarEspacios(promo.id) { esp, ops -> espacios = esp; opcionesPorEspacio = ops }
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
                                PromoThumb(promo.imagenUrl, onClick = { fotoDe = promo })
                                Spacer(Modifier.width(12.dp))
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
                                IconButton(onClick = { espaciosDe = promo; recargarEspacios(promo) }) {
                                    Icon(
                                        Icons.Filled.Category,
                                        contentDescription = "Espacios a elegir",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(onClick = { promoAEditar = promo }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (puedeBorrar) {
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
    }

    fotoDe?.let { promo ->
        AlertDialog(
            onDismissRequest = { fotoDe = null },
            title = { Text("Foto de ${promo.nombre}") },
            text = {
                ImagePickerField(
                    imagenUrl = promo.imagenUrl,
                    carpeta = "promos",
                    onImagenSubida = { url ->
                        viewModel.actualizarImagen(promo.id, url)
                        fotoDe = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { fotoDe = null }) { Text("Cerrar") } }
        )
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
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = promo.nombre,
            titulo = "Eliminar promoción",
            onConfirm = { viewModel.eliminarPromocion(promo.id); promoAEliminar = null },
            onDismiss = { promoAEliminar = null }
        )
    }

    promoAEditar?.let { promo ->
        EditarPromocionDialog(
            promocion = promo,
            onDismiss = { promoAEditar = null },
            onConfirm = { nombre, tipo, precio, descuento ->
                viewModel.actualizarPromocion(
                    promo.copy(nombre = nombre, tipo = tipo, precio = precio, descuentoPct = descuento)
                )
                promoAEditar = null
            }
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

    espaciosDe?.let { promo ->
        EspaciosPromoDialog(
            promocion = promo,
            espacios = espacios,
            opcionesPorEspacio = opcionesPorEspacio,
            itemsMenu = itemsMenu,
            onDismiss = { espaciosDe = null; espacios = emptyList(); opcionesPorEspacio = emptyMap() },
            onCrearEspacio = { nombre, cantidad, modo, categoria ->
                viewModel.crearEspacio(promo.id, nombre, cantidad, modo, categoria, espacios.size) { recargarEspacios(promo) }
            },
            onEliminarEspacio = { id -> viewModel.eliminarEspacio(id) { recargarEspacios(promo) } },
            onAgregarOpcion = { espacioId, itemMenuId -> viewModel.agregarOpcion(espacioId, itemMenuId) { recargarEspacios(promo) } },
            onEliminarOpcion = { id -> viewModel.eliminarOpcion(id) { recargarEspacios(promo) } }
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
private fun EditarPromocionDialog(
    promocion: Promocion,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, tipo: TipoPromocion, precio: Double, descuentoPct: Double) -> Unit
) {
    var nombre by remember { mutableStateOf(promocion.nombre) }
    var tipoSeleccionado by remember { mutableStateOf(promocion.tipo) }
    var expandedTipo by remember { mutableStateOf(false) }
    var precioText by remember { mutableStateOf(if (promocion.precio == 0.0) "" else promocion.precio.toLong().toString()) }
    var descuentoText by remember { mutableStateOf(if (promocion.descuentoPct == 0.0) "" else pct.format(promocion.descuentoPct)) }

    val precioValido = precioText.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false
    val descuentoValido = descuentoText.replace(",", ".").toDoubleOrNull()?.let { it > 0 && it <= 100 } ?: false
    val datosValidos = nombre.isNotBlank() && when (tipoSeleccionado) {
        TipoPromocion.COMBO -> precioValido
        TipoPromocion.DESCUENTO_PORCENTAJE -> descuentoValido
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Promoción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre") },
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
                                onClick = { tipoSeleccionado = tipo; expandedTipo = false }
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
                Text("Los items del combo se editan tocando la promoción.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
            ) { Text("Guardar") }
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
                                onAgregarItem(item.id, cantidadText.toIntOrNull() ?: return@Button)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EspaciosPromoDialog(
    promocion: Promocion,
    espacios: List<PromocionEspacio>,
    opcionesPorEspacio: Map<Int, List<PromocionEspacioOpcion>>,
    itemsMenu: List<ItemMenu>,
    onDismiss: () -> Unit,
    onCrearEspacio: (nombre: String, cantidad: Int, modo: ModoEspacioPromo, categoria: String?) -> Unit,
    onEliminarEspacio: (id: Int) -> Unit,
    onAgregarOpcion: (espacioId: Int, itemMenuId: Int) -> Unit,
    onEliminarOpcion: (id: Int) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var cantidadText by remember { mutableStateOf("1") }
    var modo by remember { mutableStateOf(ModoEspacioPromo.CATEGORIA) }
    var categoria by remember { mutableStateOf(CategoriaMenu.HAMBURGUESAS.label) }
    var expCat by remember { mutableStateOf(false) }

    val nombreById = remember(itemsMenu) { itemsMenu.associate { it.id to it.nombre } }
    val cantidadValida = cantidadText.toIntOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Espacios: ${promocion.nombre}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text("Cada espacio es algo que el cliente elige en el POS (ej: 1 hamburguesa a elección).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }

                if (espacios.isEmpty()) {
                    item { Text("Sin espacios todavía.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                } else {
                    items(espacios) { esp ->
                        EspacioCard(
                            espacio = esp,
                            opciones = opcionesPorEspacio[esp.id].orEmpty(),
                            itemsMenu = itemsMenu,
                            nombreById = nombreById,
                            onEliminar = { onEliminarEspacio(esp.id) },
                            onAgregarOpcion = { itemId -> onAgregarOpcion(esp.id, itemId) },
                            onEliminarOpcion = { id -> onEliminarOpcion(id) }
                        )
                    }
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                item { Text("Nuevo espacio:", style = MaterialTheme.typography.labelLarge) }
                item {
                    OutlinedTextField(
                        value = nombre, onValueChange = { nombre = it },
                        label = { Text("Nombre (ej: Hamburguesa)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = cantidadText, onValueChange = { cantidadText = it },
                        label = { Text("Cantidad a elegir") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = modo == ModoEspacioPromo.CATEGORIA, onClick = { modo = ModoEspacioPromo.CATEGORIA })
                        Text("Por categoría", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = modo == ModoEspacioPromo.LISTA, onClick = { modo = ModoEspacioPromo.LISTA })
                        Text("Lista específica", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (modo == ModoEspacioPromo.CATEGORIA) {
                    item {
                        ExposedDropdownMenuBox(expanded = expCat, onExpandedChange = { expCat = !expCat }) {
                            OutlinedTextField(
                                value = categoria, onValueChange = {}, readOnly = true,
                                label = { Text("Categoría elegible") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCat) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expCat, onDismissRequest = { expCat = false }) {
                                CategoriaMenu.entries.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.label) }, onClick = { categoria = c.label; expCat = false })
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Text("Después de crear el espacio, agregá las opciones específicas desde su tarjeta.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                item {
                    Button(
                        onClick = {
                            onCrearEspacio(
                                nombre.ifBlank { "Espacio" },
                                cantidadText.toIntOrNull() ?: 1,
                                modo,
                                if (modo == ModoEspacioPromo.CATEGORIA) categoria else null
                            )
                            nombre = ""; cantidadText = "1"
                        },
                        enabled = cantidadValida,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Agregar espacio") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EspacioCard(
    espacio: PromocionEspacio,
    opciones: List<PromocionEspacioOpcion>,
    itemsMenu: List<ItemMenu>,
    nombreById: Map<Int, String>,
    onEliminar: () -> Unit,
    onAgregarOpcion: (itemMenuId: Int) -> Unit,
    onEliminarOpcion: (id: Int) -> Unit
) {
    var selectedItem by remember { mutableStateOf(itemsMenu.firstOrNull()) }
    var exp by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${espacio.cantidad}x ${espacio.nombre}", style = MaterialTheme.typography.titleSmall)
                    val det = when (espacio.modo) {
                        ModoEspacioPromo.CATEGORIA -> "Categoría: ${espacio.categoria ?: "-"}"
                        ModoEspacioPromo.LISTA -> "Lista específica (${opciones.size})"
                    }
                    Text(det, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Filled.Delete, contentDescription = "Quitar espacio", tint = MaterialTheme.colorScheme.error)
                }
            }
            if (espacio.modo == ModoEspacioPromo.LISTA) {
                opciones.forEach { op ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• ${nombreById[op.itemMenuId] ?: "Item #${op.itemMenuId}"}",
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onEliminarOpcion(op.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Quitar opción", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (itemsMenu.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                        OutlinedTextField(
                            value = selectedItem?.nombre ?: "", onValueChange = {}, readOnly = true,
                            label = { Text("Agregar opción") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                            itemsMenu.forEach { it2 ->
                                DropdownMenuItem(text = { Text(it2.nombre) }, onClick = { selectedItem = it2; exp = false })
                            }
                        }
                    }
                    TextButton(onClick = { selectedItem?.let { onAgregarOpcion(it.id) } }, enabled = selectedItem != null) {
                        Text("+ Agregar opción")
                    }
                }
            }
        }
    }
}
