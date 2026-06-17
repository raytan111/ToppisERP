package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.TipoPago
import com.toppis.app.data.models.Empleado
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de Empleados.
 */
class EmpleadoRepository {

    private val client = SupabaseClient.client

    suspend fun getEmpleados(): List<Empleado> = try {
        client.postgrest.from("empleados").select().decodeList<Empleado>().sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("EmpleadoRepository", "Error getEmpleados: ${e.message}", e)
        emptyList()
    }

    suspend fun getActivos(): List<Empleado> = getEmpleados().filter { it.activo }

    suspend fun crear(nombre: String, cargo: String, tipoPago: TipoPago, monto: Double) {
        client.postgrest.from("empleados").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("cargo", cargo)
                put("tipo_pago", tipoPago.name)
                put("monto", monto)
                put("activo", true)
            }
        )
    }

    suspend fun actualizar(e: Empleado) {
        client.postgrest.from("empleados").update(
            buildJsonObject {
                put("nombre", e.nombre)
                put("cargo", e.cargo)
                put("tipo_pago", e.tipoPago.name)
                put("monto", e.monto)
                put("activo", e.activo)
            }
        ) {
            filter { eq("id", e.id) }
        }
    }

    suspend fun eliminar(id: Int) {
        client.postgrest.from("empleados").delete { filter { eq("id", id) } }
    }
}
