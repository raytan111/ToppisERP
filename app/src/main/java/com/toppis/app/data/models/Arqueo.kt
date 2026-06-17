package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Arqueo de caja de un sobre — tabla "arqueos". */
@Serializable
data class Arqueo(
    val id: Int = 0,
    @SerialName("sobre_id")
    val sobreId: Int,
    val fecha: String? = null,
    @SerialName("saldo_sistema")
    val saldoSistema: Double = 0.0,
    @SerialName("saldo_contado")
    val saldoContado: Double = 0.0,
    val diferencia: Double = 0.0,
    val ajustado: Boolean = false,
    val nota: String = "",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
