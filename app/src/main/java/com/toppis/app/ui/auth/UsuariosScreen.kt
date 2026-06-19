package com.toppis.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.Rol
import com.toppis.app.data.models.Usuario
import com.toppis.app.ui.components.ToppisTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val usuarios by viewModel.usuarios.collectAsState()
    val usuarioActual by viewModel.usuarioActual.collectAsState()
    val registroState by viewModel.registroState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCrearDialog by remember { mutableStateOf(false) }
    var errorRegistro by remember { mutableStateOf<String?>(null) }
    var aEliminar by remember { mutableStateOf<Usuario?>(null) }

    val puedeBorrar = Permisos.de(usuarioActual?.rol).puedeBorrar

    // Cargar usuarios al abrir la pantalla
    LaunchedEffect(Unit) {
        viewModel.cargarUsuarios()
    }

    // Reaccionar al resultado de crear usuario
    LaunchedEffect(registroState) {
        when (val st = registroState) {
            is RegistroState.Success -> {
                showCrearDialog = false
                errorRegistro = null
                snackbarHostState.showSnackbar("Usuario creado")
                viewModel.resetRegistroState()
            }
            is RegistroState.Error -> {
                errorRegistro = st.message
                viewModel.resetRegistroState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ToppisTopBar(
                titulo = "👥 Usuarios",
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrearDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear usuario")
            }
        }
    ) { padding ->
        if (usuarios.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay usuarios. Usá el botón + para crear uno.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(usuarios) { usuario ->
                    UsuarioCard(
                        usuario = usuario,
                        puedeBorrar = puedeBorrar && usuario.id != usuarioActual?.id,
                        onEliminar = { aEliminar = usuario }
                    )
                }
            }
        }
    }

    if (showCrearDialog) {
        CrearUsuarioDialog(
            cargando = registroState is RegistroState.Loading,
            onDismiss = { showCrearDialog = false },
            onConfirm = { nombre, email, password, rol ->
                viewModel.registrarUsuario(nombre, email, password, rol)
            }
        )
    }

    // Popup de error de creación de usuario
    errorRegistro?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorRegistro = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("No se pudo crear el usuario") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorRegistro = null }) { Text("Entendido") }
            }
        )
    }

    // Confirmación de eliminación de usuario
    aEliminar?.let { u ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar usuario") },
            text = { Text("¿Seguro que querés eliminar a \"${u.nombre}\"? Perderá el acceso a la app. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarUsuario(u.id)
                    aEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { aEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

// ── Card de usuario ────────────────────────────────────────────────────────────

@Composable
private fun UsuarioCard(
    usuario: Usuario,
    puedeBorrar: Boolean = false,
    onEliminar: () -> Unit = {}
) {
    val rolColor = if (usuario.rol == Rol.ADMIN)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    val rolTextColor = if (usuario.rol == Rol.ADMIN)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = if (usuario.activo)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.nombre,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = usuario.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!usuario.activo) {
                    Text(
                        text = "Inactivo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Surface(
                color = rolColor,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = usuario.rol.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = rolTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (puedeBorrar) {
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ── Dialog crear usuario ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearUsuarioDialog(
    cargando: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, email: String, password: String, rol: Rol) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRol by remember { mutableStateOf(Rol.CAJERO) }
    var rolExpanded by remember { mutableStateOf(false) }

    val emailValido = email.contains("@") && email.contains(".")
    val passwordValido = password.length >= 6
    val formValido = nombre.isNotBlank() && emailValido && passwordValido && !cargando
    val passwordError = password.isNotEmpty() && !passwordValido

    AlertDialog(
        onDismissRequest = { if (!cargando) onDismiss() },
        title = { Text("Nuevo usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    isError = email.isNotEmpty() && !emailValido,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    isError = passwordError,
                    supportingText = {
                        Text(
                            if (passwordError)
                                "Faltan caracteres: mínimo 6 (tenés ${password.length})."
                            else
                                "Mínimo 6 caracteres."
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = rolExpanded,
                    onExpandedChange = { rolExpanded = !rolExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRol.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rolExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = rolExpanded,
                        onDismissRequest = { rolExpanded = false }
                    ) {
                        Rol.entries.forEach { rol ->
                            DropdownMenuItem(
                                text = { Text(rol.name) },
                                onClick = {
                                    selectedRol = rol
                                    rolExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre, email, password, selectedRol) },
                enabled = formValido
            ) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !cargando) { Text("Cancelar") }
        }
    )
}

