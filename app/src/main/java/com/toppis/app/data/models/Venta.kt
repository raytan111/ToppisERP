package com.toppis.app.data.models

import com.toppis.app.data.db.entities.EstadoVenta
import com.toppis.app.data.db.entities.MetodoPago
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de venta para Supabase (tabla "ventas").
 */
@Serializable
data class Venta(
    val id: Int = 0,
    val fecha: String? = null,
    val total: Double = 0.0,
    @SerialName("metodo_pago")
    val metodoPago: MetodoPago? = null,
    @SerialName("sobre_id")
    val sobreId: Int? = null,
    @SerialName("usuario_id")
    val usuarioId: String? = null,
    val estado: EstadoVenta = EstadoVenta.COMPLETADA,
    @SerialName("incluir_envio")
    val incluirEnvio: Boolean = false,
    @SerialName("monto_envio")
    val montoEnvio: Double = 0.0,
    @SerialName("stickers_enviados")
    val stickersEnviados: Int = 0,
    val descripcion: String? = null,
    val canal: String? = null,
    @SerialName("modo_entrega")
    val modoEntrega: String? = null,
    val origen: String = "APP",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
