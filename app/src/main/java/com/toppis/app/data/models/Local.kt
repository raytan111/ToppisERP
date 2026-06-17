package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Local / sucursal — tabla "locales". */
@Serializable
data class Local(
    val id: Int = 0,
    val nombre: String,
    val direccion: String = "",
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
