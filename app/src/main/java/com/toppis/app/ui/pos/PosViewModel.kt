package com.toppis.app.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Comprobante
import com.toppis.app.data.db.entities.MetodoPago
import com.toppis.app.data.db.entities.ZonaEnvio
import com.toppis.app.data.repository.ComandaRepository
import com.toppis.app.data.repository.ComponenteReceta
import com.toppis.app.data.repository.ComprobanteRepository
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.repository.LineaComanda
import com.toppis.app.data.repository.LineaVenta
import com.toppis.app.data.repository.MenuRepository
import com.toppis.app.data.repository.ModificadorConCosto
import com.toppis.app.data.repository.ModificadorRepository
import com.toppis.app.data.repository.OpcionPos
import com.toppis.app.data.repository.PromocionRepository
import com.toppis.app.data.models.Promocion
import com.toppis.app.data.repository.SobreRepository
import com.toppis.app.data.repository.VentaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ItemCarritoMenu(
    val itemMenu: ItemMenu,
    val cantidad: Int,
    val salsas: List<OpcionPos> = emptyList(),
    val modificadores: List<ModificadorConCosto> = emptyList(),
    val ajustes: List<AjusteReceta> = emptyList(),
    val precioOverride: Double? = null,
    val promocionId: Int? = null,
    val promoNombre: String? = null
) {
    /** Precio unitario: override de promo si existe, si no precio + deltas de modificadores. */
    val precioUnitario: Double get() = precioOverride ?: (itemMenu.precio + modificadores.sumOf { it.deltaPrecio })
    /** Costo teórico unitario incluyendo modificadores, salsas y ajustes (quitar/cambiar). */
    val costoUnitario: Double get() = itemMenu.costoTeorico +
        modificadores.sumOf { it.deltaCosto } +
        salsas.sumOf { it.costo } +
        ajustes.sumOf { it.deltaCosto }
    val subtotal: Double get() = precioUnitario * cantidad
    /** Texto legible de modificadores + ajustes (para comanda y guardado). */
    val modificadoresTexto: String get() =
        (modificadores.map { it.modificador.nombre } + ajustes.map { it.descripcion }).joinToString(", ")
    /** Texto legible de salsas. */
    val salsasTexto: String get() = salsas.joinToString(", ") { it.nombre }
}

/** Tipo de ajuste sobre la receta, hecho en el POS. */
enum class TipoAjuste { QUITAR, CAMBIAR }

/** Ajuste a la receta de un plato hecho al momento de la venta (quitar o cambiar un ingrediente). */
data class AjusteReceta(
    val tipo: TipoAjuste,
    val original: ComponenteReceta,
    val reemplazo: Articulo? = null,
    val cantidadReemplazo: Double = 0.0
) {
    val deltaCosto: Double get() = when (tipo) {
        TipoAjuste.QUITAR -> -original.costoLinea
        TipoAjuste.CAMBIAR -> -original.costoLinea + (reemplazo?.costoBase ?: 0.0) * cantidadReemplazo
    }
    val descripcion: String get() = when (tipo) {
        TipoAjuste.QUITAR -> "Sin ${original.nombre}"
        TipoAjuste.CAMBIAR -> "${original.nombre} → ${reemplazo?.nombre ?: "?"}"
    }
}

sealed class PosUiState {
    object Idle : PosUiState()
    object Loading : PosUiState()
    data class VentaExitosa(
        val ventaId: Int,
        val comandaTexto: String,
        val whatsappTexto: String,
        val lineas: List<LineaComanda>,
        val zonaEnvio: ZonaEnvio,
        val total: Double
    ) : PosUiState()
    data class Error(val message: String) : PosUiState()
}

