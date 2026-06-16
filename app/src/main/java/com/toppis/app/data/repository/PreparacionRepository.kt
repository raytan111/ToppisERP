package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.DimensionUnidad
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Preparacion
import com.toppis.app.data.models.PreparacionComponente
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Componente resuelto de una preparación. */
data class ComponentePreparacion(
    val componente: PreparacionComponente,
    val nombre: String,
    val unidad: String,
    val costoBase: Double
) {
    val costoLinea: Double get() = costoBase * componente.cantidadBase
}

/**
 * Repositorio de Preparaciones (sub-recetas) basado en Supabase.
 * costo_lote = Σ(componente.costo_base × cantidad); costo_base = costo_lote / rendimiento_lote.
 */
class PreparacionRepository {

    private val client = SupabaseClient.client

    // ── Preparaciones ────────────────────────────────────────────────────────

    suspend fun getPreparaciones(): List<Preparacion> = try {
        client.postgrest.from("preparaciones").select().decodeList<Preparacion>().sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("PreparacionRepository", "Error getPreparaciones: ${e.message}", e)
        emptyList()
    }

    suspend fun crearPreparacion(
        nombre: String,
        dimension: DimensionUnidad,
        rendimientoLote: Double,
        seleccionableEnPos: Boolean = false
    ): Int? {
        return try {
            client.postgrest.from("preparaciones").insert(
                buildJsonObject {
                    put("nombre", nombre)
                    put("dimension", dimension.name)
                    put("unidad_base", dimension.unidadBase)
                    put("rendimiento_lote", rendimientoLote)
                    put("costo_lote", 0.0)
                    put("costo_base", 0.0)
                    put("seleccionable_en_pos", seleccionableEnPos)
                    put("activo", true)
                }
            ) { select() }.decodeSingle<Preparacion>().id
        } catch (e: Exception) {
            Log.e("PreparacionRepository", "Error crearPreparacion: ${e.message}", e)
            null
        }
    }

    suspend fun actualizarPreparacion(prep: Preparacion) {
        client.postgrest.from("preparaciones").update(
            buildJsonObject {
                put("nombre", prep.nombre)
                put("dimension", prep.dimension.name)
                put("unidad_base", prep.dimension.unidadBase)
                put("rendimiento_lote", prep.rendimientoLote)
                put("seleccionable_en_pos", prep.seleccionableEnPos)
                put("activo", prep.activo)
            }
        ) {
            filter { eq("id", prep.id) }
        }
        recalcularCosto(prep.id)
    }

    suspend fun eliminarPreparacion(id: Int) {
        client.postgrest.from("preparaciones").delete {
            filter { eq("id", id) }
        }
    }

    // ── Componentes de la preparación ──────────────────────────────────────────

    suspend fun getComponentes(preparacionId: Int): List<ComponentePreparacion> {
        val comps = try {
            client.postgrest.from("preparacion_componentes").select {
                filter { eq("preparacion_id", preparacionId) }
            }.decodeList<PreparacionComponente>()
        } catch (e: Exception) {
            Log.e("PreparacionRepository", "Error getComponentes: ${e.message}", e)
            emptyList()
        }
        return comps.mapNotNull { c ->
            when (c.tipoComponente) {
                TipoComponente.ARTICULO -> getArticuloPorId(c.componenteId)?.let {
                    ComponentePreparacion(c, it.nombre, it.unidadBase, it.costoBase)
                }
                TipoComponente.PREPARACION -> getPrepPorId(c.componenteId)?.let {
                    ComponentePreparacion(c, it.nombre, it.unidadBase, it.costoBase)
                }
            }
        }
    }

    suspend fun agregarComponente(
        preparacionId: Int,
        tipo: TipoComponente,
        componenteId: Int,
        cantidadBase: Double
    ) {
        client.postgrest.from("preparacion_componentes").insert(
            buildJsonObject {
                put("preparacion_id", preparacionId)
                put("tipo_componente", tipo.name)
                put("componente_id", componenteId)
                put("cantidad_base", cantidadBase)
            }
        )
        recalcularCosto(preparacionId)
    }

    suspend fun eliminarComponente(comp: PreparacionComponente) {
        client.postgrest.from("preparacion_componentes").delete {
            filter { eq("id", comp.id) }
        }
        recalcularCosto(comp.preparacionId)
    }

    /** Recalcula costo_lote y costo_base de la preparación. */
    suspend fun recalcularCosto(preparacionId: Int): Double {
        val comps = getComponentes(preparacionId)
        val costoLote = comps.sumOf { it.costoLinea }
        val prep = getPrepPorId(preparacionId)
        val rend = prep?.rendimientoLote ?: 1.0
        val costoBase = if (rend > 0) costoLote / rend else 0.0
        client.postgrest.from("preparaciones").update(
            buildJsonObject {
                put("costo_lote", costoLote)
                put("costo_base", costoBase)
            }
        ) {
            filter { eq("id", preparacionId) }
        }
        return costoBase
    }

    // ── Catálogo de artículos para armar preparaciones ──────────────────────────

    suspend fun getArticulos(): List<Articulo> = try {
        client.postgrest.from("articulos").select().decodeList<Articulo>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) {
        emptyList()
    }

    private suspend fun getArticuloPorId(id: Int): Articulo? = try {
        client.postgrest.from("articulos").select { filter { eq("id", id) } }
            .decodeSingleOrNull<Articulo>()
    } catch (e: Exception) {
        null
    }

    private suspend fun getPrepPorId(id: Int): Preparacion? = try {
        client.postgrest.from("preparaciones").select { filter { eq("id", id) } }
            .decodeSingleOrNull<Preparacion>()
    } catch (e: Exception) {
        null
    }

    // ── Realtime ─────────────────────────────────────────────────────────────────

    fun observePreparaciones(): Flow<Unit> = channelFlow {
        val channel = client.channel("preparaciones-changes-${java.util.UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "preparaciones"
        }
        val job = launch { changes.collect { send(Unit) } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }
}
