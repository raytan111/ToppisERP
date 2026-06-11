package com.toppis.app.ui.sobres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.SobreRepository

class SobreViewModelFactory(private val repository: SobreRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SobreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SobreViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

