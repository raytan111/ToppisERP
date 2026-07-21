package com.toppis.app.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.TipoLineaPedido
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Modificador
import com.toppis.app.data.models.Pedido
import com.toppis.app.data.models.PedidoItem
import com.toppis.app.data.models.Promocion
import com.toppis.app.data.repository.MenuRepository
import com.toppis.app.data.repository.ModificadorRepository
import com.toppis.app.data.repository.PedidoRepository
import com.toppis.app.data.repository.PromocionRepository
import com.toppis.app.domain.pos.PosCalculos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CarritoUiState {
    object Idle : CarritoUiState()
    data class Error(val message: String) : CarritoUiState()
}

/** Línea del carrito lista para mostrar (título + detalle + subtotal). */
data class CarritoLinea(
    val item: PedidoItem,
    val titulo: String,
    val detalle: String,
    val subtotal: Double
)

/** Producto elegido para un espacio de una promo, con sus modificadores y comentario. */
data class EleccionPromo(
    val itemMenuId: Int,
    val modIds: List<Int>,
    val comentario: String?
)

class CarritoViewModel(
    private val pedidoRepo: PedidoRepository,
    private val menuRepo: MenuRepository,
    private val modificadorRepo: ModificadorRepository,
    private val promocionRepo: PromocionRepository,
    private val sobreRepo: com.toppis.app.data.repository.SobreRepository,
    private val clienteRepo: com.toppis.app.data.repository.ClienteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CarritoUiState>(CarritoUiState.Idle)
    val uiState: StateFlow<CarritoUiState> = _uiState.asStateFlow()

    private val _pedido = MutableStateFlow<Pedido?>(null)
    val pedido: StateFlow<Pedido?> = _pedido.asStateFlow()

    private val _menu = MutableStateFlow<List<ItemMenu>>(emptyList())
    val menu: StateFlow<List<ItemMenu>> = _menu.asStateFlow()

    private val _promos = MutableStateFlow<List<Promocion>>(emptyList())
    val promos: StateFlow<List<Promocion>> = _promos.asStateFlow()

    private val _lineas = MutableStateFlow<List<CarritoLinea>>(emptyList())
    val lineas: StateFlow<List<CarritoLinea>> = _lineas.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _sobresCuenta = MutableStateFlow<List<com.toppis.app.data.models.Sobre>>(emptyList())
    val sobresCuenta: StateFlow<List<com.toppis.app.data.models.Sobre>> = _sobresCuenta.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _cliente = MutableStateFlow<com.toppis.app.data.models.Cliente?>(null)
    val cliente: StateFlow<com.toppis.app.data.models.Cliente?> = _cliente.asStateFlow()

    /** El cliente tiene cupón disponible (≥ 6 sellos). */
    private val _puedeRegalar = MutableStateFlow(false)
    val puedeRegalar: StateFlow<Boolean> = _puedeRegalar.asStateFlow()

    private var modificadores: List<Modificador> = emptyList()
    private var pedidoId: Int = 0

    fun cargar(id: Int) {
        pedidoId = id
        viewModelScope.launch {
            _cargando.value = true
            try {
                // Catálogo desde caché en memoria (evita repetir peticiones al abrir pedidos).
                if (_menu.value.isEmpty()) _menu.value = com.toppis.app.data.repository.PosCache.menu()
                if (_promos.value.isEmpty()) _promos.value = com.toppis.app.data.repository.PosCache.promos()
                if (modificadores.isEmpty()) modificadores = com.toppis.app.data.repository.PosCache.modificadores()
                if (_sobresCuenta.value.isEmpty()) _sobresCuenta.value = com.toppis.app.data.repository.PosCache.sobresCuenta()
                _pedido.value = pedidoRepo.getPedido(id)
                cargarCliente()
                recargarLineas()
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al cargar el pedido")
            } finally {
                _cargando.value = false
            }
        }
    }

    private suspend fun recargarLineas() {
        val items = pedidoRepo.getItems(pedidoId)
        val menuById = _menu.value.associateBy { it.id }
        val promoById = _promos.value.associateBy { it.id }
        val modById = modificadores.associateBy { it.id }
        val lineas = items.map { pi ->
            val unidades = pedidoRepo.getUnidades(pi.id)
            // Precalcular mods por unidad (suspend) para no llamar red dentro de lambdas.
            val modsPorUnidad = unidades.associate { u ->
                u.id to pedidoRepo.getMods(u.id).mapNotNull { m -> modById[m.modificadorId]?.nombre }
            }
            val titulo = when (pi.tipo) {
                TipoLineaPedido.PROMO -> promoById[pi.promocionId]?.nombre ?: "Promoción"
                TipoLineaPedido.PRODUCTO -> menuById[pi.itemMenuId]?.nombre ?: "Producto"
            }
            val detalle = when (pi.tipo) {
                TipoLineaPedido.PRODUCTO -> {
                    val u = unidades.firstOrNull()
                    if (u == null) "" else {
                        val mods = modsPorUnidad[u.id].orEmpty()
                        listOfNotNull(
                            if (mods.isNotEmpty()) "+ ${mods.joinToString(", ")}" else null,
                            u.comentario?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                    }
                }
                TipoLineaPedido.PROMO -> unidades.joinToString(" · ") { u ->
                    val nombreProd = menuById[u.itemMenuId]?.nombre ?: "Producto"
                    val mods = modsPorUnidad[u.id].orEmpty()
                    buildString {
                        append(nombreProd)
                        if (mods.isNotEmpty()) append(" (+${mods.joinToString(", ")})")
                        u.comentario?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                    }
                }
            }
            CarritoLinea(pi, if (pi.esRegalo) "🎁 $titulo" else titulo, detalle, pi.subtotal)
        }
        _lineas.value = lineas
    }

    /** Modificadores aplicables a un producto (por categoría + puntuales). */
    fun modificadoresDe(item: ItemMenu): List<Modificador> =
        PosCalculos.modificadoresAplicables(item.id, item.categoria, modificadores)

    /** Modificadores aplicables a un producto por su id de menú (para promos). */
    fun modificadoresDeId(itemMenuId: Int): List<Modificador> =
        _menu.value.firstOrNull { it.id == itemMenuId }?.let { modificadoresDe(it) } ?: emptyList()

    fun itemMenuPorId(id: Int): ItemMenu? = _menu.value.firstOrNull { it.id == id }

    /**
     * Carga los espacios de una promo y, por cada uno, la lista de productos elegibles
     * (por lista específica o por categoría).
     */
    fun cargarEspaciosPromo(
        promo: Promocion,
        onLoaded: (List<com.toppis.app.data.models.PromocionEspacio>, Map<Int, List<ItemMenu>>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val espacios = promocionRepo.getEspacios(promo.id)
                val elegibles = espacios.associate { esp ->
                    val ops = promocionRepo.getOpciones(esp.id)
                    esp.id to PosCalculos.elegiblesEspacio(esp, ops, _menu.value)
                }
                onLoaded(espacios, elegibles)
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al cargar la promo")
            }
        }
    }

    /** Agrega una promo (precio fijo) con los productos elegidos por espacio. */
    fun agregarPromo(promo: Promocion, elecciones: List<EleccionPromo>) {
        viewModelScope.launch {
            try {
                val itemId = pedidoRepo.agregarItem(
                    pedidoId, TipoLineaPedido.PROMO, null, promo.id,
                    cantidad = 1, precioUnitario = promo.precio, subtotal = promo.precio
                )
                elecciones.forEach { e ->
                    val unidadId = pedidoRepo.agregarUnidad(itemId, e.itemMenuId, e.comentario)
                    e.modIds.forEach { pedidoRepo.agregarMod(unidadId, it) }
                }
                refrescarTotales()
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al agregar la promo")
            }
        }
    }

    /** Precio resultante de un producto con los modificadores elegidos. */
    fun precioConMods(item: ItemMenu, modIds: List<Int>): Double =
        PosCalculos.precioProducto(item.precio, modificadores.filter { it.id in modIds }.map { it.deltaPrecio })

    private suspend fun cargarCliente() {
        val cid = _pedido.value?.clienteId
        val c = if (cid != null) clienteRepo.getClientes().firstOrNull { it.id == cid } else null
        _cliente.value = c
        _puedeRegalar.value = c != null && PosCalculos.puedeRegalar(c.sellosHamburguesa)
    }

    /** Agrega un producto al carrito con sus modificadores y comentario. */
    fun agregarProducto(item: ItemMenu, modIds: List<Int>, comentario: String?, esRegalo: Boolean = false) {
        viewModelScope.launch {
            try {
                val precio = if (esRegalo) PosCalculos.precioRegalo() else precioConMods(item, modIds)
                val itemId = pedidoRepo.agregarItem(
                    pedidoId, TipoLineaPedido.PRODUCTO, item.id, null,
                    cantidad = 1, precioUnitario = precio, subtotal = precio, esRegalo = esRegalo
                )
                val unidadId = pedidoRepo.agregarUnidad(itemId, item.id, comentario)
                if (!esRegalo) modIds.forEach { pedidoRepo.agregarMod(unidadId, it) }
                refrescarTotales()
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al agregar el producto")
            }
        }
    }

    fun cambiarCantidad(linea: CarritoLinea, nuevaCantidad: Int) {
        if (nuevaCantidad < 1) { quitarLinea(linea); return }
        viewModelScope.launch {
            try {
                val precioUnit = linea.item.precioUnitario
                pedidoRepo.actualizarCantidadItem(linea.item.id, nuevaCantidad, precioUnit * nuevaCantidad)
                refrescarTotales()
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al cambiar la cantidad")
            }
        }
    }

    fun quitarLinea(linea: CarritoLinea) {
        viewModelScope.launch {
            try {
                pedidoRepo.quitarItem(linea.item.id)
                refrescarTotales()
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al quitar la línea")
            }
        }
    }

    /** Fija la zona de envío y recalcula el total. */
    fun setEnvio(zona: com.toppis.app.data.db.entities.ZonaEnvio) {
        viewModelScope.launch {
            try {
                val total = PosCalculos.totalPedido(_lineas.value.map { it.subtotal }, zona.precio)
                pedidoRepo.actualizarTotales(pedidoId, total, zona.name, zona.precio)
                _pedido.value = pedidoRepo.getPedido(pedidoId)
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al fijar el envío")
            }
        }
    }

    private suspend fun refrescarTotales() {
        recargarLineas()
        val ped = _pedido.value
        val envio = ped?.montoEnvio ?: 0.0
        val total = PosCalculos.totalPedido(_lineas.value.map { it.subtotal }, envio)
        pedidoRepo.actualizarTotales(pedidoId, total, ped?.zonaEnvio ?: "SIN_ENVIO", envio)
        _pedido.value = pedidoRepo.getPedido(pedidoId)
    }

    /** Texto de comanda para cocina, armado desde las líneas del carrito. */
    private fun construirComanda(): String = buildString {
        appendLine("PEDIDO #$pedidoId")
        appendLine("─".repeat(28))
        _lineas.value.forEach { l ->
            appendLine("${l.item.cantidad}x ${l.titulo}")
            if (l.detalle.isNotBlank()) appendLine("   ${l.detalle}")
        }
        appendLine("─".repeat(28))
        appendLine("TOTAL: ${java.text.DecimalFormat("$#,##0").format(_pedido.value?.total ?: 0.0)}")
    }

    /** Cierra el pedido y emite la comanda. */
    fun cerrar() {
        viewModelScope.launch {
            try {
                pedidoRepo.cerrarPedido(pedidoId, construirComanda())
                _pedido.value = pedidoRepo.getPedido(pedidoId)
                _mensaje.value = "Comanda enviada a cocina"
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al cerrar el pedido")
            }
        }
    }

    /** Reabre el pedido cerrado (si no está pagado) para volver a editarlo. */
    fun reabrir() {
        viewModelScope.launch {
            try {
                if (_pedido.value?.pagado == true) {
                    _uiState.value = CarritoUiState.Error("Un pedido pagado no se puede reabrir.")
                    return@launch
                }
                pedidoRepo.reabrirPedido(pedidoId)
                _pedido.value = pedidoRepo.getPedido(pedidoId)
                _mensaje.value = "Pedido reabierto"
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al reabrir el pedido")
            }
        }
    }

    /** Cobra el pedido: crea la venta, descuenta stock e ingresa al sobre. */
    fun pagar(metodo: com.toppis.app.data.db.entities.MetodoPago, sobreId: Int, usuarioId: String?) {
        viewModelScope.launch {
            try {
                pedidoRepo.pagarPedido(pedidoId, metodo.name, sobreId, usuarioId)
                aplicarCuponera()
                _pedido.value = pedidoRepo.getPedido(pedidoId)
                _mensaje.value = "Pedido pagado"
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al pagar")
            }
        }
    }

    /** Marca el pedido como entregado. Al terminar invoca [onDone] (para cerrar el POS). */
    fun entregar(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                pedidoRepo.marcarEntregado(pedidoId)
                _pedido.value = pedidoRepo.getPedido(pedidoId)
                _mensaje.value = "Pedido entregado"
                onDone()
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al marcar entregado")
            }
        }
    }

    /** Elimina el pedido (solo si no está pagado). Al terminar invoca [onDone]. */
    fun eliminarPedido(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (_pedido.value?.pagado == true) {
                    _uiState.value = CarritoUiState.Error("No se puede eliminar un pedido ya pagado.")
                    return@launch
                }
                pedidoRepo.eliminarPedido(pedidoId)
                onDone()
            } catch (e: Exception) {
                _uiState.value = CarritoUiState.Error(e.message ?: "Error al eliminar el pedido")
            }
        }
    }



    /** Actualiza los sellos del cliente: +1 si el pedido trae hamburguesa; −6 si usó regalo. */
    private suspend fun aplicarCuponera() {
        val c = _cliente.value ?: return
        val menuById = _menu.value.associateBy { it.id }
        val items = pedidoRepo.getItems(pedidoId)
        var incluyeHamburguesa = false
        var huboRegalo = false
        for (it in items) {
            if (it.esRegalo) { huboRegalo = true; continue }
            val unidades = pedidoRepo.getUnidades(it.id)
            if (unidades.any { u -> menuById[u.itemMenuId]?.categoria.equals("Hamburguesas", ignoreCase = true) }) {
                incluyeHamburguesa = true
            }
        }
        var nuevos = PosCalculos.sellosTrasPedido(c.sellosHamburguesa, incluyeHamburguesa)
        if (huboRegalo) nuevos = PosCalculos.sellosTrasRegalo(nuevos)
        if (nuevos != c.sellosHamburguesa) {
            clienteRepo.fijarSellos(c.id, nuevos)
            _cliente.value = c.copy(sellosHamburguesa = nuevos)
            _puedeRegalar.value = PosCalculos.puedeRegalar(nuevos)
        }
    }

    fun resetMensaje() { _mensaje.value = null }

    fun resetState() { _uiState.value = CarritoUiState.Idle }
}

class CarritoViewModelFactory(
    private val pedidoRepo: PedidoRepository,
    private val menuRepo: MenuRepository,
    private val modificadorRepo: ModificadorRepository,
    private val promocionRepo: PromocionRepository,
    private val sobreRepo: com.toppis.app.data.repository.SobreRepository,
    private val clienteRepo: com.toppis.app.data.repository.ClienteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarritoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarritoViewModel(pedidoRepo, menuRepo, modificadorRepo, promocionRepo, sobreRepo, clienteRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
