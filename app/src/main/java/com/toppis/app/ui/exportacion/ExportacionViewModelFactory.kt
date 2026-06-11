package com.toppis.app.ui.exportacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.ExportacionRepository

class ExportacionViewModelFactory(
    private val repository: ExportacionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportacionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExportacionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
