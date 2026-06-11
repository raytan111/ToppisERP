package com.toppis.app.ui.flujo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.repository.FlujoCajaRepository
import com.toppis.app.data.repository.ProyeccionMes
import com.toppis.app.data.repository.PresupuestoVsReal
import com.toppis.app.data.repository.ResumenFlujoCaja
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

// ── Período de análisis ───────────────────────────────────────────────────────

enum class PeriodoFlujo(val label: String, val dias: Int) {
    HOY("Hoy", 0),
    SEMANA("Semana", 7),
    MES("Mes", 30),
    TRIMESTRE("Trimestre", 90),
    ANIO("Año", 365)
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class FlujoCajaViewModel(
    private val repository: FlujoCajaRepository
) : ViewModel() {

    private val _periodo = MutableStateFlow(PeriodoFlujo.MES)
    val periodoSeleccionado: StateFlow<PeriodoFlujo> = _periodo.asStateFlow()

    private val _resumen = MutableStateFlow<ResumenFlujoCaja?>(null)
    val resumen: StateFlow<ResumenFlujoCaja?> = _resumen.asStateFlow()

    private val _proyeccion = MutableStateFlow<List<ProyeccionMes>>(emptyList())
    val proyeccion: StateFlow<List<ProyeccionMes>> = _proyeccion.asStateFlow()

    private val _presupuestoVsReal = MutableStateFlow<List<PresupuestoVsReal>>(emptyList())
    val presupuestoVsReal: StateFlow<List<PresupuestoVsReal>> = _presupuestoVsReal.asStateFlow()

    private val _saldoTotal = MutableStateFlow(0.0)
    val saldoTotal: StateFlow<Double> = _saldoTotal.asStateFlow()

    init {
        // Recargar resumen cada vez que cambia el período
        viewModelScope.launch {
            _periodo.collect { periodo -> cargarResumen(periodo) }
        }
        viewModelScope.launch { cargarProyeccion() }
        viewModelScope.launch { _saldoTotal.value = repository.getSaldoTotal() }
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cargarPresupuestoVsReal(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        }
    }

    // ── Acciones públicas ─────────────────────────────────────────────────────

    fun cambiarPeriodo(periodo: PeriodoFlujo) {
        _periodo.value = periodo
    }

    fun guardarPresupuesto(
        categoria: CategoriaGasto,
        monto: Double,
        mes: Int,
        anio: Int
    ) {
        viewModelScope.launch {
            repository.guardarPresupuesto(categoria, monto, mes, anio)
            cargarPresupuestoVsReal(mes, anio)
        }
    }

    // ── Lógica interna ────────────────────────────────────────────────────────

    private suspend fun cargarResumen(periodo: PeriodoFlujo) {
        val hasta = System.currentTimeMillis()
        val desde = calcularDesde(periodo)
        _resumen.value = repository.getResumenPeriodo(desde, hasta)
    }

    private suspend fun cargarProyeccion() {
        _proyeccion.value = repository.getProyeccion(3)
    }

    private suspend fun cargarPresupuestoVsReal(mes: Int, anio: Int) {
        _presupuestoVsReal.value = repository.getPresupuestoVsReal(mes, anio)
    }

    private fun calcularDesde(periodo: PeriodoFlujo): Long {
        if (periodo == PeriodoFlujo.HOY) {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        return System.currentTimeMillis() - periodo.dias.toLong() * 24 * 60 * 60 * 1000
    }
}

