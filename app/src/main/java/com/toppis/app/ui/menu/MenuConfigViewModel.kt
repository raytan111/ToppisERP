package com.toppis.app.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Ingrediente
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Insumo
import com.toppis.app.data.models.Salsa
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.repository.ComponenteReceta
import com.toppis.app.data.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MenuConfigUiState {
    object Idle : MenuConfigUiState()
    data class Error(val message: String) : MenuConfigUiState()
    object Success : MenuConfigUiState()
}

class MenuConfigViewModel(
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MenuConfigUiState>(MenuConfigUiState.Idle)
    val uiState: StateFlow<MenuConfigUiState> = _uiState.asStateFlow()

    private val _itemsMenu = MutableStateFlow<List<ItemMenu>>(emptyList())
    val itemsMenu: StateFlow<List<ItemMenu>> = _itemsMenu.asStateFlow()

    private val _salsas = MutableStateFlow<List<Salsa>>(emptyList())
    val salsas: StateFlow<List<Salsa>> = _salsas.asStateFlow()

    private val _ingredientes = MutableStateFlow<List<Ingrediente>>(emptyList())
    val ingredientes: StateFlow<List<Ingrediente>> = _ingredientes.asStateFlow()

    private val _insumos = MutableStateFlow<List<Insumo>>(emptyList())
    val insumos: StateFlow<List<Insumo>> = _insumos.asStateFlow()

    init {
        refrescarItems()
        refrescarSalsas()
        refrescarIngredientes()
        refrescarInsumos()
        viewModelScope.launch { menuRepository.observeItemsMenu().collect { refrescarItems() } }
        viewModelScope.launch { menuRepository.observeSalsas().collect { refrescarSalsas() } }
    }

    private fun refrescarItems() {
        viewModelScope.launch { _itemsMenu.value = menuRepository.getAllItemsMenu() }
    }

    private fun refrescarSalsas() {
        viewModelScope.launch { _salsas.value = menuRepository.getAllSalsas() }
    }

    private fun refrescarIngredientes() {
        viewModelScope.launch { _ingredientes.value = menuRepository.getIngredientes() }
    }

    private fun refrescarInsumos() {
        viewModelScope.launch { _insumos.value = menuRepository.getInsumos() }
    }

    // ── ItemMenu CRUD ───────────────────────────────────────────────────────────

    fun crearItemMenu(nombre: String, descripcion: String, precio: Double) {
        viewModelScope.launch {
            try {
                menuRepository.crearItemMenu(nombre, descripcion, precio)
                refrescarItems()
                _uiState.value = MenuConfigUiState.Success
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al crear item")
            }
        }
    }

    fun actualizarItemMenu(item: ItemMenu) {
        viewModelScope.launch {
            try {
                menuRepository.actualizarItemMenu(item)
                refrescarItems()
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al actualizar")
            }
        }
    }

    fun eliminarItemMenu(item: ItemMenu) {
        viewModelScope.launch {
            try {
                menuRepository.eliminarItemMenu(item)
                refrescarItems()
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    // ── RecetaMenu ──────────────────────────────────────────────────────────────

    fun loadComponentesReceta(itemMenuId: Int, callback: (List<ComponenteReceta>) -> Unit) {
        viewModelScope.launch {
            try {
                val componentes = menuRepository.getComponentesReceta(itemMenuId)
                callback(componentes)
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al cargar receta")
            }
        }
    }

    fun agregarComponente(
        itemMenuId: Int,
        tipo: TipoComponente,
        componenteId: Int,
        cantidad: Double
    ) {
        viewModelScope.launch {
            try {
                menuRepository.agregarComponente(itemMenuId, tipo, componenteId, cantidad)
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al agregar componente")
            }
        }
    }

    // ── Salsas CRUD ─────────────────────────────────────────────────────────────

    fun crearSalsa(nombre: String, descripcion: String = "") {
        viewModelScope.launch {
            try {
                menuRepository.crearSalsa(nombre, descripcion)
                refrescarSalsas()
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al crear salsa")
            }
        }
    }

    fun eliminarSalsa(salsa: Salsa) {
        viewModelScope.launch {
            try {
                menuRepository.eliminarSalsa(salsa)
                refrescarSalsas()
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al eliminar salsa")
            }
        }
    }

    fun resetState() {
        _uiState.value = MenuConfigUiState.Idle
    }
}
