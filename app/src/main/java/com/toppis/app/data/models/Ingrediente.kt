package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de ingrediente para Supabase (tabla "ingredientes").
 * Insumos de cocina con costo, merma y stock fraccionable.
 */
@Serializable
data class Ingrediente(
    val id: Int = 0,
    val nombre: String,
    @SerialName("unidad_medida")
    val unidadMedida: String,
    @SerialName("stock_actual")
    val stockActual: Double = 0.0,
    @SerialName("costo_unitario")
    val costoUnitario: Double = 0.0,
    @SerialName("costo_compra")
    val costoCompra: Double = 0.0,
    @SerialName("porcentaje_merma")
    val porcentajeMerma: Double = 0.0,
    @SerialName("unidad_compra")
    val unidadCompra: String = "",
    @SerialName("cantidad_comprada")
    val cantidadComprada: Double = 0.0,
    @SerialName("cantidad_aprovechable")
    val cantidadAprovechable: Double = 0.0,
    @SerialName("costo_gramo")
    val costoGramo: Double = 0.0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
