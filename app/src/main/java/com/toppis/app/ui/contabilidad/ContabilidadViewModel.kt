package com.toppis.app.ui.contabilidad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.repository.ContabilidadRepository
import com.toppis.app.data.repository.LineaLibroCompra
import com.toppis.app.data.repository.LineaLibroVenta
import com.toppis.app.data.repository.ResumenContable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ContabilidadViewModel(
    private val repository: ContabilidadRepository
) : ViewModel() {

    private val cal = Calendar.getInstance()

    private val _mes = MutableStateFlow(cal.get(Calendar.MONTH) + 1)
    val mes: StateFlow<Int> = _mes.asStateFlow()

    private val _anio = MutableStateFlow(cal.get(Calendar.YEAR))
    val anio: StateFlow<Int> = _anio.asStateFlow()

    private val _resumen = MutableStateFlow<ResumenContable?>(null)
    val resumen: StateFlow<ResumenContable?> = _resumen.asStateFlow()

    private val _libroVentas = MutableStateFlow<List<LineaLibroVenta>>(emptyList())
    val libroVentas: StateFlow<List<LineaLibroVenta>> = _libroVentas.asStateFlow()

    private val _libroCompras = MutableStateFlow<List<LineaLibroCompra>>(emptyList())
    val libroCompras: StateFlow<List<LineaLibroCompra>> = _libroCompras.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    init {
        cargar()
    }

    fun cambiarPeriodo(mes: Int, anio: Int) {
        _mes.value = mes
        _anio.value = anio
        cargar()
    }

    private fun cargar() {
        viewModelScope.launch {
            _resumen.value = repository.getResumen(_mes.value, _anio.value)
            _libroVentas.value = repository.getLibroVentas(_mes.value, _anio.value)
            _libroCompras.value = repository.getLibroCompras(_mes.value, _anio.value)
        }
    }

    fun cerrarMes(usuarioId: String?) {
        viewModelScope.launch {
            try {
                val r = _resumen.value ?: repository.getResumen(_mes.value, _anio.value)
                repository.cerrarMes(r, usuarioId)
                _mensaje.value = "Mes cerrado: snapshot guardado"
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Error al cerrar el mes"
            }
        }
    }

    fun limpiarMensaje() { _mensaje.value = null }
}

class ContabilidadViewModelFactory(
    private val repository: ContabilidadRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContabilidadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContabilidadViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
