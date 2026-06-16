package com.toppis.app.ui.mermas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.MotivoMerma
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Preparacion
import com.toppis.app.data.repository.MermaConNombre
import com.toppis.app.data.repository.MermaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MermaUiState {
    object Idle : MermaUiState()
    data class Error(val message: String) : MermaUiState()
    object Success : MermaUiState()
}

class MermaViewModel(
    private val repository: MermaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MermaUiState>(MermaUiState.Idle)
    val uiState: StateFlow<MermaUiState> = _uiState.asStateFlow()

    private val _mermas = MutableStateFlow<List<MermaConNombre>>(emptyList())
    val mermas: StateFlow<List<MermaConNombre>> = _mermas.asStateFlow()

    private val _articulos = MutableStateFlow<List<Articulo>>(emptyList())
    val articulos: StateFlow<List<Articulo>> = _articulos.asStateFlow()

    private val _preparaciones = MutableStateFlow<List<Preparacion>>(emptyList())
    val preparaciones: StateFlow<List<Preparacion>> = _preparaciones.asStateFlow()

    init {
        refrescar()
        viewModelScope.launch { _articulos.value = repository.getArticulos() }
        viewModelScope.launch { _preparaciones.value = repository.getPreparaciones() }
        viewModelScope.launch { repository.observeMermas().collect { refrescar() } }
    }

    private fun refrescar() {
        viewModelScope.launch { _mermas.value = repository.getMermasConNombre() }
    }

    fun registrarMerma(
        tipo: TipoComponente,
        componenteId: Int,
        cantidadBase: Double,
        motivo: MotivoMerma,
        nota: String,
        usuarioId: String?
    ) {
        viewModelScope.launch {
            try {
                repository.registrarMerma(tipo, componenteId, cantidadBase, motivo, nota, usuarioId)
                refrescar()
                _uiState.value = MermaUiState.Success
            } catch (e: Exception) {
                _uiState.value = MermaUiState.Error(e.message ?: "Error al registrar merma")
            }
        }
    }

    fun eliminarMerma(id: Int) {
        viewModelScope.launch {
            try {
                repository.eliminarMerma(id)
                refrescar()
            } catch (e: Exception) {
                _uiState.value = MermaUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun resetState() { _uiState.value = MermaUiState.Idle }
}
