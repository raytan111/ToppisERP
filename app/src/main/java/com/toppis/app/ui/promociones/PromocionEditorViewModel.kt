package com.toppis.app.ui.promociones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.ModoEspacioPromo
import com.toppis.app.data.db.entities.TipoPromocion
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Promocion
import com.toppis.app.data.repository.PromocionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Grupo de elección en edición (en memoria hasta guardar). */
data class GrupoDraft(
    val nombre: String,
    val cantidad: Int,
    val modo: ModoEspacioPromo,
    val categoria: String?,          // si modo = CATEGORIA
    val permiteRepetir: Boolean,
    val opciones: List<Int>          // itemMenuId (si modo = LISTA)
)

/**
 * ViewModel del editor de promociones (pantalla dedicada). Mantiene un borrador en
 * memoria (nombre, precio, imagen, grupos) y lo persiste al guardar. Al editar, si la
 * promo ya existe, reemplaza sus grupos por los del borrador.
 */
class PromocionEditorViewModel(
    private val repo: PromocionRepository
) : ViewModel() {

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _precioText = MutableStateFlow("")
    val precioText: StateFlow<String> = _precioText.asStateFlow()

    private val _imagenUrl = MutableStateFlow<String?>(null)
    val imagenUrl: StateFlow<String?> = _imagenUrl.asStateFlow()

    private val _grupos = MutableStateFlow<List<GrupoDraft>>(emptyList())
    val grupos: StateFlow<List<GrupoDraft>> = _grupos.asStateFlow()

    private val _itemsMenu = MutableStateFlow<List<ItemMenu>>(emptyList())
    val itemsMenu: StateFlow<List<ItemMenu>> = _itemsMenu.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var promocionId: Int? = null

    val esValido: Boolean
        get() = _nombre.value.isNotBlank() &&
            (_precioText.value.replace(",", ".").toDoubleOrNull()?.let { it > 0 } ?: false) &&
            _grupos.value.isNotEmpty() &&
            _grupos.value.all { g ->
                g.cantidad >= 1 && (g.modo == ModoEspacioPromo.CATEGORIA && !g.categoria.isNullOrBlank() ||
                    g.modo == ModoEspacioPromo.LISTA && g.opciones.isNotEmpty())
            }

    fun cargar(id: Int?) {
        promocionId = id
        viewModelScope.launch {
            _cargando.value = true
            try {
                _itemsMenu.value = repo.getItemsMenu()
                if (id != null) {
                    val promo = repo.getPromociones().firstOrNull { it.id == id }
                    if (promo != null) {
                        _nombre.value = promo.nombre
                        _precioText.value = if (promo.precio == 0.0) "" else promo.precio.toLong().toString()
                        _imagenUrl.value = promo.imagenUrl
                        val espacios = repo.getEspacios(id)
                        _grupos.value = espacios.map { esp ->
                            GrupoDraft(
                                nombre = esp.nombre,
                                cantidad = esp.cantidad,
                                modo = esp.modo,
                                categoria = esp.categoria,
                                permiteRepetir = esp.permiteRepetir,
                                opciones = if (esp.modo == ModoEspacioPromo.LISTA)
                                    repo.getOpciones(esp.id).map { it.itemMenuId } else emptyList()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al cargar la promoción"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun setNombre(v: String) { _nombre.value = v }
    fun setPrecio(v: String) { _precioText.value = v }
    fun setImagen(url: String) { _imagenUrl.value = url }

    fun agregarGrupo(g: GrupoDraft) { _grupos.value = _grupos.value + g }
    fun actualizarGrupo(index: Int, g: GrupoDraft) {
        _grupos.value = _grupos.value.toMutableList().also { if (index in it.indices) it[index] = g }
    }
    fun quitarGrupo(index: Int) {
        _grupos.value = _grupos.value.toMutableList().also { if (index in it.indices) it.removeAt(index) }
    }

    /** Persiste la promo con sus grupos/opciones. Llama [onSaved] con el id al terminar. */
    fun guardar(onSaved: (Int) -> Unit) {
        if (!esValido) { _error.value = "Completá nombre, precio y al menos un grupo válido."; return }
        val precio = _precioText.value.replace(",", ".").toDoubleOrNull() ?: return
        viewModelScope.launch {
            try {
                val id = promocionId ?: repo.crearPromocion(_nombre.value.trim(), TipoPromocion.COMBO, precio, 0.0)
                if (id == null) { _error.value = "No se pudo crear la promoción"; return@launch }
                if (promocionId != null) {
                    repo.actualizarPromocion(
                        Promocion(id = id, nombre = _nombre.value.trim(), tipo = TipoPromocion.COMBO, precio = precio, activo = true)
                    )
                    // Reemplazar grupos: borrar los existentes y recrear desde el borrador.
                    repo.getEspacios(id).forEach { repo.eliminarEspacio(it.id) }
                }
                _imagenUrl.value?.let { repo.actualizarImagen(id, it) }
                _grupos.value.forEachIndexed { orden, g ->
                    val espId = repo.crearEspacio(id, g.nombre.ifBlank { "Grupo" }, g.cantidad, g.modo,
                        if (g.modo == ModoEspacioPromo.CATEGORIA) g.categoria else null, orden, g.permiteRepetir)
                    if (g.modo == ModoEspacioPromo.LISTA) {
                        g.opciones.forEach { repo.agregarOpcion(espId, it) }
                    }
                }
                onSaved(id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al guardar la promoción"
            }
        }
    }

    fun resetError() { _error.value = null }
}

class PromocionEditorViewModelFactory(
    private val repo: PromocionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PromocionEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PromocionEditorViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
