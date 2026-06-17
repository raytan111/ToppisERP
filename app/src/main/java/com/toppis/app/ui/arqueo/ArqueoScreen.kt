package com.toppis.app.ui.arqueo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.Sobre
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArqueoScreen(
    viewModel: ArqueoViewModel,
    usuarioId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val cuentas by viewModel.cuentas.collectAsState()
    val arqueos by viewModel.arqueos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val money = DecimalFormat("$#,##0")

    val snackbarHostState = remember { SnackbarHostState() }
    var showNuevo by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ArqueoUiState.Error -> {
                errorMsg = (uiState as ArqueoUiState.Error).message
                viewModel.resetState()
            }
            ArqueoUiState.Success -> { snackbarHostState.showSnackbar("Arqueo registrado"); viewModel.resetState() }
            else -> {}
        }
    }

    errorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("No se pudo registrar el arqueo") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("Entendido") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Arqueo de Caja", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.refrescar(); showNuevo = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo arqueo")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (arqueos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin arqueos. Usá + para hacer uno.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(arqueos) { a ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(a.nombreSobre, style = MaterialTheme.typography.titleMedium)
                                Text(a.arqueo.fecha?.take(16)?.replace("T", " ") ?: "",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("Sistema ${money.format(a.arqueo.saldoSistema)} · Contado ${money.format(a.arqueo.saldoContado)}",
                                    style = MaterialTheme.typography.bodySmall)
                                val difColor = when {
                                    a.arqueo.diferencia == 0.0 -> MaterialTheme.colorScheme.primary
                                    a.arqueo.diferencia < 0 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.tertiary
                                }
                                Text("Diferencia: ${if (a.arqueo.diferencia > 0) "+" else ""}${money.format(a.arqueo.diferencia)}" +
                                    if (a.arqueo.ajustado) " (ajustado)" else "",
                                    style = MaterialTheme.typography.labelMedium, color = difColor)
                                if (a.arqueo.nota.isNotBlank()) Text(a.arqueo.nota, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNuevo) {
        NuevoArqueoDialog(
            cuentas = cuentas,
            onDismiss = { showNuevo = false },
            onConfirm = { sobreId, contado, nota, ajustar ->
                viewModel.registrar(sobreId, contado, nota, ajustar, usuarioId)
                showNuevo = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoArqueoDialog(
    cuentas: List<Sobre>,
    onDismiss: () -> Unit,
    onConfirm: (sobreId: Int, contado: Double, nota: String, ajustar: Boolean) -> Unit
) {
    var sobre by remember { mutableStateOf(cuentas.firstOrNull()) }
    var exp by remember { mutableStateOf(false) }
    var contadoText by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var ajustar by remember { mutableStateOf(true) }
    val money = DecimalFormat("$#,##0")

    LaunchedEffect(cuentas) { if (sobre == null) sobre = cuentas.firstOrNull() }

    val contado = contadoText.replace(",", ".").toDoubleOrNull()
    val dif = if (contado != null && sobre != null) contado - sobre!!.saldo else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo arqueo") },
        text = {
            if (cuentas.isEmpty()) {
                Text("No hay cuentas (sobres tipo Cuenta) para arquear.", color = MaterialTheme.colorScheme.outline)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }) {
                        OutlinedTextField(
                            value = sobre?.nombre ?: "", onValueChange = {}, readOnly = true,
                            label = { Text("Cuenta") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                            cuentas.forEach { s ->
                                DropdownMenuItem(text = { Text("${s.nombre} (${money.format(s.saldo)})") }, onClick = { sobre = s; exp = false })
                            }
                        }
                    }
                    sobre?.let {
                        Text("Saldo en sistema: ${money.format(it.saldo)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    OutlinedTextField(
                        value = contadoText, onValueChange = { contadoText = it },
                        label = { Text("Efectivo/saldo contado (CLP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    if (dif != null) {
                        val difColor = when {
                            dif == 0.0 -> MaterialTheme.colorScheme.primary
                            dif < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                        Text("Diferencia: ${if (dif > 0) "+" else ""}${money.format(dif)}",
                            style = MaterialTheme.typography.titleSmall, color = difColor)
                    }
                    OutlinedTextField(
                        value = nota, onValueChange = { nota = it },
                        label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ajustar, onCheckedChange = { ajustar = it })
                        Text("Ajustar el saldo del sistema al contado")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(sobre!!.id, contado ?: 0.0, nota, ajustar) },
                enabled = sobre != null && contado != null && contado >= 0
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
