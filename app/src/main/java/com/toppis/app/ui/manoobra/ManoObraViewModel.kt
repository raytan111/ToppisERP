package com.toppis.app.ui.manoobra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Empleado
import com.toppis.app.data.models.Propina
import com.toppis.app.data.repository.JornadaConNombre
import com.toppis.app.data.repository.ManoObraRepository
import com.toppis.app.data.repository.PrimeCost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

sealed class ManoObraUiState {
    object Idle : ManoObraUiState()
    data class Error(val message: String) : ManoObraUiState()
    object Success : ManoObraUiState()
}

class ManoObraViewModel(
    private val repository: ManoObraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManoObraUiState>(ManoObraUiState.Idle)
    val uiState: StateFlow<ManoObraUiState> = _uiState.asStateFlow()

    private val _periodo = MutableStateFlow(YearMonth.now())
    val periodo: StateFlow<YearMonth> = _periodo.asStateFlow()

    private val _prime = MutableStateFlow<PrimeCost?>(null)
    val prime: StateFlow<PrimeCost?> = _prime.asStateFlow()

    private val _jornadas = MutableStateFlow<List<JornadaConNombre>>(emptyList())
    val jornadas: StateFlow<List<JornadaConNombre>> = _jornadas.asStateFlow()

    private val _propinas = MutableStateFlow<List<Propina>>(emptyList())
    val propinas: StateFlow<List<Propina>> = _propinas.asStateFlow()

    private val _empleados = MutableStateFlow<List<Empleado>>(emptyList())
    val empleados: StateFlow<List<Empleado>> = _empleados.asStateFlow()

    init {
        viewModelScope.launch { _empleados.value = repository.getEmpleados().filter { it.activo } }
        cargar()
    }

    fun mesAnterior() { _periodo.value = _periodo.value.minusMonths(1); cargar() }
    fun mesSiguiente() { _periodo.value = _periodo.value.plusMonths(1); cargar() }

    private fun rango(): Pair<String, String> {
        val ym = _periodo.value
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return ym.atDay(1).atStartOfDay().format(fmt) to ym.plusMonths(1).atDay(1).atStartOfDay().format(fmt)
    }

    fun cargar() {
        val (desde, hasta) = rango()
        viewModelScope.launch { _prime.value = repository.getPrimeCost(desde, hasta) }
        viewModelScope.launch { _jornadas.value = repository.getJornadas(desde, hasta) }
        viewModelScope.launch { _propinas.value = repository.getPropinas(desde, hasta) }
    }

    fun registrarJornada(empleado: Empleado, fecha: String, cantidad: Double, nota: String, usuarioId: String?) {
        viewModelScope.launch {
            try { repository.registrarJornada(empleado, fecha, cantidad, nota, usuarioId); cargar(); _uiState.value = ManoObraUiState.Success }
            catch (e: Exception) { _uiState.value = ManoObraUiState.Error(e.message ?: "Error al registrar jornada") }
        }
    }

    fun eliminarJornada(id: Int) {
        viewModelScope.launch { try { repository.eliminarJornada(id); cargar() } catch (_: Exception) {} }
    }

    fun registrarPropina(fecha: String, monto: Double, nota: String) {
        viewModelScope.launch {
            try { repository.registrarPropina(fecha, monto, nota); cargar(); _uiState.value = ManoObraUiState.Success }
            catch (e: Exception) { _uiState.value = ManoObraUiState.Error(e.message ?: "Error al registrar propina") }
        }
    }

    fun eliminarPropina(id: Int) {
        viewModelScope.launch { try { repository.eliminarPropina(id); cargar() } catch (_: Exception) {} }
    }

    fun resetState() { _uiState.value = ManoObraUiState.Idle }
}
