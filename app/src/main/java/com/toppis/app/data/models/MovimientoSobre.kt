package com.toppis.app.data.models

import com.toppis.app.data.db.entities.TipoMovimiento
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de movimiento de sobre para Supabase (tabla "movimientos_sobre").
 *
 * Para transferencias, [origenId] y [destinoId] indican el flujo del dinero.
 * Para INGRESO/EGRESO se usa solo uno de ellos según corresponda.
 */
@Serializable
data class MovimientoSobre(
    val id: Int = 0,
    @SerialName("origen_id")
    val origenId: Int? = null,
    @SerialName("destino_id")
    val destinoId: Int? = null,
    val monto: Double,
    val tipo: TipoMovimiento,
    val descripcion: String,
    val fecha: String? = null,
    @SerialName("usuario_id")
    val usuarioId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
