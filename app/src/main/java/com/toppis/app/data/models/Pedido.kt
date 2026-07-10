package com.toppis.app.data.models

import com.toppis.app.data.db.entities.EstadoPedido
import com.toppis.app.data.db.entities.MetodoPago
import com.toppis.app.data.db.entities.TipoLineaPedido
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pedido / carrito (tabla "pedidos"). Capa operativa del POS: persiste en la nube y
 * se sincroniza por Realtime. Al pagar se materializa en `ventas`.
 */
@Serializable
data class Pedido(
    val id: Int = 0,
    @SerialName("cliente_id")
    val clienteId: Int? = null,
    val estado: EstadoPedido = EstadoPedido.ABIERTO,
    val pagado: Boolean = false,
    val entregado: Boolean = false,
    @SerialName("metodo_pago")
    val metodoPago: MetodoPago? = null,
    @SerialName("sobre_id")
    val sobreId: Int? = null,
    @SerialName("venta_id")
    val ventaId: Int? = null,
    @SerialName("zona_envio")
    val zonaEnvio: String = "SIN_ENVIO",
    @SerialName("monto_envio")
    val montoEnvio: Double = 0.0,
    val total: Double = 0.0,
    @SerialName("comanda_texto")
    val comandaTexto: String? = null,
    @SerialName("local_id")
    val localId: Int? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("closed_at")
    val closedAt: String? = null,
    @SerialName("paid_at")
    val paidAt: String? = null,
    @SerialName("delivered_at")
    val deliveredAt: String? = null
) {
    /** Debe la plata: se entregó pero no se pagó. */
    val tieneDeuda: Boolean get() = entregado && !pagado

    /** Sigue en la lista activa del POS mientras falte pagar o entregar. */
    val activo: Boolean get() = !(pagado && entregado)
}

/**
 * Línea de cobro de un pedido (tabla "pedido_items"). Un PRODUCTO o una PROMO.
 * El precio/subtotal viven acá; las unidades a preparar en `pedido_unidades`.
 */
@Serializable
data class PedidoItem(
    val id: Int = 0,
    @SerialName("pedido_id")
    val pedidoId: Int,
    val tipo: TipoLineaPedido,
    @SerialName("item_menu_id")
    val itemMenuId: Int? = null,
    @SerialName("promocion_id")
    val promocionId: Int? = null,
    val cantidad: Int = 1,
    @SerialName("precio_unitario")
    val precioUnitario: Double = 0.0,
    val subtotal: Double = 0.0,
    @SerialName("es_regalo")
    val esRegalo: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Producto físico a preparar dentro de una línea (tabla "pedido_unidades").
 * Para un PRODUCTO simple hay 1 unidad; para una PROMO, una por cada espacio elegido.
 */
@Serializable
data class PedidoUnidad(
    val id: Int = 0,
    @SerialName("pedido_item_id")
    val pedidoItemId: Int,
    @SerialName("item_menu_id")
    val itemMenuId: Int,
    val comentario: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/** Modificador (agregado) aplicado a una unidad (tabla "pedido_unidad_mods"). */
@Serializable
data class PedidoUnidadMod(
    val id: Int = 0,
    @SerialName("pedido_unidad_id")
    val pedidoUnidadId: Int,
    @SerialName("modificador_id")
    val modificadorId: Int,
    @SerialName("created_at")
    val createdAt: String? = null
)
