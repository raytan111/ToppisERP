package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.Comprobante
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.FechaUtil
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de Comprobantes (Fase 2A: comprobantes internos).
 * La emisión usa la función RPC `emitir_comprobante` (calcula neto/IVA y folio).
 */
class ComprobanteRepository {

    private val client = SupabaseClient.client

    /**
     * Emite el comprobante interno para una venta. Retorna el comprobante creado.
     * Lanza excepción con mensaje legible si falla (ej: venta ya tiene comprobante).
     */
    suspend fun emitirComprobante(ventaId: Int, usuarioId: String?): Comprobante {
        val params = buildJsonObject {
            put("p_venta_id", ventaId)
            if (usuarioId == null) put("p_usuario", JsonNull) else put("p_usuario", usuarioId)
        }
        try {
            client.postgrest.rpc("emitir_comprobante", params)
            // La RPC devuelve un JSON resumen; recargamos el comprobante completo
            return getByVentaId(ventaId)
                ?: throw Exception("Comprobante emitido pero no se pudo leer")
        } catch (e: Exception) {
            Log.e("ComprobanteRepository", "Error emitirComprobante: ${e.message}", e)
            throw Exception(extraerMensajeError(e.message))
        }
    }

    suspend fun getByVentaId(ventaId: Int): Comprobante? = try {
        client.postgrest.from("comprobantes").select {
            filter { eq("venta_id", ventaId) }
        }.decodeSingleOrNull<Comprobante>()
    } catch (e: Exception) {
        null
    }

    /** Total de IVA provisionado desde [desdeMillis] (suma del IVA de comprobantes). */
    suspend fun getIvaProvisionado(desdeMillis: Long = 0L): Double = try {
        val comprobantes = client.postgrest.from("comprobantes").select().decodeList<Comprobante>()
        comprobantes
            .filter { desdeMillis == 0L || FechaUtil.isoToMillis(it.fechaEmision) >= desdeMillis }
            .sumOf { it.iva }
    } catch (e: Exception) {
        0.0
    }

    private fun extraerMensajeError(raw: String?): String {
        if (raw == null) return "Error al emitir el comprobante"
        val regex = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
        regex.find(raw)?.let { return it.groupValues[1] }
        if (raw.contains("ya tiene comprobante")) return "Esta venta ya tiene un comprobante emitido"
        return "Error al emitir el comprobante"
    }
}
