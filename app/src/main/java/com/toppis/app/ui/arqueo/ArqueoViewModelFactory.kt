package com.toppis.app.ui.arqueo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.ArqueoRepository

class ArqueoViewModelFactory(
    private val repository: ArqueoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArqueoViewModel::class.java)) {
            return ArqueoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
