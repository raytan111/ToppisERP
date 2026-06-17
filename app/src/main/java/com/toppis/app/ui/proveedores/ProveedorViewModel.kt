package com.toppis.app.ui.proveedores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Proveedor
import com.toppis.app.data.repository.ProveedorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProveedorUiState {
    object Idle : ProveedorUiState()
    data class Error(val message: String) : ProveedorUiState()
    object Success : ProveedorUiState()
}

class ProveedorViewModel(
    private val repository: ProveedorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProveedorUiState>(ProveedorUiState.Idle)
    val uiState: StateFlow<ProveedorUiState> = _uiState.asStateFlow()

    private val _proveedores = MutableStateFlow<List<Proveedor>>(emptyList())
    val proveedores: StateFlow<List<Proveedor>> = _proveedores.asStateFlow()

    init { refrescar() }

    private fun refrescar() {
        viewModelScope.launch { _proveedores.value = repository.getProveedores() }
    }

    fun crear(nombre: String, contacto: String, telefono: String, email: String, nota: String) {
        viewModelScope.launch {
            try {
                repository.crear(nombre, contacto, telefono, email, nota)
                refrescar(); _uiState.value = ProveedorUiState.Success
            } catch (e: Exception) {
                _uiState.value = ProveedorUiState.Error(e.message ?: "Error al crear proveedor")
            }
        }
    }

    fun actualizar(p: Proveedor) {
        viewModelScope.launch {
            try {
                repository.actualizar(p); refrescar()
            } catch (e: Exception) {
                _uiState.value = ProveedorUiState.Error(e.message ?: "Error al actualizar")
            }
        }
    }

    fun eliminar(id: Int) {
        viewModelScope.launch {
            try {
                repository.eliminar(id); refrescar()
            } catch (e: Exception) {
                _uiState.value = ProveedorUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun resetState() { _uiState.value = ProveedorUiState.Idle }
}
