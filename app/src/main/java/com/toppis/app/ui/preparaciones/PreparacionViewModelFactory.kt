package com.toppis.app.ui.preparaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.PreparacionRepository

class PreparacionViewModelFactory(
    private val preparacionRepository: PreparacionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PreparacionViewModel::class.java)) {
            return PreparacionViewModel(preparacionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
