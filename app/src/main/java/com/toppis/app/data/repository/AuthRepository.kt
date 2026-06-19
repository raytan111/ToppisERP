package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.Rol
import com.toppis.app.data.models.Usuario
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de autenticación basado en Supabase Auth.
 *
 * - Login/logout usan Supabase GoTrue (Auth).
 * - Los datos del perfil (nombre, rol, activo) viven en la tabla "usuarios".
 */
class AuthRepository {

    private val client = SupabaseClient.client

    // ── Login ───────────────────────────────────────────────────────────────

    /**
     * Autentica con email/password contra Supabase Auth y luego obtiene el
     * perfil desde la tabla "usuarios". Retorna null si las credenciales son
     * inválidas o el usuario está inactivo.
     */
    suspend fun login(email: String, password: String): Result<Usuario> {
        return try {
            client.auth.signInWith(Email) {
                this.email = email.trim().lowercase()
                this.password = password
            }

            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Sesión iniciada pero sin usuario (currentUser null)"))

            val usuario = client.postgrest.from("usuarios")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<Usuario>()

            when {
                usuario == null -> {
                    client.auth.signOut()
                    Result.failure(Exception("Auth OK pero no existe perfil en tabla 'usuarios' para id=$userId"))
                }
                !usuario.activo -> {
                    client.auth.signOut()
                    Result.failure(Exception("Usuario inactivo"))
                }
                else -> Result.success(usuario)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en login: ${e.message}", e)
            Result.failure(Exception(mensajeErrorLogin(e)))
        }
    }

    /** Traduce errores comunes de login a mensajes claros en español. */
    private fun mensajeErrorLogin(e: Exception): String {
        val raw = (e.message ?: "").lowercase()
        return when {
            raw.contains("email_not_confirmed") || raw.contains("not confirmed") ->
                "El email no está confirmado. Pedile al administrador que lo confirme en Supabase o que desactive la confirmación de email."
            raw.contains("invalid_credentials") || raw.contains("invalid login") ||
                raw.contains("invalid_grant") || raw.contains("invalid email or password") ->
                "Email o contraseña incorrectos."
            raw.contains("network") || raw.contains("timeout") || raw.contains("connect") ->
                "Error de conexión. Revisá tu internet e intentá de nuevo."
            else -> e.message ?: "No se pudo iniciar sesión."
        }
    }

    // ── Logout ──────────────────────────────────────────────────────────────

    suspend fun logout() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {
            // Ignorar errores de logout
        }
    }

    // ── Usuario actual ────────────────────────────────────────────────────────

    /**
     * Local asignado al usuario (para scope de no-admins). Retorna (id, nombre)
     * del primer local activo asignado en usuarios_locales, o null.
     */
    suspend fun getLocalAsignado(usuarioId: String): Pair<Int, String>? {
        return try {
            val asignaciones = client.postgrest.from("usuarios_locales")
                .select { filter { eq("usuario_id", usuarioId); eq("activo", true) } }
                .decodeList<com.toppis.app.data.models.UsuarioLocal>()
            val localId = asignaciones.firstOrNull()?.localId ?: return null
            val local = client.postgrest.from("locales")
                .select { filter { eq("id", localId) } }
                .decodeSingleOrNull<com.toppis.app.data.models.Local>()
            local?.let { it.id to it.nombre }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error getLocalAsignado: ${e.message}", e)
            null
        }
    }

    /** Retorna el perfil del usuario con sesión activa, o null si no hay sesión. */
    suspend fun getCurrentUser(): Usuario? {
        // Esperar a que Supabase Auth restaure la sesión persistida desde el
        // almacenamiento antes de consultar el usuario actual. Sin esto, en el
        // arranque de la app currentUserOrNull() puede devolver null aunque haya
        // una sesión válida guardada.
        try {
            client.auth.awaitInitialization()
        } catch (_: Exception) {
            // Si falla la espera, continuamos con el chequeo normal.
        }
        val userId = client.auth.currentUserOrNull()?.id ?: return null
        return try {
            client.postgrest.from("usuarios")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<Usuario>()
        } catch (e: Exception) {
            null
        }
    }

    // ── Lista de usuarios (para UsuariosScreen) ───────────────────────────────

