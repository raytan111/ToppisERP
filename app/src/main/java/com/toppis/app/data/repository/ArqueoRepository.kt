package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.TipoSobre
import com.toppis.app.data.models.Arqueo
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Arqueo con el nombre del sobre. */
data class ArqueoConNombre(
    val arqueo: Arqueo,
    val nombreSobre: String
)

/**
 * Repositorio de Arqueos de caja. El registro es atómico vía RPC registrar_arqueo.
 */
class ArqueoRepository {

    private val client = SupabaseClient.client

    /** Sobres tipo CUENTA (dinero real) para arquear. */
    suspend fun getCuentas(): List<Sobre> = try {
        client.postgrest.from("sobres").select().decodeList<Sobre>()
            .filter { it.tipo == TipoSobre.CUENTA }.sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("ArqueoRepository", "Error getCuentas: ${e.message}", e)
        emptyList()
    }

    suspend fun getArqueos(): List<ArqueoConNombre> {
        val arqueos = try {
            client.postgrest.from("arqueos").select().decodeList<Arqueo>().sortedByDescending { it.fecha }
        } catch (e: Exception) { emptyList() }
        val sobres = try {
            client.postgrest.from("sobres").select().decodeList<Sobre>().associateBy { it.id }
        } catch (e: Exception) { emptyMap() }
        return arqueos.map { ArqueoConNombre(it, sobres[it.sobreId]?.nombre ?: "Sobre #${it.sobreId}") }
    }

    suspend fun registrarArqueo(sobreId: Int, contado: Double, nota: String, ajustar: Boolean, usuarioId: String?) {
        val params = buildJsonObject {
            put("p_sobre_id", sobreId)
            put("p_contado", contado)
            put("p_nota", nota)
            put("p_ajustar", ajustar)
            if (usuarioId == null) put("p_usuario", JsonNull) else put("p_usuario", usuarioId)
            LocalSession.activoId.value?.let { put("p_local_id", it) }
        }
        try {
            client.postgrest.rpc("registrar_arqueo", params)
        } catch (e: Exception) {
            Log.e("ArqueoRepository", "Error registrarArqueo: ${e.message}", e)
            val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(e.message ?: "")?.groupValues?.get(1)
            throw Exception(msg ?: "Error al registrar arqueo")
        }
    }
}
