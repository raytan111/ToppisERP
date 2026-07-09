package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.CierreSemanal
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.SemanaOperativa
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de cierres semanales (snapshots congelados) — tabla "cierres_semanales".
 */
class CierreSemanalRepository {

    private val client = SupabaseClient.client

    /** Devuelve el snapshot de la semana si ya está cerrada, o null. */
    suspend fun getSnapshot(semana: SemanaOperativa): CierreSemanal? = try {
        client.postgrest.from("cierres_semanales").select {
            filter { eq("semana_inicio", semana.lunesInicio.toString()) }
        }.decodeList<CierreSemanal>().firstOrNull()
    } catch (e: Exception) {
        Log.e("CierreSemanalRepository", "Error getSnapshot: ${e.message}", e)
        null
    }

    /** Confirma el cierre de una semana (guarda el snapshot). Idempotente por semana. */
    suspend fun confirmarCierre(
        semana: SemanaOperativa,
        ventas: Double,
        variable: Double,
        foodTeorico: Double,
        manoObra: Double,
        fijos: Double,
        resultado: Double,
        foodPct: Double,
        laborPct: Double,
        breakEven: Double?,
        margen: Double,
        usuarioId: String?
    ) {
        client.postgrest.rpc("confirmar_cierre_semanal", buildJsonObject {
            put("p_semana_inicio", semana.lunesInicio.toString())
            put("p_semana_fin", semana.sabadoFin.toString())
            put("p_ventas", ventas)
            put("p_variable", variable)
            put("p_food_teorico", foodTeorico)
            put("p_mano_obra", manoObra)
            put("p_fijos", fijos)
            put("p_resultado", resultado)
            put("p_food_pct", foodPct)
            put("p_labor_pct", laborPct)
            if (breakEven == null) put("p_break_even", JsonNull) else put("p_break_even", breakEven)
            put("p_margen", margen)
            if (usuarioId == null) put("p_usuario", JsonNull) else put("p_usuario", usuarioId)
            LocalSession.activoId.value?.let { put("p_local_id", it) }
        })
    }
}
