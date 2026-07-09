package com.toppis.app.data.models

import com.toppis.app.data.db.entities.EstadoCierre
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Snapshot congelado del resultado de caja de una semana (tabla "cierres_semanales").
 * Una vez cerrada la semana, estos valores no se recalculan al cambiar precios.
 */
@Serializable
data class CierreSemanal(
    val id: Int = 0,
    @SerialName("semana_inicio")
    val semanaInicio: String,          // yyyy-MM-dd (lunes)
    @SerialName("semana_fin")
    val semanaFin: String,             // yyyy-MM-dd (sábado)
    @SerialName("ventas_cobradas")
    val ventasCobradas: Double = 0.0,
    @SerialName("costo_variable")
    val costoVariable: Double = 0.0,
    @SerialName("food_teorico")
    val foodTeorico: Double = 0.0,
    @SerialName("mano_obra_pagada")
    val manoObraPagada: Double = 0.0,
    @SerialName("fijos_prorrateados")
    val fijosProrrateados: Double = 0.0,
    val resultado: Double = 0.0,
    @SerialName("food_pct")
    val foodPct: Double = 0.0,
    @SerialName("labor_pct")
    val laborPct: Double = 0.0,
    @SerialName("break_even")
    val breakEven: Double? = null,
    @SerialName("margen_contribucion")
    val margenContribucion: Double = 0.0,
    val estado: EstadoCierre = EstadoCierre.CERRADO,
    @SerialName("local_id")
    val localId: Int? = null,
    @SerialName("closed_at")
    val closedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
