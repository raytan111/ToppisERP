package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Proveedor de mercadería — tabla "proveedores".
 */
@Serializable
data class Proveedor(
    val id: Int = 0,
    val nombre: String,
    val contacto: String = "",
    val telefono: String = "",
    val email: String = "",
    val nota: String = "",
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
