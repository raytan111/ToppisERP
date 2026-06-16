package com.toppis.app.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.toppis.app.data.repository.ComandaRepository
import com.toppis.app.data.repository.ComprobanteRepository
import com.toppis.app.data.repository.MenuRepository
import com.toppis.app.data.repository.ModificadorRepository
import com.toppis.app.data.repository.SobreRepository
import com.toppis.app.data.repository.VentaRepository

class PosViewModelFactory(
    private val ventaRepository: VentaRepository,
    private val sobreRepository: SobreRepository,
    private val menuRepository: MenuRepository,
    private val comandaRepository: ComandaRepository,
    private val comprobanteRepository: ComprobanteRepository,
    private val modificadorRepository: ModificadorRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PosViewModel(
                ventaRepository, sobreRepository, menuRepository,
                comandaRepository, comprobanteRepository, modificadorRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
