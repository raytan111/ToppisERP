package com.toppis.app.ui.compras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.ArticuloRepository

class CompraSugeridaViewModelFactory(
    private val repository: ArticuloRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompraSugeridaViewModel::class.java)) {
            return CompraSugeridaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
