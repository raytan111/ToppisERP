package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cabecera de un conteo de inventario (stock take) — tabla "conteos".
 */
@Serializable
data class Conteo(
    val id: Int = 0,
    val fecha: String? = null,
    val estado: String = "ABIERTO",
    val nota: String = "",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)

/**
 * Detalle de conteo por artículo — tabla "conteo_detalle".
 */
@Serializable
data class ConteoDetalle(
    val id: Int = 0,
    @SerialName("conteo_id")
    val conteoId: Int,
    @SerialName("articulo_id")
    val articuloId: Int,
    @SerialName("stock_sistema")
    val stockSistema: Double = 0.0,
    @SerialName("stock_contado")
    val stockContado: Double = 0.0,
    val diferencia: Double = 0.0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
