package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cliente identificado por los 3 últimos dígitos del WhatsApp (tabla "clientes").
 * El nombre es opcional al vender y se completa después. `sellosHamburguesa` es el
 * avance de la cuponera (6 pedidos con hamburguesa → una Cheese gratis).
 */
@Serializable
data class Cliente(
    val id: Int = 0,
    val telefono3: String,
    val nombre: String? = null,
    @SerialName("sellos_hamburguesa")
    val sellosHamburguesa: Int = 0,
    @SerialName("local_id")
    val localId: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    /** Etiqueta legible para la lista de pedidos: "123 · Juan" o solo "123". */
    val etiqueta: String
        get() = if (nombre.isNullOrBlank()) telefono3 else "$telefono3 · $nombre"
}
