package com.toppis.app.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Cliente
import com.toppis.app.data.repository.ClienteRepository
import com.toppis.app.data.repository.PedidoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Resumen de un cliente para la lista: pedidos totales y si tiene deuda. */
data class ClienteResumen(
    val cliente: Cliente,
    val pedidos: Int,
    val deuda: Boolean
)

class ClientesViewModel(
    private val clienteRepo: ClienteRepository,
    private val pedidoRepo: PedidoRepository
) : ViewModel() {

    private val _clientes = MutableStateFlow<List<ClienteResumen>>(emptyList())
    val clientes: StateFlow<List<ClienteResumen>> = _clientes.asStateFlow()

    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            try {
                val clientes = clienteRepo.getClientes()
                val pedidos = pedidoRepo.getTodosPedidos().groupBy { it.clienteId }
                _clientes.value = clientes.map { c ->
                    val ped = pedidos[c.id].orEmpty()
                    ClienteResumen(
                        cliente = c,
                        pedidos = ped.size,
                        deuda = ped.any { it.tieneDeuda }
                    )
                }.sortedByDescending { it.deuda }
            } catch (_: Exception) {
            } finally {
                _cargandoInicial.value = false
            }
        }
    }

    fun actualizarNombre(id: Int, nombre: String) {
        viewModelScope.launch {
            try { clienteRepo.actualizarNombre(id, nombre); cargar() } catch (_: Exception) {}
        }
    }

    fun actualizarTelefono3(id: Int, telefono3: String) {
        viewModelScope.launch {
            try { clienteRepo.actualizarTelefono3(id, telefono3); cargar() } catch (_: Exception) {}
        }
    }

    fun fijarSellos(id: Int, sellos: Int) {
        viewModelScope.launch {
            try { clienteRepo.fijarSellos(id, sellos); cargar() } catch (_: Exception) {}
        }
    }

    fun eliminar(id: Int) {
        viewModelScope.launch {
            try { clienteRepo.eliminar(id); cargar() } catch (_: Exception) {}
        }
    }
}

class ClientesViewModelFactory(
    private val clienteRepo: ClienteRepository,
    private val pedidoRepo: PedidoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientesViewModel(clienteRepo, pedidoRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
