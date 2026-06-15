package com.toppis.app.ui.gastos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.models.Gasto
import com.toppis.app.data.models.Sobre
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosScreen(
    viewModel: GastoViewModel,
    usuarioId: String? = null,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val gastos by viewModel.gastos.collectAsState()
    val totalGastos by viewModel.totalGastos.collectAsState()
    val sobres by viewModel.sobres.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }

    val sobresConSaldo = sobres.filter { it.saldo > 0 }

    LaunchedEffect(uiState) {
        when (uiState) {
            is GastoUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as GastoUiState.Error).message)
            }
            is GastoUiState.Success -> { }
            GastoUiState.Loading -> { }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card de total
            item {
                TotalGastosCard(total = totalGastos)
            }

            // Advertencia si no hay sobres con saldo
            if (sobresConSaldo.isEmpty() && sobres.isNotEmpty()) {
                item {
                    SinSaldoWarningCard()
                }
            }

            // Lista de gastos
            if (gastos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay gastos registrados",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(gastos) { gasto ->
                    GastoCard(gasto = gasto, sobres = sobres)
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Registrar Gasto")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAddDialog) {
        AddGastoDialog(
            sobresConSaldo = sobresConSaldo,
            onDismiss = { showAddDialog = false },
            onConfirm = { descripcion, monto, categoria, sobreId, comprobante, tieneIva ->
                viewModel.registrarGasto(
                    descripcion = descripcion,
                    monto = monto,
                    categoria = categoria,
                    sobreId = sobreId,
                    usuarioId = usuarioId,
                    comprobante = comprobante.ifBlank { null },
                    tieneIva = tieneIva
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TotalGastosCard(total: Double) {
    val formatter = DecimalFormat("$#,##0 CLP")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Total gastos del período",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = formatter.format(total),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun SinSaldoWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Advertencia",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "No hay sobres con saldo disponible para registrar gastos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun GastoCard(gasto: Gasto, sobres: List<Sobre>) {
    val moneyFormatter = DecimalFormat("$#,##0 CLP")
    val nombreSobre = sobres.find { it.id == gasto.sobreId }?.nombre ?: "Sobre eliminado"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = gasto.descripcion,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = moneyFormatter.format(gasto.monto),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(gasto.categoria.label) }
                )
                Text(
                    text = "📁 $nombreSobre",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatFechaIso(gasto.fecha),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!gasto.comprobante.isNullOrBlank()) {
                Text(
                    text = "Comprobante: ${gasto.comprobante}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGastoDialog(
    sobresConSaldo: List<Sobre>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, CategoriaGasto, Int, String, Boolean) -> Unit
) {
    if (sobresConSaldo.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Sin saldo disponible") },
            text = { Text("No hay sobres con saldo para registrar un gasto. Agregá fondos a un sobre primero.") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Entendido") }
            }
        )
        return
    }

    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var comprobante by remember { mutableStateOf("") }
    var tieneIva by remember { mutableStateOf(false) }
    var selectedCategoria by remember { mutableStateOf(CategoriaGasto.OTROS) }
    var selectedSobre by remember { mutableStateOf(sobresConSaldo.first()) }
    var categoriaExpanded by remember { mutableStateOf(false) }
    var sobreExpanded by remember { mutableStateOf(false) }

    val moneyFormatter = DecimalFormat("$#,##0 CLP")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Gasto") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                // Dropdown Categoría
                ExposedDropdownMenuBox(
                    expanded = categoriaExpanded,
                    onExpandedChange = { categoriaExpanded = !categoriaExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategoria.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoriaExpanded,
                        onDismissRequest = { categoriaExpanded = false }
                    ) {
                        CategoriaGasto.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    selectedCategoria = cat
                                    categoriaExpanded = false
                                }
                            )
                        }
                    }
                }

                // Dropdown Sobre origen
                ExposedDropdownMenuBox(
                    expanded = sobreExpanded,
                    onExpandedChange = { sobreExpanded = !sobreExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${selectedSobre.nombre} (${moneyFormatter.format(selectedSobre.saldo)})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sobre origen") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sobreExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sobreExpanded,
                        onDismissRequest = { sobreExpanded = false }
                    ) {
                        sobresConSaldo.forEach { sobre ->
                            DropdownMenuItem(
                                text = { Text("${sobre.nombre} (${moneyFormatter.format(sobre.saldo)})") },
                                onClick = {
                                    selectedSobre = sobre
                                    sobreExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comprobante,
                    onValueChange = { comprobante = it },
                    label = { Text("Comprobante (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Check IVA (factura): habilita IVA crédito en contabilidad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = tieneIva,
                        onCheckedChange = { tieneIva = it }
                    )
                    Column {
                        Text("Con factura (tiene IVA)", style = MaterialTheme.typography.bodyMedium)
                        val m = monto.toDoubleOrNull()
                        if (tieneIva && m != null && m > 0) {
                            val neto = Math.round(m / 1.19)
                            val iva = m - neto
                            Text(
                                "Neto ${DecimalFormat("$#,##0").format(neto)} · IVA ${DecimalFormat("$#,##0").format(iva)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val montoDouble = monto.toDoubleOrNull()
            val montoValido = montoDouble != null && montoDouble > 0 && montoDouble <= selectedSobre.saldo
            TextButton(
                onClick = {
                    if (montoDouble != null) {
                        onConfirm(descripcion, montoDouble, selectedCategoria, selectedSobre.id, comprobante, tieneIva)
                    }
                },
                enabled = descripcion.isNotBlank() && montoValido
            ) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}







/**
 * Formatea un timestamp ISO (de Supabase) a "dd/MM/yyyy HH:mm" en hora local.
 */
private fun formatFechaIso(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        OffsetDateTime.parse(iso)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    } catch (e: Exception) {
        iso.take(16).replace("T", " ")
    }
}
