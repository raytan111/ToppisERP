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
 * Sesión del local activo. Persiste en SharedPreferences y expone el id/nombre
 * para sellar transacciones nuevas.
 */
object LocalSession {
    private const val PREFS = "toppis_session"
    private const val KEY_ID = "local_id"
    private const val KEY_NOMBRE = "local_nombre"

    private var prefs: android.content.SharedPreferences? = null

    private val _activoId = MutableStateFlow<Int?>(null)
    val activoId: StateFlow<Int?> = _activoId.asStateFlow()

    private val _activoNombre = MutableStateFlow<String?>(null)
    val activoNombre: StateFlow<String?> = _activoNombre.asStateFlow()

    /** Inicializa desde SharedPreferences (llamar en MainActivity.onCreate). */
    fun init(context: android.content.Context) {
        val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        prefs = p
        val id = p.getInt(KEY_ID, -1)
        if (id > 0) {
            _activoId.value = id
            _activoNombre.value = p.getString(KEY_NOMBRE, null)
        }
    }

    fun setActivo(id: Int?, nombre: String?) {
        _activoId.value = id
        _activoNombre.value = nombre
        prefs?.edit()?.apply {
            if (id == null) { remove(KEY_ID); remove(KEY_NOMBRE) }
            else { putInt(KEY_ID, id); putString(KEY_NOMBRE, nombre) }
            apply()
        }
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
