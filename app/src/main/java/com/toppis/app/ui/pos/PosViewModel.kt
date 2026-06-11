package com.toppis.app.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Comprobante
import com.toppis.app.data.db.entities.MetodoPago
import com.toppis.app.data.models.Salsa
import com.toppis.app.data.db.entities.ZonaEnvio
import com.toppis.app.data.repository.ComandaRepository
import com.toppis.app.data.repository.ComprobanteRepository
import com.toppis.app.data.repository.LineaComanda
import com.toppis.app.data.repository.LineaVenta
import com.toppis.app.data.repository.MenuRepository
import com.toppis.app.data.repository.SobreRepository
import com.toppis.app.data.repository.VentaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ItemCarritoMenu(
    val itemMenu: ItemMenu,
    val cantidad: Int,
    val salsas: List<String> = emptyList()
) {
    val subtotal: Double get() = itemMenu.precio * cantidad
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
    private val comprobanteRepository: ComprobanteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PosUiState>(PosUiState.Idle)
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val _itemsMenu = MutableStateFlow<List<ItemMenu>>(emptyList())
    val itemsMenu: StateFlow<List<ItemMenu>> = _itemsMenu.asStateFlow()

    private val _salsasDisponibles = MutableStateFlow<List<Salsa>>(emptyList())
    val salsasDisponibles: StateFlow<List<Salsa>> = _salsasDisponibles.asStateFlow()

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
        // Realtime: el menú y las salsas se actualizan al instante
        viewModelScope.launch { menuRepository.observeItemsMenu().collect { refrescarMenu() } }
        viewModelScope.launch { menuRepository.observeSalsas().collect { refrescarSalsas() } }
    }

    private fun refrescarMenu() {
        viewModelScope.launch { _itemsMenu.value = menuRepository.getItemsMenuActivos() }
    }

    private fun refrescarSalsas() {
        viewModelScope.launch { _salsasDisponibles.value = menuRepository.getSalsasActivas() }
    }

    private val _carrito = MutableStateFlow<List<ItemCarritoMenu>>(emptyList())
    val carrito: StateFlow<List<ItemCarritoMenu>> = _carrito.asStateFlow()

    private val _totalCarrito = MutableStateFlow(0.0)
    val totalCarrito: StateFlow<Double> = _totalCarrito.asStateFlow()

    private fun calcularTotal(items: List<ItemCarritoMenu>) {
        _totalCarrito.value = items.sumOf { it.subtotal }
    }

    fun agregarAlCarrito(itemMenu: ItemMenu, salsas: List<String>) {
        val current = _carrito.value.toMutableList()
        // Si el mismo item con las mismas salsas ya existe, incrementar cantidad
        val index = current.indexOfFirst {
            it.itemMenu.id == itemMenu.id && it.salsas == salsas
        }
        if (index != -1) {
            current[index] = current[index].copy(cantidad = current[index].cantidad + 1)
        } else {
            current.add(ItemCarritoMenu(itemMenu, 1, salsas))
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
                    LineaVenta(
                        itemMenuId = item.itemMenu.id,
                        cantidad = item.cantidad,
                        precioUnitario = item.itemMenu.precio,
                        subtotal = item.subtotal,
                        salsas = item.salsas.joinToString(", ")
                    )
                }

                // Líneas para construir textos (con nombre)
                val lineasComanda = carritoActual.map { item ->
                    LineaComanda(
                        nombre = item.itemMenu.nombre,
                        cantidad = item.cantidad,
                        subtotal = item.subtotal,
                        salsas = item.salsas.joinToString(", ")
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

