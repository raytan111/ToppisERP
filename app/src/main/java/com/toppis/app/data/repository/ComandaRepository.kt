package com.toppis.app.data.repository

import com.toppis.app.data.db.entities.ZonaEnvio
import com.toppis.app.data.models.Comanda
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Línea para construir el texto de comanda/WhatsApp (sin dependencias de BD). */
data class LineaComanda(
    val nombre: String,
    val cantidad: Int,
    val subtotal: Double,
    val salsas: String
)

/**
 * Repositorio de Comandas basado en Supabase.
 * El insert de la comanda lo hace la RPC de venta; aquí quedan consultas,
 * cambio de estado y los constructores de texto (comanda / WhatsApp).
 */
class ComandaRepository {

    private val client = SupabaseClient.client

    suspend fun getComandaByVentaId(ventaId: Int): Comanda? = try {
        client.postgrest.from("comandas").select {
            filter { eq("venta_id", ventaId) }
        }.decodeSingleOrNull<Comanda>()
    } catch (e: Exception) {
        null
    }

    suspend fun marcarEntregada(comandaId: Int) {
        client.postgrest.from("comandas").update(
            buildJsonObject { put("estado", "ENTREGADA") }
        ) {
            filter { eq("id", comandaId) }
        }
    }

    // ── Constructores de texto (puros, sin BD) ─────────────────────────────────

    fun buildComandaTexto(
        ventaId: Int,
        lineas: List<LineaComanda>,
        zonaEnvio: ZonaEnvio,
        total: Double
    ): String {
        val fmt = DecimalFormat("$#,##0")
        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL"))
        val sb = StringBuilder()

        sb.appendLine("COMANDA #$ventaId — ${dateFmt.format(Date())}")
        sb.appendLine("─".repeat(35))

        var subtotalItems = 0.0
        for (l in lineas) {
            sb.appendLine("${l.cantidad}x ${l.nombre}  ${fmt.format(l.subtotal)}")
            if (l.salsas.isNotBlank()) sb.appendLine("   Salsas: ${l.salsas}")
            subtotalItems += l.subtotal
        }

        sb.appendLine("─".repeat(35))
        sb.appendLine("Subtotal:  ${fmt.format(subtotalItems)}")
        if (zonaEnvio != ZonaEnvio.SIN_ENVIO) {
            sb.appendLine("Envío (${zonaEnvio.label}):  ${fmt.format(zonaEnvio.precio)}")
        }
        sb.appendLine("TOTAL:  ${fmt.format(total)}")
        return sb.toString()
    }

    fun buildWhatsApp(
        ventaId: Int,
        lineas: List<LineaComanda>,
        zonaEnvio: ZonaEnvio,
        total: Double
    ): String {
        val fmt = DecimalFormat("$#,##0")
        val sb = StringBuilder()

        sb.appendLine("*Toppis — Pedido #$ventaId*")
        sb.appendLine("")
        for (l in lineas) {
            sb.appendLine("• ${l.cantidad}x ${l.nombre} — ${fmt.format(l.subtotal)}")
            if (l.salsas.isNotBlank()) sb.appendLine("  _Salsas: ${l.salsas}_")
        }
        sb.appendLine("")
        if (zonaEnvio != ZonaEnvio.SIN_ENVIO) {
            sb.appendLine("Envío (${zonaEnvio.label}): ${fmt.format(zonaEnvio.precio)}")
        }
        sb.appendLine("*Total: ${fmt.format(total)}*")
        return sb.toString()
    }
}
