package com.toppis.app.ui.modificadores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.ModificadorRepository

class ModificadorViewModelFactory(
    private val modificadorRepository: ModificadorRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModificadorViewModel::class.java)) {
            return ModificadorViewModel(modificadorRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
