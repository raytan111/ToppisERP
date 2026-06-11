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

    // ── Acciones ──────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { usuario ->
                    _usuarioActual.value = usuario
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
            val ok = repository.registrarUsuario(nombre, email, password, rol)
            if (!ok) {
                _authState.value = AuthState.Error("El email ya está registrado o hubo un error")
            } else {
                cargarUsuarios()
            }
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
