package com.toppis.app.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.models.Venta
import com.toppis.app.data.repository.ReporteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/** Períodos disponibles para filtrar el reporte. */
enum class PeriodoReporte(val label: String) {
    HOY("Hoy"),
    SEMANA("Semana"),
    MES("Mes")
}

private fun inicioDePeriodo(periodo: PeriodoReporte): Long {
    val cal = Calendar.getInstance()
    return when (periodo) {
        PeriodoReporte.HOY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        PeriodoReporte.SEMANA -> {
            cal.add(Calendar.DAY_OF_YEAR, -7)
            cal.timeInMillis
        }
        PeriodoReporte.MES -> {
            cal.add(Calendar.MONTH, -1)
            cal.timeInMillis
        }
    }
}

class ReporteViewModel(
    private val repository: ReporteRepository
) : ViewModel() {

    private val _periodo = MutableStateFlow(PeriodoReporte.MES)
    val periodo: StateFlow<PeriodoReporte> = _periodo.asStateFlow()

    private val _totalVentas = MutableStateFlow(0.0)
    val totalVentas: StateFlow<Double> = _totalVentas.asStateFlow()

    private val _totalGastos = MutableStateFlow(0.0)
    val totalGastos: StateFlow<Double> = _totalGastos.asStateFlow()

    private val _balanceNeto = MutableStateFlow(0.0)
    val balanceNeto: StateFlow<Double> = _balanceNeto.asStateFlow()

    private val _ultimasVentas = MutableStateFlow<List<Venta>>(emptyList())
    val ultimasVentas: StateFlow<List<Venta>> = _ultimasVentas.asStateFlow()

    private val _gastosPorCategoria = MutableStateFlow<Map<CategoriaGasto, Double>>(emptyMap())
    val gastosPorCategoria: StateFlow<Map<CategoriaGasto, Double>> = _gastosPorCategoria.asStateFlow()

    private val _sobres = MutableStateFlow<List<Sobre>>(emptyList())
    val sobres: StateFlow<List<Sobre>> = _sobres.asStateFlow()

    private val _ivaProvisionado = MutableStateFlow(0.0)
    val ivaProvisionado: StateFlow<Double> = _ivaProvisionado.asStateFlow()

    init {
        recargar()
    }

    fun seleccionarPeriodo(nuevo: PeriodoReporte) {
        _periodo.value = nuevo
        recargar()
    }

    private fun recargar() {
        viewModelScope.launch {
            val inicio = inicioDePeriodo(_periodo.value)
            val ventas = repository.getVentasDesde(inicio)
            val gastos = repository.getGastosDesde(inicio)

            _totalVentas.value = ventas.sumOf { it.total }
            _totalGastos.value = gastos.sumOf { it.monto }
            _balanceNeto.value = _totalVentas.value - _totalGastos.value
            _ultimasVentas.value = ventas.take(10)
            _gastosPorCategoria.value = gastos
                .groupBy { it.categoria }
                .mapValues { (_, list) -> list.sumOf { it.monto } }
                .filter { it.value > 0 }
            _sobres.value = repository.getSobres()
            _ivaProvisionado.value = repository.getIvaProvisionadoDesde(inicio)
        }
    }
}
