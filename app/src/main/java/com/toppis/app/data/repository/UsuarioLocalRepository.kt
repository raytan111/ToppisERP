package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.models.Local
import com.toppis.app.data.models.Usuario
import com.toppis.app.data.models.UsuarioLocal
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Asignación con datos resueltos para mostrar. */
data class AsignacionLocal(
    val asignacion: UsuarioLocal,
    val nombreUsuario: String,
    val nombreLocal: String
)

/**
 * Repositorio de asignaciones usuario↔local.
 */
class UsuarioLocalRepository {

    private val client = SupabaseClient.client

    suspend fun getAsignaciones(): List<AsignacionLocal> {
        val asignaciones = try {
            client.postgrest.from("usuarios_locales").select().decodeList<UsuarioLocal>()
        } catch (e: Exception) {
            Log.e("UsuarioLocalRepository", "Error: ${e.message}", e)
            emptyList()
        }
        val usuarios = try {
            client.postgrest.from("usuarios").select().decodeList<Usuario>().associateBy { it.id }
        } catch (_: Exception) { emptyMap() }
        val locales = try {
            client.postgrest.from("locales").select().decodeList<Local>().associateBy { it.id }
        } catch (_: Exception) { emptyMap() }
        return asignaciones.map { a ->
            AsignacionLocal(a, usuarios[a.usuarioId]?.nombre ?: a.usuarioId.take(8), locales[a.localId]?.nombre ?: "Local #${a.localId}")
        }
    }

    suspend fun getUsuarios(): List<Usuario> = try {
        client.postgrest.from("usuarios").select().decodeList<Usuario>().sortedBy { it.nombre }
    } catch (_: Exception) { emptyList() }

    suspend fun getLocales(): List<Local> = try {
        client.postgrest.from("locales").select().decodeList<Local>().filter { it.activo }.sortedBy { it.nombre }
    } catch (_: Exception) { emptyList() }

    suspend fun asignar(usuarioId: String, localId: Int, rolLocal: String) {
        client.postgrest.from("usuarios_locales").insert(
            buildJsonObject {
                put("usuario_id", usuarioId)
                put("local_id", localId)
                put("rol_local", rolLocal)
                put("activo", true)
            }
        )
    }

    suspend fun eliminar(id: Int) {
        client.postgrest.from("usuarios_locales").delete { filter { eq("id", id) } }
    }

    /** Locales asignados a un usuario (para restringir la sesión). */
    suspend fun localesDelUsuario(usuarioId: String): List<Int> = try {
        client.postgrest.from("usuarios_locales").select {
            filter { eq("usuario_id", usuarioId); eq("activo", true) }
        }.decodeList<UsuarioLocal>().map { it.localId }
    } catch (_: Exception) { emptyList() }
}
