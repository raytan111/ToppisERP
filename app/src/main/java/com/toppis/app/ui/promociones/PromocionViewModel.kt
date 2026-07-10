package com.toppis.app.ui.promociones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.TipoPromocion
import com.toppis.app.data.models.AnalisisPromocion
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Promocion
import com.toppis.app.data.repository.PromocionItemDetalle
import com.toppis.app.data.repository.PromocionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PromocionUiState {
    object Idle : PromocionUiState()
    data class Error(val message: String) : PromocionUiState()
    object Success : PromocionUiState()
}

class PromocionViewModel(
    private val promocionRepository: PromocionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PromocionUiState>(PromocionUiState.Idle)
    val uiState: StateFlow<PromocionUiState> = _uiState.asStateFlow()

    private val _promociones = MutableStateFlow<List<Promocion>>(emptyList())
    val promociones: StateFlow<List<Promocion>> = _promociones.asStateFlow()

    private val _itemsMenu = MutableStateFlow<List<ItemMenu>>(emptyList())
    val itemsMenu: StateFlow<List<ItemMenu>> = _itemsMenu.asStateFlow()

    init {
        refrescarPromociones()
        refrescarItemsMenu()
    }

    private fun refrescarPromociones() {
        viewModelScope.launch {
            try {
                _promociones.value = promocionRepository.getPromociones()
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al cargar promociones")
            }
        }
    }

    private fun refrescarItemsMenu() {
        viewModelScope.launch {
            try {
                _itemsMenu.value = promocionRepository.getItemsMenu()
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al cargar items del menú")
            }
        }
    }

    fun crearPromocion(
        nombre: String,
        tipo: TipoPromocion,
        precio: Double,
        descuentoPct: Double
    ) {
        viewModelScope.launch {
            try {
                promocionRepository.crearPromocion(nombre, tipo, precio, descuentoPct)
                refrescarPromociones()
                _uiState.value = PromocionUiState.Success
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al crear promoción")
            }
        }
    }

    /** Actualiza nombre/tipo/precio/descuento de una promoción. */
    fun actualizarPromocion(promo: Promocion) {
        viewModelScope.launch {
            try {
                promocionRepository.actualizarPromocion(promo)
                refrescarPromociones()
                _uiState.value = PromocionUiState.Success
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al actualizar promoción")
            }
        }
    }

    /** Actualiza la imagen de una promoción. */
    fun actualizarImagen(id: Int, url: String) {
        viewModelScope.launch {
            try {
                promocionRepository.actualizarImagen(id, url)
                refrescarPromociones()
                _uiState.value = PromocionUiState.Success
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al actualizar imagen")
            }
        }
    }

    fun eliminarPromocion(id: Int) {
        viewModelScope.launch {
            try {
                promocionRepository.eliminarPromocion(id)
                refrescarPromociones()
                _uiState.value = PromocionUiState.Success
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al eliminar promoción")
            }
        }
    }

    /** Resuelve los items de una promo contra el catálogo del menú y calcula su análisis. */
    fun cargarDetalle(
        promocion: Promocion,
        callback: (items: List<PromocionItemDetalle>, analisis: AnalisisPromocion) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val promoItems = promocionRepository.getItems(promocion.id)
                val menu = _itemsMenu.value.ifEmpty { promocionRepository.getItemsMenu() }
                val detalle = promoItems.mapNotNull { pi ->
                    menu.firstOrNull { it.id == pi.itemMenuId }?.let { PromocionItemDetalle(pi, it) }
                }
                val analisis = promocionRepository.analizar(promocion, detalle)
                callback(detalle, analisis)
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al cargar detalle de promoción")
            }
        }
    }

    fun agregarItem(promocionId: Int, itemMenuId: Int, cantidad: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                promocionRepository.agregarItem(promocionId, itemMenuId, cantidad)
                onDone()
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al agregar item")
            }
        }
    }

    fun eliminarItem(id: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                promocionRepository.eliminarItem(id)
                onDone()
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al eliminar item")
            }
        }
    }

    // ── Espacios configurables ──────────────────────────────────────────────────

    /** Carga los espacios de una promo y, por cada uno, sus opciones (para modo LISTA). */
    fun cargarEspacios(
        promocionId: Int,
        callback: (espacios: List<com.toppis.app.data.models.PromocionEspacio>,
                   opcionesPorEspacio: Map<Int, List<com.toppis.app.data.models.PromocionEspacioOpcion>>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val espacios = promocionRepository.getEspacios(promocionId)
                val opciones = espacios.associate { it.id to promocionRepository.getOpciones(it.id) }
                callback(espacios, opciones)
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al cargar espacios")
            }
        }
    }

    fun crearEspacio(
        promocionId: Int,
        nombre: String,
        cantidad: Int,
        modo: com.toppis.app.data.db.entities.ModoEspacioPromo,
        categoria: String?,
        orden: Int,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                promocionRepository.crearEspacio(promocionId, nombre, cantidad, modo, categoria, orden)
                onDone()
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al crear espacio")
            }
        }
    }

    fun eliminarEspacio(id: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                promocionRepository.eliminarEspacio(id); onDone()
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al eliminar espacio")
            }
        }
    }

    fun agregarOpcion(espacioId: Int, itemMenuId: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                promocionRepository.agregarOpcion(espacioId, itemMenuId); onDone()
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al agregar opción")
            }
        }
    }

    fun eliminarOpcion(id: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                promocionRepository.eliminarOpcion(id); onDone()
            } catch (e: Exception) {
                _uiState.value = PromocionUiState.Error(e.message ?: "Error al eliminar opción")
            }
        }
    }

    fun resetState() {
        _uiState.value = PromocionUiState.Idle
    }
}
