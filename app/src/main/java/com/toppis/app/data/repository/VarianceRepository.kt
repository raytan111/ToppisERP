package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Merma
import com.toppis.app.data.models.Preparacion
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Fila cruda del RPC consumo_teorico_periodo. */
@Serializable
data class ConsumoTeoricoRow(
    val tipo: String,
    @kotlinx.serialization.SerialName("componente_id")
    val componenteId: Int,
    val cantidad: Double = 0.0,
    val costo: Double = 0.0
)

/** Fila de análisis de inventario por componente. */
data class VarianceItem(
    val nombre: String,
    val unidad: String,
    val consumoTeorico: Double,     // cantidad en unidad base
    val costoTeorico: Double,
    val mermaCantidad: Double,
    val mermaCosto: Double
)

/**
 * Repositorio de Variance: consumo teórico (ventas) vs merma registrada.
 */
class VarianceRepository {

    private val client = SupabaseClient.client

    suspend fun getAnalisis(desdeIso: String, hastaIso: String): List<VarianceItem> {
        val teorico = try {
            client.postgrest.rpc("consumo_teorico_periodo", buildJsonObject {
                put("p_desde", desdeIso)
                put("p_hasta", hastaIso)
            }).decodeList<ConsumoTeoricoRow>()
        } catch (e: Exception) {
            Log.e("VarianceRepository", "Error consumo_teorico: ${e.message}", e)
            emptyList()
        }

        val mermas = try {
            client.postgrest.from("mermas").select().decodeList<Merma>()
                .filter { (it.fecha ?: "") >= desdeIso && (it.fecha ?: "") < hastaIso }
        } catch (e: Exception) { emptyList() }

        val articulos = getArticulos().associateBy { it.id }
        val preps = getPreparaciones().associateBy { it.id }

        // Merma agregada por (tipo, id)
        val mermaPorComp = mermas.groupBy { it.tipoComponente to it.componenteId }
            .mapValues { (_, lista) -> lista.sumOf { it.cantidadBase } to lista.sumOf { it.costo } }

        // Unir claves de teórico + merma
        data class Key(val tipo: TipoComponente, val id: Int)
        val claves = LinkedHashSet<Key>()
        teorico.forEach { claves.add(Key(TipoComponente.valueOf(it.tipo), it.componenteId)) }
        mermaPorComp.keys.forEach { claves.add(Key(it.first, it.second)) }

        return claves.map { k ->
            val t = teorico.firstOrNull { it.tipo == k.tipo.name && it.componenteId == k.id }
            val m = mermaPorComp[k.tipo to k.id]
            val (nombre, unidad) = when (k.tipo) {
                TipoComponente.ARTICULO -> articulos[k.id]?.let { it.nombre to it.unidadBase } ?: ("Artículo #${k.id}" to "")
                TipoComponente.PREPARACION -> preps[k.id]?.let { it.nombre to it.unidadBase } ?: ("Prep #${k.id}" to "")
            }
            VarianceItem(
                nombre = nombre,
                unidad = unidad,
                consumoTeorico = t?.cantidad ?: 0.0,
                costoTeorico = t?.costo ?: 0.0,
                mermaCantidad = m?.first ?: 0.0,
                mermaCosto = m?.second ?: 0.0
            )
        }.sortedByDescending { it.costoTeorico + it.mermaCosto }
    }

    private suspend fun getArticulos(): List<Articulo> = try {
        client.postgrest.from("articulos").select().decodeList<Articulo>()
    } catch (e: Exception) { emptyList() }

    private suspend fun getPreparaciones(): List<Preparacion> = try {
        client.postgrest.from("preparaciones").select().decodeList<Preparacion>()
    } catch (e: Exception) { emptyList() }
}
