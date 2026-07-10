package com.toppis.app.domain.pos

import com.toppis.app.data.db.entities.ModoEspacioPromo
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Modificador
import com.toppis.app.data.models.PromocionEspacio
import com.toppis.app.data.models.PromocionEspacioOpcion

/**
 * Capa de cálculo PURA del POS: sin dependencias de red ni Android. Base de los tests
 * de propiedad. Todo el dinero en CLP con IVA incluido.
 */
object PosCalculos {

    // ── Precios y totales ─────────────────────────────────────────────────────

    /** Subtotal de una línea = precio unitario × cantidad. */
    fun precioLinea(precioUnitario: Double, cantidad: Int): Double = precioUnitario * cantidad

    /** Total del pedido = suma de subtotales de líneas + envío. */
    fun totalPedido(subtotales: List<Double>, envio: Double): Double = subtotales.sum() + envio

    /** Precio de un producto = base + suma de los delta de sus modificadores (≥ base). */
    fun precioProducto(base: Double, deltasModificadores: List<Double>): Double =
        base + deltasModificadores.sum()

    /** El precio de una promo es SIEMPRE su precio fijo (independiente de lo elegido). */
    fun precioPromo(precioFijo: Double): Double = precioFijo

    // ── Modificadores ─────────────────────────────────────────────────────────

    /**
     * Modificadores aplicables a un producto: los de su misma categoría del menú, más
     * los puntuales de ese item. Solo activos.
     */
    fun modificadoresAplicables(
        itemMenuId: Int,
        categoria: String?,
        modificadores: List<Modificador>
    ): List<Modificador> = modificadores.filter { m ->
        m.activo && (
            m.itemMenuId == itemMenuId ||
            (!m.categoria.isNullOrBlank() && !categoria.isNullOrBlank() &&
                m.categoria.equals(categoria.trim(), ignoreCase = true))
        )
    }

    // ── Promociones configurables ──────────────────────────────────────────────

    /** Items elegibles de un espacio: por lista de opciones o por categoría del menú. */
    fun elegiblesEspacio(
        espacio: PromocionEspacio,
        opciones: List<PromocionEspacioOpcion>,
        itemsMenu: List<ItemMenu>
    ): List<ItemMenu> = when (espacio.modo) {
        ModoEspacioPromo.LISTA -> {
            val ids = opciones.filter { it.espacioId == espacio.id }.map { it.itemMenuId }.toSet()
            itemsMenu.filter { it.id in ids }
        }
        ModoEspacioPromo.CATEGORIA -> {
            val cat = espacio.categoria?.trim()
            if (cat.isNullOrBlank()) emptyList()
            else itemsMenu.filter { it.categoria.trim().equals(cat, ignoreCase = true) }
        }
    }

    /**
     * true si la promo está completa: cada espacio tiene elegida al menos su cantidad.
     * [elegidosPorEspacio] = espacio.id → nº de productos elegidos para ese espacio.
     */
    fun promoCompleta(espacios: List<PromocionEspacio>, elegidosPorEspacio: Map<Int, Int>): Boolean =
        espacios.all { (elegidosPorEspacio[it.id] ?: 0) >= it.cantidad }

    // ── Estados del pedido ──────────────────────────────────────────────────────

    /** Debe la plata: se entregó pero no se pagó. */
    fun tieneDeuda(entregado: Boolean, pagado: Boolean): Boolean = entregado && !pagado

    /** Sigue en la lista activa del POS mientras falte pagar o entregar. */
    fun activoEnLista(pagado: Boolean, entregado: Boolean): Boolean = !(pagado && entregado)

    // ── Cuponera ────────────────────────────────────────────────────────────────

    /** Sellos tras pagar un pedido: +1 solo si el pedido incluye una hamburguesa. */
    fun sellosTrasPedido(sellos: Int, incluyeHamburguesa: Boolean): Int =
        if (incluyeHamburguesa) sellos + 1 else sellos

    /** Puede regalar cuando junta al menos [umbral] sellos. */
    fun puedeRegalar(sellos: Int, umbral: Int = 6): Boolean = sellos >= umbral

    /** Sellos tras aplicar el regalo: resta el umbral, nunca baja de 0. */
    fun sellosTrasRegalo(sellos: Int, umbral: Int = 6): Int = (sellos - umbral).coerceAtLeast(0)

    /** Precio de un ítem regalo (cupón): siempre 0. */
    fun precioRegalo(): Double = 0.0

    // ── Estado / pago / stock ─────────────────────────────────────────────────

    /** Se materializa la venta solo si el pedido aún no tiene una (idempotencia del pago). */
    fun debeMaterializarVenta(ventaIdActual: Int?): Boolean = ventaIdActual == null

    /** Solo se cierra un pedido ABIERTO (genera comanda; no toca dinero ni stock). */
    fun puedeCerrar(estado: com.toppis.app.data.db.entities.EstadoPedido): Boolean =
        estado == com.toppis.app.data.db.entities.EstadoPedido.ABIERTO

    /**
     * Clave de descuento de stock de una unidad: depende solo del producto y sus
     * modificadores, NO del comentario ("sin tomate" no altera el stock).
     */
    fun claveStockUnidad(itemMenuId: Int, modificadorIds: List<Int>): String =
        itemMenuId.toString() + "|" + modificadorIds.sorted().joinToString(",")
}
