package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de insumo para Supabase (tabla "insumos").
 * Productos con stock unitario (ej: packaging, bebidas).
 */
@Serializable
data class Insumo(
    val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    val precio: Double = 0.0,
    val stock: Int = 0,
    @SerialName("unidad_medida")
    val unidadMedida: String,
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
