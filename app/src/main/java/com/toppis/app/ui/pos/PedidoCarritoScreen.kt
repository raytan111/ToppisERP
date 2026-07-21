package com.toppis.app.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TaskAlt
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
import kotlinx.coroutines.launch
import com.toppis.app.data.db.entities.CategoriaMenu
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Promocion
import com.toppis.app.domain.pos.PosCalculos
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
    var showEntregarConfirm by remember { mutableStateOf(false) }
    var showEliminar by remember { mutableStateOf(false) }
    var lineaAQuitar by remember { mutableStateOf<CarritoLinea?>(null) }
    var carritoExpandido by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Solo se puede agregar/editar con el pedido ABIERTO y sin pagar.
    val editable = pedido?.let {
        it.estado == com.toppis.app.data.db.entities.EstadoPedido.ABIERTO && !it.pagado
    } ?: false
    fun avisarCerrado() {
        scope.launch { snackbarHostState.showSnackbar("El pedido está cerrado: no se pueden agregar más productos.") }
    }

    // Se abre solo al ingresar el primer producto.
    LaunchedEffect(lineas.isNotEmpty()) { if (lineas.isNotEmpty()) carritoExpandido = true }

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
        topBar = {
            ToppisTopBar(
                titulo = "Pedido #$pedidoId",
                onBack = onNavigateBack,
                actions = {
                    // Eliminar pedido (solo icono) mientras no esté pagado.
                    if (pedido?.pagado == false) {
                        IconButton(onClick = { showEliminar = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar pedido", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
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
                                if (categoria == null) {
                                    // "Todas": agrupadas por categoría con encabezado.
                                    CategoriaMenu.entries.forEach { cm ->
                                        val delGrupo = menu.filter { CategoriaMenu.porLabel(it.categoria) == cm }
                                        if (delGrupo.isNotEmpty()) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Text(
                                                    cm.label,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(top = 6.dp)
                                                )
                                            }
                                            items(delGrupo, key = { it.id }) { item ->
                                                ProductoCard(item) { if (editable) productoPopup = item else avisarCerrado() }
                                            }
                                        }
                                    }
                                } else {
                                    items(filtrado, key = { it.id }) { item ->
                                        ProductoCard(item) { if (editable) productoPopup = item else avisarCerrado() }
                                    }
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
                                    if (!editable) { avisarCerrado(); return@PromoCard }
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

            // ── Carrito (abajo, colapsable) ───────────────────────────────
            CarritoPanel(
                lineas = lineas,
                total = pedido?.total ?: 0.0,
                expandido = carritoExpandido,
                onToggle = { carritoExpandido = !carritoExpandido },
                onMas = { if (editable) viewModel.cambiarCantidad(it, it.item.cantidad + 1) else avisarCerrado() },
                onMenos = { if (editable) viewModel.cambiarCantidad(it, it.item.cantidad - 1) else avisarCerrado() },
                onQuitar = { if (editable) lineaAQuitar = it else avisarCerrado() }
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
                    onReabrir = { viewModel.reabrir() },
                    onCobrar = { showPagar = true },
                    onEntregar = { if (p.pagado) showEntregarConfirm = true else showEntregarSinPagar = true },
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

    lineaAQuitar?.let { linea ->
        com.toppis.app.ui.components.ToppisConfirmDialog(
            titulo = "Quitar del carrito",
            mensaje = "¿Quitar \"${linea.titulo}\" del pedido?",
            textoConfirmar = "Quitar",
            onConfirm = { viewModel.quitarLinea(linea); lineaAQuitar = null },
            onDismiss = { lineaAQuitar = null }
        )
    }

    if (showEntregarSinPagar) {
        com.toppis.app.ui.components.ToppisConfirmDialog(
            titulo = "Entregar sin pagar",
            mensaje = "Este pedido todavía no está pagado. ¿Marcar como entregado igual? Quedará con deuda.",
            textoConfirmar = "Entregar",
            onConfirm = { showEntregarSinPagar = false; viewModel.entregar { onNavigateBack() } },
            onDismiss = { showEntregarSinPagar = false }
        )
    }

    if (showEntregarConfirm) {
        com.toppis.app.ui.components.ToppisConfirmDialog(
            titulo = "Entregar pedido",
            mensaje = "¿Confirmás que el pedido fue entregado? Se cerrará el pedido.",
            textoConfirmar = "Entregar",
            onConfirm = { showEntregarConfirm = false; viewModel.entregar { onNavigateBack() } },
            onDismiss = { showEntregarConfirm = false }
        )
    }

    if (showEliminar) {
        com.toppis.app.ui.components.ToppisConfirmDialog(
            titulo = "Eliminar pedido",
            mensaje = "¿Eliminar este pedido? Se borrará del sistema y no se puede deshacer.",
            textoConfirmar = "Eliminar",
            onConfirm = { showEliminar = false; viewModel.eliminarPedido { onNavigateBack() } },
            onDismiss = { showEliminar = false }
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
    expandido: Boolean,
    onToggle: () -> Unit,
    onMas: (CarritoLinea) -> Unit,
    onMenos: (CarritoLinea) -> Unit,
    onQuitar: (CarritoLinea) -> Unit
) {
    val cantidadTotal = lineas.sumOf { it.item.cantidad }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            // Encabezado compacto (siempre visible, toca para abrir/cerrar).
            Row(
                Modifier.fillMaxWidth().clickable(enabled = lineas.isNotEmpty(), onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lineas.isNotEmpty()) {
                    Icon(
                        if (expandido) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                        contentDescription = if (expandido) "Cerrar carrito" else "Abrir carrito"
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    if (lineas.isEmpty()) "Carrito vacío" else "Carrito · $cantidadTotal",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("Total ${money.format(total)}", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }

            if (expandido && lineas.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(lineas, key = { it.item.id }) { linea ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(linea.titulo, style = MaterialTheme.typography.bodyMedium)
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
    onCancelar: () -> Unit,
    onAgregar: (List<EleccionPromo>) -> Unit
) {
    // Unidades elegidas: (espacioId, itemMenuId). Permite repetidos.
    val elegidas = remember(espacios) { mutableStateListOf<Pair<Int, Int>>() }
    fun countGrupo(espId: Int) = elegidas.count { it.first == espId }
    fun idsGrupo(espId: Int) = elegidas.filter { it.first == espId }.map { it.second }
    val nombresMenu = remember(elegiblesPorEspacio) {
        elegiblesPorEspacio.values.flatten().associate { it.id to it.nombre }
    }

    val completa = espacios.isNotEmpty() &&
        espacios.all { PosCalculos.grupoCompleto(it.cantidad, countGrupo(it.id)) }

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
            if (espacios.isEmpty()) {
                Text("Esta promo no tiene grupos configurados. Agregalos en Cocina → Promociones.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    espacios.forEach { esp ->
                        val elegibles = elegiblesPorEspacio[esp.id].orEmpty()
                        val yaElegidos = idsGrupo(esp.id)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${esp.nombre}  (${countGrupo(esp.id)}/${esp.cantidad})",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                            )
                            if (elegibles.isEmpty()) {
                                Text("Sin opciones elegibles.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            } else {
                                elegibles.chunked(3).forEach { fila ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        fila.forEach { item ->
                                            val puede = PosCalculos.puedeAgregarAlGrupo(esp.permiteRepetir, yaElegidos, item.id, esp.cantidad)
                                            PromoOpcionCard(
                                                item = item,
                                                habilitado = puede,
                                                modifier = Modifier.weight(1f),
                                                onClick = { if (puede) elegidas.add(esp.id to item.id) }
                                            )
                                        }
                                        repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                                if (yaElegidos.isNotEmpty()) {
                                    yaElegidos.forEach { itemId ->
                                        Row(
                                            Modifier.fillMaxWidth().clickable {
                                                val idx = elegidas.indexOfFirst { it.first == esp.id && it.second == itemId }
                                                if (idx >= 0) elegidas.removeAt(idx)
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("• ${nombresMenu[itemId] ?: "Producto"}", modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodySmall)
                                            Icon(Icons.Filled.Close, contentDescription = "Quitar",
                                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAgregar(elegidas.map { EleccionPromo(it.second, emptyList(), null) }) },
                enabled = completa
            ) { Text("Agregar ${money.format(promo.precio)}") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun PromoOpcionCard(
    item: ItemMenu,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.then(if (habilitado) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (habilitado) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(70.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (item.imagenUrl != null) {
                    AsyncImage(model = item.imagenUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(item.nombre, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun ColumnScope.AccionesPedido(
    pedido: com.toppis.app.data.models.Pedido,
    hayLineas: Boolean,
    onCerrar: () -> Unit,
    onReabrir: () -> Unit,
    onCobrar: () -> Unit,
    onEntregar: () -> Unit,
    onVerComanda: () -> Unit
) {
    val abierto = pedido.estado == com.toppis.app.data.db.entities.EstadoPedido.ABIERTO
    val listo = !abierto  // CERRADO (o pagado) = marcado como listo
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // ── Listo (toggle): marca/desmarca; el relleno cambia solo como efecto. ──
            FilledIconToggleButton(
                checked = listo,
                onCheckedChange = { if (abierto) onCerrar() else onReabrir() },
                enabled = if (abierto) hayLineas else !pedido.pagado
            ) {
                Icon(
                    if (listo) Icons.Filled.TaskAlt else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Listo", modifier = Modifier.size(24.dp)
                )
            }

            // ── Cobrar (billetes): solo cuando está cerrado y no pagado. ──
            if (!pedido.pagado) {
                FilledIconButton(onClick = onCobrar, enabled = listo && hayLineas) {
                    Icon(Icons.Filled.Payments, contentDescription = "Cobrar", modifier = Modifier.size(24.dp))
                }
            }

            // ── Entregar ──
            if (!pedido.entregado) {
                FilledTonalIconButton(onClick = onEntregar) {
                    Icon(Icons.Filled.DeliveryDining, contentDescription = "Entregar", modifier = Modifier.size(24.dp))
                }
            }

            if (listo) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onVerComanda) { Text("Ver comanda") }
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
