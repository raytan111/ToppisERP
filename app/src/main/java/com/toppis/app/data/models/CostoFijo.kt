package com.toppis.app.data.models

import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.db.entities.Periodicidad
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Costo fijo recurrente (tabla "costos_fijos"). Monto en CLP con IVA incluido.
 * Se prorratea a la semana según su periodicidad.
 */
@Serializable
data class CostoFijo(
    val id: Int = 0,
    val nombre: String,
    val categoria: CategoriaGasto,
    val monto: Double = 0.0,
    val periodicidad: Periodicidad = Periodicidad.MENSUAL,
    val activo: Boolean = true,
    @SerialName("local_id")
    val localId: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
