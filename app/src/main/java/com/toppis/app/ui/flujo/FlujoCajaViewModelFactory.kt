package com.toppis.app.ui.flujo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.FlujoCajaRepository

class FlujoCajaViewModelFactory(
    private val repository: FlujoCajaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FlujoCajaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlujoCajaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

