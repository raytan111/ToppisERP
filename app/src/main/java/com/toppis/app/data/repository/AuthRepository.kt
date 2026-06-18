package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.Rol
import com.toppis.app.data.models.Usuario
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

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
            Result.failure(e)
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
     * @return true si se creó correctamente, false si el email ya existe o falla.
     */
    suspend fun registrarUsuario(
        nombre: String,
        email: String,
        password: String,
        rol: Rol
    ): Boolean {
        val normalizedEmail = email.trim().lowercase()
        // Guardar la sesión del admin actual para restaurarla luego
        // (signUp cambia la sesión al usuario recién creado)
        val sesionAdmin = client.auth.currentSessionOrNull()
        return try {
            val result = client.auth.signUpWith(Email) {
                this.email = normalizedEmail
                this.password = password
            }
            val newUserId = result?.id ?: return false

            client.postgrest.from("usuarios").insert(
                Usuario(
                    id = newUserId,
                    nombre = nombre.trim(),
                    email = normalizedEmail,
                    rol = rol,
                    activo = true
                )
            )
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en registrarUsuario: ${e.message}", e)
            false
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
}
