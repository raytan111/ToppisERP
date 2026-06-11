package com.toppis.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.repository.DashboardKpi
import com.toppis.app.data.repository.DashboardRepository
import com.toppis.app.data.repository.DatoSerie
import com.toppis.app.data.repository.DistribucionEgreso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _kpi = MutableStateFlow<DashboardKpi?>(null)
    val kpi: StateFlow<DashboardKpi?> = _kpi.asStateFlow()

    private val _serieTiempo = MutableStateFlow<List<DatoSerie>>(emptyList())
    val serieTiempo: StateFlow<List<DatoSerie>> = _serieTiempo.asStateFlow()

    private val _distribucion = MutableStateFlow<List<DistribucionEgreso>>(emptyList())
    val distribucion: StateFlow<List<DistribucionEgreso>> = _distribucion.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            try {
                val (desde, hasta) = DashboardRepository.rangoMesActual()
                val (desdeAnt, hastaAnt) = DashboardRepository.rangoMesAnterior()

                _kpi.value = repository.getKpi(desde, hasta, desdeAnt, hastaAnt)
                _serieTiempo.value = repository.getSerieTiempo(desde, hasta)
                _distribucion.value = repository.getDistribucionEgresos(desde, hasta)
            } catch (_: Exception) { }
        }
    }
}
