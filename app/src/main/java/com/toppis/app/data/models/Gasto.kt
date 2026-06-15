package com.toppis.app.data.models

import com.toppis.app.data.db.entities.CategoriaGasto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de gasto para Supabase (tabla "gastos").
 */
@Serializable
data class Gasto(
    val id: Long = 0,
    val descripcion: String,
    val monto: Double,
    val categoria: CategoriaGasto,
    @SerialName("sobre_id")
    val sobreId: Int? = null,
    @SerialName("usuario_id")
    val usuarioId: String? = null,
    val fecha: String? = null,
    val comprobante: String? = null,
    @SerialName("tiene_iva")
    val tieneIva: Boolean = false,
    @SerialName("monto_neto")
    val montoNeto: Double? = null,
    @SerialName("monto_iva")
    val montoIva: Double? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
