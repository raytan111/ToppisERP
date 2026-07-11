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

/**
 * ViewModel de la pantalla de cocina (KDS): comandas pendientes (pedidos CERRADO no
 * entregados), en tiempo real. Permite marcar entregado.
 */
class ComandasViewModel(
    private val pedidoRepo: PedidoRepository,
    private val clienteRepo: ClienteRepository
) : ViewModel() {

    private val _comandas = MutableStateFlow<List<Pedido>>(emptyList())
    val comandas: StateFlow<List<Pedido>> = _comandas.asStateFlow()

    private val _clientesById = MutableStateFlow<Map<Int, Cliente>>(emptyMap())
    val clientesById: StateFlow<Map<Int, Cliente>> = _clientesById.asStateFlow()

    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init {
        cargar()
        viewModelScope.launch { pedidoRepo.observeCambios().collect { cargar() } }
    }

    fun cargar() {
        viewModelScope.launch {
            try {
                _clientesById.value = clienteRepo.getClientes().associateBy { it.id }
                _comandas.value = pedidoRepo.getComandasPendientes()
            } catch (_: Exception) {
            } finally {
                _cargandoInicial.value = false
            }
        }
    }

    fun clienteDe(pedido: Pedido): Cliente? = pedido.clienteId?.let { _clientesById.value[it] }

    fun marcarEntregado(pedidoId: Int) {
        viewModelScope.launch {
            try {
                pedidoRepo.marcarEntregado(pedidoId)
                cargar()
            } catch (_: Exception) {}
        }
    }
}

class ComandasViewModelFactory(
    private val pedidoRepo: PedidoRepository,
    private val clienteRepo: ClienteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ComandasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ComandasViewModel(pedidoRepo, clienteRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
