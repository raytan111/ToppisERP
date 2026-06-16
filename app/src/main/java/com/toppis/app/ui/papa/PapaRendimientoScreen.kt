package com.toppis.app.ui.papa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.PapaRendimiento
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private fun parseDouble(text: String): Double? = text.replace(",", ".").toDoubleOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapaRendimientoScreen(
    viewModel: PapaRendimientoViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val articulos by viewModel.articulos.collectAsState()
    val registros by viewModel.registros.collectAsState()

    var selectedArticulo by remember { mutableStateOf<Articulo?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var pesoCrudoText by remember { mutableStateOf("") }
    var pesoPeladoText by remember { mutableStateOf("") }
    var pesoPrefritoText by remember { mutableStateOf("") }
    var pesoFritoText by remember { mutableStateOf("") }

    val pctFormat = DecimalFormat("0.#")
    val pesoFormat = DecimalFormat("#,##0.##")

    val crudo = parseDouble(pesoCrudoText)
    val frito = parseDouble(pesoFritoText)
    val rendimientoVivo: Double? =
        if (crudo != null && frito != null && crudo > 0 && frito > 0) frito / crudo else null

    Scaffold(
        topBar = { ToppisTopBar(titulo = "Rendimiento de Papa", onBack = onNavigateBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // ── Selección de artículo ───────────────────────────────────────────
            item {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedArticulo?.nombre ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Artículo (papa)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        if (articulos.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay artículos disponibles") },
                                onClick = { expandedDropdown = false },
                                enabled = false
                            )
                        } else {
                            articulos.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text("${a.nombre} (${a.unidadBase})") },
                                    onClick = {
                                        selectedArticulo = a
                                        expandedDropdown = false
                                        viewModel.seleccionarArticulo(a.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── Formulario de pesos ─────────────────────────────────────────────
            if (selectedArticulo != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Registrar pesos por etapa", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = pesoCrudoText,
                                onValueChange = { pesoCrudoText = it },
                                label = { Text("Peso crudo") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pesoPeladoText,
                                onValueChange = { pesoPeladoText = it },
                                label = { Text("Peso pelado") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pesoPrefritoText,
                                onValueChange = { pesoPrefritoText = it },
                                label = { Text("Peso prefrito") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pesoFritoText,
                                onValueChange = { pesoFritoText = it },
                                label = { Text("Peso frito") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (rendimientoVivo != null) {
                                Text(
                                    "Rendimiento estimado: ${pctFormat.format(rendimientoVivo * 100.0)}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            val pesoPelado = parseDouble(pesoPeladoText)
                            val pesoPrefrito = parseDouble(pesoPrefritoText)
                            val formularioValido = crudo != null && crudo > 0 &&
                                pesoPelado != null && pesoPrefrito != null &&
                                frito != null && frito > 0

                            Button(
                                onClick = {
                                    val art = selectedArticulo ?: return@Button
                                    viewModel.registrar(
                                        art.id,
                                        crudo ?: 0.0,
                                        pesoPelado ?: 0.0,
                                        pesoPrefrito ?: 0.0,
                                        frito ?: 0.0
                                    )
                                    pesoCrudoText = ""
                                    pesoPeladoText = ""
                                    pesoPrefritoText = ""
                                    pesoFritoText = ""
                                },
                                enabled = formularioValido,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Registrar pesos") }
                        }
                    }
                }

                // ── Aplicar rendimiento al artículo ─────────────────────────────
                item {
                    val promedio = if (registros.isNotEmpty()) {
                        registros.map { it.rendimiento }.filter { it > 0 }.average()
                            .let { if (it.isNaN()) 0.0 else it }
                    } else 0.0
                    val ultimo = registros.firstOrNull()?.rendimiento ?: 0.0
                    val rendimientoAAplicar = if (ultimo > 0) ultimo else promedio

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Aplicar rendimiento al artículo ajusta su costo base " +
                                    "(costo = costo de compra / rendimiento), afectando el food cost.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (rendimientoAAplicar > 0) {
                                Text(
                                    "Rendimiento a aplicar (último registro): ${pctFormat.format(rendimientoAAplicar * 100.0)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Button(
                                onClick = {
                                    val art = selectedArticulo ?: return@Button
                                    viewModel.aplicarRendimientoAlArticulo(art.id, rendimientoAAplicar)
                                },
                                enabled = rendimientoAAplicar > 0,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Aplicar rendimiento al artículo") }
                        }
                    }
                }

                // ── Lista de registros ──────────────────────────────────────────
                item {
                    Text(
                        "Registros",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (registros.isEmpty()) {
                    item {
                        Text(
                            "Sin registros para este artículo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    items(registros) { reg ->
                        RegistroCard(
                            registro = reg,
                            pesoFormat = pesoFormat,
                            pctFormat = pctFormat,
                            onEliminar = {
                                val art = selectedArticulo ?: return@RegistroCard
                                viewModel.eliminar(reg.id, art.id)
                            }
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "Elegí un artículo para registrar y ver sus pesos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun RegistroCard(
    registro: PapaRendimiento,
    pesoFormat: DecimalFormat,
    pctFormat: DecimalFormat,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    registro.fecha ?: "Sin fecha",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Crudo ${pesoFormat.format(registro.pesoCrudo)} · " +
                        "Pelado ${pesoFormat.format(registro.pesoPelado)} · " +
                        "Prefrito ${pesoFormat.format(registro.pesoPrefrito)} · " +
                        "Frito ${pesoFormat.format(registro.pesoFrito)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Rendimiento: ${pctFormat.format(registro.rendimiento * 100.0)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
