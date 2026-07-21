package com.toppis.app.ui.sobres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.repository.SobreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SobreUiState {
    object Loading : SobreUiState()
    object Success : SobreUiState()
    data class Error(val message: String) : SobreUiState()
}

class SobreViewModel(private val repository: SobreRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SobreUiState>(SobreUiState.Success)
    val uiState: StateFlow<SobreUiState> = _uiState.asStateFlow()

    private val _sobres = MutableStateFlow<List<Sobre>>(emptyList())
    val sobres: StateFlow<List<Sobre>> = _sobres.asStateFlow()

    /** true mientras se hace la primera carga (para mostrar skeleton). */
    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    // Historial de movimientos del sobre seleccionado.
    private val _movimientos = MutableStateFlow<List<com.toppis.app.data.models.MovimientoSobre>>(emptyList())
    val movimientos: StateFlow<List<com.toppis.app.data.models.MovimientoSobre>> = _movimientos.asStateFlow()

    private val _cargandoMovimientos = MutableStateFlow(false)
    val cargandoMovimientos: StateFlow<Boolean> = _cargandoMovimientos.asStateFlow()

    fun cargarMovimientos(sobreId: Int) {
        viewModelScope.launch {
            _cargandoMovimientos.value = true
            _movimientos.value = emptyList()
            try { _movimientos.value = repository.getMovimientos(sobreId) }
            finally { _cargandoMovimientos.value = false }
        }
    }

    // Todos los movimientos (para deslizar entre sobres en el historial).
    private val _movimientosTodos = MutableStateFlow<List<com.toppis.app.data.models.MovimientoSobre>>(emptyList())
    val movimientosTodos: StateFlow<List<com.toppis.app.data.models.MovimientoSobre>> = _movimientosTodos.asStateFlow()

    fun cargarTodosMovimientos() {
        viewModelScope.launch {
            _cargandoMovimientos.value = true
            try { _movimientosTodos.value = repository.getTodosMovimientos() }
            finally { _cargandoMovimientos.value = false }
        }
    }

    fun limpiarMovimientos() { _movimientos.value = emptyList() }

    init {
        // Carga inicial
        refrescar()
        // Observador Realtime: refresca cuando otro dispositivo cambia algo
        viewModelScope.launch {
            repository.observeCambios().collect {
                refrescar()
            }
        }
    }

    /** Recarga la lista de sobres desde Supabase. */
    private fun refrescar() {
        viewModelScope.launch {
            _sobres.value = repository.getSobres()
            _cargandoInicial.value = false
        }
    }

    /** Recarga manual (al abrir la pantalla). */
    fun recargar() = refrescar()

    fun crearSobre(nombre: String, descripcion: String, tipo: com.toppis.app.data.db.entities.TipoSobre = com.toppis.app.data.db.entities.TipoSobre.CUENTA) {
        viewModelScope.launch {
            _uiState.value = SobreUiState.Loading
            try {
                repository.crearSobre(nombre, descripcion, tipo)
                refrescar()
                _uiState.value = SobreUiState.Success
            } catch (e: Exception) {
                _uiState.value = SobreUiState.Error(e.message ?: "Error al crear el sobre")
            }
        }
    }

    fun transferir(origenId: Long, destinoId: Long, monto: Double, descripcion: String, usuarioId: String?) {
        viewModelScope.launch {
            _uiState.value = SobreUiState.Loading
            try {
                repository.transferir(origenId, destinoId, monto, descripcion, usuarioId)
                refrescar()
                _uiState.value = SobreUiState.Success
            } catch (e: Exception) {
                _uiState.value = SobreUiState.Error(e.message ?: "Error al realizar la transferencia")
            }
        }
    }

    fun editarSobre(sobre: Sobre) {
        viewModelScope.launch {
            _uiState.value = SobreUiState.Loading
            try {
                repository.actualizarSobre(sobre)
                refrescar()
                _uiState.value = SobreUiState.Success
            } catch (e: Exception) {
                _uiState.value = SobreUiState.Error(e.message ?: "Error al editar el sobre")
            }
        }
    }

    fun eliminarSobre(sobre: Sobre) {
        viewModelScope.launch {
            _uiState.value = SobreUiState.Loading
            try {
                val eliminado = repository.eliminarSobre(sobre)
                if (eliminado) {
                    refrescar()
                    _uiState.value = SobreUiState.Success
                } else {
                    _uiState.value = SobreUiState.Error("No se puede eliminar un sobre con saldo")
                }
            } catch (e: Exception) {
                _uiState.value = SobreUiState.Error(e.message ?: "Error al eliminar el sobre")
            }
        }
    }
}
