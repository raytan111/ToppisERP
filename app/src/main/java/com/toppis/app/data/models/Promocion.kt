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
    @SerialName("imagen_url")
    val imagenUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)

/**
 * Espacio configurable de una promo (tabla "promocion_espacios"): un "slot" que el
 * cajero completa eligiendo, por lista de opciones o por categoría del menú.
 */
@Serializable
data class PromocionEspacio(
    val id: Int = 0,
    @SerialName("promocion_id")
    val promocionId: Int,
    val nombre: String,
    val cantidad: Int = 1,
    val modo: com.toppis.app.data.db.entities.ModoEspacioPromo,
    val categoria: String? = null,
    @SerialName("permite_repetir")
    val permiteRepetir: Boolean = true,
    val orden: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null
)

/** Opción elegible de un espacio en modo LISTA (tabla "promocion_espacio_opciones"). */
@Serializable
data class PromocionEspacioOpcion(
    val id: Int = 0,
    @SerialName("espacio_id")
    val espacioId: Int,
    @SerialName("item_menu_id")
    val itemMenuId: Int,
    @SerialName("created_at")
    val createdAt: String? = null
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
