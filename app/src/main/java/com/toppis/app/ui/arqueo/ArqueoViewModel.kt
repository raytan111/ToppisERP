package com.toppis.app.ui.arqueo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.repository.ArqueoConNombre
import com.toppis.app.data.repository.ArqueoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ArqueoUiState {
    object Idle : ArqueoUiState()
    data class Error(val message: String) : ArqueoUiState()
    object Success : ArqueoUiState()
}

class ArqueoViewModel(
    private val repository: ArqueoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArqueoUiState>(ArqueoUiState.Idle)
    val uiState: StateFlow<ArqueoUiState> = _uiState.asStateFlow()

    private val _cuentas = MutableStateFlow<List<Sobre>>(emptyList())
    val cuentas: StateFlow<List<Sobre>> = _cuentas.asStateFlow()

    private val _arqueos = MutableStateFlow<List<ArqueoConNombre>>(emptyList())
    val arqueos: StateFlow<List<ArqueoConNombre>> = _arqueos.asStateFlow()

    init { refrescar() }

    fun refrescar() {
        viewModelScope.launch { _cuentas.value = repository.getCuentas() }
        viewModelScope.launch { _arqueos.value = repository.getArqueos() }
    }

    fun registrar(sobreId: Int, contado: Double, nota: String, ajustar: Boolean, usuarioId: String?) {
        viewModelScope.launch {
            try {
                repository.registrarArqueo(sobreId, contado, nota, ajustar, usuarioId)
                refrescar()
                _uiState.value = ArqueoUiState.Success
            } catch (e: Exception) {
                _uiState.value = ArqueoUiState.Error(e.message ?: "Error al registrar arqueo")
            }
        }
    }

    fun resetState() { _uiState.value = ArqueoUiState.Idle }
}
