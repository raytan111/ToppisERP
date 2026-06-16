package com.toppis.app.data.repository

import com.toppis.app.data.models.Gasto
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.MovimientoSobre
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.models.Venta
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.FechaUtil
import io.github.jan.supabase.postgrest.postgrest

/**
 * Repositorio de Exportación: obtiene todos los datos desde Supabase para
 * generar archivos Excel/CSV/ZIP.
 */
class ExportacionRepository {

    private val client = SupabaseClient.client

    suspend fun getVentas(desde: Long = 0L): List<Venta> {
        val ventas = client.postgrest.from("ventas").select().decodeList<Venta>()
        return if (desde > 0L) ventas.filter { FechaUtil.isoToMillis(it.fecha) >= desde } else ventas
    }

    suspend fun getGastos(desde: Long = 0L): List<Gasto> {
        val gastos = client.postgrest.from("gastos").select().decodeList<Gasto>()
        return if (desde > 0L) gastos.filter { FechaUtil.isoToMillis(it.fecha) >= desde } else gastos
    }

    suspend fun getSobres(): List<Sobre> =
        client.postgrest.from("sobres").select().decodeList<Sobre>()

    suspend fun getMovimientos(): List<MovimientoSobre> =
        client.postgrest.from("movimientos_sobre").select().decodeList<MovimientoSobre>()

    suspend fun getArticulos(): List<Articulo> =
        client.postgrest.from("articulos").select().decodeList<Articulo>()
}
