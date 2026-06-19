package com.toppis.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.Rol
import com.toppis.app.data.models.Usuario
import com.toppis.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Estado de autenticación ───────────────────────────────────────────────────

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val usuario: Usuario) : AuthState()
    data class Error(val message: String) : AuthState()
}

/** Estado del flujo de creación de usuario (separado del login). */
sealed class RegistroState {
    object Idle : RegistroState()
    object Loading : RegistroState()
    object Success : RegistroState()
    data class Error(val message: String) : RegistroState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    /** Usuario actualmente logueado; null = no autenticado. */
    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    /** Estado del último intento de login o registro. */
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Lista reactiva de todos los usuarios (para UsuariosScreen). */
    private val _usuarios = MutableStateFlow<List<Usuario>>(emptyList())
    val usuarios: StateFlow<List<Usuario>> = _usuarios.asStateFlow()

    /** Estado del flujo de creación de usuario. */
    private val _registroState = MutableStateFlow<RegistroState>(RegistroState.Idle)
    val registroState: StateFlow<RegistroState> = _registroState.asStateFlow()

    /** Se intenta restaurar la sesión al iniciar (auto-login). */
    init {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val usuario = repository.getCurrentUser()
            if (usuario != null && usuario.activo) {
                _usuarioActual.value = usuario
                aplicarScopeLocal(usuario)
                _authState.value = AuthState.Success(usuario)
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    /**
     * Fija el local activo según el local asignado al usuario, para roles que
     * están restringidos a su local (todos menos ADMIN). ADMIN puede cambiar de
     * local libremente desde la pantalla de Locales.
     */
    private fun aplicarScopeLocal(usuario: Usuario) {
        if (usuario.rol == Rol.ADMIN) return
        viewModelScope.launch {
            val asignado = repository.getLocalAsignado(usuario.id)
            if (asignado != null) {
                com.toppis.app.data.repository.LocalSession.setActivo(asignado.first, asignado.second)
            }
        }
    }

    // ── Acciones ──────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { usuario ->
                    _usuarioActual.value = usuario
                    aplicarScopeLocal(usuario)
                    _authState.value = AuthState.Success(usuario)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Error desconocido al iniciar sesión")
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _usuarioActual.value = null
            _authState.value = AuthState.Idle
        }
    }

    fun registrarUsuario(nombre: String, email: String, password: String, rol: Rol) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            repository.registrarUsuario(nombre, email, password, rol).fold(
                onSuccess = {
                    cargarUsuarios()
                    _registroState.value = RegistroState.Success
                },
                onFailure = { error ->
                    _registroState.value = RegistroState.Error(error.message ?: "No se pudo crear el usuario")
                }
            )
        }
    }

    fun resetRegistroState() {
        _registroState.value = RegistroState.Idle
    }

    /** Elimina un usuario por completo (cuenta Auth + perfil) vía Edge Function. Solo ADMIN. */
    fun eliminarUsuario(usuarioId: String) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            repository.eliminarUsuarioCompleto(usuarioId).fold(
                onSuccess = {
                    cargarUsuarios()
                    _registroState.value = RegistroState.Success
                },
                onFailure = { error ->
                    _registroState.value = RegistroState.Error(error.message ?: "No se pudo eliminar el usuario")
                }
            )
        }
    }

    /** Cambia la contraseña de un usuario vía Edge Function. Solo ADMIN. */
    fun resetPassword(usuarioId: String, nuevaPassword: String) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            repository.resetPassword(usuarioId, nuevaPassword).fold(
                onSuccess = { _registroState.value = RegistroState.Success },
                onFailure = { error ->
                    _registroState.value = RegistroState.Error(error.message ?: "No se pudo cambiar la contraseña")
                }
            )
        }
    }

    /** Edita nombre/rol/activo de un usuario (solo ADMIN según RLS). */
    fun actualizarUsuario(usuarioId: String, nombre: String, rol: Rol, activo: Boolean) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            repository.actualizarUsuario(usuarioId, nombre, rol, activo).fold(
                onSuccess = {
                    cargarUsuarios()
                    _registroState.value = RegistroState.Success
                },
                onFailure = { error ->
                    _registroState.value = RegistroState.Error(error.message ?: "No se pudo actualizar el usuario")
                }
            )
        }
    }

    /** Carga la lista de usuarios desde Supabase. */
    fun cargarUsuarios() {
        viewModelScope.launch {
            _usuarios.value = repository.getUsuarios()
        }
    }

    /** Limpia el estado de error/success sin afectar al usuario logueado. */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
