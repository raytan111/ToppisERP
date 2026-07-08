package com.toppis.app.ui.comprobantes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Comprobante
import com.toppis.app.data.repository.ComprobanteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ComprobantesViewModel(
    private val repository: ComprobanteRepository
) : ViewModel() {

    private val _comprobantes = MutableStateFlow<List<Comprobante>>(emptyList())
    val comprobantes: StateFlow<List<Comprobante>> = _comprobantes.asStateFlow()

    /** true mientras se hace la primera carga (para mostrar skeleton). */
    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init {
        refrescar()
        viewModelScope.launch {
            repository.observeCambios().collect { refrescar() }
        }
    }

    private fun refrescar() {
        viewModelScope.launch {
            _comprobantes.value = repository.getComprobantes()
            _cargandoInicial.value = false
        }
    }

    /** Recarga manual (al abrir la pantalla). */
    fun recargar() = refrescar()
}

class ComprobantesViewModelFactory(
    private val repository: ComprobanteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ComprobantesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ComprobantesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
