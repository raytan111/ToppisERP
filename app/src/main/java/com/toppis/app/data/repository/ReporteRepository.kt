package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.EstadoVenta
import com.toppis.app.data.models.Gasto
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.models.Venta
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.FechaUtil
import io.github.jan.supabase.postgrest.postgrest

/**
 * Repositorio de Reportes basado en Supabase (solo lectura + agregación).
 */
class ReporteRepository {

    private val client = SupabaseClient.client

    /** Ventas COMPLETADAS desde [inicioMillis]. */
    suspend fun getVentasDesde(inicioMillis: Long): List<Venta> = try {
        val iso = FechaUtil.millisToIso(inicioMillis)
        client.postgrest.from("ventas").select {
            filter { gte("fecha", iso) }
        }.decodeList<Venta>()
            .filter { it.estado == EstadoVenta.COMPLETADA }
            .sortedByDescending { it.fecha }
    } catch (e: Exception) {
        Log.e("ReporteRepository", "Error getVentasDesde: ${e.message}", e)
        emptyList()
    }

    /** Gastos desde [inicioMillis]. */
    suspend fun getGastosDesde(inicioMillis: Long): List<Gasto> = try {
        val iso = FechaUtil.millisToIso(inicioMillis)
        client.postgrest.from("gastos").select {
            filter { gte("fecha", iso) }
        }.decodeList<Gasto>()
    } catch (e: Exception) {
        Log.e("ReporteRepository", "Error getGastosDesde: ${e.message}", e)
        emptyList()
    }

    suspend fun getSobres(): List<Sobre> = try {
        client.postgrest.from("sobres").select().decodeList<Sobre>().sortedBy { it.id }
    } catch (e: Exception) {
        emptyList()
    }
}
