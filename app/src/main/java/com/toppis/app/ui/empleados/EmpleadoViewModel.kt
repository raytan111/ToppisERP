package com.toppis.app.ui.empleados

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.TipoPago
import com.toppis.app.data.models.Empleado
import com.toppis.app.data.repository.EmpleadoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EmpleadoUiState {
    object Idle : EmpleadoUiState()
    data class Error(val message: String) : EmpleadoUiState()
    object Success : EmpleadoUiState()
}

class EmpleadoViewModel(
    private val repository: EmpleadoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EmpleadoUiState>(EmpleadoUiState.Idle)
    val uiState: StateFlow<EmpleadoUiState> = _uiState.asStateFlow()

    private val _empleados = MutableStateFlow<List<Empleado>>(emptyList())
    val empleados: StateFlow<List<Empleado>> = _empleados.asStateFlow()

    init { refrescar() }

    private fun refrescar() {
        viewModelScope.launch { _empleados.value = repository.getEmpleados() }
    }

    fun crear(nombre: String, cargo: String, tipoPago: TipoPago, monto: Double) {
        viewModelScope.launch {
            try { repository.crear(nombre, cargo, tipoPago, monto); refrescar(); _uiState.value = EmpleadoUiState.Success }
            catch (e: Exception) { _uiState.value = EmpleadoUiState.Error(e.message ?: "Error al crear empleado") }
        }
    }

    fun actualizar(e: Empleado) {
        viewModelScope.launch {
            try { repository.actualizar(e); refrescar() }
            catch (ex: Exception) { _uiState.value = EmpleadoUiState.Error(ex.message ?: "Error al actualizar") }
        }
    }

    fun eliminar(id: Int) {
        viewModelScope.launch {
            try { repository.eliminar(id); refrescar() }
            catch (e: Exception) { _uiState.value = EmpleadoUiState.Error(e.message ?: "Error al eliminar") }
        }
    }

    fun resetState() { _uiState.value = EmpleadoUiState.Idle }
}
