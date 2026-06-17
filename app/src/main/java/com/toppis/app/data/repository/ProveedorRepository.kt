package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.Proveedor
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de Proveedores.
 */
class ProveedorRepository {

    private val client = SupabaseClient.client

    suspend fun getProveedores(): List<Proveedor> = try {
        client.postgrest.from("proveedores").select().decodeList<Proveedor>().sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("ProveedorRepository", "Error getProveedores: ${e.message}", e)
        emptyList()
    }

    suspend fun getActivos(): List<Proveedor> = getProveedores().filter { it.activo }

    suspend fun crear(nombre: String, contacto: String, telefono: String, email: String, nota: String) {
        client.postgrest.from("proveedores").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("contacto", contacto)
                put("telefono", telefono)
                put("email", email)
                put("nota", nota)
                put("activo", true)
            }
        )
    }

    suspend fun actualizar(p: Proveedor) {
        client.postgrest.from("proveedores").update(
            buildJsonObject {
                put("nombre", p.nombre)
                put("contacto", p.contacto)
                put("telefono", p.telefono)
                put("email", p.email)
                put("nota", p.nota)
                put("activo", p.activo)
            }
        ) {
            filter { eq("id", p.id) }
        }
    }

    suspend fun eliminar(id: Int) {
        client.postgrest.from("proveedores").delete { filter { eq("id", id) } }
    }
}
