package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.MotivoMerma
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Merma
import com.toppis.app.data.models.Preparacion
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Merma con el nombre del componente resuelto (para mostrar en historial). */
data class MermaConNombre(
    val merma: Merma,
    val nombre: String
)

/**
 * Repositorio de Mermas (waste log). El registro es atómico vía RPC registrar_merma.
 */
class MermaRepository {

    private val client = SupabaseClient.client

    suspend fun getMermas(): List<Merma> = try {
        client.postgrest.from("mermas").select().decodeList<Merma>().sortedByDescending { it.fecha }
    } catch (e: Exception) {
        Log.e("MermaRepository", "Error getMermas: ${e.message}", e)
        emptyList()
    }

    /** Mermas con nombre resuelto (artículo/preparación). */
    suspend fun getMermasConNombre(): List<MermaConNombre> {
        val mermas = getMermas()
        val articulos = getArticulos().associateBy { it.id }
        val preps = getPreparaciones().associateBy { it.id }
        return mermas.map { m ->
            val nombre = when (m.tipoComponente) {
                TipoComponente.ARTICULO -> articulos[m.componenteId]?.let { "${it.nombre} (${it.unidadBase})" } ?: "Artículo #${m.componenteId}"
                TipoComponente.PREPARACION -> preps[m.componenteId]?.let { "${it.nombre} (${it.unidadBase})" } ?: "Prep #${m.componenteId}"
            }
            MermaConNombre(m, nombre)
        }
    }

    /** Registra una merma de forma atómica (inserta + descuenta stock). */
    suspend fun registrarMerma(
        tipo: TipoComponente,
        componenteId: Int,
        cantidadBase: Double,
        motivo: MotivoMerma,
        nota: String,
        usuarioId: String?
    ) {
        val params = buildJsonObject {
            put("p_tipo", tipo.name)
            put("p_componente_id", componenteId)
            put("p_cantidad", cantidadBase)
            put("p_motivo", motivo.name)
            put("p_nota", nota)
            if (usuarioId == null) put("p_usuario", JsonNull) else put("p_usuario", usuarioId)
        }
        client.postgrest.rpc("registrar_merma", params)
    }

    suspend fun eliminarMerma(id: Int) {
        client.postgrest.from("mermas").delete { filter { eq("id", id) } }
    }

    // ── Catálogos ──────────────────────────────────────────────────────────────

    suspend fun getArticulos(): List<Articulo> = try {
        client.postgrest.from("articulos").select().decodeList<Articulo>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) { emptyList() }

    suspend fun getPreparaciones(): List<Preparacion> = try {
        client.postgrest.from("preparaciones").select().decodeList<Preparacion>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) { emptyList() }

    fun observeMermas(): Flow<Unit> = channelFlow {
        val channel = client.channel("mermas-changes-${java.util.UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "mermas" }
        val job = launch { changes.collect { send(Unit) } }
        channel.subscribe()
        awaitClose { job.cancel(); launch { channel.unsubscribe() } }
    }
}
