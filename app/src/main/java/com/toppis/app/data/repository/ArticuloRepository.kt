package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.DimensionUnidad
import com.toppis.app.data.models.Articulo
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

/**
 * Repositorio de Artículos (insumos + ingredientes unificados) basado en Supabase.
 * Stock y costo en unidad base; costo_base = (costo_compra / factor) / rendimiento.
 */
class ArticuloRepository {

    private val client = SupabaseClient.client

    // ── Lectura ────────────────────────────────────────────────────────────────

    suspend fun getArticulos(): List<Articulo> = try {
        client.postgrest.from("articulos").select().decodeList<Articulo>().sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("ArticuloRepository", "Error getArticulos: ${e.message}", e)
        emptyList()
    }

    suspend fun getArticulosActivos(): List<Articulo> =
        getArticulos().filter { it.activo }

    suspend fun getArticuloPorId(id: Int): Articulo? = try {
        client.postgrest.from("articulos").select { filter { eq("id", id) } }
            .decodeSingleOrNull<Articulo>()
    } catch (e: Exception) {
        Log.e("ArticuloRepository", "Error getArticuloPorId: ${e.message}", e)
        null
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    suspend fun crearArticulo(
        nombre: String,
        dimension: DimensionUnidad,
        unidadCompra: String,
        factorCompra: Double,
        costoCompra: Double,
        rendimiento: Double = 1.0,
        stockBase: Double = 0.0,
        parLevel: Double = 0.0,
        perecible: Boolean = false,
        vidaUtilDias: Int = 0,
        esVendible: Boolean = false,
        seleccionableEnPos: Boolean = false
    ) {
        val costoBase = Articulo.calcularCostoBase(costoCompra, factorCompra, rendimiento)
        client.postgrest.from("articulos").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("dimension", dimension.name)
                put("unidad_base", dimension.unidadBase)
                put("unidad_compra", unidadCompra)
                put("factor_compra", factorCompra)
                put("costo_compra", costoCompra)
                put("costo_base", costoBase)
                put("rendimiento", rendimiento)
                put("stock_base", stockBase)
                put("par_level", parLevel)
                put("perecible", perecible)
                put("vida_util_dias", vidaUtilDias)
                put("es_vendible", esVendible)
                put("seleccionable_en_pos", seleccionableEnPos)
                put("activo", true)
            }
        )
    }

    suspend fun actualizarArticulo(articulo: Articulo) {
        val costoBase = Articulo.calcularCostoBase(
            articulo.costoCompra, articulo.factorCompra, articulo.rendimiento
        )
        client.postgrest.from("articulos").update(
            buildJsonObject {
                put("nombre", articulo.nombre)
                put("dimension", articulo.dimension.name)
                put("unidad_base", articulo.dimension.unidadBase)
                put("unidad_compra", articulo.unidadCompra)
                put("factor_compra", articulo.factorCompra)
                put("costo_compra", articulo.costoCompra)
                put("costo_base", costoBase)
                put("rendimiento", articulo.rendimiento)
                put("stock_base", articulo.stockBase)
                put("par_level", articulo.parLevel)
                put("perecible", articulo.perecible)
                put("vida_util_dias", articulo.vidaUtilDias)
                put("es_vendible", articulo.esVendible)
                put("seleccionable_en_pos", articulo.seleccionableEnPos)
                put("activo", articulo.activo)
            }
        ) {
            filter { eq("id", articulo.id) }
        }
    }

    suspend fun actualizarStock(articuloId: Int, nuevoStock: Double) {
        client.postgrest.from("articulos").update(
            buildJsonObject { put("stock_base", nuevoStock) }
        ) {
            filter { eq("id", articuloId) }
        }
    }

    /** Actualiza el rendimiento del artículo (usado por papa_rendimientos) y recalcula costo. */
    suspend fun actualizarRendimiento(articuloId: Int, rendimiento: Double) {
        val art = getArticuloPorId(articuloId) ?: return
        val costoBase = Articulo.calcularCostoBase(art.costoCompra, art.factorCompra, rendimiento)
        client.postgrest.from("articulos").update(
            buildJsonObject {
                put("rendimiento", rendimiento)
                put("costo_base", costoBase)
            }
        ) {
            filter { eq("id", articuloId) }
        }
    }

    suspend fun eliminarArticulo(articuloId: Int) {
        client.postgrest.from("articulos").delete {
            filter { eq("id", articuloId) }
        }
    }

    // ── Realtime ─────────────────────────────────────────────────────────────────

    fun observeArticulos(): Flow<Unit> = channelFlow {
        val channel = client.channel("articulos-changes-${java.util.UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "articulos"
        }
        val job = launch { changes.collect { send(Unit) } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }
}
