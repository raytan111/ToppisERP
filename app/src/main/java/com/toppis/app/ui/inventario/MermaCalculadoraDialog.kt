package com.toppis.app.ui.inventario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat

enum class UnidadCompra(val label: String) {
    KG("Kilogramo (kg)"),
    LITRO("Litro (lt)"),
    GRAMO("Gramo (gr)"),
    ML("Mililitro (ml)"),
    UNIDAD("Unidad")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearIngredienteConMermaDialog(
    ingredienteInicial: com.toppis.app.data.models.Ingrediente? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        nombre: String,
        unidad: String,
        stock: Double,
        costo: Double,
        costoCompra: Double,
        porcentajeMerma: Double,
        unidadCompra: String,
        cantidadComprada: Double,
        cantidadAprovechable: Double,
        costoGramo: Double
    ) -> Unit
) {
    val esEdicion = ingredienteInicial != null

    // Unidad de compra inicial (parseo seguro del enum guardado)
    val unidadCompraInicial = ingredienteInicial?.unidadCompra
        ?.let { runCatching { UnidadCompra.valueOf(it) }.getOrNull() }
        ?: UnidadCompra.KG

    var nombre by remember { mutableStateOf(ingredienteInicial?.nombre ?: "") }
    var unidadMedida by remember { mutableStateOf(ingredienteInicial?.unidadMedida ?: "") }
    var stockText by remember { mutableStateOf(ingredienteInicial?.stockActual?.toString() ?: "") }
    var costoText by remember { mutableStateOf(ingredienteInicial?.costoUnitario?.toString() ?: "") }

    // Campos de merma (pre-llenados directamente desde la base)
    var unidadCompraSelected by remember { mutableStateOf(unidadCompraInicial) }
    var cantidadCompradaText by remember {
        mutableStateOf(ingredienteInicial?.cantidadComprada?.let { if (it == 0.0) "" else it.toString() } ?: "")
    }
    var costoCompraText by remember {
        mutableStateOf(ingredienteInicial?.costoCompra?.let { if (it == 0.0) "" else it.toString() } ?: "")
    }
    var porcentajeMermaText by remember { mutableStateOf(ingredienteInicial?.porcentajeMerma?.toString() ?: "0") }
    var expandedUnidad by remember { mutableStateOf(false) }

    val stockValido = stockText.replace(",", ".").toDoubleOrNull()?.let { it >= 0 } ?: false
    val costoValido = costoText.replace(",", ".").toDoubleOrNull()?.let { it >= 0 } ?: false
    val cantidadComprada = cantidadCompradaText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val costoCompra = costoCompraText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val porcentajeMerma = porcentajeMermaText.replace(",", ".").toDoubleOrNull() ?: 0.0

    // Calcular cantidad aprovechable (convertir a gramos/ml)
    val cantidadAprovechable = cantidadComprada * (1 - porcentajeMerma / 100.0) * when (unidadCompraSelected) {
        UnidadCompra.KG -> 1000.0    // 1 kg = 1000 gr
        UnidadCompra.LITRO -> 1000.0  // 1 lt = 1000 ml
        UnidadCompra.GRAMO -> 1.0
        UnidadCompra.ML -> 1.0
        UnidadCompra.UNIDAD -> 1.0
    }
    val costoGramo = if (cantidadAprovechable > 0) costoCompra / cantidadAprovechable else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (esEdicion) "Editar Ingrediente" else "Nuevo Ingrediente") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // ── Datos básicos ──
                item {
                    Text("Datos básicos:", style = MaterialTheme.typography.labelMedium)
                }

                item {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = unidadMedida,
                        onValueChange = { unidadMedida = it },
                        label = { Text("Unidad (ej: KG, LT, UN)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock inicial") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Calculadora de Merma ──
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Calculadora de Merma", style = MaterialTheme.typography.labelMedium)
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedUnidad,
                        onExpandedChange = { expandedUnidad = !expandedUnidad }
                    ) {
                        OutlinedTextField(
                            value = unidadCompraSelected.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad de compra") },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedUnidad,
                            onDismissRequest = { expandedUnidad = false }
                        ) {
                            UnidadCompra.entries.forEach { unidad ->
                                DropdownMenuItem(
                                    text = { Text(unidad.label) },
                                    onClick = {
                                        unidadCompraSelected = unidad
                                        expandedUnidad = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = cantidadCompradaText,
                        onValueChange = { cantidadCompradaText = it },
                        label = { Text("Cantidad comprada (ej: 1.0 ${unidadCompraSelected.label.split(" ")[0]})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = costoCompraText,
                        onValueChange = { costoCompraText = it },
                        label = { Text("Costo de compra (CLP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = porcentajeMermaText,
                        onValueChange = { porcentajeMermaText = it },
                        label = { Text("% Merma (pérdida)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Resumen de calculadora ──
                item {
                    if (cantidadComprada > 0 && costoCompra > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Resumen:", style = MaterialTheme.typography.labelMedium)

                                val formatter = DecimalFormat("$#,##0")
                                val formatterDecimal = DecimalFormat("0.00")
                                val unitLabel = when (unidadCompraSelected) {
                                    UnidadCompra.KG -> "kg"
                                    UnidadCompra.LITRO -> "lt"
                                    UnidadCompra.GRAMO -> "gr"
                                    UnidadCompra.ML -> "ml"
                                    UnidadCompra.UNIDAD -> "un"
                                }
                                val unitOut = when (unidadCompraSelected) {
                                    UnidadCompra.KG, UnidadCompra.LITRO -> "gr/ml"
                                    else -> unidadCompraSelected.label.split(" ")[0].lowercase()
                                }

                                Text(
                                    "$cantidadComprada $unitLabel a ${formatter.format(costoCompra)} = " +
                                    "${formatterDecimal.format(cantidadAprovechable)} $unitOut aprovechables (merma: $porcentajeMermaText%)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Costo por $unitOut: ${formatter.format(costoGramo)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // ── Costo por porción ──
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Costo por porción", style = MaterialTheme.typography.labelMedium)
                }

                item {
                    OutlinedTextField(
                        value = costoText,
                        onValueChange = { costoText = it },
                        label = { Text("Costo unitario/porción (CLP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        nombre,
                        unidadMedida,
                        stockText.replace(",", ".").toDouble(),
                        costoText.replace(",", ".").toDouble(),
                        costoCompra,
                        porcentajeMerma,
                        unidadCompraSelected.name,
                        cantidadComprada,
                        cantidadAprovechable,
                        costoGramo
                    )
                },
                enabled = nombre.isNotBlank() && unidadMedida.isNotBlank() && stockValido && costoValido
            ) { Text(if (esEdicion) "Guardar" else "Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}





