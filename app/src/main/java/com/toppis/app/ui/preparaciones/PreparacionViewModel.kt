package com.toppis.app.ui.preparaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.DimensionUnidad
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Preparacion
import com.toppis.app.data.models.PreparacionComponente
import com.toppis.app.data.repository.ComponentePreparacion
import com.toppis.app.data.repository.PreparacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PreparacionUiState {
    object Idle : PreparacionUiState()
    data class Error(val message: String) : PreparacionUiState()
    object Success : PreparacionUiState()
}

class PreparacionViewModel(
    private val preparacionRepository: PreparacionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PreparacionUiState>(PreparacionUiState.Idle)
    val uiState: StateFlow<PreparacionUiState> = _uiState.asStateFlow()

    private val _preparaciones = MutableStateFlow<List<Preparacion>>(emptyList())
    val preparaciones: StateFlow<List<Preparacion>> = _preparaciones.asStateFlow()

    private val _articulos = MutableStateFlow<List<Articulo>>(emptyList())
    val articulos: StateFlow<List<Articulo>> = _articulos.asStateFlow()

    /** true mientras se hace la primera carga (para mostrar skeleton). */
    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init {
        refrescarPreparaciones()
        refrescarArticulos()
        viewModelScope.launch {
            preparacionRepository.observePreparaciones().collect { refrescarPreparaciones() }
        }
    }

    private fun refrescarPreparaciones() {
        viewModelScope.launch {
            _preparaciones.value = preparacionRepository.getPreparaciones()
            _cargandoInicial.value = false
        }
    }

    private fun refrescarArticulos() {
        viewModelScope.launch { _articulos.value = preparacionRepository.getArticulos() }
    }

    // ── Preparacion CRUD ──────────────────────────────────────────────────────

    fun crearPreparacion(
        nombre: String,
        dimension: DimensionUnidad,
        rendimientoLote: Double,
        seleccionableEnPos: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                preparacionRepository.crearPreparacion(nombre, dimension, rendimientoLote, seleccionableEnPos)
                refrescarPreparaciones()
                _uiState.value = PreparacionUiState.Success
            } catch (e: Exception) {
                _uiState.value = PreparacionUiState.Error(e.message ?: "Error al crear preparación")
            }
        }
    }

    fun actualizarPreparacion(prep: Preparacion) {
        viewModelScope.launch {
            try {
                preparacionRepository.actualizarPreparacion(prep)
                refrescarPreparaciones()
            } catch (e: Exception) {
                _uiState.value = PreparacionUiState.Error(e.message ?: "Error al actualizar")
            }
        }
    }

    fun eliminarPreparacion(id: Int) {
        viewModelScope.launch {
            try {
                preparacionRepository.eliminarPreparacion(id)
                refrescarPreparaciones()
            } catch (e: Exception) {
                _uiState.value = PreparacionUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    // ── Componentes de la preparación ─────────────────────────────────────────

    fun loadComponentes(prepId: Int, callback: (List<ComponentePreparacion>) -> Unit) {
        viewModelScope.launch {
            try {
                callback(preparacionRepository.getComponentes(prepId))
            } catch (e: Exception) {
                _uiState.value = PreparacionUiState.Error(e.message ?: "Error al cargar componentes")
            }
        }
    }

    fun agregarComponente(
        prepId: Int,
        tipo: TipoComponente,
        compId: Int,
        cantidad: Double,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                preparacionRepository.agregarComponente(prepId, tipo, compId, cantidad)
                refrescarPreparaciones()
                onDone()
            } catch (e: Exception) {
                _uiState.value = PreparacionUiState.Error(e.message ?: "Error al agregar componente")
            }
        }
    }

    fun eliminarComponente(comp: PreparacionComponente, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                preparacionRepository.eliminarComponente(comp)
                refrescarPreparaciones()
                onDone()
            } catch (e: Exception) {
                _uiState.value = PreparacionUiState.Error(e.message ?: "Error al eliminar componente")
            }
        }
    }

    fun resetState() {
        _uiState.value = PreparacionUiState.Idle
    }
}
