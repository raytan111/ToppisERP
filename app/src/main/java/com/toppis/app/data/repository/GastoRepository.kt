package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.models.Gasto
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
 * Repositorio de Gastos basado en Supabase.
 * El registro es atómico vía la función RPC `registrar_gasto`.
 * La lectura respeta RLS (los cajeros solo ven sus propios gastos).
 */
class GastoRepository {

    private val client = SupabaseClient.client

    suspend fun getGastos(): List<Gasto> = try {
        client.postgrest.from("gastos").select()
            .decodeList<Gasto>()
            .sortedByDescending { it.id }
    } catch (e: Exception) {
        Log.e("GastoRepository", "Error al leer gastos: ${e.message}", e)
        emptyList()
    }

    fun observeCambios(): Flow<Unit> = channelFlow {
        val channel = client.channel("gastos-changes-${java.util.UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "gastos"
        }
        val job = launch { changes.collect { send(Unit) } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }

    /**
     * Registra un gasto de forma atómica vía RPC:
     * valida saldo, inserta gasto, descuenta del sobre y registra el movimiento.
     */
    suspend fun registrarGasto(
        descripcion: String,
        monto: Double,
        categoria: CategoriaGasto,
        sobreId: Int,
        usuarioId: String? = null,
        comprobante: String? = null
    ) {
        val params = buildJsonObject {
            put("p_descripcion", descripcion)
            put("p_monto", monto)
            put("p_categoria", categoria.name)
            put("p_sobre_id", sobreId)
            if (usuarioId == null) put("p_usuario", JsonNull) else put("p_usuario", usuarioId)
            if (comprobante == null) put("p_comprobante", JsonNull) else put("p_comprobante", comprobante)
        }
        try {
            client.postgrest.rpc("registrar_gasto", params)
        } catch (e: Exception) {
            Log.e("GastoRepository", "Error en registrarGasto: ${e.message}", e)
            throw Exception(extraerMensajeError(e.message))
        }
    }

    private fun extraerMensajeError(raw: String?): String {
        if (raw == null) return "Error al registrar el gasto"
        val regex = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
        val match = regex.find(raw)
        if (match != null) return match.groupValues[1]
        if (raw.contains("Saldo insuficiente")) return "Saldo insuficiente en el sobre"
        return "Error al registrar el gasto. Intentá de nuevo."
    }
}
