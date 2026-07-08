package com.toppis.app.ui.locales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Local
import com.toppis.app.data.repository.LocalRepository
import com.toppis.app.data.repository.LocalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LocalUiState {
    object Idle : LocalUiState()
    data class Error(val message: String) : LocalUiState()
    object Success : LocalUiState()
}

class LocalViewModel(
    private val repository: LocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocalUiState>(LocalUiState.Idle)
    val uiState: StateFlow<LocalUiState> = _uiState.asStateFlow()

    private val _locales = MutableStateFlow<List<Local>>(emptyList())
    val locales: StateFlow<List<Local>> = _locales.asStateFlow()

    val activoId: StateFlow<Int?> = LocalSession.activoId

    /** true mientras se hace la primera carga (para mostrar skeleton). */
    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init { refrescar() }

    private fun refrescar() {
        viewModelScope.launch {
            _locales.value = repository.getLocales()
            _cargandoInicial.value = false
        }
    }

    fun crear(nombre: String, direccion: String) {
        viewModelScope.launch {
            try { repository.crear(nombre, direccion); refrescar(); _uiState.value = LocalUiState.Success }
            catch (e: Exception) { _uiState.value = LocalUiState.Error(e.message ?: "Error al crear local") }
        }
    }

    fun actualizar(l: Local) {
        viewModelScope.launch {
            try { repository.actualizar(l); refrescar() }
            catch (e: Exception) { _uiState.value = LocalUiState.Error(e.message ?: "Error al actualizar") }
        }
    }

    fun eliminar(id: Int) {
        viewModelScope.launch {
            try { repository.eliminar(id); refrescar() }
            catch (e: Exception) { _uiState.value = LocalUiState.Error(e.message ?: "Error al eliminar") }
        }
    }

    fun marcarActivo(l: Local) {
        LocalSession.setActivo(l.id, l.nombre)
    }

    fun resetState() { _uiState.value = LocalUiState.Idle }
}
