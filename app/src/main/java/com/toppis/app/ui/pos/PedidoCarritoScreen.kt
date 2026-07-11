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
    usuarioId: String? = null,
    onPromoClick: (Promocion) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val pedido by viewModel.pedido.collectAsState()
    val menu by viewModel.menu.collectAsState()
    val promos by viewModel.promos.collectAsState()
    val lineas by viewModel.lineas.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val sobresCuenta by viewModel.sobresCuenta.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val puedeRegalar by viewModel.puedeRegalar.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) }
    var categoria by remember { mutableStateOf<String?>(null) }
    var productoPopup by remember { mutableStateOf<ItemMenu?>(null) }
    var promoPopup by remember { mutableStateOf<Promocion?>(null) }
    var espaciosPromo by remember { mutableStateOf<List<com.toppis.app.data.models.PromocionEspacio>>(emptyList()) }
    var elegiblesPromo by remember { mutableStateOf<Map<Int, List<ItemMenu>>>(emptyMap()) }
    var showPagar by remember { mutableStateOf(false) }
    var showComanda by remember { mutableStateOf(false) }
    var showEntregarSinPagar by remember { mutableStateOf(false) }

    LaunchedEffect(pedidoId) { viewModel.cargar(pedidoId) }
    LaunchedEffect(uiState) {
        (uiState as? CarritoUiState.Error)?.let { errorMsg = it.message; viewModel.resetState() }
    }
    LaunchedEffect(mensaje) {
        mensaje?.let { snackbarHostState.showSnackbar(it); viewModel.resetMensaje() }
    }
    errorMsg?.let { msg ->
        com.toppis.app.ui.components.ToppisErrorDialog(mensaje = msg, onDismiss = { errorMsg = null })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Pedido #$pedidoId", onBack = onNavigateBack) }
    ) { padding ->
        if (cargando) { com.toppis.app.ui.components.SkeletonList(); return@Scaffold }
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (puedeRegalar) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "🎁 Este cliente tiene cupón: al elegir la Cheese marcá \"Es regalo (cupón)\".",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

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

            pedido?.let { p ->
                if (!p.pagado) {
                    EnvioSelector(
                        zonaActual = runCatching { com.toppis.app.data.db.entities.ZonaEnvio.valueOf(p.zonaEnvio) }
                            .getOrDefault(com.toppis.app.data.db.entities.ZonaEnvio.SIN_ENVIO),
                        onSelect = { viewModel.setEnvio(it) }
                    )
                }
                AccionesPedido(
                    pedido = p,
                    hayLineas = lineas.isNotEmpty(),
                    onCerrar = { viewModel.cerrar() },
                    onCobrar = { showPagar = true },
                    onEntregar = { if (p.pagado) viewModel.entregar() else showEntregarSinPagar = true },
                    onVerComanda = { showComanda = true }
                )
            }
        }
    }

    productoPopup?.let { item ->
        ProductoModsDialog(
            item = item,
            modificadores = viewModel.modificadoresDe(item),
            puedeRegalar = puedeRegalar,
            precioCon = { ids -> viewModel.precioConMods(item, ids) },
            onCancelar = { productoPopup = null },
            onAgregar = { modIds, comentario, esRegalo ->
                viewModel.agregarProducto(item, modIds, comentario, esRegalo)
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

    if (showPagar) {
        PagarDialog(
            total = pedido?.total ?: 0.0,
            sobres = sobresCuenta,
            onDismiss = { showPagar = false },
            onConfirm = { metodo, sobreId ->
                showPagar = false
                viewModel.pagar(metodo, sobreId, usuarioId)
            }
        )
    }

    if (showComanda) {
        AlertDialog(
            onDismissRequest = { showComanda = false },
            title = { Text("Comanda") },
            text = { Text(pedido?.comandaTexto ?: "Sin comanda todavía.") },
            confirmButton = { TextButton(onClick = { showComanda = false }) { Text("Cerrar") } }
        )
    }

    if (showEntregarSinPagar) {
        com.toppis.app.ui.components.ToppisConfirmDialog(
            titulo = "Entregar sin pagar",
            mensaje = "Este pedido todavía no está pagado. ¿Marcar como entregado igual? Quedará con deuda.",
            onConfirm = { showEntregarSinPagar = false; viewModel.entregar() },
            onDismiss = { showEntregarSinPagar = false }
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
    puedeRegalar: Boolean,
    precioCon: (List<Int>) -> Double,
    onCancelar: () -> Unit,
    onAgregar: (modIds: List<Int>, comentario: String?, esRegalo: Boolean) -> Unit
) {
    val seleccionados = remember { mutableStateListOf<Int>() }
    var comentario by remember { mutableStateOf("") }
    var esRegalo by remember { mutableStateOf(false) }
    val precio = if (esRegalo) 0.0 else precioCon(seleccionados.toList())

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
                if (puedeRegalar) {
                    Row(
                        Modifier.fillMaxWidth().clickable { esRegalo = !esRegalo },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = esRegalo, onCheckedChange = { esRegalo = it })
                        Text("🎁 Es regalo (cupón) — precio $0")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAgregar(seleccionados.toList(), comentario.ifBlank { null }, esRegalo) }) {
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

@Composable
private fun ColumnScope.AccionesPedido(
    pedido: com.toppis.app.data.models.Pedido,
    hayLineas: Boolean,
    onCerrar: () -> Unit,
    onCobrar: () -> Unit,
    onEntregar: () -> Unit,
    onVerComanda: () -> Unit
) {
    val abierto = pedido.estado == com.toppis.app.data.db.entities.EstadoPedido.ABIERTO
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (abierto) {
                Button(onClick = onCerrar, enabled = hayLineas, modifier = Modifier.weight(1f)) {
                    Text("Cerrar y enviar comanda")
                }
            } else {
                OutlinedButton(onClick = onVerComanda, modifier = Modifier.weight(1f)) { Text("Ver comanda") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!pedido.pagado) {
                Button(onClick = onCobrar, enabled = hayLineas, modifier = Modifier.weight(1f)) { Text("Cobrar") }
            }
            if (!pedido.entregado) {
                OutlinedButton(onClick = onEntregar, modifier = Modifier.weight(1f)) { Text("Entregar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PagarDialog(
    total: Double,
    sobres: List<com.toppis.app.data.models.Sobre>,
    onDismiss: () -> Unit,
    onConfirm: (com.toppis.app.data.db.entities.MetodoPago, Int) -> Unit
) {
    var metodo by remember { mutableStateOf(com.toppis.app.data.db.entities.MetodoPago.EFECTIVO) }
    var sobre by remember { mutableStateOf(sobres.firstOrNull()) }
    var exp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cobrar ${money.format(total)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Método de pago", style = MaterialTheme.typography.labelLarge)
                com.toppis.app.data.db.entities.MetodoPago.entries.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clickable { metodo = m },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = metodo == m, onClick = { metodo = m })
                        Text(m.label)
                    }
                }
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                    OutlinedTextField(
                        value = sobre?.nombre ?: "Elegí un sobre", onValueChange = {}, readOnly = true,
                        label = { Text("Sobre destino") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        sobres.forEach { s ->
                            DropdownMenuItem(text = { Text(s.nombre) }, onClick = { sobre = s; exp = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { sobre?.let { onConfirm(metodo, it.id) } }, enabled = sobre != null) {
                Text("Confirmar pago")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvioSelector(
    zonaActual: com.toppis.app.data.db.entities.ZonaEnvio,
    onSelect: (com.toppis.app.data.db.entities.ZonaEnvio) -> Unit
) {
    var exp by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Envío:", style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }, modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = if (zonaActual == com.toppis.app.data.db.entities.ZonaEnvio.SIN_ENVIO) "Sin envío"
                else "${zonaActual.label} · ${money.format(zonaActual.precio)}",
                onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                com.toppis.app.data.db.entities.ZonaEnvio.entries.forEach { z ->
                    DropdownMenuItem(
                        text = { Text(if (z == com.toppis.app.data.db.entities.ZonaEnvio.SIN_ENVIO) "Sin envío" else "${z.label} · ${money.format(z.precio)}") },
                        onClick = { onSelect(z); exp = false }
                    )
                }
            }
        }
    }
}
