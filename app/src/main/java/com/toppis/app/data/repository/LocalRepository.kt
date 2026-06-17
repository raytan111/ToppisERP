package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.Local
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Sesión del local activo (en memoria). El id se usa para sellar transacciones.
 */
object LocalSession {
    private val _activoId = MutableStateFlow<Int?>(null)
    val activoId: StateFlow<Int?> = _activoId.asStateFlow()

    private val _activoNombre = MutableStateFlow<String?>(null)
    val activoNombre: StateFlow<String?> = _activoNombre.asStateFlow()

    fun setActivo(id: Int?, nombre: String?) {
        _activoId.value = id
        _activoNombre.value = nombre
    }
}

/**
 * Repositorio de Locales / sucursales.
 */
class LocalRepository {

    private val client = SupabaseClient.client

    suspend fun getLocales(): List<Local> = try {
        client.postgrest.from("locales").select().decodeList<Local>().sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("LocalRepository", "Error getLocales: ${e.message}", e)
        emptyList()
    }

    suspend fun crear(nombre: String, direccion: String) {
        client.postgrest.from("locales").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("direccion", direccion)
                put("activo", true)
            }
        )
    }

    suspend fun actualizar(l: Local) {
        client.postgrest.from("locales").update(
            buildJsonObject {
                put("nombre", l.nombre)
                put("direccion", l.direccion)
                put("activo", l.activo)
            }
        ) {
            filter { eq("id", l.id) }
        }
    }

    suspend fun eliminar(id: Int) {
        client.postgrest.from("locales").delete { filter { eq("id", id) } }
    }
}
