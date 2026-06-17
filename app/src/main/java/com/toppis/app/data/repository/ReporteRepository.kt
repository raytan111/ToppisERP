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

    /** Ventas COMPLETADAS desde [inicioMillis], opcionalmente filtradas por local. */
    suspend fun getVentasDesde(inicioMillis: Long, localId: Int? = null): List<Venta> = try {
        val iso = FechaUtil.millisToIso(inicioMillis)
        client.postgrest.from("ventas").select {
            filter {
                gte("fecha", iso)
                if (localId != null) eq("local_id", localId)
            }
        }.decodeList<Venta>()
            .filter { it.estado == EstadoVenta.COMPLETADA }
            .sortedByDescending { it.fecha }
    } catch (e: Exception) {
        Log.e("ReporteRepository", "Error getVentasDesde: ${e.message}", e)
        emptyList()
    }

    /** Gastos desde [inicioMillis], opcionalmente filtrados por local. */
    suspend fun getGastosDesde(inicioMillis: Long, localId: Int? = null): List<Gasto> = try {
        val iso = FechaUtil.millisToIso(inicioMillis)
        client.postgrest.from("gastos").select {
            filter {
                gte("fecha", iso)
                if (localId != null) eq("local_id", localId)
            }
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

    /** Total de IVA provisionado (suma del IVA de los comprobantes) desde [inicioMillis]. */
    suspend fun getIvaProvisionadoDesde(inicioMillis: Long): Double = try {
        val iso = FechaUtil.millisToIso(inicioMillis)
        client.postgrest.from("comprobantes").select {
            filter { gte("fecha_emision", iso) }
        }.decodeList<com.toppis.app.data.models.Comprobante>()
            .sumOf { it.iva }
    } catch (e: Exception) {
        Log.e("ReporteRepository", "Error getIvaProvisionado: ${e.message}", e)
        0.0
    }
}
