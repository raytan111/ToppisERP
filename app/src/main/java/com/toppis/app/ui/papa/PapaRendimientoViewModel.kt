package com.toppis.app.ui.papa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.PapaRendimiento
import com.toppis.app.data.repository.ArticuloRepository
import com.toppis.app.data.repository.PapaRendimientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PapaRendimientoViewModel(
    private val papaRepo: PapaRendimientoRepository,
    private val articuloRepo: ArticuloRepository
) : ViewModel() {

    private val _articulos = MutableStateFlow<List<Articulo>>(emptyList())
    val articulos: StateFlow<List<Articulo>> = _articulos.asStateFlow()

    private val _registros = MutableStateFlow<List<PapaRendimiento>>(emptyList())
    val registros: StateFlow<List<PapaRendimiento>> = _registros.asStateFlow()

    init {
        refrescarArticulos()
    }

    private fun refrescarArticulos() {
        viewModelScope.launch { _articulos.value = papaRepo.getArticulos() }
    }

    fun seleccionarArticulo(articuloId: Int) {
        viewModelScope.launch { _registros.value = papaRepo.getRegistros(articuloId) }
    }

    fun registrar(
        articuloId: Int,
        pesoCrudo: Double,
        pesoPelado: Double,
        pesoPrefrito: Double,
        pesoFrito: Double
    ) {
        viewModelScope.launch {
            papaRepo.registrar(articuloId, pesoCrudo, pesoPelado, pesoPrefrito, pesoFrito)
            _registros.value = papaRepo.getRegistros(articuloId)
        }
    }

    fun eliminar(id: Int, articuloId: Int) {
        viewModelScope.launch {
            papaRepo.eliminar(id)
            _registros.value = papaRepo.getRegistros(articuloId)
        }
    }

    fun aplicarRendimientoAlArticulo(articuloId: Int, rendimiento: Double) {
        viewModelScope.launch {
            articuloRepo.actualizarRendimiento(articuloId, rendimiento)
        }
    }
}
