package com.toppis.app.ui.costos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.PasoRutina
import com.toppis.app.data.db.entities.TipoSobre
import com.toppis.app.data.models.CostoFijo
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.repository.CostoFijoRepository
import com.toppis.app.data.repository.RutinaSemanalRepository
import com.toppis.app.data.repository.SobreRepository
import com.toppis.app.data.util.FechaUtil
import com.toppis.app.data.util.SemanaOperativa
import com.toppis.app.domain.costos.CostosCalculos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RutinaUiState {
    object Idle : RutinaUiState()
    data class Error(val message: String) : RutinaUiState()
    data class Ok(val message: String) : RutinaUiState()
}

class RutinaSemanalViewModel(
    private val rutinaRepo: RutinaSemanalRepository,
    private val costoFijoRepo: CostoFijoRepository,
    private val sobreRepo: SobreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RutinaUiState>(RutinaUiState.Idle)
    val uiState: StateFlow<RutinaUiState> = _uiState.asStateFlow()

    private val _semana = MutableStateFlow(FechaUtil.semanaActual())
    val semana: StateFlow<SemanaOperativa> = _semana.asStateFlow()

    private val _pasos = MutableStateFlow<Set<PasoRutina>>(emptySet())
    val pasos: StateFlow<Set<PasoRutina>> = _pasos.asStateFlow()

    private val _fijos = MutableStateFlow<List<CostoFijo>>(emptyList())
    val fijos: StateFlow<List<CostoFijo>> = _fijos.asStateFlow()

    private val _sobres = MutableStateFlow<List<Sobre>>(emptyList())
    val sobres: StateFlow<List<Sobre>> = _sobres.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    /** Total a provisionar en la semana (suma de prorrateos de fijos activos). */
    val totalProvision: Double get() = CostosCalculos.totalFijosSemanales(_fijos.value)

    val sobresCuenta: List<Sobre> get() = _sobres.value.filter { it.tipo == TipoSobre.CUENTA }
    val sobresFondo: List<Sobre> get() = _sobres.value.filter { it.tipo == TipoSobre.FONDO }

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _cargando.value = true
            _fijos.value = costoFijoRepo.getCostosFijos().filter { it.activo }
            _sobres.value = sobreRepo.getSobres()
            _pasos.value = rutinaRepo.getPasos(_semana.value)
                .filter { it.completado }.map { it.paso }.toSet()
            _cargando.value = false
        }
    }

    fun marcarPaso(paso: PasoRutina, completado: Boolean) {
        viewModelScope.launch {
            try {
                rutinaRepo.marcarPaso(_semana.value, paso, completado)
                _pasos.value = if (completado) _pasos.value + paso else _pasos.value - paso
            } catch (e: Exception) {
                _uiState.value = RutinaUiState.Error(e.message ?: "Error al marcar el paso")
            }
        }
    }

    /** Aparta en un sobre FONDO el total de fijos de la semana (transfiere desde una CUENTA). */
    fun provisionar(origenId: Int, destinoId: Int, usuarioId: String?) {
        val total = totalProvision
        if (total <= 0.0) { _uiState.value = RutinaUiState.Error("No hay costos fijos por provisionar."); return }
        viewModelScope.launch {
            try {
                sobreRepo.transferir(origenId.toLong(), destinoId.toLong(), total,
                    "Provisión de costos fijos (${_semana.value.etiqueta})", usuarioId)
                rutinaRepo.marcarPaso(_semana.value, PasoRutina.PROVISION, true)
                _pasos.value = _pasos.value + PasoRutina.PROVISION
                _sobres.value = sobreRepo.getSobres()
                _uiState.value = RutinaUiState.Ok("Provisión realizada")
            } catch (e: Exception) {
                _uiState.value = RutinaUiState.Error(e.message ?: "Error al provisionar")
            }
        }
    }

    fun crearFondo(nombre: String) {
        viewModelScope.launch {
            try {
                sobreRepo.crearSobre(nombre, "Provisión de costos fijos", TipoSobre.FONDO)
                _sobres.value = sobreRepo.getSobres()
                _uiState.value = RutinaUiState.Ok("Sobre creado")
            } catch (e: Exception) {
                _uiState.value = RutinaUiState.Error(e.message ?: "Error al crear el sobre")
            }
        }
    }

    fun resetState() { _uiState.value = RutinaUiState.Idle }
}

class RutinaSemanalViewModelFactory(
    private val rutinaRepo: RutinaSemanalRepository,
    private val costoFijoRepo: CostoFijoRepository,
    private val sobreRepo: SobreRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RutinaSemanalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RutinaSemanalViewModel(rutinaRepo, costoFijoRepo, sobreRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
