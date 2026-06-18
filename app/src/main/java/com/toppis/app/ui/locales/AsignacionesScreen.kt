package com.toppis.app.ui.locales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.Local
import com.toppis.app.data.models.Usuario
import com.toppis.app.data.repository.AsignacionLocal
import com.toppis.app.data.repository.UsuarioLocalRepository
import com.toppis.app.ui.components.ToppisTopBar
import kotlinx.coroutines.launch

private val ROLES = listOf("ADMIN_LOCAL", "ENCARGADO", "FRANQUICIADO")
private val ROLES_LABEL = mapOf("ADMIN_LOCAL" to "Admin local", "ENCARGADO" to "Encargado", "FRANQUICIADO" to "Franquiciado")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignacionesScreen(
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repo = remember { UsuarioLocalRepository() }
    var asignaciones by remember { mutableStateOf<List<AsignacionLocal>>(emptyList()) }
    var usuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var locales by remember { mutableStateOf<List<Local>>(emptyList()) }
    var showCrear by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun refrescar() { scope.launch { asignaciones = repo.getAsignaciones() } }
    LaunchedEffect(Unit) { asignaciones = repo.getAsignaciones(); usuarios = repo.getUsuarios(); locales = repo.getLocales() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Asignar Usuarios a Locales", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCrear = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Asignar")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (asignaciones.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin asignaciones. Usá + para asignar un usuario a un local.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                    items(asignaciones) { a ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(a.nombreUsuario, style = MaterialTheme.typography.titleSmall)
                                    Text("${a.nombreLocal} · ${ROLES_LABEL[a.asignacion.rolLocal] ?: a.asignacion.rolLocal}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { scope.launch { repo.eliminar(a.asignacion.id); refrescar() } }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCrear) {
        AsignarDialog(
            usuarios = usuarios,
            locales = locales,
            onDismiss = { showCrear = false },
            onConfirm = { uid, lid, rol ->
                scope.launch {
                    try { repo.asignar(uid, lid, rol); refrescar(); showCrear = false }
                    catch (e: Exception) { snackbarHostState.showSnackbar(e.message ?: "Error") }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AsignarDialog(
    usuarios: List<Usuario>,
    locales: List<Local>,
    onDismiss: () -> Unit,
    onConfirm: (usuarioId: String, localId: Int, rol: String) -> Unit
) {
    var usuario by remember { mutableStateOf(usuarios.firstOrNull()) }
    var local by remember { mutableStateOf(locales.firstOrNull()) }
    var rol by remember { mutableStateOf("ENCARGADO") }
    var expU by remember { mutableStateOf(false) }
    var expL by remember { mutableStateOf(false) }
    var expR by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar usuario a local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expU, onExpandedChange = { expU = !expU }) {
                    OutlinedTextField(value = usuario?.nombre ?: "", onValueChange = {}, readOnly = true, label = { Text("Usuario") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expU) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = expU, onDismissRequest = { expU = false }) {
                        usuarios.forEach { u -> DropdownMenuItem(text = { Text(u.nombre) }, onClick = { usuario = u; expU = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = expL, onExpandedChange = { expL = !expL }) {
                    OutlinedTextField(value = local?.nombre ?: "", onValueChange = {}, readOnly = true, label = { Text("Local") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expL) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = expL, onDismissRequest = { expL = false }) {
                        locales.forEach { l -> DropdownMenuItem(text = { Text(l.nombre) }, onClick = { local = l; expL = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = expR, onExpandedChange = { expR = !expR }) {
                    OutlinedTextField(value = ROLES_LABEL[rol] ?: rol, onValueChange = {}, readOnly = true, label = { Text("Rol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expR) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = expR, onDismissRequest = { expR = false }) {
                        ROLES.forEach { r -> DropdownMenuItem(text = { Text(ROLES_LABEL[r] ?: r) }, onClick = { rol = r; expR = false }) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(usuario!!.id, local!!.id, rol) }, enabled = usuario != null && local != null) { Text("Asignar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
