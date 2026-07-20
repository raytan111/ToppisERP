package com.toppis.app.ui.manoobra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.Periodicidad
import com.toppis.app.data.db.entities.TipoPago
import com.toppis.app.data.models.Empleado
import com.toppis.app.data.models.Propina
import com.toppis.app.data.repository.JornadaConNombre
import com.toppis.app.data.repository.ManoObraRepository
import com.toppis.app.data.repository.PrimeCost
import com.toppis.app.data.util.FechaUtil
import com.toppis.app.data.util.SemanaOperativa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class ManoObraUiState {
    object Idle : ManoObraUiState()
    data class Error(val message: String) : ManoObraUiState()
    data class Success(val message: String) : ManoObraUiState()
}

/** Vista de período: por semana (operativa) o por mes (informativo). */
enum class PeriodoModo { SEMANA, MES }

/** Costo de un empleado con su equivalente semanal y mensual. */
data class EmpleadoCosto(
    val empleado: Empleado,
    val esFijo: Boolean,
    val mensual: Double,
    val semanal: Double
)

class ManoObraViewModel(
    private val repository: ManoObraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManoObraUiState>(ManoObraUiState.Idle)
    val uiState: StateFlow<ManoObraUiState> = _uiState.asStateFlow()

    private val _modo = MutableStateFlow(PeriodoModo.SEMANA)
    val modo: StateFlow<PeriodoModo> = _modo.asStateFlow()

    private val _semana = MutableStateFlow(FechaUtil.semanaActual())
    val semana: StateFlow<SemanaOperativa> = _semana.asStateFlow()

    private val _mes = MutableStateFlow(YearMonth.now())
    val mes: StateFlow<YearMonth> = _mes.asStateFlow()

    /** Etiqueta legible del período actual (para el encabezado). */
    private val _etiqueta = MutableStateFlow("")
    val etiqueta: StateFlow<String> = _etiqueta.asStateFlow()

    private val _prime = MutableStateFlow<PrimeCost?>(null)
    val prime: StateFlow<PrimeCost?> = _prime.asStateFlow()

    private val _jornadas = MutableStateFlow<List<JornadaConNombre>>(emptyList())
    val jornadas: StateFlow<List<JornadaConNombre>> = _jornadas.asStateFlow()

    private val _propinas = MutableStateFlow<List<Propina>>(emptyList())
    val propinas: StateFlow<List<Propina>> = _propinas.asStateFlow()

    private val _empleados = MutableStateFlow<List<Empleado>>(emptyList())
    val empleados: StateFlow<List<Empleado>> = _empleados.asStateFlow()

    /** Desglose de costo por empleado (equivalente semanal y mensual). */
    private val _empleadosCosto = MutableStateFlow<List<EmpleadoCosto>>(emptyList())
    val empleadosCosto: StateFlow<List<EmpleadoCosto>> = _empleadosCosto.asStateFlow()

    init {
        viewModelScope.launch {
            val emps = repository.getEmpleados().filter { it.activo }
            _empleados.value = emps
            _empleadosCosto.value = emps.map { e ->
                val esFijo = e.tipoPago == TipoPago.SUELDO_FIJO
                EmpleadoCosto(
                    empleado = e,
                    esFijo = esFijo,
                    mensual = if (esFijo) e.monto else 0.0,
                    semanal = if (esFijo) e.monto / Periodicidad.MENSUAL.divisorSemanal else 0.0
                )
            }
        }
        cargar()
    }

    fun setModo(m: PeriodoModo) { if (_modo.value != m) { _modo.value = m; cargar() } }

    fun anterior() {
        if (_modo.value == PeriodoModo.SEMANA) _semana.value = FechaUtil.semanaOffset(_semana.value, -1)
        else _mes.value = _mes.value.minusMonths(1)
        cargar()
    }

    fun siguiente() {
        if (_modo.value == PeriodoModo.SEMANA) _semana.value = FechaUtil.semanaOffset(_semana.value, 1)
        else _mes.value = _mes.value.plusMonths(1)
        cargar()
    }

    private fun rango(): Triple<String, String, Double> {
        return if (_modo.value == PeriodoModo.SEMANA) {
            val s = _semana.value
            Triple(s.isoDesde, s.isoHasta, 1.0 / Periodicidad.MENSUAL.divisorSemanal)
        } else {
            val ym = _mes.value
            val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            Triple(ym.atDay(1).atStartOfDay().format(fmt), ym.plusMonths(1).atDay(1).atStartOfDay().format(fmt), 1.0)
        }
    }

    fun cargar() {
        _etiqueta.value = if (_modo.value == PeriodoModo.SEMANA) _semana.value.etiqueta
        else {
            val ym = _mes.value
            val mesNom = ym.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("es", "CL"))
                .replaceFirstChar { it.uppercase() }
            "$mesNom ${ym.year}"
        }
        val (desde, hasta, factor) = rango()
        viewModelScope.launch { _prime.value = repository.getPrimeCost(desde, hasta, factor) }
        viewModelScope.launch { _jornadas.value = repository.getJornadas(desde, hasta) }
        viewModelScope.launch { _propinas.value = repository.getPropinas(desde, hasta) }
    }

    /** Ajusta el período visible para que contenga [fecha] (yyyy-MM-dd). */
    private fun irAPeriodoDe(fecha: String) {
        val f = runCatching { java.time.LocalDate.parse(fecha.take(10)) }.getOrNull() ?: return
        if (_modo.value == PeriodoModo.SEMANA) _semana.value = FechaUtil.semanaDe(f)
        else _mes.value = YearMonth.from(f)
    }

    fun registrarJornada(empleado: Empleado, fecha: String, cantidad: Double, nota: String, usuarioId: String?) {
        viewModelScope.launch {
            try {
                repository.registrarJornada(empleado, fecha, cantidad, nota, usuarioId)
                irAPeriodoDe(fecha); cargar()
                _uiState.value = ManoObraUiState.Success("Turno registrado")
            } catch (e: Exception) {
                _uiState.value = ManoObraUiState.Error(e.message ?: "Error al registrar jornada")
            }
        }
    }

    fun eliminarJornada(id: Int) {
        viewModelScope.launch { try { repository.eliminarJornada(id); cargar() } catch (_: Exception) {} }
    }

    fun registrarPropina(fecha: String, monto: Double, nota: String) {
        viewModelScope.launch {
            try {
                repository.registrarPropina(fecha, monto, nota)
                irAPeriodoDe(fecha); cargar()
                _uiState.value = ManoObraUiState.Success("Propina registrada")
            } catch (e: Exception) {
                _uiState.value = ManoObraUiState.Error(e.message ?: "Error al registrar propina")
            }
        }
    }

    fun eliminarPropina(id: Int) {
        viewModelScope.launch { try { repository.eliminarPropina(id); cargar() } catch (_: Exception) {} }
    }

    fun resetState() { _uiState.value = ManoObraUiState.Idle }
}
