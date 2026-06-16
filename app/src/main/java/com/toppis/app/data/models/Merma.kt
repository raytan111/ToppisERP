package com.toppis.app.data.models

import com.toppis.app.data.db.entities.MotivoMerma
import com.toppis.app.data.db.entities.TipoComponente
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Registro de merma (pérdida de stock con motivo) — tabla "mermas".
 */
@Serializable
data class Merma(
    val id: Int = 0,
    @SerialName("tipo_componente")
    val tipoComponente: TipoComponente,
    @SerialName("componente_id")
    val componenteId: Int,
    @SerialName("cantidad_base")
    val cantidadBase: Double,
    val motivo: MotivoMerma,
    val costo: Double = 0.0,
    val nota: String = "",
    val fecha: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
