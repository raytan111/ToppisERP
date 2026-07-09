package com.toppis.app.ui.costos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.db.entities.Periodicidad
import com.toppis.app.data.models.CostoFijo
import com.toppis.app.data.repository.CostoFijoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CostoFijoUiState {
    object Idle : CostoFijoUiState()
    data class Error(val message: String) : CostoFijoUiState()
    object Success : CostoFijoUiState()
}

class CostoFijoViewModel(
    private val repository: CostoFijoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CostoFijoUiState>(CostoFijoUiState.Idle)
    val uiState: StateFlow<CostoFijoUiState> = _uiState.asStateFlow()

    private val _costos = MutableStateFlow<List<CostoFijo>>(emptyList())
    val costos: StateFlow<List<CostoFijo>> = _costos.asStateFlow()

    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init { refrescar() }

    fun refrescar() {
        viewModelScope.launch {
            _costos.value = repository.getCostosFijos()
            _cargandoInicial.value = false
        }
    }

    fun crear(nombre: String, categoria: CategoriaGasto, monto: Double, periodicidad: Periodicidad) {
        viewModelScope.launch {
            try {
                repository.crear(nombre, categoria, monto, periodicidad)
                refrescar(); _uiState.value = CostoFijoUiState.Success
            } catch (e: Exception) {
                _uiState.value = CostoFijoUiState.Error(e.message ?: "Error al crear el costo fijo")
            }
        }
    }

    fun actualizar(costo: CostoFijo) {
        viewModelScope.launch {
            try {
                repository.actualizar(costo)
                refrescar(); _uiState.value = CostoFijoUiState.Success
            } catch (e: Exception) {
                _uiState.value = CostoFijoUiState.Error(e.message ?: "Error al actualizar")
            }
        }
    }

    fun eliminar(id: Int) {
        viewModelScope.launch {
            try {
                repository.eliminar(id); refrescar()
            } catch (e: Exception) {
                _uiState.value = CostoFijoUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun resetState() { _uiState.value = CostoFijoUiState.Idle }
}

class CostoFijoViewModelFactory(
    private val repository: CostoFijoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CostoFijoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CostoFijoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
