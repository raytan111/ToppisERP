package com.toppis.app.data.models

import com.toppis.app.data.db.entities.CategoriaGasto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de presupuesto para Supabase (tabla "presupuestos").
 * Único por (mes, anio, categoria_gasto).
 */
@Serializable
data class Presupuesto(
    val id: Int = 0,
    val mes: Int,
    val anio: Int,
    @SerialName("categoria_gasto")
    val categoria: CategoriaGasto,
    @SerialName("monto_presupuestado")
    val montoPresupuestado: Double,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
