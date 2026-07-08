package com.toppis.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
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
    var enEdicion by remember { mutableStateOf<Usuario?>(null) }
    var resetPasswordDe by remember { mutableStateOf<Usuario?>(null) }

    val permisos = Permisos.de(usuarioActual?.rol)
    val puedeBorrar = permisos.puedeBorrar
    val puedeEditar = permisos.puedeEditar
    val rolesAsignables = permisos.rolesAsignables

    // Cargar usuarios al abrir la pantalla
    LaunchedEffect(Unit) {
        viewModel.cargarUsuarios()
    }

    // Reaccionar al resultado de crear usuario
    LaunchedEffect(registroState) {
        when (val st = registroState) {
            is RegistroState.Success -> {
                showCrearDialog = false
                enEdicion = null
                resetPasswordDe = null
                errorRegistro = null
                snackbarHostState.showSnackbar("Guardado")
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
                        puedeEditar = puedeEditar,
                        puedeBorrar = puedeBorrar && usuario.id != usuarioActual?.id,
                        onEditar = { enEdicion = usuario },
                        onResetPassword = { resetPasswordDe = usuario },
                        onEliminar = { aEliminar = usuario }
                    )
                }
            }
        }
    }

    if (showCrearDialog) {
        CrearUsuarioDialog(
            cargando = registroState is RegistroState.Loading,
            rolesAsignables = rolesAsignables,
            onDismiss = { showCrearDialog = false },
            onConfirm = { nombre, email, password, rol ->
                viewModel.registrarUsuario(nombre, email, password, rol)
            }
        )
    }

    enEdicion?.let { u ->
        EditarUsuarioDialog(
            usuario = u,
            cargando = registroState is RegistroState.Loading,
            rolesAsignables = rolesAsignables,
            onDismiss = { enEdicion = null },
            onConfirm = { nombre, rol, activo ->
                viewModel.actualizarUsuario(u.id, nombre, rol, activo)
            }
        )
    }

    resetPasswordDe?.let { u ->
        ResetPasswordDialog(
            usuario = u,
            cargando = registroState is RegistroState.Loading,
            onDismiss = { resetPasswordDe = null },
            onConfirm = { nuevaPassword ->
                viewModel.resetPassword(u.id, nuevaPassword)
            }
        )
    }

    // Popup de error de creación de usuario
    errorRegistro?.let { msg ->
        com.toppis.app.ui.components.ToppisErrorDialog(
            mensaje = msg,
            titulo = "Operación no completada",
            onDismiss = { errorRegistro = null }
        )
    }

    // Confirmación de eliminación de usuario
    aEliminar?.let { u ->
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = u.nombre,
            titulo = "Eliminar usuario",
            mensaje = "¿Seguro que querés eliminar a \"${u.nombre}\"? Perderá el acceso a la app. Esta acción no se puede deshacer.",
            onConfirm = { viewModel.eliminarUsuario(u.id); aEliminar = null },
            onDismiss = { aEliminar = null }
        )
    }
}

// ── Card de usuario ────────────────────────────────────────────────────────────

@Composable
private fun UsuarioCard(
    usuario: Usuario,
    puedeEditar: Boolean = false,
    puedeBorrar: Boolean = false,
    onEditar: () -> Unit = {},
    onResetPassword: () -> Unit = {},
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
                    text = com.toppis.app.data.repository.AuthRepository.nombreUsuario(usuario.email),
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
            if (puedeEditar) {
                IconButton(onClick = onEditar) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onResetPassword) {
                    Icon(Icons.Filled.Key, contentDescription = "Cambiar contraseña")
                }
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
    rolesAsignables: List<Rol> = Rol.entries,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, email: String, password: String, rol: Rol) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRol by remember { mutableStateOf(rolesAsignables.firstOrNull() ?: Rol.CAJERO) }
    var rolExpanded by remember { mutableStateOf(false) }

    val usuarioValido = email.isNotBlank() && !email.contains(" ")
    val passwordValido = password.length >= 6
    val formValido = nombre.isNotBlank() && usuarioValido && passwordValido && !cargando
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
                    onValueChange = { email = it.trim() },
                    label = { Text("Usuario") },
                    isError = email.isNotEmpty() && !usuarioValido,
                    supportingText = { Text("Sin espacios. Ej: juan, cajero1") },
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
                        rolesAsignables.forEach { rol ->
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


// ── Dialog editar usuario ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarUsuarioDialog(
    usuario: Usuario,
    cargando: Boolean = false,
    rolesAsignables: List<Rol> = Rol.entries,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, rol: Rol, activo: Boolean) -> Unit
) {
    var nombre by remember { mutableStateOf(usuario.nombre) }
    var selectedRol by remember { mutableStateOf(usuario.rol) }
    var activo by remember { mutableStateOf(usuario.activo) }
    var rolExpanded by remember { mutableStateOf(false) }

    val formValido = nombre.isNotBlank() && !cargando

    AlertDialog(
        onDismissRequest = { if (!cargando) onDismiss() },
        title = { Text("Editar usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = com.toppis.app.data.repository.AuthRepository.nombreUsuario(usuario.email),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Usuario (no editable)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
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
                        rolesAsignables.forEach { rol ->
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Activo", modifier = Modifier.weight(1f))
                    Switch(checked = activo, onCheckedChange = { activo = it })
                }
                if (!activo) {
                    Text(
                        "Inactivo: el usuario no podrá iniciar sesión.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nombre, selectedRol, activo) },
                enabled = formValido
            ) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !cargando) { Text("Cancelar") }
        }
    )
}

// ── Dialog reset de contraseña ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetPasswordDialog(
    usuario: Usuario,
    cargando: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (nuevaPassword: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    val passwordValido = password.length >= 6
    val passwordError = password.isNotEmpty() && !passwordValido

    AlertDialog(
        onDismissRequest = { if (!cargando) onDismiss() },
        title = { Text("Cambiar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Nueva contraseña para ${usuario.nombre} (${com.toppis.app.data.repository.AuthRepository.nombreUsuario(usuario.email)}).",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nueva contraseña") },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = passwordValido && !cargando
            ) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Cambiar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !cargando) { Text("Cancelar") }
        }
    )
}
