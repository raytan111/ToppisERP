package com.toppis.app.data.models

import com.toppis.app.data.db.entities.TipoPromocion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Promoción (tabla "promociones"). Creador manual con análisis de costo/ganancia.
 */
@Serializable
data class Promocion(
    val id: Int = 0,
    val nombre: String,
    val tipo: TipoPromocion,
    val precio: Double = 0.0,
    @SerialName("descuento_pct")
    val descuentoPct: Double = 0.0,
    @SerialName("fecha_inicio")
    val fechaInicio: String? = null,
    @SerialName("fecha_fin")
    val fechaFin: String? = null,
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)

/**
 * Item incluido en una promoción (tabla "promocion_items").
 */
@Serializable
data class PromocionItem(
    val id: Int = 0,
    @SerialName("promocion_id")
    val promocionId: Int,
    @SerialName("item_menu_id")
    val itemMenuId: Int,
    val cantidad: Int = 1,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)

/**
 * Resultado del análisis de una promoción (no es tabla, se calcula).
 */
data class AnalisisPromocion(
    val costoPromo: Double,
    val precioNormal: Double,
    val precioPromo: Double,
    val ganancia: Double,
    val gananciaPct: Double,
    val foodCostPct: Double,
    val ahorroCliente: Double
)
