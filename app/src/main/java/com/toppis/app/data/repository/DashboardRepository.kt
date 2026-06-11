package com.toppis.app.data.repository

import com.toppis.app.data.db.entities.EstadoVenta
import com.toppis.app.data.models.Gasto
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.models.Venta
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.FechaUtil
import io.github.jan.supabase.postgrest.postgrest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DashboardKpi(
    val ingresos: Double,
    val egresos: Double,
    val gananciaNeta: Double,
    val flujoCajaTotal: Double,
    val margenPorcentaje: Double,
    val ingresosAnterior: Double,
    val egresosAnterior: Double,
    val gananciaNetaAnterior: Double
)

data class DatoSerie(val label: String, val ingresos: Double, val egresos: Double)

data class DistribucionEgreso(val categoria: String, val monto: Double)

/**
 * Repositorio del Dashboard basado en Supabase (lectura + agregación de KPIs).
 */
class DashboardRepository {

    private val client = SupabaseClient.client
    private val sdfDia get() = SimpleDateFormat("dd/MM", Locale.getDefault())

    // ── Fetch helpers ─────────────────────────────────────────────────────────

    private suspend fun fetchVentas(): List<Venta> = try {
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

    private suspend fun fetchSobres(): List<Sobre> = try {
        client.postgrest.from("sobres").select().decodeList<Sobre>()
    } catch (e: Exception) {
        emptyList()
    }

    private fun ventaMillis(v: Venta) = FechaUtil.isoToMillis(v.fecha)
    private fun gastoMillis(g: Gasto) = FechaUtil.isoToMillis(g.fecha)

    // ── KPIs ────────────────────────────────────────────────────────────────────

    suspend fun getKpi(desde: Long, hasta: Long, desdeAnterior: Long, hastaAnterior: Long): DashboardKpi {
        val todasVentas = fetchVentas()
        val todosGastos = fetchGastos()

        val ventas = todasVentas.filter { ventaMillis(it) in desde..hasta }
        val gastos = todosGastos.filter { gastoMillis(it) in desde..hasta }
        val ventasAnt = todasVentas.filter { ventaMillis(it) in desdeAnterior..hastaAnterior }
        val gastosAnt = todosGastos.filter { gastoMillis(it) in desdeAnterior..hastaAnterior }

        val ingresos = ventas.sumOf { it.total }
        val egresos = gastos.sumOf { it.monto }
        val gananciaNeta = ingresos - egresos

        val ingresosAnt = ventasAnt.sumOf { it.total }
        val egresosAnt = gastosAnt.sumOf { it.monto }
        val gananciaNetaAnt = ingresosAnt - egresosAnt

        val flujoCajaTotal = fetchSobres().sumOf { it.saldo }
        val margen = if (ingresos > 0) (gananciaNeta / ingresos) * 100.0 else 0.0

        return DashboardKpi(
            ingresos = ingresos,
            egresos = egresos,
            gananciaNeta = gananciaNeta,
            flujoCajaTotal = flujoCajaTotal,
            margenPorcentaje = margen,
            ingresosAnterior = ingresosAnt,
            egresosAnterior = egresosAnt,
            gananciaNetaAnterior = gananciaNetaAnt
        )
    }

    suspend fun getSerieTiempo(desde: Long, hasta: Long): List<DatoSerie> {
        val ventas = fetchVentas().filter { ventaMillis(it) in desde..hasta }
        val gastos = fetchGastos().filter { gastoMillis(it) in desde..hasta }

        val ventasPorDia = ventas.groupBy { sdfDia.format(Date(ventaMillis(it))) }
            .mapValues { (_, list) -> list.sumOf { it.total } }
        val gastosPorDia = gastos.groupBy { sdfDia.format(Date(gastoMillis(it))) }
            .mapValues { (_, list) -> list.sumOf { it.monto } }

        val todasFechas = (ventasPorDia.keys + gastosPorDia.keys).distinct().sorted()
        return todasFechas.map { fecha ->
            DatoSerie(
                label = fecha,
                ingresos = ventasPorDia[fecha] ?: 0.0,
                egresos = gastosPorDia[fecha] ?: 0.0
            )
        }
    }

    suspend fun getDistribucionEgresos(desde: Long, hasta: Long): List<DistribucionEgreso> {
        val gastos = fetchGastos().filter { gastoMillis(it) in desde..hasta }
        val porCategoria = gastos.groupBy { it.categoria }
            .mapValues { (_, list) -> list.sumOf { it.monto } }
            .filter { it.value > 0 }

        val ventas = fetchVentas().filter { ventaMillis(it) in desde..hasta }
        val totalEnvios = ventas.filter { it.incluirEnvio }.sumOf { it.montoEnvio }

        val result = mutableListOf<DistribucionEgreso>()
        porCategoria.forEach { (cat, monto) -> result.add(DistribucionEgreso(cat.label, monto)) }
        if (totalEnvios > 0) result.add(DistribucionEgreso("Envíos", totalEnvios))
        return result.sortedByDescending { it.monto }
    }

    companion object {
        fun rangoMesActual(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val desde = cal.timeInMillis
            val hasta = System.currentTimeMillis()
            return Pair(desde, hasta)
        }

        fun rangoMesAnterior(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val desde = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
            val hasta = cal.timeInMillis
            return Pair(desde, hasta)
        }
    }
}
