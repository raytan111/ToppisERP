package com.toppis.app.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.DimensionUnidad
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.repository.ArticuloRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class InventarioUiState {
    object Loading : InventarioUiState()
    object Success : InventarioUiState()
    data class Error(val message: String) : InventarioUiState()
}

/**
 * ViewModel del módulo de Inventario (Artículos unificados).
 */
class InventarioViewModel(private val repository: ArticuloRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<InventarioUiState>(InventarioUiState.Success)
    val uiState: StateFlow<InventarioUiState> = _uiState.asStateFlow()

    private val _articulos = MutableStateFlow<List<Articulo>>(emptyList())
    val articulos: StateFlow<List<Articulo>> = _articulos.asStateFlow()

    /** true mientras se hace la primera carga (para mostrar skeleton). */
    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial.asStateFlow()

    init {
        refrescar()
        viewModelScope.launch {
            repository.observeArticulos().collect { refrescar() }
        }
    }

    private fun refrescar() {
        viewModelScope.launch {
            _articulos.value = repository.getArticulos()
            _cargandoInicial.value = false
        }
    }

    /** Recarga manual (al abrir la pantalla). */
    fun recargar() = refrescar()

    fun crearArticulo(
        nombre: String,
        dimension: DimensionUnidad,
        unidadCompra: String,
        factorCompra: Double,
        costoCompra: Double,
        rendimiento: Double,
        stockBase: Double,
        parLevel: Double,
        perecible: Boolean,
        vidaUtilDias: Int,
        esVendible: Boolean,
        seleccionableEnPos: Boolean,
        cantidadPos: Double
    ) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.crearArticulo(
                    nombre, dimension, unidadCompra, factorCompra, costoCompra,
                    rendimiento, stockBase, parLevel, perecible, vidaUtilDias,
                    esVendible, seleccionableEnPos, cantidadPos
                )
                refrescar()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al crear artículo")
            }
        }
    }

    fun editarArticulo(articulo: Articulo) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.actualizarArticulo(articulo)
                refrescar()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al editar artículo")
            }
        }
    }

    fun actualizarStock(articuloId: Int, nuevoStock: Double) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.actualizarStock(articuloId, nuevoStock)
                refrescar()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al actualizar stock")
            }
        }
    }

    fun eliminarArticulo(articuloId: Int) {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                repository.eliminarArticulo(articuloId)
                refrescar()
                _uiState.value = InventarioUiState.Success
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error al eliminar artículo")
            }
        }
    }

    fun resetState() {
        _uiState.value = InventarioUiState.Success
    }
}
