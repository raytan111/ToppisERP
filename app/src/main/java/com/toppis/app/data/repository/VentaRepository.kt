package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.MetodoPago
import com.toppis.app.data.db.entities.ZonaEnvio
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Componente de modificador para descontar/devolver stock en la venta. */
data class ModComponenteVenta(
    val tipo: String,        // ARTICULO | PREPARACION
    val componenteId: Int,
    val cantidadBase: Double,
    val accion: String       // AGREGAR | QUITAR
)

/** Línea de venta para enviar a la función RPC. */
data class LineaVenta(
    val itemMenuId: Int,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double,
    val salsas: String,
    val costoUnitario: Double = 0.0,
    val modificadores: String = "",
    val promocionId: Int? = null,
    val modsComponentes: List<ModComponenteVenta> = emptyList()
)

/**
 * Repositorio de Ventas basado en Supabase.
 * El registro de venta es atómico vía la función RPC `registrar_venta_menu`.
 */
class VentaRepository {

    private val client = SupabaseClient.client

    /**
     * Registra una venta completa de forma atómica en el servidor.
     * Retorna el id de la venta creada.
     */
    suspend fun registrarVentaMenu(
        items: List<LineaVenta>,
        metodoPago: MetodoPago,
        sobreId: Int,
        usuarioId: String? = null,
        zonaEnvio: ZonaEnvio = ZonaEnvio.SIN_ENVIO,
        comandaTexto: String
    ): Int {
        require(sobreId > 0) { "sobreId inválido: $sobreId" }
        require(items.isNotEmpty()) { "No hay ítems en el carrito" }

        val itemsJson = buildJsonArray {
            items.forEach { item ->
                add(
                    buildJsonObject {
                        put("item_menu_id", item.itemMenuId)
                        put("cantidad", item.cantidad)
                        put("precio_unitario", item.precioUnitario)
                        put("subtotal", item.subtotal)
                        put("salsas", item.salsas)
                        put("costo_unitario", item.costoUnitario)
                        put("modificadores", item.modificadores)
                        put("promocion_id", item.promocionId?.toString() ?: "")
                        putJsonArray("mods_comp") {
                            item.modsComponentes.forEach { mc ->
                                add(
                                    buildJsonObject {
                                        put("tipo", mc.tipo)
                                        put("componente_id", mc.componenteId)
                                        put("cantidad", mc.cantidadBase)
                                        put("accion", mc.accion)
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }

        val params = buildJsonObject {
            put("p_metodo_pago", metodoPago.name)
            put("p_sobre_id", sobreId)
            if (usuarioId == null) put("p_usuario", JsonNull) else put("p_usuario", usuarioId)
            put("p_monto_envio", zonaEnvio.precio)
            put("p_incluir_envio", zonaEnvio != ZonaEnvio.SIN_ENVIO)
            put("p_items", itemsJson)
            put("p_comanda_texto", comandaTexto)
            LocalSession.activoId.value?.let { put("p_local_id", it) }
        }

        return try {
            client.postgrest.rpc("registrar_venta_menu", params)
                .decodeAs<Int>()
        } catch (e: Exception) {
            Log.e("VentaRepository", "Error en registrarVentaMenu: ${e.message}", e)
            throw Exception(extraerMensajeError(e.message))
        }
    }

    /**
     * Extrae un mensaje legible del error de PostgREST.
     * Los errores de RAISE EXCEPTION vienen en el campo "message" del JSON de respuesta.
     */
    private fun extraerMensajeError(raw: String?): String {
        if (raw == null) return "Error al procesar la venta"
        // Buscar el patrón "message":"..."
        val regex = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
        val match = regex.find(raw)
        if (match != null) return match.groupValues[1]
        // Fallback: si contiene "Stock insuficiente", extraer esa parte
        val stockIdx = raw.indexOf("Stock insuficiente")
        if (stockIdx >= 0) {
            val fin = raw.indexOf('\n', stockIdx).let { if (it < 0) raw.length else it }
            return raw.substring(stockIdx, fin).trim()
        }
        return "Error al procesar la venta. Revisá el stock e intentá de nuevo."
    }
}
