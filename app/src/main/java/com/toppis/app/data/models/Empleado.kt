package com.toppis.app.data.models

import com.toppis.app.data.db.entities.TipoPago
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Empleado — tabla "empleados". */
@Serializable
data class Empleado(
    val id: Int = 0,
    val nombre: String,
    val cargo: String = "",
    @SerialName("tipo_pago")
    val tipoPago: TipoPago = TipoPago.POR_TURNO,
    val monto: Double = 0.0,
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)

/** Jornada trabajada (turno/horas) — tabla "jornadas". */
@Serializable
data class Jornada(
    val id: Int = 0,
    @SerialName("empleado_id")
    val empleadoId: Int,
    val fecha: String? = null,
    val cantidad: Double = 1.0,
    val costo: Double = 0.0,
    val nota: String = "",
    @SerialName("created_at")
    val createdAt: String? = null
)

/** Propina del día (total) — tabla "propinas". */
@Serializable
data class Propina(
    val id: Int = 0,
    val fecha: String? = null,
    val monto: Double = 0.0,
    val nota: String = "",
    @SerialName("created_at")
    val createdAt: String? = null
)
