package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de detalle de venta para Supabase (tabla "items_venta_menu").
 */
@Serializable
data class ItemVentaMenu(
    val id: Int = 0,
    @SerialName("venta_id")
    val ventaId: Int = 0,
    @SerialName("item_menu_id")
    val itemMenuId: Int,
    val cantidad: Int,
    @SerialName("precio_unitario")
    val precioUnitario: Double,
    val subtotal: Double,
    @SerialName("salsas_seleccionadas")
    val salsasSeleccionadas: String = "",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
