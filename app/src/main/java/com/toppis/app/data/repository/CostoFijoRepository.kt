package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.db.entities.Periodicidad
import com.toppis.app.data.models.CostoFijo
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de costos fijos recurrentes (tabla "costos_fijos").
 * Los montos se guardan en CLP con IVA incluido.
 */
class CostoFijoRepository {

    private val client = SupabaseClient.client

    suspend fun getCostosFijos(): List<CostoFijo> = try {
        client.postgrest.from("costos_fijos").select().decodeList<CostoFijo>()
            .sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("CostoFijoRepository", "Error getCostosFijos: ${e.message}", e)
        emptyList()
    }

    /** Crea un costo fijo. Lanza excepción si el monto es negativo (Req 1.6). */
    suspend fun crear(nombre: String, categoria: CategoriaGasto, monto: Double, periodicidad: Periodicidad) {
        require(monto >= 0.0) { "El monto debe ser mayor o igual a 0." }
        client.postgrest.from("costos_fijos").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("categoria", categoria.name)
                put("monto", monto)
                put("periodicidad", periodicidad.name)
                put("activo", true)
                LocalSession.activoId.value?.let { put("local_id", it) }
            }
        )
    }

    /** Actualiza un costo fijo existente. Lanza excepción si el monto es negativo. */
    suspend fun actualizar(costo: CostoFijo) {
        require(costo.monto >= 0.0) { "El monto debe ser mayor o igual a 0." }
        client.postgrest.from("costos_fijos").update(
            buildJsonObject {
                put("nombre", costo.nombre)
                put("categoria", costo.categoria.name)
                put("monto", costo.monto)
                put("periodicidad", costo.periodicidad.name)
                put("activo", costo.activo)
            }
        ) { filter { eq("id", costo.id) } }
    }

    suspend fun eliminar(id: Int) {
        client.postgrest.from("costos_fijos").delete { filter { eq("id", id) } }
    }
}
