package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de salsa/complemento para Supabase (tabla "salsas").
 */
@Serializable
data class Salsa(
    val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    val activa: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
