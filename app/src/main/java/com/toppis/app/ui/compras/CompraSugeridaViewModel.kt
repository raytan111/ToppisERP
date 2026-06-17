package com.toppis.app.ui.compras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.UnidadMedida
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.repository.ArticuloRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Sugerencia de compra para un artículo bajo su par level. */
data class CompraSugerida(
    val articulo: Articulo,
    val faltanteBase: Double,
    val faltanteCompra: Double,   // en unidad de compra (kg/L/un)
    val unidadCompra: String,
    val costoEstimado: Double
)

class CompraSugeridaViewModel(
    private val repository: ArticuloRepository
) : ViewModel() {

    private val _sugerencias = MutableStateFlow<List<CompraSugerida>>(emptyList())
    val sugerencias: StateFlow<List<CompraSugerida>> = _sugerencias.asStateFlow()

    private val _costoTotal = MutableStateFlow(0.0)
    val costoTotal: StateFlow<Double> = _costoTotal.asStateFlow()

    init {
        refrescar()
        viewModelScope.launch { repository.observeArticulos().collect { refrescar() } }
    }

    fun refrescar() {
        viewModelScope.launch {
            val articulos = repository.getArticulosActivos()
            val lista = articulos.filter { it.parLevel > 0 && it.stockBase < it.parLevel }.map { a ->
                val faltanteBase = a.parLevel - a.stockBase
                val factor = UnidadMedida.porAbreviatura(a.unidadCompra)?.factorBase ?: 1.0
                val faltanteCompra = if (factor > 0) faltanteBase / factor else faltanteBase
                // costo por unidad base de compra (sin rendimiento) = costo_compra / factor_compra
                val costoBaseCompra = if (a.factorCompra > 0) a.costoCompra / a.factorCompra else 0.0
                val costoEstimado = faltanteBase * costoBaseCompra
                CompraSugerida(a, faltanteBase, faltanteCompra, a.unidadCompra, costoEstimado)
            }.sortedByDescending { it.costoEstimado }
            _sugerencias.value = lista
            _costoTotal.value = lista.sumOf { it.costoEstimado }
        }
    }
}
