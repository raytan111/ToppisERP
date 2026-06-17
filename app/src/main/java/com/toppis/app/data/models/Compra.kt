package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Cabecera de compra — tabla "compras". */
@Serializable
data class Compra(
    val id: Int = 0,
    @SerialName("proveedor_id")
    val proveedorId: Int? = null,
    val fecha: String? = null,
    val total: Double = 0.0,
    @SerialName("tiene_iva")
    val tieneIva: Boolean = false,
    val nota: String = "",
    @SerialName("gasto_id")
    val gastoId: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)

/** Detalle de compra — tabla "compra_detalle". */
@Serializable
data class CompraDetalle(
    val id: Int = 0,
    @SerialName("compra_id")
    val compraId: Int,
    @SerialName("articulo_id")
    val articuloId: Int,
    @SerialName("cantidad_base")
    val cantidadBase: Double,
    @SerialName("costo_por_base")
    val costoPorBase: Double,
    val subtotal: Double = 0.0,
    val vencimiento: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
