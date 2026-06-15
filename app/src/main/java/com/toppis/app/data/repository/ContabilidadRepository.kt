package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.EstadoVenta
import com.toppis.app.data.models.Comprobante
import com.toppis.app.data.models.Gasto
import com.toppis.app.data.models.Venta
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.FechaUtil
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Calendar
import kotlin.math.roundToLong

/** Resumen contable mensual (base del F29). */
data class ResumenContable(
    val mes: Int,
    val anio: Int,
    val ventasNetas: Double,
    val ivaDebito: Double,
    val comprasNetas: Double,
    val ivaCredito: Double,
    val ivaAPagar: Double,
    val resultado: Double,
    val totalVentas: Double,
    val totalGastos: Double
)

/** Línea del libro de ventas. */
data class LineaLibroVenta(
    val ventaId: Int,
    val fecha: String?,
    val neto: Double,
    val iva: Double,
    val total: Double,
    val folioComprobante: Int?
)

/** Línea del libro de compras. */
data class LineaLibroCompra(
    val gastoId: Long,
    val fecha: String?,
    val descripcion: String,
    val neto: Double,
    val iva: Double,
    val total: Double
)

/**
 * Repositorio de Contabilidad (Fase 3): agrega ventas/gastos/comprobantes
 * en términos contables (IVA débito/crédito, libros, cierre mensual).
 */
class ContabilidadRepository {

    private val client = SupabaseClient.client

    private fun rangoMes(mes: Int, anio: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(anio, mes - 1, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
        val inicio = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val fin = cal.timeInMillis
        return inicio to fin
    }

    private suspend fun ventasDelMes(mes: Int, anio: Int): List<Venta> {
        val (inicio, fin) = rangoMes(mes, anio)
        return try {
            client.postgrest.from("ventas").select().decodeList<Venta>()
                .filter { it.estado == EstadoVenta.COMPLETADA }
                .filter { FechaUtil.isoToMillis(it.fecha) in inicio..fin }
        } catch (e: Exception) {
            Log.e("ContabilidadRepository", "Error ventasDelMes: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun gastosDelMes(mes: Int, anio: Int): List<Gasto> {
        val (inicio, fin) = rangoMes(mes, anio)
        return try {
            client.postgrest.from("gastos").select().decodeList<Gasto>()
                .filter { FechaUtil.isoToMillis(it.fecha) in inicio..fin }
        } catch (e: Exception) {
            Log.e("ContabilidadRepository", "Error gastosDelMes: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun comprobantesDelMes(mes: Int, anio: Int): List<Comprobante> {
        val (inicio, fin) = rangoMes(mes, anio)
        return try {
            client.postgrest.from("comprobantes").select().decodeList<Comprobante>()
                .filter { FechaUtil.isoToMillis(it.fechaEmision) in inicio..fin }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun netoDe(total: Double): Double = (total / 1.19).roundToLong().toDouble()

    suspend fun getResumen(mes: Int, anio: Int): ResumenContable {
        val ventas = ventasDelMes(mes, anio)
        val gastos = gastosDelMes(mes, anio)

        val totalVentas = ventas.sumOf { it.total }
        val ivaDebito = ventas.sumOf { it.total - netoDe(it.total) }
        val ventasNetas = totalVentas - ivaDebito

        val totalGastos = gastos.sumOf { it.monto }
        val ivaCredito = gastos.filter { it.tieneIva }.sumOf { it.montoIva ?: 0.0 }
        val comprasNetas = totalGastos - ivaCredito

        return ResumenContable(
            mes = mes,
            anio = anio,
            ventasNetas = ventasNetas,
            ivaDebito = ivaDebito,
            comprasNetas = comprasNetas,
            ivaCredito = ivaCredito,
            ivaAPagar = ivaDebito - ivaCredito,
            resultado = ventasNetas - comprasNetas,
            totalVentas = totalVentas,
            totalGastos = totalGastos
        )
    }

    suspend fun getLibroVentas(mes: Int, anio: Int): List<LineaLibroVenta> {
        val ventas = ventasDelMes(mes, anio)
        val comprobantes = comprobantesDelMes(mes, anio).associateBy { it.ventaId }
        return ventas.map { v ->
            val neto = netoDe(v.total)
            LineaLibroVenta(
                ventaId = v.id,
                fecha = v.fecha,
                neto = neto,
                iva = v.total - neto,
                total = v.total,
                folioComprobante = comprobantes[v.id]?.folio
            )
        }.sortedBy { it.ventaId }
    }

    suspend fun getLibroCompras(mes: Int, anio: Int): List<LineaLibroCompra> {
        return gastosDelMes(mes, anio).map { g ->
            LineaLibroCompra(
                gastoId = g.id,
                fecha = g.fecha,
                descripcion = g.descripcion,
                neto = g.montoNeto ?: g.monto,
                iva = g.montoIva ?: 0.0,
                total = g.monto
            )
        }.sortedBy { it.gastoId }
    }

    /** Guarda (o actualiza) el cierre mensual con el resumen calculado. */
    suspend fun cerrarMes(resumen: ResumenContable, usuarioId: String?) {
        client.postgrest.from("cierres_mensuales").upsert(
            buildJsonObject {
                put("mes", resumen.mes)
                put("anio", resumen.anio)
                put("ventas_netas", resumen.ventasNetas)
                put("iva_debito", resumen.ivaDebito)
                put("compras_netas", resumen.comprasNetas)
                put("iva_credito", resumen.ivaCredito)
                put("iva_a_pagar", resumen.ivaAPagar)
                put("resultado", resumen.resultado)
            }
        ) {
            onConflict = "mes,anio"
        }
    }
}
