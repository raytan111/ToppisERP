package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Asignación de un usuario a un local con un rol — tabla "usuarios_locales". */
@Serializable
data class UsuarioLocal(
    val id: Int = 0,
    @SerialName("usuario_id")
    val usuarioId: String,
    @SerialName("local_id")
    val localId: Int,
    @SerialName("rol_local")
    val rolLocal: String = "ENCARGADO",
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null
)
