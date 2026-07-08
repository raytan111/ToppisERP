package com.toppis.app.ui.conteos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Conteo
import com.toppis.app.data.repository.ConteoRepository
import com.toppis.app.data.repository.LineaConteo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConteoUiState {
    object Idle : ConteoUiState()
    data class Error(val message: String) : ConteoUiState()
    object Success : ConteoUiState()
}

class ConteoViewModel(
    private val repository: ConteoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConteoUiState>(ConteoUiState.Idle)
    val uiState: StateFlow<ConteoUiState> = _uiState.asStateFlow()

    private val _conteos = MutableStateFlow<List<Conteo>>(emptyList())
    val conteos: StateFlow<List<Conteo>> = _conteos.asStateFlow()

    private val _articulos = MutableStateFlow<List<Articulo>>(emptyList())
    val articulos: StateFlow<List<Articulo>> = _articulos.asStateFlow()

    /** true mientras se hace la primera carga (para mostrar skeleton). */
    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init {
        refrescar()
        refrescarArticulos()
    }

    private fun refrescar() {
        viewModelScope.launch {
            _conteos.value = repository.getConteos()
            _cargandoInicial.value = false
        }
    }

    fun refrescarArticulos() {
        viewModelScope.launch { _articulos.value = repository.getArticulos() }
    }

    fun guardarConteo(lineas: List<LineaConteo>, nota: String, usuarioId: String?, cerrar: Boolean) {
        viewModelScope.launch {
            try {
                repository.crearConteo(lineas, nota, usuarioId, cerrar)
                refrescar()
                refrescarArticulos()
                _uiState.value = ConteoUiState.Success
            } catch (e: Exception) {
                _uiState.value = ConteoUiState.Error(e.message ?: "Error al guardar conteo")
            }
        }
    }

    fun eliminarConteo(id: Int) {
        viewModelScope.launch {
            try {
                repository.eliminarConteo(id)
                refrescar()
            } catch (e: Exception) {
                _uiState.value = ConteoUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun resetState() { _uiState.value = ConteoUiState.Idle }
}
