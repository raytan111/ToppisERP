package com.toppis.app.data.models

import com.toppis.app.data.db.entities.TipoComponente
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Componente de la receta de una preparación (tabla "preparacion_componentes").
 * Puede referir a un artículo o a otra preparación (anidación permitida).
 */
@Serializable
data class PreparacionComponente(
    val id: Int = 0,
    @SerialName("preparacion_id")
    val preparacionId: Int,
    @SerialName("tipo_componente")
    val tipoComponente: TipoComponente,
    @SerialName("componente_id")
    val componenteId: Int,
    @SerialName("cantidad_base")
    val cantidadBase: Double,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
