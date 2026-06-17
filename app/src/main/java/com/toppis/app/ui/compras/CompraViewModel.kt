package com.toppis.app.ui.compras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Compra
import com.toppis.app.data.models.Proveedor
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.repository.CompraRepository
import com.toppis.app.data.repository.LineaCompra
import com.toppis.app.data.repository.LoteVencimiento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CompraUiState {
    object Idle : CompraUiState()
    data class Error(val message: String) : CompraUiState()
    object Success : CompraUiState()
}

class CompraViewModel(
    private val repository: CompraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompraUiState>(CompraUiState.Idle)
    val uiState: StateFlow<CompraUiState> = _uiState.asStateFlow()

    private val _compras = MutableStateFlow<List<Compra>>(emptyList())
    val compras: StateFlow<List<Compra>> = _compras.asStateFlow()

    private val _articulos = MutableStateFlow<List<Articulo>>(emptyList())
    val articulos: StateFlow<List<Articulo>> = _articulos.asStateFlow()

    private val _proveedores = MutableStateFlow<List<Proveedor>>(emptyList())
    val proveedores: StateFlow<List<Proveedor>> = _proveedores.asStateFlow()

    private val _sobres = MutableStateFlow<List<Sobre>>(emptyList())
    val sobres: StateFlow<List<Sobre>> = _sobres.asStateFlow()

    private val _porVencer = MutableStateFlow<List<LoteVencimiento>>(emptyList())
    val porVencer: StateFlow<List<LoteVencimiento>> = _porVencer.asStateFlow()

    init { refrescar() }

    fun refrescar() {
        viewModelScope.launch { _compras.value = repository.getCompras() }
        viewModelScope.launch { _articulos.value = repository.getArticulos() }
        viewModelScope.launch { _proveedores.value = repository.getProveedores() }
        viewModelScope.launch { _sobres.value = repository.getSobres() }
        viewModelScope.launch { _porVencer.value = repository.getProximosAVencer() }
    }

    fun registrarCompra(
        proveedorId: Int?,
        tieneIva: Boolean,
        nota: String,
        lineas: List<LineaCompra>,
        sobreId: Int?,
        usuarioId: String?
    ) {
        viewModelScope.launch {
            try {
                repository.registrarCompra(proveedorId, tieneIva, nota, lineas, sobreId, usuarioId)
                refrescar()
                _uiState.value = CompraUiState.Success
            } catch (e: Exception) {
                _uiState.value = CompraUiState.Error(e.message ?: "Error al registrar compra")
            }
        }
    }

    fun resetState() { _uiState.value = CompraUiState.Idle }
}