    suspend fun getUsuarios(): List<Usuario> {
        return try {
            client.postgrest.from("usuarios")
                .select(Columns.ALL)
                .decodeList<Usuario>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Registro de nuevos usuarios ───────────────────────────────────────────

    /**
     * Crea un nuevo usuario en Supabase Auth y su perfil en la tabla "usuarios".
     *
     * NOTA: signUpWith reemplaza temporalmente la sesión actual por la del nuevo
     * usuario. Por eso, tras crear el perfil, este método NO mantiene la sesión
     * del nuevo usuario activa para el admin. Para MVP es aceptable; en una fase
     * posterior se moverá a una Edge Function con service_role.
     *
     * @return Result.success si se creó, Result.failure con mensaje claro si no.
     */
    suspend fun registrarUsuario(
        nombre: String,
        email: String,
        password: String,
        rol: Rol
    ): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        // Guardar la sesión del admin actual para restaurarla luego
        // (signUp cambia la sesión al usuario recién creado)
        val sesionAdmin = client.auth.currentSessionOrNull()
        return try {
            val result = client.auth.signUpWith(Email) {
                this.email = normalizedEmail
                this.password = password
            }
            val newUserId = result?.id
                ?: return Result.failure(Exception("No se pudo crear el usuario (sin id)."))

            client.postgrest.from("usuarios").insert(
                Usuario(
                    id = newUserId,
                    nombre = nombre.trim(),
                    email = normalizedEmail,
                    rol = rol,
                    activo = true
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en registrarUsuario: ${e.message}", e)
            Result.failure(Exception(mensajeErrorRegistro(e)))
        } finally {
            // Restaurar la sesión del admin (signUp la había reemplazado)
            if (sesionAdmin != null) {
                try {
                    client.auth.importSession(sesionAdmin)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "No se pudo restaurar sesión admin: ${e.message}", e)
                }
            }
        }
    }

    /** Traduce errores comunes de registro a mensajes claros en español. */
    private fun mensajeErrorRegistro(e: Exception): String {
        val raw = (e.message ?: "").lowercase()
        return when {
            raw.contains("weak_password") || raw.contains("at least 6") ->
                "La contraseña es muy débil: debe tener al menos 6 caracteres."
            raw.contains("already") || raw.contains("registered") || raw.contains("exists") ||
                raw.contains("duplicate") ->
                "El email ya está registrado."
            raw.contains("invalid") && raw.contains("email") ->
                "El email no es válido."
            raw.contains("network") || raw.contains("timeout") || raw.contains("connect") ->
                "Error de conexión. Revisá tu internet e intentá de nuevo."
            else -> e.message ?: "No se pudo crear el usuario."
        }
    }

    // ── Editar usuario ──────────────────────────────────────────────────────

    /**
     * Actualiza el perfil de un usuario (nombre, rol, activo) en la tabla
     * "usuarios". El email no se edita (está ligado a la cuenta de Auth). La RLS
     * sólo lo permite a ADMIN.
     */
    suspend fun actualizarUsuario(
        usuarioId: String,
        nombre: String,
        rol: Rol,
        activo: Boolean
    ): Result<Unit> {
        return try {
            client.postgrest.from("usuarios").update(
                buildJsonObject {
                    put("nombre", nombre.trim())
                    put("rol", rol.name)
                    put("activo", activo)
                }
            ) {
                filter { eq("id", usuarioId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en actualizarUsuario: ${e.message}", e)
            val raw = (e.message ?: "").lowercase()
            val msg = when {
                raw.contains("permission") || raw.contains("policy") || raw.contains("row-level") ->
                    "No tenés permiso para editar usuarios."
                else -> e.message ?: "No se pudo actualizar el usuario."
            }
            Result.failure(Exception(msg))
        }
    }

    // ── Eliminar usuario ──────────────────────────────────────────────────────

    /**
     * Elimina el perfil de un usuario de la tabla "usuarios". La RLS sólo lo
     * permite a ADMIN. Nota: no borra la cuenta de auth.users (eso requiere
     * service_role); al quitar el perfil, el usuario ya no puede operar ni
     * aparece en la lista, y el login le falla por falta de perfil.
     */
    suspend fun eliminarUsuario(usuarioId: String): Result<Unit> {
        return try {
            client.postgrest.from("usuarios").delete { filter { eq("id", usuarioId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en eliminarUsuario: ${e.message}", e)
            val raw = (e.message ?: "").lowercase()
            val msg = when {
                raw.contains("permission") || raw.contains("policy") || raw.contains("row-level") ->
                    "No tenés permiso para eliminar usuarios."
                raw.contains("foreign key") || raw.contains("violates") ->
                    "No se puede eliminar: el usuario tiene registros asociados."
                else -> e.message ?: "No se pudo eliminar el usuario."
            }
            Result.failure(Exception(msg))
        }
    }
}
