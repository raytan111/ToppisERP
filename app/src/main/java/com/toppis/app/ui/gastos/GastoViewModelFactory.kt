package com.toppis.app.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.GastoRepository
import com.toppis.app.data.repository.SobreRepository

class GastoViewModelFactory(
    private val gastoRepository: GastoRepository,
    private val sobreRepository: SobreRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GastoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GastoViewModel(gastoRepository, sobreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

