package com.toppis.app.data.models

import com.toppis.app.data.db.entities.TipoComponente
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de componente de receta para Supabase (tabla "recetas_menu").
 * Relaciona un item del menú con un ingrediente o insumo y su cantidad.
 */
@Serializable
data class RecetaMenu(
    val id: Int = 0,
    @SerialName("item_menu_id")
    val itemMenuId: Int,
    @SerialName("tipo_componente")
    val tipoComponente: TipoComponente,
    @SerialName("componente_id")
    val componenteId: Int,
    val cantidad: Double,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
