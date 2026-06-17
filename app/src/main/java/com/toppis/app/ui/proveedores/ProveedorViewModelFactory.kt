package com.toppis.app.ui.proveedores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.ProveedorRepository

class ProveedorViewModelFactory(
    private val repository: ProveedorRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProveedorViewModel::class.java)) {
            return ProveedorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
