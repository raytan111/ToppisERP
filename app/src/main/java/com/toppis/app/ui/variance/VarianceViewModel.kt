package com.toppis.app.ui.variance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.repository.VarianceItem
import com.toppis.app.data.repository.VarianceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class VarianceViewModel(
    private val repository: VarianceRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<VarianceItem>>(emptyList())
    val items: StateFlow<List<VarianceItem>> = _items.asStateFlow()

    private val _periodo = MutableStateFlow(YearMonth.now())
    val periodo: StateFlow<YearMonth> = _periodo.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    init { cargar() }

    fun mesAnterior() { _periodo.value = _periodo.value.minusMonths(1); cargar() }
    fun mesSiguiente() { _periodo.value = _periodo.value.plusMonths(1); cargar() }

    private fun cargar() {
        val ym = _periodo.value
        val desde = ym.atDay(1).atStartOfDay()
        val hasta = ym.plusMonths(1).atDay(1).atStartOfDay()
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        viewModelScope.launch {
            _cargando.value = true
            _items.value = repository.getAnalisis(desde.format(fmt), hasta.format(fmt))
            _cargando.value = false
        }
    }
}
