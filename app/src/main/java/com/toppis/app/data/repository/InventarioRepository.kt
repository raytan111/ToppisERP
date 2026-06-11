package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.Ingrediente
import com.toppis.app.data.models.Insumo
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
 * Repositorio de Inventario (insumos + ingredientes) basado en Supabase.
 */
class InventarioRepository {

    private val client = SupabaseClient.client

    // ── Lectura ────────────────────────────────────────────────────────────────

    suspend fun getInsumos(): List<Insumo> = try {
        client.postgrest.from("insumos").select().decodeList<Insumo>().sortedBy { it.id }
    } catch (e: Exception) {
        Log.e("InventarioRepository", "Error al leer insumos: ${e.message}", e)
        emptyList()
    }

    suspend fun getIngredientes(): List<Ingrediente> = try {
        client.postgrest.from("ingredientes").select().decodeList<Ingrediente>().sortedBy { it.id }
    } catch (e: Exception) {
        Log.e("InventarioRepository", "Error al leer ingredientes: ${e.message}", e)
        emptyList()
    }

    // ── Realtime ─────────────────────────────────────────────────────────────────

    fun observeInsumos(): Flow<Unit> = observarTabla("insumos", "insumos-changes")
    fun observeIngredientes(): Flow<Unit> = observarTabla("ingredientes", "ingredientes-changes")

    private fun observarTabla(tabla: String, canal: String): Flow<Unit> = channelFlow {
        // Nombre de canal único por suscripción para evitar reusar uno ya unido
        val channel = client.channel("$canal-${java.util.UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = tabla
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

    suspend fun crearInsumo(
        nombre: String,
        descripcion: String,
        precio: Double,
        unidadMedida: String,
        stockInicial: Int = 0
    ) {
        client.postgrest.from("insumos").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("descripcion", descripcion)
                put("precio", precio)
                put("stock", stockInicial)
                put("unidad_medida", unidadMedida)
                put("activo", true)
            }
        )
    }

    suspend fun crearIngrediente(
        nombre: String,
        unidadMedida: String,
        stockActual: Double,
        costoUnitario: Double,
        costoCompra: Double = 0.0,
        porcentajeMerma: Double = 0.0,
        unidadCompra: String = "",
        cantidadComprada: Double = 0.0,
        cantidadAprovechable: Double = 0.0,
        costoGramo: Double = 0.0
    ) {
        client.postgrest.from("ingredientes").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("unidad_medida", unidadMedida)
                put("stock_actual", stockActual)
                put("costo_unitario", costoUnitario)
                put("costo_compra", costoCompra)
                put("porcentaje_merma", porcentajeMerma)
                put("unidad_compra", unidadCompra)
                put("cantidad_comprada", cantidadComprada)
                put("cantidad_aprovechable", cantidadAprovechable)
                put("costo_gramo", costoGramo)
            }
        )
    }

    suspend fun actualizarStockIngrediente(ingredienteId: Int, nuevoStock: Double) {
        client.postgrest.from("ingredientes").update(
            buildJsonObject {
                put("stock_actual", nuevoStock)
            }
        ) {
            filter { eq("id", ingredienteId) }
        }
    }

    // ── Actualizar / Eliminar ───────────────────────────────────────────────────

    suspend fun actualizarInsumo(insumo: Insumo) {
        client.postgrest.from("insumos").update(
            buildJsonObject {
                put("nombre", insumo.nombre)
                put("descripcion", insumo.descripcion)
                put("precio", insumo.precio)
                put("stock", insumo.stock)
                put("unidad_medida", insumo.unidadMedida)
            }
        ) {
            filter { eq("id", insumo.id) }
        }
    }

    suspend fun eliminarInsumo(insumoId: Int) {
        client.postgrest.from("insumos").delete {
            filter { eq("id", insumoId) }
        }
    }

    suspend fun actualizarIngrediente(ingrediente: Ingrediente) {
        client.postgrest.from("ingredientes").update(
            buildJsonObject {
                put("nombre", ingrediente.nombre)
                put("unidad_medida", ingrediente.unidadMedida)
                put("stock_actual", ingrediente.stockActual)
                put("costo_unitario", ingrediente.costoUnitario)
                put("costo_compra", ingrediente.costoCompra)
                put("porcentaje_merma", ingrediente.porcentajeMerma)
                put("unidad_compra", ingrediente.unidadCompra)
                put("cantidad_comprada", ingrediente.cantidadComprada)
                put("cantidad_aprovechable", ingrediente.cantidadAprovechable)
                put("costo_gramo", ingrediente.costoGramo)
            }
        ) {
            filter { eq("id", ingrediente.id) }
        }
    }

    suspend fun eliminarIngrediente(ingredienteId: Int) {
        client.postgrest.from("ingredientes").delete {
            filter { eq("id", ingredienteId) }
        }
    }
}
