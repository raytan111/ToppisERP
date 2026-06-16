package com.toppis.app.ui.foodcost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.repository.FoodCostItem
import com.toppis.app.data.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Clasificación de menu engineering. */
enum class ClaseMenu(val label: String) {
    ESTRELLA("⭐ Estrella"),
    VACA("🐄 Caballo de batalla"),
    PUZZLE("🧩 Puzzle"),
    PERRO("🐶 Perro"),
    SIN_DATOS("Sin receta")
}

data class FoodCostFila(
    val item: FoodCostItem,
    val clase: ClaseMenu
)

class FoodCostViewModel(
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _filas = MutableStateFlow<List<FoodCostFila>>(emptyList())
    val filas: StateFlow<List<FoodCostFila>> = _filas.asStateFlow()

    private val _foodCostPromedio = MutableStateFlow(0.0)
    val foodCostPromedio: StateFlow<Double> = _foodCostPromedio.asStateFlow()

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            val items = menuRepository.getFoodCostItems()
            val conReceta = items.filter { it.costoTeorico > 0 }
            val margenProm = if (conReceta.isNotEmpty()) conReceta.map { it.margen }.average() else 0.0

            _filas.value = items.map { fc ->
                val clase = when {
                    fc.costoTeorico <= 0.0 -> ClaseMenu.SIN_DATOS
                    fc.margen >= margenProm && fc.foodCostPct <= 35 -> ClaseMenu.ESTRELLA
                    fc.margen < margenProm && fc.foodCostPct <= 35 -> ClaseMenu.VACA
                    fc.margen >= margenProm -> ClaseMenu.PUZZLE
                    else -> ClaseMenu.PERRO
                }
                FoodCostFila(fc, clase)
            }.sortedByDescending { it.item.foodCostPct }

            _foodCostPromedio.value = if (conReceta.isNotEmpty()) conReceta.map { it.foodCostPct }.average() else 0.0
        }
    }
}
