package com.toppis.app.data.models

import com.toppis.app.data.db.entities.DimensionUnidad
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sub-receta / preparación producida por lote (tabla "preparaciones").
 * Ej: salsa bechamel, salsa cheddar. Genera stock con costo por unidad base.
 *
 * costo_base = costo_lote / rendimiento_lote
 */
@Serializable
data class Preparacion(
    val id: Int = 0,
    val nombre: String,
    val dimension: DimensionUnidad,
    @SerialName("unidad_base")
    val unidadBase: String,
    @SerialName("rendimiento_lote")
    val rendimientoLote: Double = 1.0,
    @SerialName("costo_lote")
    val costoLote: Double = 0.0,
    @SerialName("costo_base")
    val costoBase: Double = 0.0,
    @SerialName("stock_base")
    val stockBase: Double = 0.0,
    @SerialName("seleccionable_en_pos")
    val seleccionableEnPos: Boolean = false,
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
