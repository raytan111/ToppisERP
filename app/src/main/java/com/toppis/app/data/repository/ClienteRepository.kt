package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.Cliente
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de Clientes (tabla "clientes"). Identificación por los 3 últimos dígitos
 * del WhatsApp; nombre opcional y editable; sellos de la cuponera.
 */
class ClienteRepository {

    private val client = SupabaseClient.client

    suspend fun getClientes(): List<Cliente> = try {
        client.postgrest.from("clientes").select().decodeList<Cliente>()
            .sortedBy { it.nombre ?: it.telefono3 }
    } catch (e: Exception) {
        Log.e("ClienteRepository", "Error getClientes: ${e.message}", e)
        emptyList()
    }

    /** Clientes que coinciden en los 3 dígitos (puede haber varios). */
    suspend fun buscarPorTelefono3(telefono3: String): List<Cliente> = try {
        client.postgrest.from("clientes").select {
            filter { eq("telefono3", telefono3) }
        }.decodeList<Cliente>()
    } catch (e: Exception) {
        Log.e("ClienteRepository", "Error buscarPorTelefono3: ${e.message}", e)
        emptyList()
    }

    /**
     * Devuelve un cliente para esos 3 dígitos, creándolo si no existe. Si hay varias
     * coincidencias, usa la que calce por nombre; si el nombre viene vacío, la primera.
     */
    suspend fun obtenerOCrear(telefono3: String, nombre: String?): Cliente {
        val matches = buscarPorTelefono3(telefono3)
        if (matches.isNotEmpty()) {
            if (nombre.isNullOrBlank()) return matches.first()
            matches.firstOrNull { it.nombre.equals(nombre.trim(), ignoreCase = true) }?.let { return it }
        }
        return crear(telefono3, nombre)
    }

    /** Crea un cliente (nombre opcional) y lo devuelve. */
    suspend fun crear(telefono3: String, nombre: String?): Cliente {
        return client.postgrest.from("clientes").insert(
            buildJsonObject {
                put("telefono3", telefono3)
                if (!nombre.isNullOrBlank()) put("nombre", nombre.trim())
                LocalSession.activoId.value?.let { put("local_id", it) }
            }
        ) { select() }.decodeSingle<Cliente>()
    }

    /** Actualiza el nombre del cliente. */
    suspend fun actualizarNombre(id: Int, nombre: String) {
        client.postgrest.from("clientes").update(
            buildJsonObject { put("nombre", nombre.trim()); put("updated_at", java.time.Instant.now().toString()) }
        ) { filter { eq("id", id) } }
    }

    /** Fija los sellos de la cuponera (para cargar cupones ya existentes). */
    suspend fun fijarSellos(id: Int, sellos: Int) {
        client.postgrest.from("clientes").update(
            buildJsonObject { put("sellos_hamburguesa", sellos.coerceAtLeast(0)) }
        ) { filter { eq("id", id) } }
    }
}
