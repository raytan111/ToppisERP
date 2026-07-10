package com.toppis.app.data.models

import com.toppis.app.data.db.entities.AccionModificador
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.db.entities.TipoModificador
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modificador aplicable a un item del menú (tabla "modificadores").
 * Define un delta de precio; su delta de receta vive en modificador_componentes.
 */
@Serializable
data class Modificador(
    val id: Int = 0,
    val nombre: String,
    val tipo: TipoModificador,
    @SerialName("item_menu_id")
    val itemMenuId: Int? = null,
    /** Categoría del menú a la que aplica (ej "Hamburguesas"); null = solo por item. */
    val categoria: String? = null,
    @SerialName("delta_precio")
    val deltaPrecio: Double = 0.0,
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)

/**
 * Componente del delta de receta de un modificador (tabla "modificador_componentes").
 */
@Serializable
data class ModificadorComponente(
    val id: Int = 0,
    @SerialName("modificador_id")
    val modificadorId: Int,
    val accion: AccionModificador,
    @SerialName("tipo_componente")
    val tipoComponente: TipoComponente,
    @SerialName("componente_id")
    val componenteId: Int,
    @SerialName("cantidad_base")
    val cantidadBase: Double,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
