package com.toppis.app.ui.papa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.ArticuloRepository
import com.toppis.app.data.repository.PapaRendimientoRepository

class PapaRendimientoViewModelFactory(
    private val papaRepo: PapaRendimientoRepository,
    private val articuloRepo: ArticuloRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PapaRendimientoViewModel::class.java)) {
            return PapaRendimientoViewModel(papaRepo, articuloRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
