package com.toppis.app.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.models.Gasto
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.repository.GastoRepository
import com.toppis.app.data.repository.SobreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GastoUiState {
    object Loading : GastoUiState()
    object Success : GastoUiState()
    data class Error(val message: String) : GastoUiState()
}

class GastoViewModel(
    private val gastoRepository: GastoRepository,
    private val sobreRepository: SobreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GastoUiState>(GastoUiState.Success)
    val uiState: StateFlow<GastoUiState> = _uiState.asStateFlow()

    private val _gastos = MutableStateFlow<List<Gasto>>(emptyList())
    val gastos: StateFlow<List<Gasto>> = _gastos.asStateFlow()

    private val _totalGastos = MutableStateFlow(0.0)
    val totalGastos: StateFlow<Double> = _totalGastos.asStateFlow()

    private val _sobres = MutableStateFlow<List<Sobre>>(emptyList())
    val sobres: StateFlow<List<Sobre>> = _sobres.asStateFlow()

    init {
        refrescarGastos()
        refrescarSobres()
        viewModelScope.launch {
            gastoRepository.observeCambios().collect { refrescarGastos() }
        }
    }

    private fun refrescarGastos() {
        viewModelScope.launch {
            val lista = gastoRepository.getGastos()
            _gastos.value = lista
            _totalGastos.value = lista.sumOf { it.monto }
        }
    }

    private fun refrescarSobres() {
        viewModelScope.launch { _sobres.value = sobreRepository.getSobres() }
    }

    fun registrarGasto(
        descripcion: String,
        monto: Double,
        categoria: CategoriaGasto,
        sobreId: Int,
        usuarioId: String? = null,
        comprobante: String? = null,
        tieneIva: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = GastoUiState.Loading
            try {
                gastoRepository.registrarGasto(descripcion, monto, categoria, sobreId, usuarioId, comprobante, tieneIva)
                refrescarGastos()
                refrescarSobres()
                _uiState.value = GastoUiState.Success
            } catch (e: Exception) {
                _uiState.value = GastoUiState.Error(e.message ?: "Error al registrar gasto")
            }
        }
    }
}

