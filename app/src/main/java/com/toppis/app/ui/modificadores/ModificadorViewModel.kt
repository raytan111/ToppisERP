package com.toppis.app.ui.modificadores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.AccionModificador
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.db.entities.TipoModificador
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Modificador
import com.toppis.app.data.models.ModificadorComponente
import com.toppis.app.data.models.Preparacion
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

    private val _articulos = MutableStateFlow<List<Articulo>>(emptyList())
    val articulos: StateFlow<List<Articulo>> = _articulos.asStateFlow()

    private val _preparaciones = MutableStateFlow<List<Preparacion>>(emptyList())
    val preparaciones: StateFlow<List<Preparacion>> = _preparaciones.asStateFlow()

    /** Delta de costo (food cost) de cada modificador, por id. */
    private val _costos = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val costos: StateFlow<Map<Int, Double>> = _costos.asStateFlow()

    /** true mientras se hace la primera carga (para mostrar skeleton). */
    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init {
        refrescar()
        viewModelScope.launch { _articulos.value = modificadorRepository.getArticulos() }
        viewModelScope.launch { _preparaciones.value = modificadorRepository.getPreparaciones() }
    }

    private fun refrescar() {
        viewModelScope.launch {
            try {
                _modificadores.value = modificadorRepository.getModificadores()
                _costos.value = modificadorRepository.getCostoPorModificador()
            } catch (e: Exception) {
                _uiState.value = ModificadorUiState.Error(e.message ?: "Error al cargar modificadores")
            } finally {
                _cargandoInicial.value = false
            }
        }
    }

    fun crearModificador(
        nombre: String,
        tipo: TipoModificador,
        deltaPrecio: Double,
        categoria: String? = null,
        itemMenuId: Int? = null
    ) {
        viewModelScope.launch {
            try {
                modificadorRepository.crearModificador(nombre, tipo, itemMenuId, deltaPrecio, categoria)
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

    // ── Componentes (delta de receta) ────────────────────────────────────────

    fun loadComponentes(modId: Int, callback: (List<ModificadorComponente>) -> Unit) {
        viewModelScope.launch {
            try {
                callback(modificadorRepository.getComponentes(modId))
            } catch (e: Exception) {
                _uiState.value = ModificadorUiState.Error(e.message ?: "Error al cargar componentes")
            }
        }
    }

    fun agregarComponente(
        modId: Int,
        accion: AccionModificador,
        tipo: TipoComponente,
        compId: Int,
        cantidad: Double,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                modificadorRepository.agregarComponente(modId, accion, tipo, compId, cantidad)
                onDone()
            } catch (e: Exception) {
                _uiState.value = ModificadorUiState.Error(e.message ?: "Error al agregar componente")
            }
        }
    }

    fun eliminarComponente(id: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                modificadorRepository.eliminarComponente(id)
                onDone()
            } catch (e: Exception) {
                _uiState.value = ModificadorUiState.Error(e.message ?: "Error al eliminar componente")
            }
        }
    }

    /** Nombre legible de un componente por tipo+id (para mostrar en la receta). */
    fun nombreComponente(tipo: TipoComponente, id: Int): String = when (tipo) {
        TipoComponente.ARTICULO -> _articulos.value.firstOrNull { it.id == id }?.let { "${it.nombre} (${it.unidadBase})" } ?: "Artículo #$id"
        TipoComponente.PREPARACION -> _preparaciones.value.firstOrNull { it.id == id }?.let { "${it.nombre} (${it.unidadBase})" } ?: "Prep #$id"
    }

    fun resetState() {
        _uiState.value = ModificadorUiState.Idle
    }
}
