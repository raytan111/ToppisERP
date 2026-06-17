package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.TipoPago
import com.toppis.app.data.models.Empleado
import com.toppis.app.data.models.Jornada
import com.toppis.app.data.models.Propina
import com.toppis.app.data.models.Venta
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Resultado del Prime Cost de un período. */
data class PrimeCost(
    val ventas: Double,
    val foodCost: Double,
    val laborCost: Double,
    val propinas: Double
) {
    val primeCost: Double get() = foodCost + laborCost
    val foodPct: Double get() = if (ventas > 0) foodCost / ventas * 100 else 0.0
    val laborPct: Double get() = if (ventas > 0) laborCost / ventas * 100 else 0.0
    val primePct: Double get() = if (ventas > 0) primeCost / ventas * 100 else 0.0
}

/** Jornada con el nombre del empleado. */
data class JornadaConNombre(val jornada: Jornada, val nombre: String)

/**
 * Repositorio de Mano de obra: jornadas, propinas y cálculo de Prime Cost.
 */
class ManoObraRepository {

    private val client = SupabaseClient.client

    // ── Jornadas ────────────────────────────────────────────────────────────

    suspend fun getJornadas(desdeIso: String, hastaIso: String): List<JornadaConNombre> {
        val js = try {
            client.postgrest.from("jornadas").select().decodeList<Jornada>()
                .filter { (it.fecha ?: "") >= desdeIso.take(10) && (it.fecha ?: "") < hastaIso.take(10) }
                .sortedByDescending { it.fecha }
        } catch (e: Exception) { emptyList() }
        val emp = getEmpleados().associateBy { it.id }
        return js.map { JornadaConNombre(it, emp[it.empleadoId]?.nombre ?: "Empleado #${it.empleadoId}") }
    }

    suspend fun registrarJornada(empleado: Empleado, fecha: String, cantidad: Double, nota: String, usuarioId: String?) {
        val costo = cantidad * empleado.monto  // POR_TURNO/POR_HORA: cantidad × valor
        client.postgrest.from("jornadas").insert(
            buildJsonObject {
                put("empleado_id", empleado.id)
                put("fecha", fecha)
                put("cantidad", cantidad)
                put("costo", costo)
                put("nota", nota)
            }
        )
    }

    suspend fun eliminarJornada(id: Int) {
        client.postgrest.from("jornadas").delete { filter { eq("id", id) } }
    }

    // ── Propinas ────────────────────────────────────────────────────────────

    suspend fun getPropinas(desdeIso: String, hastaIso: String): List<Propina> = try {
        client.postgrest.from("propinas").select().decodeList<Propina>()
            .filter { (it.fecha ?: "") >= desdeIso.take(10) && (it.fecha ?: "") < hastaIso.take(10) }
            .sortedByDescending { it.fecha }
    } catch (e: Exception) { emptyList() }

    suspend fun registrarPropina(fecha: String, monto: Double, nota: String) {
        client.postgrest.from("propinas").insert(
            buildJsonObject {
                put("fecha", fecha)
                put("monto", monto)
                put("nota", nota)
            }
        )
    }

    suspend fun eliminarPropina(id: Int) {
        client.postgrest.from("propinas").delete { filter { eq("id", id) } }
    }

    // ── Empleados (helper) ────────────────────────────────────────────────────

    suspend fun getEmpleados(): List<Empleado> = try {
        client.postgrest.from("empleados").select().decodeList<Empleado>()
    } catch (e: Exception) { emptyList() }

    // ── Prime Cost ────────────────────────────────────────────────────────────

    suspend fun getPrimeCost(desdeIso: String, hastaIso: String): PrimeCost {
        // Ventas del período (completadas)
        val ventas = try {
            client.postgrest.from("ventas").select().decodeList<Venta>()
                .filter { it.estado.name == "COMPLETADA" && (it.fecha ?: "") >= desdeIso && (it.fecha ?: "") < hastaIso }
                .sumOf { it.total }
        } catch (e: Exception) { 0.0 }

        // Food cost teórico (consumo según ventas) vía RPC de Fase 5
        val food = try {
            client.postgrest.rpc("consumo_teorico_periodo", buildJsonObject {
                put("p_desde", desdeIso)
                put("p_hasta", hastaIso)
            }).decodeList<ConsumoTeoricoRow>().sumOf { it.costo }
        } catch (e: Exception) {
            Log.e("ManoObraRepository", "Error consumo_teorico: ${e.message}", e)
            0.0
        }

        // Labor: jornadas del período + sueldos fijos mensuales (empleados activos)
        val jornadasCosto = getJornadas(desdeIso, hastaIso).sumOf { it.jornada.costo }
        val sueldosFijos = getEmpleados().filter { it.activo && it.tipoPago == TipoPago.SUELDO_FIJO }.sumOf { it.monto }
        val labor = jornadasCosto + sueldosFijos

        val propinas = getPropinas(desdeIso, hastaIso).sumOf { it.monto }

        return PrimeCost(ventas = ventas, foodCost = food, laborCost = labor, propinas = propinas)
    }
}
