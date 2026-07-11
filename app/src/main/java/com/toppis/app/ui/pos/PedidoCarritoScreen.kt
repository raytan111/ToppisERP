package com.toppis.app.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.toppis.app.data.db.entities.CategoriaMenu
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Promocion
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val money = DecimalFormat("$#,##0")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoCarritoScreen(
    viewModel: CarritoViewModel,
    pedidoId: Int,
    onPromoClick: (Promocion) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val pedido by viewModel.pedido.collectAsState()
    val menu by viewModel.menu.collectAsState()
    val promos by viewModel.promos.collectAsState()
    val lineas by viewModel.lineas.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) }
    var categoria by remember { mutableStateOf<String?>(null) }
    var productoPopup by remember { mutableStateOf<ItemMenu?>(null) }
    var promoPopup by remember { mutableStateOf<Promocion?>(null) }
    var espaciosPromo by remember { mutableStateOf<List<com.toppis.app.data.models.PromocionEspacio>>(emptyList()) }
    var elegiblesPromo by remember { mutableStateOf<Map<Int, List<ItemMenu>>>(emptyMap()) }

    LaunchedEffect(pedidoId) { viewModel.cargar(pedidoId) }
    LaunchedEffect(uiState) {
        (uiState as? CarritoUiState.Error)?.let { errorMsg = it.message; viewModel.resetState() }
    }
    errorMsg?.let { msg ->
        com.toppis.app.ui.components.ToppisErrorDialog(mensaje = msg, onDismiss = { errorMsg = null })
    }

    Scaffold(
        topBar = { ToppisTopBar(titulo = "Pedido #$pedidoId", onBack = onNavigateBack) }
    ) { padding ->
        if (cargando) { com.toppis.app.ui.components.SkeletonList(); return@Scaffold }
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Catálogo (arriba) ─────────────────────────────────────────
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Menú") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Promociones") })
            }

            Box(Modifier.weight(1f)) {
                if (tab == 0) {
                    Column {
                        CategoriaFiltro(categoria) { categoria = it }
                        val filtrado = if (categoria == null) menu
                        else menu.filter { it.categoria.equals(categoria, ignoreCase = true) }
                        if (filtrado.isEmpty()) {
                            com.toppis.app.ui.components.EmptyState(
                                icon = Icons.Filled.Restaurant, titulo = "Sin productos",
                                subtitulo = "No hay productos en esta categoría."
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                items(filtrado, key = { it.id }) { item ->
                                    ProductoCard(item) { productoPopup = item }
                                }
                            }
                        }
                    }
                } else {
                    if (promos.isEmpty()) {
                        com.toppis.app.ui.components.EmptyState(
                            icon = Icons.Filled.Restaurant, titulo = "Sin promociones",
                            subtitulo = "Creá promociones en Cocina → Promociones."
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            items(promos, key = { it.id }) { promo ->
                                PromoCard(promo) {
                                    onPromoClick(promo)
                                    viewModel.cargarEspaciosPromo(promo) { esp, eleg ->
                                        espaciosPromo = esp; elegiblesPromo = eleg; promoPopup = promo
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Carrito (abajo) ───────────────────────────────────────────
            CarritoPanel(
                lineas = lineas,
                total = pedido?.total ?: 0.0,
                onMas = { viewModel.cambiarCantidad(it, it.item.cantidad + 1) },
                onMenos = { viewModel.cambiarCantidad(it, it.item.cantidad - 1) },
                onQuitar = { viewModel.quitarLinea(it) }
            )
        }
    }

    productoPopup?.let { item ->
        ProductoModsDialog(
            item = item,
            modificadores = viewModel.modificadoresDe(item),
            precioCon = { ids -> viewModel.precioConMods(item, ids) },
            onCancelar = { productoPopup = null },
            onAgregar = { modIds, comentario ->
                viewModel.agregarProducto(item, modIds, comentario)
                productoPopup = null
            }
        )
    }

    promoPopup?.let { promo ->
        PromoConfigDialog(
            promo = promo,
            espacios = espaciosPromo,
            elegiblesPorEspacio = elegiblesPromo,
            modsDe = { item -> viewModel.modificadoresDe(item) },
            onCancelar = { promoPopup = null; espaciosPromo = emptyList(); elegiblesPromo = emptyMap() },
            onAgregar = { elecciones ->
                viewModel.agregarPromo(promo, elecciones)
                promoPopup = null; espaciosPromo = emptyList(); elegiblesPromo = emptyMap()
            }
        )
    }
}

@Composable
private fun CategoriaFiltro(seleccion: String?, onSelect: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = seleccion == null, onClick = { onSelect(null) }, label = { Text("Todas") })
        CategoriaMenu.entries.forEach { c ->
            FilterChip(selected = seleccion == c.label, onClick = { onSelect(c.label) }, label = { Text(c.label) })
        }
    }
}

@Composable
private fun ProductoCard(item: ItemMenu, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(96.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (item.imagenUrl != null) {
                    AsyncImage(
                        model = item.imagenUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(item.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(money.format(item.precio), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PromoCard(promo: Promocion, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(96.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (promo.imagenUrl != null) {
                    AsyncImage(model = promo.imagenUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(promo.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(money.format(promo.precio), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ColumnScope.CarritoPanel(
    lineas: List<CarritoLinea>,
    total: Double,
    onMas: (CarritoLinea) -> Unit,
    onMenos: (CarritoLinea) -> Unit,
    onQuitar: (CarritoLinea) -> Unit
) {
    Column(Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 300.dp).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Carrito", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Total ${money.format(total)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(6.dp))
        if (lineas.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Todavía no hay productos.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(lineas, key = { it.item.id }) { linea ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(linea.titulo, style = MaterialTheme.typography.bodyLarge)
                            if (linea.detalle.isNotBlank()) {
                                Text(linea.detalle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(money.format(linea.subtotal), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onMenos(linea) }) { Icon(Icons.Filled.Remove, contentDescription = "Menos") }
                        Text("${linea.item.cantidad}", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onMas(linea) }) { Icon(Icons.Filled.Add, contentDescription = "Más") }
                        IconButton(onClick = { onQuitar(linea) }) { Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoModsDialog(
    item: ItemMenu,
    modificadores: List<com.toppis.app.data.models.Modificador>,
    precioCon: (List<Int>) -> Double,
    onCancelar: () -> Unit,
    onAgregar: (modIds: List<Int>, comentario: String?) -> Unit
) {
    val seleccionados = remember { mutableStateListOf<Int>() }
    var comentario by remember { mutableStateOf("") }
    val precio = precioCon(seleccionados.toList())

    AlertDialog(
        onDismissRequest = { /* protegido: no cierra al tocar afuera */ },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.nombre, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancelar) { Icon(Icons.Filled.Close, contentDescription = "Cancelar") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (modificadores.isEmpty()) {
                    Text("Este producto no tiene modificadores.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                } else {
                    Text("Agregados:", style = MaterialTheme.typography.labelLarge)
                    modificadores.forEach { m ->
                        val checked = m.id in seleccionados
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (checked) seleccionados.remove(m.id) else seleccionados.add(m.id)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = {
                                if (it) seleccionados.add(m.id) else seleccionados.remove(m.id)
                            })
                            Text(m.nombre, modifier = Modifier.weight(1f))
                            val signo = if (m.deltaPrecio >= 0) "+" else "-"
                            Text("$signo${money.format(kotlin.math.abs(m.deltaPrecio))}",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                OutlinedTextField(
                    value = comentario, onValueChange = { comentario = it },
                    label = { Text("Comentario (ej: sin tomate)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAgregar(seleccionados.toList(), comentario.ifBlank { null }) }) {
                Text("Agregar ${money.format(precio)}")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromoConfigDialog(
    promo: Promocion,
    espacios: List<com.toppis.app.data.models.PromocionEspacio>,
    elegiblesPorEspacio: Map<Int, List<ItemMenu>>,
    modsDe: (ItemMenu) -> List<com.toppis.app.data.models.Modificador>,
    onCancelar: () -> Unit,
    onAgregar: (List<EleccionPromo>) -> Unit
) {
    // Un "slot" por cada unidad a elegir (espacio.cantidad veces).
    val slots = remember(espacios) { espacios.flatMap { esp -> List(esp.cantidad) { esp } } }
    val seleccion = remember(slots) { mutableStateListOf<Int?>().apply { repeat(slots.size) { add(null) } } }
    val comentarios = remember(slots) { mutableStateListOf<String>().apply { repeat(slots.size) { add("") } } }
    val modsPorSlot = remember(slots) { List(slots.size) { mutableStateListOf<Int>() } }

    val completa = slots.isNotEmpty() && seleccion.all { it != null }

    AlertDialog(
        onDismissRequest = { /* protegido */ },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${promo.nombre} · ${money.format(promo.precio)}", modifier = Modifier.weight(1f))
                IconButton(onClick = onCancelar) { Icon(Icons.Filled.Close, contentDescription = "Cancelar") }
            }
        },
        text = {
            if (slots.isEmpty()) {
                Text("Esta promo no tiene espacios configurados. Agregalos en Cocina → Promociones.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(slots) { i, esp ->
                        val elegibles = elegiblesPorEspacio[esp.id].orEmpty()
                        val seleccionadoId = seleccion[i]
                        val itemSel = elegibles.firstOrNull { it.id == seleccionadoId }
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(esp.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (elegibles.isEmpty()) {
                                    Text("Sin opciones elegibles.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                } else {
                                    var exp by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                                        OutlinedTextField(
                                            value = itemSel?.nombre ?: "Elegí…", onValueChange = {}, readOnly = true,
                                            label = { Text("Producto") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                            elegibles.forEach { opt ->
                                                DropdownMenuItem(text = { Text(opt.nombre) }, onClick = {
                                                    seleccion[i] = opt.id; modsPorSlot[i].clear(); exp = false
                                                })
                                            }
                                        }
                                    }
                                    // Modificadores del producto elegido (opcionales).
                                    itemSel?.let { sel ->
                                        val mods = modsDe(sel)
                                        mods.forEach { m ->
                                            val checked = m.id in modsPorSlot[i]
                                            Row(
                                                Modifier.fillMaxWidth().clickable {
                                                    if (checked) modsPorSlot[i].remove(m.id) else modsPorSlot[i].add(m.id)
                                                },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(checked = checked, onCheckedChange = {
                                                    if (it) modsPorSlot[i].add(m.id) else modsPorSlot[i].remove(m.id)
                                                })
                                                Text(m.nombre, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        OutlinedTextField(
                                            value = comentarios[i], onValueChange = { comentarios[i] = it },
                                            label = { Text("Comentario (opcional)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val elecciones = slots.indices.map { i ->
                        EleccionPromo(seleccion[i]!!, modsPorSlot[i].toList(), comentarios[i].ifBlank { null })
                    }
                    onAgregar(elecciones)
                },
                enabled = completa
            ) { Text("Agregar ${money.format(promo.precio)}") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
