package com.toppis.app.ui.costos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.repository.ConfigCostosRepository
import com.toppis.app.data.repository.ObjetivosCostos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ObjetivosViewModel(
    private val repo: ConfigCostosRepository
) : ViewModel() {

    private val _objetivos = MutableStateFlow(ObjetivosCostos())
    val objetivos: StateFlow<ObjetivosCostos> = _objetivos.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _cargando.value = true
            _objetivos.value = repo.getObjetivos()
            _cargando.value = false
        }
    }

    /** Guarda los objetivos. Los porcentajes se reciben como fracción (0.32 = 32%). */
    fun guardar(pctFood: Double, pctManoObra: Double, pctArriendo: Double, umbralContratar: Double) {
        viewModelScope.launch {
            repo.guardar(ConfigCostosRepository.KEY_FOOD, pctFood)
            repo.guardar(ConfigCostosRepository.KEY_MANO_OBRA, pctManoObra)
            repo.guardar(ConfigCostosRepository.KEY_ARRIENDO, pctArriendo)
            repo.guardar(ConfigCostosRepository.KEY_UMBRAL_CONTRATAR, umbralContratar)
            _objetivos.value = ObjetivosCostos(pctFood, pctManoObra, pctArriendo, umbralContratar)
            _guardado.value = true
        }
    }

    fun resetGuardado() { _guardado.value = false }
}

class ObjetivosViewModelFactory(
    private val repo: ConfigCostosRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ObjetivosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ObjetivosViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
