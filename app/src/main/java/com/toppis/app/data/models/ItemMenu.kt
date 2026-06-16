package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de item del menú para Supabase (tabla "items_menu").
 */
@Serializable
data class ItemMenu(
    val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    val precio: Double = 0.0,
    @SerialName("costo_teorico")
    val costoTeorico: Double = 0.0,
    val categoria: String = "",
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
