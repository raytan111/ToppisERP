package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.PapaRendimiento
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de registros de rendimiento de papa (formulario de pesos por etapa - QF1).
 */
class PapaRendimientoRepository {

    private val client = SupabaseClient.client

    suspend fun getRegistros(articuloId: Int): List<PapaRendimiento> = try {
        client.postgrest.from("papa_rendimientos").select {
            filter { eq("articulo_id", articuloId) }
        }.decodeList<PapaRendimiento>().sortedByDescending { it.fecha }
    } catch (e: Exception) {
        Log.e("PapaRendimientoRepository", "Error getRegistros: ${e.message}", e)
        emptyList()
    }

    suspend fun getTodos(): List<PapaRendimiento> = try {
        client.postgrest.from("papa_rendimientos").select()
            .decodeList<PapaRendimiento>().sortedByDescending { it.fecha }
    } catch (e: Exception) {
        emptyList()
    }

    /** Registra pesos y calcula el rendimiento (peso_frito / peso_crudo). */
    suspend fun registrar(
        articuloId: Int,
        pesoCrudo: Double,
        pesoPelado: Double,
        pesoPrefrito: Double,
        pesoFrito: Double,
        fecha: String? = null
    ) {
        val rendimiento = if (pesoCrudo > 0) pesoFrito / pesoCrudo else 0.0
        client.postgrest.from("papa_rendimientos").insert(
            buildJsonObject {
                put("articulo_id", articuloId)
                if (fecha != null) put("fecha", fecha)
                put("peso_crudo", pesoCrudo)
                put("peso_pelado", pesoPelado)
                put("peso_prefrito", pesoPrefrito)
                put("peso_frito", pesoFrito)
                put("rendimiento", rendimiento)
            }
        )
    }

    suspend fun eliminar(id: Int) {
        client.postgrest.from("papa_rendimientos").delete { filter { eq("id", id) } }
    }

    /** Artículos candidatos (perecibles tipo papa). Devuelve todos los activos para elegir. */
    suspend fun getArticulos(): List<Articulo> = try {
        client.postgrest.from("articulos").select().decodeList<Articulo>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) {
        emptyList()
    }
}
