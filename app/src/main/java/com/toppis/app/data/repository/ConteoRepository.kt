package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Conteo
import com.toppis.app.data.models.ConteoDetalle
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Línea editable de conteo (artículo + stock sistema + contado). */
data class LineaConteo(
    val articulo: Articulo,
    val stockSistema: Double,
    val stockContado: Double
) {
    val diferencia: Double get() = stockContado - stockSistema
}

/**
 * Repositorio de Conteos de inventario. Cerrar el conteo ajusta el stock (RPC).
 */
class ConteoRepository {

    private val client = SupabaseClient.client

    suspend fun getConteos(): List<Conteo> = try {
        client.postgrest.from("conteos").select().decodeList<Conteo>().sortedByDescending { it.fecha }
    } catch (e: Exception) {
        Log.e("ConteoRepository", "Error getConteos: ${e.message}", e)
        emptyList()
    }

    suspend fun getDetalle(conteoId: Int): List<ConteoDetalle> = try {
        client.postgrest.from("conteo_detalle").select { filter { eq("conteo_id", conteoId) } }
            .decodeList<ConteoDetalle>()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun getArticulos(): List<Articulo> = try {
        client.postgrest.from("articulos").select().decodeList<Articulo>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) { emptyList() }

    /** Crea un conteo con su detalle (stock sistema vs contado) y opcionalmente lo cierra. */
    suspend fun crearConteo(lineas: List<LineaConteo>, nota: String, usuarioId: String?, cerrar: Boolean): Int? {
        return try {
            val conteoId = client.postgrest.from("conteos").insert(
                buildJsonObject {
                    put("estado", if (cerrar) "ABIERTO" else "ABIERTO") // se cierra vía RPC abajo
                    put("nota", nota)
                    if (usuarioId == null) put("created_by", JsonNull) else put("created_by", usuarioId)
                }
            ) { select() }.decodeSingle<Conteo>().id

            lineas.forEach { l ->
                client.postgrest.from("conteo_detalle").insert(
                    buildJsonObject {
                        put("conteo_id", conteoId)
                        put("articulo_id", l.articulo.id)
                        put("stock_sistema", l.stockSistema)
                        put("stock_contado", l.stockContado)
                        put("diferencia", l.diferencia)
                        if (usuarioId == null) put("created_by", JsonNull) else put("created_by", usuarioId)
                    }
                )
            }

            if (cerrar) cerrarConteo(conteoId)
            conteoId
        } catch (e: Exception) {
            Log.e("ConteoRepository", "Error crearConteo: ${e.message}", e)
            throw e
        }
    }

    /** Cierra el conteo: ajusta el stock de los artículos al valor contado. */
    suspend fun cerrarConteo(conteoId: Int) {
        client.postgrest.rpc("cerrar_conteo", buildJsonObject { put("p_conteo_id", conteoId) })
    }

    suspend fun eliminarConteo(id: Int) {
        client.postgrest.from("conteos").delete { filter { eq("id", id) } }
    }
}
