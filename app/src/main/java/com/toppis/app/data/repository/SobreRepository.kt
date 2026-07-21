package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.MovimientoSobre
import com.toppis.app.data.models.Sobre
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

/**
 * Repositorio de Sobres basado en Supabase.
 *
 * - getSobres(): lectura puntual (suspend).
 * - observeCambios(): Flow Realtime que emite cuando cambia la tabla.
 * - transferir(): función RPC atómica `transferir_entre_sobres`.
 */
class SobreRepository {

    private val client = SupabaseClient.client

    // ── Lectura puntual ───────────────────────────────────────────────────────

    suspend fun getSobres(): List<Sobre> {
        return try {
            client.postgrest.from("sobres")
                .select()
                .decodeList<Sobre>()
                .sortedBy { it.id }
        } catch (e: Exception) {
            Log.e("SobreRepository", "Error al leer sobres: ${e.message}", e)
            emptyList()
        }
    }

    /** Movimientos donde el sobre participa (como origen o destino), más nuevos primero. */
    suspend fun getMovimientos(sobreId: Int): List<MovimientoSobre> = try {
        client.postgrest.from("movimientos_sobre").select().decodeList<MovimientoSobre>()
            .filter { it.origenId == sobreId || it.destinoId == sobreId }
            .sortedByDescending { it.createdAt ?: it.fecha ?: "" }
    } catch (e: Exception) {
        Log.e("SobreRepository", "Error movimientos: ${e.message}", e)
        emptyList()
    }

    /** Todos los movimientos (para deslizar entre sobres sin recargar), más nuevos primero. */
    suspend fun getTodosMovimientos(): List<MovimientoSobre> = try {
        client.postgrest.from("movimientos_sobre").select().decodeList<MovimientoSobre>()
            .sortedByDescending { it.createdAt ?: it.fecha ?: "" }
    } catch (e: Exception) {
        Log.e("SobreRepository", "Error movimientos: ${e.message}", e)
        emptyList()
    }

    // ── Observador Realtime ─────────────────────────────────────────────────────

    /**
     * Emite Unit cada vez que la tabla "sobres" cambia (insert/update/delete).
     * Si Realtime falla, simplemente no emite (la app sigue refrescando tras cada operación).
     */
    fun observeCambios(): Flow<Unit> = channelFlow {
        val channel = client.channel("sobres-changes-${java.util.UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "sobres"
        }
        val job = launch {
            changes.collect { send(Unit) }
        }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    suspend fun crearSobre(nombre: String, descripcion: String, tipo: com.toppis.app.data.db.entities.TipoSobre = com.toppis.app.data.db.entities.TipoSobre.CUENTA) {
        client.postgrest.from("sobres").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("descripcion", descripcion)
                put("saldo", 0.0)
                put("tipo", tipo.name)
            }
        )
        PosCache.invalidarSobres()
    }

    suspend fun actualizarSobre(sobre: Sobre) {
        client.postgrest.from("sobres").update(
            buildJsonObject {
                put("nombre", sobre.nombre)
                put("descripcion", sobre.descripcion)
                put("tipo", sobre.tipo.name)
            }
        ) {
            filter { eq("id", sobre.id) }
        }
        PosCache.invalidarSobres()
    }

    /**
     * Elimina un sobre si su saldo es 0. Retorna true si se eliminó.
     */
    suspend fun eliminarSobre(sobre: Sobre): Boolean {
        if (sobre.saldo != 0.0) return false
        client.postgrest.from("sobres").delete {
            filter { eq("id", sobre.id) }
        }
        PosCache.invalidarSobres()
        return true
    }

    /**
     * Transfiere [monto] desde [origenId] a [destinoId] de forma atómica
     * mediante la función RPC `transferir_entre_sobres`.
     */
    suspend fun transferir(
        origenId: Long,
        destinoId: Long,
        monto: Double,
        descripcion: String,
        usuarioId: String?
    ) {
        if (monto <= 0) throw IllegalArgumentException("El monto debe ser mayor a 0")

        client.postgrest.rpc(
            "transferir_entre_sobres",
            buildJsonObject {
                put("p_origen", origenId.toInt())
                put("p_destino", destinoId.toInt())
                put("p_monto", monto)
                put("p_descripcion", descripcion)
                if (usuarioId == null) put("p_usuario", JsonNull) else put("p_usuario", usuarioId)
            }
        )
    }
}
