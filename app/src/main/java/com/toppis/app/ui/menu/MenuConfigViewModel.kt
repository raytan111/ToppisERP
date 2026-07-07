package com.toppis.app.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Preparacion
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

    private val _articulos = MutableStateFlow<List<Articulo>>(emptyList())
    val articulos: StateFlow<List<Articulo>> = _articulos.asStateFlow()

    private val _preparaciones = MutableStateFlow<List<Preparacion>>(emptyList())
    val preparaciones: StateFlow<List<Preparacion>> = _preparaciones.asStateFlow()

    init {
        refrescarItems()
        refrescarCatalogos()
        viewModelScope.launch { menuRepository.observeItemsMenu().collect { refrescarItems() } }
    }

    private fun refrescarItems() {
        viewModelScope.launch { _itemsMenu.value = menuRepository.getAllItemsMenu() }
    }

    private fun refrescarCatalogos() {
        viewModelScope.launch { _articulos.value = menuRepository.getArticulos() }
        viewModelScope.launch { _preparaciones.value = menuRepository.getPreparaciones() }
    }

    // ── ItemMenu CRUD ───────────────────────────────────────────────────────────

    fun crearItemMenu(nombre: String, descripcion: String, precio: Double, categoria: String = "", imagenUrl: String? = null) {
        viewModelScope.launch {
            try {
                menuRepository.crearItemMenu(nombre, descripcion, precio, categoria, imagenUrl)
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
                callback(menuRepository.getComponentesReceta(itemMenuId))
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al cargar receta")
            }
        }
    }

    fun agregarComponente(
        itemMenuId: Int,
        tipo: TipoComponente,
        componenteId: Int,
        cantidadBase: Double,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                menuRepository.agregarComponente(itemMenuId, tipo, componenteId, cantidadBase)
                refrescarItems()
                onDone()
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al agregar componente")
            }
        }
    }

    fun eliminarComponente(receta: com.toppis.app.data.models.RecetaMenu, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                menuRepository.eliminarComponente(receta)
                refrescarItems()
                onDone()
            } catch (e: Exception) {
                _uiState.value = MenuConfigUiState.Error(e.message ?: "Error al eliminar componente")
            }
        }
    }

    fun resetState() {
        _uiState.value = MenuConfigUiState.Idle
    }
}
