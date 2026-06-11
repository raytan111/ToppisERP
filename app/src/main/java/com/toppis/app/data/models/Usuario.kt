package com.toppis.app.data.models

import com.toppis.app.data.db.entities.Rol
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de usuario para Supabase (tabla "usuarios").
 *
 * El [id] es un UUID (String) que referencia a auth.users de Supabase.
 * El campo [rol] se serializa como "ADMIN" / "CAJERO", coincidiendo con
 * el enum PostgreSQL `rol`.
 *
 * Los campos de auditoría son nullable porque la base de datos los completa
 * automáticamente y no siempre se envían desde el cliente.
 */
@Serializable
data class Usuario(
    val id: String = "",
    val nombre: String,
    val email: String,
    val rol: Rol,
    val activo: Boolean = true,
    @SerialName("fecha_creacion")
    val fechaCreacion: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
