package com.toppis.app.ui.modificadores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.TipoModificador
import com.toppis.app.data.models.Modificador
import com.toppis.app.data.repository.ModificadorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ModificadorUiState {
    object Idle : ModificadorUiState()
    data class Error(val message: String) : ModificadorUiState()
    object Success : ModificadorUiState()
}

class ModificadorViewModel(
    private val modificadorRepository: ModificadorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ModificadorUiState>(ModificadorUiState.Idle)
    val uiState: StateFlow<ModificadorUiState> = _uiState.asStateFlow()

    private val _modificadores = MutableStateFlow<List<Modificador>>(emptyList())
    val modificadores: StateFlow<List<Modificador>> = _modificadores.asStateFlow()

    init {
        refrescar()
    }

    private fun refrescar() {
        viewModelScope.launch {
            try {
                _modificadores.value = modificadorRepository.getModificadores()
            } catch (e: Exception) {
                _uiState.value = ModificadorUiState.Error(e.message ?: "Error al cargar modificadores")
            }
        }
    }

    fun crearModificador(
        nombre: String,
        tipo: TipoModificador,
        deltaPrecio: Double,
        itemMenuId: Int? = null
    ) {
        viewModelScope.launch {
            try {
                modificadorRepository.crearModificador(nombre, tipo, itemMenuId, deltaPrecio)
                refrescar()
                _uiState.value = ModificadorUiState.Success
            } catch (e: Exception) {
                _uiState.value = ModificadorUiState.Error(e.message ?: "Error al crear modificador")
            }
        }
    }

    fun actualizarModificador(mod: Modificador) {
        viewModelScope.launch {
            try {
                modificadorRepository.actualizarModificador(mod)
                refrescar()
                _uiState.value = ModificadorUiState.Success
            } catch (e: Exception) {
                _uiState.value = ModificadorUiState.Error(e.message ?: "Error al actualizar modificador")
            }
        }
    }

    fun eliminarModificador(id: Int) {
        viewModelScope.launch {
            try {
                modificadorRepository.eliminarModificador(id)
                refrescar()
                _uiState.value = ModificadorUiState.Success
            } catch (e: Exception) {
                _uiState.value = ModificadorUiState.Error(e.message ?: "Error al eliminar modificador")
            }
        }
    }

    fun resetState() {
        _uiState.value = ModificadorUiState.Idle
    }
}