class PosViewModel(
    private val ventaRepository: VentaRepository,
    private val sobreRepository: SobreRepository,
    private val menuRepository: MenuRepository,
    private val comandaRepository: ComandaRepository,
    private val comprobanteRepository: ComprobanteRepository,
    private val modificadorRepository: ModificadorRepository,
    private val promocionRepository: PromocionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PosUiState>(PosUiState.Idle)
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val _itemsMenu = MutableStateFlow<List<ItemMenu>>(emptyList())
    val itemsMenu: StateFlow<List<ItemMenu>> = _itemsMenu.asStateFlow()

    private val _salsasDisponibles = MutableStateFlow<List<OpcionPos>>(emptyList())
    val salsasDisponibles: StateFlow<List<OpcionPos>> = _salsasDisponibles.asStateFlow()

    private val _promociones = MutableStateFlow<List<Promocion>>(emptyList())
    val promociones: StateFlow<List<Promocion>> = _promociones.asStateFlow()

    // Comprobante emitido tras la venta (Fase 2A)
    private val _comprobante = MutableStateFlow<Comprobante?>(null)
    val comprobante: StateFlow<Comprobante?> = _comprobante.asStateFlow()

    private val _comprobanteError = MutableStateFlow<String?>(null)
    val comprobanteError: StateFlow<String?> = _comprobanteError.asStateFlow()

    fun emitirComprobante(ventaId: Int, usuarioId: String?) {
        viewModelScope.launch {
            _comprobanteError.value = null
            try {
                _comprobante.value = comprobanteRepository.emitirComprobante(ventaId, usuarioId)
            } catch (e: Exception) {
                _comprobanteError.value = e.message ?: "Error al emitir comprobante"
            }
        }
    }

    fun limpiarComprobante() {
        _comprobante.value = null
        _comprobanteError.value = null
    }

    init {
        refrescarMenu()
        refrescarSalsas()
        refrescarPromociones()
        // Realtime: menú, salsas (artículos) y promos se actualizan al instante
        viewModelScope.launch { menuRepository.observeItemsMenu().collect { refrescarMenu(); refrescarSalsas(); refrescarPromociones() } }
        viewModelScope.launch { menuRepository.observeArticulos().collect { refrescarSalsas() } }
    }

    private fun refrescarMenu() {
        viewModelScope.launch { _itemsMenu.value = menuRepository.getItemsMenuActivos() }
    }

    private fun refrescarSalsas() {
        viewModelScope.launch {
            _salsasDisponibles.value = menuRepository.getOpcionesPos()
        }
    }

    fun refrescarPromociones() {
        viewModelScope.launch {
            _promociones.value = promocionRepository.getPromociones().filter { it.activo }
        }
    }

    /** Agrega una promoción al carrito expandiéndola en sus items (precio distribuido). */
    fun agregarPromoAlCarrito(promo: Promocion) {
        viewModelScope.launch {
            val promoItems = promocionRepository.getItems(promo.id)
            val menu = menuRepository.getAllItemsMenu()
            val detalle = promoItems.mapNotNull { pi -> menu.firstOrNull { it.id == pi.itemMenuId }?.let { it to pi.cantidad } }
            if (detalle.isEmpty()) return@launch

            val precioNormal = detalle.sumOf { (item, cant) -> item.precio * cant }
            val precioPromo = when (promo.tipo) {
                com.toppis.app.data.db.entities.TipoPromocion.COMBO -> promo.precio
                com.toppis.app.data.db.entities.TipoPromocion.DESCUENTO_PORCENTAJE -> precioNormal * (1.0 - promo.descuentoPct / 100.0)
            }
            val factor = if (precioNormal > 0) precioPromo / precioNormal else 1.0

            val current = _carrito.value.toMutableList()
            detalle.forEach { (item, cant) ->
                current.add(
                    ItemCarritoMenu(
                        itemMenu = item,
                        cantidad = cant,
                        precioOverride = item.precio * factor,
                        promocionId = promo.id,
                        promoNombre = promo.nombre
                    )
                )
            }
            _carrito.value = current
            calcularTotal(current)
        }
    }

    private val _carrito = MutableStateFlow<List<ItemCarritoMenu>>(emptyList())
    val carrito: StateFlow<List<ItemCarritoMenu>> = _carrito.asStateFlow()

    private val _totalCarrito = MutableStateFlow(0.0)
    val totalCarrito: StateFlow<Double> = _totalCarrito.asStateFlow()

    private fun calcularTotal(items: List<ItemCarritoMenu>) {
        _totalCarrito.value = items.sumOf { it.subtotal }
    }

    /** Carga los modificadores disponibles (con costo) para un item del menú. */
    fun cargarModificadores(itemMenuId: Int, callback: (List<ModificadorConCosto>) -> Unit) {
        viewModelScope.launch {
            callback(modificadorRepository.getModificadoresConCosto(itemMenuId))
        }
    }

    private val _articulosPos = MutableStateFlow<List<Articulo>>(emptyList())
    val articulosPos: StateFlow<List<Articulo>> = _articulosPos.asStateFlow()

    /** Carga la receta de un item (para editar quitar/cambiar en el POS). */
    fun cargarReceta(itemMenuId: Int, callback: (List<ComponenteReceta>) -> Unit) {
        viewModelScope.launch {
            if (_articulosPos.value.isEmpty()) _articulosPos.value = menuRepository.getArticulos()
            callback(menuRepository.getComponentesReceta(itemMenuId))
        }
    }

    /** Aplica una lista de ajustes (quitar/cambiar) a una línea del carrito. */
    fun aplicarAjustes(index: Int, ajustes: List<AjusteReceta>) {
        val current = _carrito.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(ajustes = ajustes)
            _carrito.value = current
            calcularTotal(current)
        }
    }

    fun agregarAlCarrito(
        itemMenu: ItemMenu,
        salsas: List<OpcionPos>,
        modificadores: List<ModificadorConCosto> = emptyList()
    ) {
        val current = _carrito.value.toMutableList()
        val modIds = modificadores.map { it.modificador.id }.sorted()
        val salsaIds = salsas.map { "${it.tipo}-${it.id}" }.sorted()
        val index = current.indexOfFirst {
            it.itemMenu.id == itemMenu.id &&
                it.salsas.map { s -> "${s.tipo}-${s.id}" }.sorted() == salsaIds &&
                it.modificadores.map { m -> m.modificador.id }.sorted() == modIds &&
                it.promocionId == null
        }
        if (index != -1) {
            current[index] = current[index].copy(cantidad = current[index].cantidad + 1)
        } else {
            current.add(ItemCarritoMenu(itemMenu, 1, salsas, modificadores))
        }
        _carrito.value = current
        calcularTotal(current)
    }

    fun quitarDelCarrito(index: Int) {
        val current = _carrito.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
        }
        _carrito.value = current
        calcularTotal(current)
    }

    fun cambiarCantidad(index: Int, cantidad: Int) {
        if (cantidad <= 0) {
            quitarDelCarrito(index)
            return
        }
        val current = _carrito.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(cantidad = cantidad)
        }
        _carrito.value = current
        calcularTotal(current)
    }

    fun procesarVenta(
        metodoPago: MetodoPago,
        sobreId: Int,
        usuarioId: String? = null,
        zonaEnvio: ZonaEnvio = ZonaEnvio.SIN_ENVIO
    ) {
        if (_carrito.value.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = PosUiState.Loading
            try {
                val carritoActual = _carrito.value
                val total = _totalCarrito.value + zonaEnvio.precio

                // Líneas para la RPC
                val lineasVenta = carritoActual.map { item ->
                    val modsComp = item.modificadores.flatMap { m ->
                        m.componentes.map { c ->
                            com.toppis.app.data.repository.ModComponenteVenta(
                                tipo = c.tipoComponente.name,
                                componenteId = c.componenteId,
                                cantidadBase = c.cantidadBase,
                                accion = c.accion.name
                            )
                        }
                    }
                    // Las salsas también descuentan stock (AGREGAR su cantidad_pos)
                    val salsasComp = item.salsas.filter { it.cantidad > 0 }.map { s ->
                        com.toppis.app.data.repository.ModComponenteVenta(
                            tipo = s.tipo.name,
                            componenteId = s.id,
                            cantidadBase = s.cantidad,
                            accion = "AGREGAR"
                        )
                    }
                    // Ajustes quitar/cambiar: QUITAR devuelve el original; CAMBIAR agrega el reemplazo
                    val ajustesComp = item.ajustes.flatMap { aj ->
                        val devolver = com.toppis.app.data.repository.ModComponenteVenta(
                            tipo = aj.original.recetaMenu.tipoComponente.name,
                            componenteId = aj.original.recetaMenu.componenteId,
                            cantidadBase = aj.original.recetaMenu.cantidadBase,
                            accion = "QUITAR"
                        )
                        when (aj.tipo) {
                            com.toppis.app.ui.pos.TipoAjuste.QUITAR -> listOf(devolver)
                            com.toppis.app.ui.pos.TipoAjuste.CAMBIAR -> listOfNotNull(
                                devolver,
                                aj.reemplazo?.let {
                                    com.toppis.app.data.repository.ModComponenteVenta(
                                        tipo = "ARTICULO",
                                        componenteId = it.id,
                                        cantidadBase = aj.cantidadReemplazo,
                                        accion = "AGREGAR"
                                    )
                                }
                            )
                        }
                    }
                    LineaVenta(
                        itemMenuId = item.itemMenu.id,
                        cantidad = item.cantidad,
                        precioUnitario = item.precioUnitario,
                        subtotal = item.subtotal,
                        salsas = item.salsasTexto,
                        costoUnitario = item.costoUnitario,
                        modificadores = item.modificadoresTexto,
                        promocionId = item.promocionId,
                        modsComponentes = modsComp + salsasComp + ajustesComp
                    )
                }

                // Líneas para construir textos (con nombre). Modificadores se anexan al detalle.
                val lineasComanda = carritoActual.map { item ->
                    val detalleExtra = listOfNotNull(
                        item.modificadoresTexto.takeIf { it.isNotBlank() },
                        item.salsasTexto.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    LineaComanda(
                        nombre = item.itemMenu.nombre,
                        cantidad = item.cantidad,
                        subtotal = item.subtotal,
                        salsas = detalleExtra
                    )
                }

                // El id real se conoce tras insertar; construimos textos con placeholder
                // y luego reconstruimos con el id devuelto.
                val comandaTmp = comandaRepository.buildComandaTexto(0, lineasComanda, zonaEnvio, total)

                val ventaId = ventaRepository.registrarVentaMenu(
                    items = lineasVenta,
                    metodoPago = metodoPago,
                    sobreId = sobreId,
                    usuarioId = usuarioId,
                    zonaEnvio = zonaEnvio,
                    comandaTexto = comandaTmp
                )

                // Textos finales con el id correcto
                val comandaTexto = comandaRepository.buildComandaTexto(ventaId, lineasComanda, zonaEnvio, total)
                val whatsappTexto = comandaRepository.buildWhatsApp(ventaId, lineasComanda, zonaEnvio, total)

                limpiarCarrito()
                _uiState.value = PosUiState.VentaExitosa(
                    ventaId = ventaId,
                    comandaTexto = comandaTexto,
                    whatsappTexto = whatsappTexto,
                    lineas = lineasComanda,
                    zonaEnvio = zonaEnvio,
                    total = total
                )
            } catch (e: Exception) {
                _uiState.value = PosUiState.Error(e.message ?: "Error al procesar la venta")
            }
        }
    }

    fun limpiarCarrito() {
        _carrito.value = emptyList()
        _totalCarrito.value = 0.0
    }

    fun resetState() {
        _uiState.value = PosUiState.Idle
    }
}

