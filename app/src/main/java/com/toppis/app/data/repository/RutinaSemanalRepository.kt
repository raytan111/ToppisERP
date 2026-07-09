package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.PasoRutina
import com.toppis.app.data.models.PasoRutinaSemanal
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.SemanaOperativa
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de la rutina semanal: pasos completados por semana
 * (tabla "pasos_rutina_semanal").
 */
class RutinaSemanalRepository {

    private val client = SupabaseClient.client

    suspend fun getPasos(semana: SemanaOperativa): List<PasoRutinaSemanal> = try {
        client.postgrest.from("pasos_rutina_semanal").select {
            filter { eq("semana_inicio", semana.lunesInicio.toString()) }
        }.decodeList<PasoRutinaSemanal>()
    } catch (e: Exception) {
        Log.e("RutinaSemanalRepository", "Error getPasos: ${e.message}", e)
        emptyList()
    }

    /** Marca (o desmarca) un paso de la semana. Upsert por (local_id, semana_inicio, paso). */
    suspend fun marcarPaso(semana: SemanaOperativa, paso: PasoRutina, completado: Boolean) {
        client.postgrest.from("pasos_rutina_semanal").upsert(
            buildJsonObject {
                put("semana_inicio", semana.lunesInicio.toString())
                put("paso", paso.name)
                put("completado", completado)
                if (completado) put("completado_at", java.time.Instant.now().toString())
                LocalSession.activoId.value?.let { put("local_id", it) }
            }
        ) { onConflict = "local_id,semana_inicio,paso" }
    }
}
