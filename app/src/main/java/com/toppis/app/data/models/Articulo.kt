package com.toppis.app.data.models

import com.toppis.app.data.db.entities.DimensionUnidad
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de artículo unificado para Supabase (tabla "articulos").
 * Reemplaza ingredientes + insumos. Stock y costo en unidad base.
 *
 * costo_base = (costo_compra / factor_compra) / rendimiento
 */
@Serializable
data class Articulo(
    val id: Int = 0,
    val nombre: String,
    val dimension: DimensionUnidad,
    @SerialName("unidad_base")
    val unidadBase: String,
    @SerialName("unidad_compra")
    val unidadCompra: String = "",
    @SerialName("factor_compra")
    val factorCompra: Double = 1.0,
    @SerialName("costo_compra")
    val costoCompra: Double = 0.0,
    @SerialName("costo_base")
    val costoBase: Double = 0.0,
    val rendimiento: Double = 1.0,
    @SerialName("stock_base")
    val stockBase: Double = 0.0,
    @SerialName("par_level")
    val parLevel: Double = 0.0,
    val perecible: Boolean = false,
    @SerialName("vida_util_dias")
    val vidaUtilDias: Int = 0,
    @SerialName("es_vendible")
    val esVendible: Boolean = false,
    @SerialName("seleccionable_en_pos")
    val seleccionableEnPos: Boolean = false,
    @SerialName("cantidad_pos")
    val cantidadPos: Double = 0.0,
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
) {
    companion object {
        /** Calcula el costo por unidad base. */
        fun calcularCostoBase(costoCompra: Double, factorCompra: Double, rendimiento: Double): Double {
            if (factorCompra <= 0.0 || rendimiento <= 0.0) return 0.0
            return (costoCompra / factorCompra) / rendimiento
        }
    }
}
