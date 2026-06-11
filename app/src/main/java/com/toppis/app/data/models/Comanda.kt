package com.toppis.app.data.models

import com.toppis.app.data.db.entities.EstadoComanda
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de comanda (orden de cocina) para Supabase (tabla "comandas").
 */
@Serializable
data class Comanda(
    val id: Int = 0,
    @SerialName("venta_id")
    val ventaId: Int,
    val fecha: String? = null,
    @SerialName("detalle_texto")
    val detalleTexto: String,
    val estado: EstadoComanda = EstadoComanda.PENDIENTE,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
