package com.toppis.app.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Ingrediente
import com.toppis.app.data.models.Insumo
import com.toppis.app.data.repository.InventarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InventarioUiState {
    object Loading : InventarioUiState()
    object Success : InventarioUiState()
    data class Error(val message: String) : InventarioUiState()
}

class InventarioViewModel(private val repository: InventarioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<InventarioUiState>(InventarioUiState.Success)
    val uiState: StateFlow<InventarioUiState> = _uiState.asStateFlow()

    private val _insumos = MutableStateFlow<List<Insumo>>(emptyList())
    val insumos: StateFlow<List<Insumo>> = _insumos.asStateFlow()

    private val _ingredientes = MutableStateFlow<List<Ingrediente>>(emptyList())
    val ingredientes: StateFlow<List<Ingrediente>> = _ingredientes.asStateFlow()

    init {
        refrescarInsumos()
        refrescarIngredientes()
        // Observadores Realtime
        viewModelScope.launch {
            repository.observeInsumos().collect { refrescarInsumos() }
        }
        viewModelScope.launch {
            repository.observeIngredientes().collect { refrescarIngredientes() }
        }
    }

    private fun refrescarInsumos() {
        viewModelScope.launch { _insumos.value = repository.getInsumos() }
    }

    private fun refrescarIngredientes() {
        viewModelScope.launch { _ingredientes.value = repository.getIngredientes() }
    }

    fun crearInsumo(nombre: String, descripcion: String, precio: Double, unidadMedida: String, stockInicial: Int = 0) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.crearInsumo(nombre, descripcion, precio, unidadMedida, stockInicial)
                refrescarInsumos()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al crear insumo")
            }
        }
    }

    fun crearIngrediente(
        nombre: String,
        unidadMedida: String,
        stockActual: Double,
        costoUnitario: Double,
        costoCompra: Double = 0.0,
        porcentajeMerma: Double = 0.0,
        unidadCompra: String = "",
        cantidadComprada: Double = 0.0,
        cantidadAprovechable: Double = 0.0,
        costoGramo: Double = 0.0
    ) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.crearIngrediente(
                    nombre, unidadMedida, stockActual, costoUnitario,
                    costoCompra, porcentajeMerma, unidadCompra, cantidadComprada, cantidadAprovechable, costoGramo
                )
                refrescarIngredientes()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al crear ingrediente")
            }
        }
    }

    fun actualizarStock(ingredienteId: Int, nuevoStock: Double) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.actualizarStockIngrediente(ingredienteId, nuevoStock)
                refrescarIngredientes()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al actualizar stock")
            }
        }
    }

    fun editarInsumo(insumo: Insumo) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.actualizarInsumo(insumo)
                refrescarInsumos()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al editar insumo")
            }
        }
    }

    fun eliminarInsumo(insumoId: Int) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.eliminarInsumo(insumoId)
                refrescarInsumos()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al eliminar insumo")
            }
        }
    }

    fun editarIngrediente(ingrediente: Ingrediente) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.actualizarIngrediente(ingrediente)
                refrescarIngredientes()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al editar ingrediente")
            }
        }
    }

    fun eliminarIngrediente(ingredienteId: Int) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.eliminarIngrediente(ingredienteId)
                refrescarIngredientes()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al eliminar ingrediente")
            }
        }
    }
}
