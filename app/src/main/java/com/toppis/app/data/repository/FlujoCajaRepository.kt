package com.toppis.app.data.repository

import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.db.entities.EstadoVenta
import com.toppis.app.data.models.Gasto
import com.toppis.app.data.models.Presupuesto
import com.toppis.app.data.models.Venta
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.FechaUtil
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ── Data classes de resultados ────────────────────────────────────────────────

data class ResumenFlujoCaja(
    val totalIngresos: Double,
    val totalEgresos: Double,
    val resultadoOperacional: Double,
    val margenPorcentaje: Double,
    val ventasPorDia: Map<String, Double>,
    val gastosPorDia: Map<String, Double>,
    val gastosPorCategoria: Map<CategoriaGasto, Double>
)

data class ProyeccionMes(
    val mes: Int,
    val anio: Int,
    val ingresoEstimado: Double,
    val egresoEstimado: Double,
    val basadoEnPromedio: Boolean
)

data class PresupuestoVsReal(
    val categoria: CategoriaGasto,
    val presupuestado: Double,
    val gastadoReal: Double,
    val diferencia: Double,
    val porcentajeUsado: Double
)

/**
 * Repositorio de Flujo de Caja basado en Supabase.
 */
class FlujoCajaRepository {

    private val client = SupabaseClient.client
    private val sdfDia get() = SimpleDateFormat("dd/MM", Locale.getDefault())

    // ── Fetch helpers ─────────────────────────────────────────────────────────

    private suspend fun fetchVentasCompletadas(): List<Venta> = try {
        client.postgrest.from("ventas").select().decodeList<Venta>()
            .filter { it.estado == EstadoVenta.COMPLETADA }
    } catch (e: Exception) {
        emptyList()
    }

    private suspend fun fetchGastos(): List<Gasto> = try {
        client.postgrest.from("gastos").select().decodeList<Gasto>()
    } catch (e: Exception) {
        emptyList()
    }

    private fun vMillis(v: Venta) = FechaUtil.isoToMillis(v.fecha)
    private fun gMillis(g: Gasto) = FechaUtil.isoToMillis(g.fecha)

    // ── Resumen de período ────────────────────────────────────────────────────

    suspend fun getResumenPeriodo(desde: Long, hasta: Long): ResumenFlujoCaja {
        val ventas = fetchVentasCompletadas().filter { vMillis(it) in desde..hasta }
        val gastos = fetchGastos().filter { gMillis(it) in desde..hasta }

        val totalIngresos = ventas.sumOf { it.total }
        val totalEgresos = gastos.sumOf { it.monto }
        val resultado = totalIngresos - totalEgresos
        val margen = if (totalIngresos > 0) (resultado / totalIngresos) * 100.0 else 0.0

        val ventasPorDia = ventas.groupBy { sdfDia.format(Date(vMillis(it))) }
            .mapValues { (_, list) -> list.sumOf { it.total } }
        val gastosPorDia = gastos.groupBy { sdfDia.format(Date(gMillis(it))) }
            .mapValues { (_, list) -> list.sumOf { it.monto } }
        val gastosPorCat = gastos.groupBy { it.categoria }
            .mapValues { (_, list) -> list.sumOf { it.monto } }

        return ResumenFlujoCaja(
            totalIngresos = totalIngresos,
            totalEgresos = totalEgresos,
            resultadoOperacional = resultado,
            margenPorcentaje = margen,
            ventasPorDia = ventasPorDia,
            gastosPorDia = gastosPorDia,
            gastosPorCategoria = gastosPorCat
        )
    }

    // ── Proyección (promedio últimos 3 meses) ──────────────────────────────────

    suspend fun getProyeccion(meses: Int = 3): List<ProyeccionMes> {
        val ventas = fetchVentasCompletadas()
        val gastos = fetchGastos()

        val historialIngresos = mutableListOf<Double>()
        val historialEgresos = mutableListOf<Double>()

        repeat(3) { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -(i + 1))
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val inicio = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            val fin = cal.timeInMillis

            historialIngresos.add(ventas.filter { vMillis(it) in inicio..fin }.sumOf { it.total })
            historialEgresos.add(gastos.filter { gMillis(it) in inicio..fin }.sumOf { it.monto })
        }

        val promedioIngresos = if (historialIngresos.isEmpty()) 0.0 else historialIngresos.average()
        val promedioEgresos = if (historialEgresos.isEmpty()) 0.0 else historialEgresos.average()

        return (1..meses).map { i ->
            val cal = Calendar.getInstance().also { it.add(Calendar.MONTH, i) }
            ProyeccionMes(
                mes = cal.get(Calendar.MONTH) + 1,
                anio = cal.get(Calendar.YEAR),
                ingresoEstimado = promedioIngresos,
                egresoEstimado = promedioEgresos,
                basadoEnPromedio = true
            )
        }
    }

    // ── Saldo total de todos los sobres ─────────────────────────────────────────

    suspend fun getSaldoTotal(): Double = try {
        client.postgrest.from("sobres").select(Columns.list("saldo"))
            .decodeList<Map<String, Double>>()
            .sumOf { it["saldo"] ?: 0.0 }
    } catch (e: Exception) {
        0.0
    }

    // ── Presupuesto vs Real ─────────────────────────────────────────────────────

    suspend fun getPresupuestoVsReal(mes: Int, anio: Int): List<PresupuestoVsReal> {
        val cal = Calendar.getInstance()
        cal.set(anio, mes - 1, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
        val inicio = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val fin = cal.timeInMillis

        val presupuestos = getPresupuestos(mes, anio)
        val gastosMes = fetchGastos().filter { gMillis(it) in inicio..fin }

        return CategoriaGasto.entries.mapNotNull { categoria ->
            val presupuesto = presupuestos.find { it.categoria == categoria }
            val gastadoReal = gastosMes.filter { it.categoria == categoria }.sumOf { it.monto }
            if (presupuesto == null && gastadoReal == 0.0) return@mapNotNull null

            val presupuestado = presupuesto?.montoPresupuestado ?: 0.0
            val diferencia = presupuestado - gastadoReal
            val porcentaje = if (presupuestado > 0) (gastadoReal / presupuestado) * 100.0 else 100.0

            PresupuestoVsReal(
                categoria = categoria,
                presupuestado = presupuestado,
                gastadoReal = gastadoReal,
                diferencia = diferencia,
                porcentajeUsado = porcentaje
            )
        }
    }

    private suspend fun getPresupuestos(mes: Int, anio: Int): List<Presupuesto> = try {
        client.postgrest.from("presupuestos").select {
            filter {
                eq("mes", mes)
                eq("anio", anio)
            }
        }.decodeList<Presupuesto>()
    } catch (e: Exception) {
        emptyList()
    }

    // ── Guardar / actualizar presupuesto (upsert) ────────────────────────────────

    suspend fun guardarPresupuesto(
        categoria: CategoriaGasto,
        monto: Double,
        mes: Int,
        anio: Int
    ) {
        client.postgrest.from("presupuestos").upsert(
            buildJsonObject {
                put("mes", mes)
                put("anio", anio)
                put("categoria_gasto", categoria.name)
                put("monto_presupuestado", monto)
            }
        ) {
            onConflict = "mes,anio,categoria_gasto"
        }
    }
}
