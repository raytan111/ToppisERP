package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.ConfigCosto
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Objetivos de control de costos, con sus defaults. */
data class ObjetivosCostos(
    val pctFood: Double = 0.32,
    val pctManoObra: Double = 0.30,
    val pctArriendoTecho: Double = 0.10,
    val umbralContratarMo: Double = 0.0
)

/**
 * Repositorio de configuración de objetivos (tabla "config_costos", clave/valor).
 * Devuelve defaults si no hay valores guardados.
 */
class ConfigCostosRepository {

    private val client = SupabaseClient.client

    companion object {
        const val KEY_FOOD = "pct_food_objetivo"
        const val KEY_MANO_OBRA = "pct_mano_obra_objetivo"
        const val KEY_ARRIENDO = "pct_arriendo_techo"
        const val KEY_UMBRAL_CONTRATAR = "umbral_contratar_mo"
    }

    suspend fun getObjetivos(): ObjetivosCostos {
        val defaults = ObjetivosCostos()
        return try {
            val filas = client.postgrest.from("config_costos").select().decodeList<ConfigCosto>()
            val mapa = filas.associate { it.clave to it.valor }
            ObjetivosCostos(
                pctFood = mapa[KEY_FOOD] ?: defaults.pctFood,
                pctManoObra = mapa[KEY_MANO_OBRA] ?: defaults.pctManoObra,
                pctArriendoTecho = mapa[KEY_ARRIENDO] ?: defaults.pctArriendoTecho,
                umbralContratarMo = mapa[KEY_UMBRAL_CONTRATAR] ?: defaults.umbralContratarMo
            )
        } catch (e: Exception) {
            Log.e("ConfigCostosRepository", "Error getObjetivos: ${e.message}", e)
            defaults
        }
    }

    /** Guarda (upsert) un objetivo por clave. */
    suspend fun guardar(clave: String, valor: Double, localId: Int = 0) {
        client.postgrest.from("config_costos").upsert(
            buildJsonObject {
                put("local_id", localId)
                put("clave", clave)
                put("valor", valor)
            }
        ) { onConflict = "local_id,clave" }
    }
}
