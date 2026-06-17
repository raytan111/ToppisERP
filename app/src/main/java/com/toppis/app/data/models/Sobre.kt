package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de sobre (caja de dinero) para Supabase (tabla "sobres").
 *
 * El [id] es SERIAL (Int). Los campos de auditoría y fecha los completa
 * la base de datos automáticamente.
 */
@Serializable
data class Sobre(
    val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    val saldo: Double = 0.0,
    val tipo: com.toppis.app.data.db.entities.TipoSobre = com.toppis.app.data.db.entities.TipoSobre.CUENTA,
    @SerialName("fecha_creacion")
    val fechaCreacion: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
