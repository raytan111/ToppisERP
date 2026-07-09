package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.db.entities.EstadoCierre
import com.toppis.app.data.db.entities.EstadoVenta
import com.toppis.app.data.db.entities.GrupoCosto
import com.toppis.app.data.models.Compra
import com.toppis.app.data.models.Empleado
import com.toppis.app.data.models.Gasto
import com.toppis.app.data.models.Jornada
import com.toppis.app.data.models.Venta
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.SemanaOperativa
import com.toppis.app.domain.costos.CostosCalculos
import com.toppis.app.domain.costos.ManoObraDisponible
import com.toppis.app.domain.costos.ResultadoSemanal
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

/**
 * Agregador del resultado semanal (mirada de caja). Reúne ventas, compras,
 * gastos, jornadas, fijos y food teórico de una semana y arma el ResultadoSemanal.
 * Si la semana está cerrada, devuelve el snapshot congelado.
 */
class ResultadoSemanalRepository(
    private val configRepo: ConfigCostosRepository,
    private val costoFijoRepo: CostoFijoRepository,
    private val cierreRepo: CierreSemanalRepository
) {
    private val client = SupabaseClient.client

    private fun enSemana(fecha: String?, semana: SemanaOperativa): Boolean {
        if (fecha.isNullOrBlank()) return false
        val d = runCatching { LocalDate.parse(fecha.take(10)) }.getOrNull() ?: return false
        return semana.contiene(d)
    }

    suspend fun getResultado(semana: SemanaOperativa): ResultadoSemanal {
        val objetivos = configRepo.getObjetivos()
        val costosFijos = costoFijoRepo.getCostosFijos()
        val fijos = CostosCalculos.totalFijosSemanales(costosFijos)
        val arriendoPro = costosFijos.filter { it.activo && it.categoria == CategoriaGasto.ARRIENDO }
            .sumOf { CostosCalculos.prorrateoSemanal(it.monto, it.periodicidad) }

        // Si la semana ya está cerrada, usar el snapshot congelado.
        val snap = cierreRepo.getSnapshot(semana)
        if (snap != null) {
            return armar(
                semana, snap.ventasCobradas, snap.costoVariable, snap.foodTeorico,
                snap.manoObraPagada, snap.fijosProrrateados, arriendoPro, objetivos,
                EstadoCierre.CERRADO
            )
        }

        // Cálculo en vivo con precios actuales.
        val ventas = fetchVentas().filter { it.estado == EstadoVenta.COMPLETADA && enSemana(it.fecha, semana) }
        val comprasSemana = fetchCompras().filter { enSemana(it.fecha, semana) }
        val gastos = fetchGastos().filter { enSemana(it.fecha, semana) }
        val jornadas = fetchJornadas().filter { enSemana(it.fecha, semana) }

        val linked = comprasSemana.mapNotNull { it.gastoId }.toSet()
        val gastosSinLink = gastos.filter { it.id !in linked }

        val ventasCobradas = ventas.sumOf { it.total }
        val costoVariable = comprasSemana.sumOf { it.total } +
            gastosSinLink.filter { CostosCalculos.grupoDe(it.categoria) == GrupoCosto.VARIABLE }.sumOf { it.monto }
        val manoObra = jornadas.sumOf { it.costo } +
            gastosSinLink.filter { it.categoria == CategoriaGasto.SUELDOS }.sumOf { it.monto }
        val foodTeorico = fetchFoodTeorico(semana)

        return armar(
            semana, ventasCobradas, costoVariable, foodTeorico, manoObra, fijos,
            arriendoPro, objetivos, EstadoCierre.ABIERTO
        )
    }

    /** Ensambla el ResultadoSemanal aplicando la capa de cálculo pura. */
    private fun armar(
        semana: SemanaOperativa,
        ventas: Double, variable: Double, foodTeorico: Double, manoObra: Double,
        fijos: Double, arriendoPro: Double, objetivos: ObjetivosCostos, estado: EstadoCierre
    ): ResultadoSemanal {
        val resultado = CostosCalculos.resultadoSemanal(ventas, variable, manoObra, fijos)
        val margen = CostosCalculos.margenContribucion(ventas, variable)
        val be = CostosCalculos.breakEven(fijos, margen)
        val foodPct = CostosCalculos.porcentajeSobreVentas(foodTeorico, ventas)
        val laborPct = CostosCalculos.porcentajeSobreVentas(manoObra, ventas)
        val arriendoPct = CostosCalculos.porcentajeSobreVentas(arriendoPro, ventas)
        return ResultadoSemanal(
            semana = semana,
            ventasCobradas = ventas,
            costoVariable = variable,
            foodTeorico = foodTeorico,
            manoObraPagada = manoObra,
            fijosProrrateados = fijos,
            resultado = resultado,
            foodPct = foodPct,
            laborPct = laborPct,
            arriendoPct = arriendoPct,
            margenContribucion = margen,
            breakEven = be,
            faltaVender = CostosCalculos.faltaVender(ventas, be),
            estado = estado,
            semaforoFood = CostosCalculos.semaforo(foodPct, objetivos.pctFood * 100.0),
            semaforoLabor = CostosCalculos.semaforo(laborPct, objetivos.pctManoObra * 100.0),
            semaforoArriendo = CostosCalculos.semaforo(arriendoPct, objetivos.pctArriendoTecho * 100.0),
            bajoBreakEven = CostosCalculos.bajoBreakEven(ventas, be),
            alertaArriendo = CostosCalculos.alertaArriendo(arriendoPro, ventas, objetivos.pctArriendoTecho)
        )
    }

    /** Mano de obra disponible para la semana (objetivo % × ventas), repartida entre empleados activos. */
    suspend fun getManoObraDisponible(semana: SemanaOperativa): ManoObraDisponible {
        val objetivos = configRepo.getObjetivos()
        val ventas = fetchVentas().filter { it.estado == EstadoVenta.COMPLETADA && enSemana(it.fecha, semana) }
            .sumOf { it.total }
        val empleadosActivos = fetchEmpleados().count { it.activo }
        val total = CostosCalculos.manoObraDisponible(objetivos.pctManoObra, ventas)
        val porPersona = CostosCalculos.manoObraPorPersona(total, empleadosActivos)
        return ManoObraDisponible(
            total = total,
            empleadosActivos = empleadosActivos,
            porPersona = porPersona,
            alcanzaParaContratar = CostosCalculos.alcanzaParaContratar(porPersona, total, objetivos.umbralContratarMo),
            esPresupuestoParaContratar = empleadosActivos == 0 && ventas > 0.0
        )
    }

    // ── Fetchers ──────────────────────────────────────────────────────────────

    private suspend fun fetchVentas(): List<Venta> = try {
        client.postgrest.from("ventas").select().decodeList<Venta>()
    } catch (e: Exception) { Log.e("ResultadoSemanal", "ventas: ${e.message}"); emptyList() }

    private suspend fun fetchCompras(): List<Compra> = try {
        client.postgrest.from("compras").select().decodeList<Compra>()
    } catch (e: Exception) { Log.e("ResultadoSemanal", "compras: ${e.message}"); emptyList() }

    private suspend fun fetchGastos(): List<Gasto> = try {
        client.postgrest.from("gastos").select().decodeList<Gasto>()
    } catch (e: Exception) { Log.e("ResultadoSemanal", "gastos: ${e.message}"); emptyList() }

    private suspend fun fetchJornadas(): List<Jornada> = try {
        client.postgrest.from("jornadas").select().decodeList<Jornada>()
    } catch (e: Exception) { Log.e("ResultadoSemanal", "jornadas: ${e.message}"); emptyList() }

    private suspend fun fetchEmpleados(): List<Empleado> = try {
        client.postgrest.from("empleados").select().decodeList<Empleado>()
    } catch (e: Exception) { Log.e("ResultadoSemanal", "empleados: ${e.message}"); emptyList() }

    private suspend fun fetchFoodTeorico(semana: SemanaOperativa): Double = try {
        client.postgrest.rpc("consumo_teorico_periodo", buildJsonObject {
            put("p_desde", semana.isoDesde)
            put("p_hasta", semana.isoHasta)
        }).decodeList<ConsumoTeoricoRow>().sumOf { it.costo }
    } catch (e: Exception) { Log.e("ResultadoSemanal", "food: ${e.message}"); 0.0 }
}
