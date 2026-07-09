package com.toppis.app.ui.costos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.repository.CierreSemanalRepository
import com.toppis.app.data.repository.ResultadoSemanalRepository
import com.toppis.app.data.util.FechaUtil
import com.toppis.app.data.util.SemanaOperativa
import com.toppis.app.domain.costos.ManoObraDisponible
import com.toppis.app.domain.costos.ResultadoSemanal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CierreUiState {
    object Idle : CierreUiState()
    data class Error(val message: String) : CierreUiState()
    object Success : CierreUiState()
}

class CierreSemanalViewModel(
    private val resultadoRepo: ResultadoSemanalRepository,
    private val cierreRepo: CierreSemanalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CierreUiState>(CierreUiState.Idle)
    val uiState: StateFlow<CierreUiState> = _uiState.asStateFlow()

    private val _semana = MutableStateFlow(FechaUtil.semanaActual())
    val semana: StateFlow<SemanaOperativa> = _semana.asStateFlow()

    private val _resultado = MutableStateFlow<ResultadoSemanal?>(null)
    val resultado: StateFlow<ResultadoSemanal?> = _resultado.asStateFlow()

    private val _manoObra = MutableStateFlow<ManoObraDisponible?>(null)
    val manoObra: StateFlow<ManoObraDisponible?> = _manoObra.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    init { cargar() }

    fun cambiarSemana(delta: Long) {
        _semana.value = FechaUtil.semanaOffset(_semana.value, delta)
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val s = _semana.value
                _resultado.value = resultadoRepo.getResultado(s)
                _manoObra.value = resultadoRepo.getManoObraDisponible(s)
            } catch (e: Exception) {
                _uiState.value = CierreUiState.Error(e.message ?: "Error al cargar el resultado")
            } finally {
                _cargando.value = false
            }
        }
    }

    fun confirmarCierre(usuarioId: String?) {
        val r = _resultado.value ?: return
        viewModelScope.launch {
            try {
                cierreRepo.confirmarCierre(
                    semana = r.semana,
                    ventas = r.ventasCobradas,
                    variable = r.costoVariable,
                    foodTeorico = r.foodTeorico,
                    manoObra = r.manoObraPagada,
                    fijos = r.fijosProrrateados,
                    resultado = r.resultado,
                    foodPct = r.foodPct,
                    laborPct = r.laborPct,
                    breakEven = r.breakEven,
                    margen = r.margenContribucion,
                    usuarioId = usuarioId
                )
                cargar()
                _uiState.value = CierreUiState.Success
            } catch (e: Exception) {
                _uiState.value = CierreUiState.Error(e.message ?: "Error al confirmar el cierre")
            }
        }
    }

    fun resetState() { _uiState.value = CierreUiState.Idle }
}

class CierreSemanalViewModelFactory(
    private val resultadoRepo: ResultadoSemanalRepository,
    private val cierreRepo: CierreSemanalRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CierreSemanalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CierreSemanalViewModel(resultadoRepo, cierreRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
