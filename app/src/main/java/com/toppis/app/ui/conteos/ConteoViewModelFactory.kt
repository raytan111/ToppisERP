package com.toppis.app.ui.conteos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.ConteoRepository

class ConteoViewModelFactory(
    private val repository: ConteoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConteoViewModel::class.java)) {
            return ConteoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
