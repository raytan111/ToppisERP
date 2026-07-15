package com.toppis.app.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Cliente
import com.toppis.app.data.models.Pedido
import com.toppis.app.data.repository.ClienteRepository
import com.toppis.app.data.repository.PedidoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PedidosUiState {
    object Idle : PedidosUiState()
    data class Error(val message: String) : PedidosUiState()
}

/**
 * ViewModel de la lista de pedidos activos del POS. Los pedidos persisten en la nube y
 * se refrescan por Realtime.
 */
class PedidosViewModel(
    private val pedidoRepo: PedidoRepository,
    private val clienteRepo: ClienteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PedidosUiState>(PedidosUiState.Idle)
    val uiState: StateFlow<PedidosUiState> = _uiState.asStateFlow()

    private val _pedidos = MutableStateFlow<List<Pedido>>(emptyList())
    val pedidos: StateFlow<List<Pedido>> = _pedidos.asStateFlow()

    private val _clientesById = MutableStateFlow<Map<Int, Cliente>>(emptyMap())
    val clientesById: StateFlow<Map<Int, Cliente>> = _clientesById.asStateFlow()

    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            pedidoRepo.observeCambios().collect { cargar() }
        }
    }

    fun cargar() {
        viewModelScope.launch {
            try {
                _clientesById.value = clienteRepo.getClientes().associateBy { it.id }
                _pedidos.value = pedidoRepo.getPedidosActivos()
            } catch (e: Exception) {
                _uiState.value = PedidosUiState.Error(e.message ?: "Error al cargar pedidos")
            } finally {
                _cargandoInicial.value = false
            }
        }
    }

    /** Crea un pedido para un cliente NUEVO (o reusa si calza teléfono + nombre exactos). */
    fun crearPedido(telefono3: String, nombre: String?, onCreado: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val cliente = clienteRepo.obtenerOCrear(telefono3.trim(), nombre?.trim())
                val pedido = pedidoRepo.crearPedido(cliente.id)
                cargar()
                onCreado(pedido.id)
            } catch (e: Exception) {
                _uiState.value = PedidosUiState.Error(e.message ?: "Error al crear el pedido")
            }
        }
    }

    /** Crea un pedido para un cliente ya existente (seleccionado de la lista). */
    fun crearPedidoParaCliente(clienteId: Int, onCreado: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val pedido = pedidoRepo.crearPedido(clienteId)
                cargar()
                onCreado(pedido.id)
            } catch (e: Exception) {
                _uiState.value = PedidosUiState.Error(e.message ?: "Error al crear el pedido")
            }
        }
    }

    fun clienteDe(pedido: Pedido): Cliente? = pedido.clienteId?.let { _clientesById.value[it] }

    fun resetState() { _uiState.value = PedidosUiState.Idle }
}

class PedidosViewModelFactory(
    private val pedidoRepo: PedidoRepository,
    private val clienteRepo: ClienteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PedidosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PedidosViewModel(pedidoRepo, clienteRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
