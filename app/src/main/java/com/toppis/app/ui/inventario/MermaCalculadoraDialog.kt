package com.toppis.app.ui.inventario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.DimensionUnidad
import com.toppis.app.data.db.entities.UnidadMedida
import com.toppis.app.data.models.Articulo
import java.text.DecimalFormat

/**
 * Diálogo crear/editar Artículo.
 * - Dimensión y Unidad de compra se SELECCIONAN (no se escriben).
 * - El factor de conversión a unidad base se calcula solo (kg→1000 g, L→1000 ml).
 * - costo_base = (costo_compra / factor) / rendimiento
 *
 * Convención de guardado:
 *   unidad_compra  = abreviatura de la unidad elegida ("kg", "L", "un"...)
 *   factor_compra  = cantidadPorCompra × unidad.factorBase   (en unidad base)
 *   stock_base     = stockInput × unidad.factorBase
 *   par_level      = parInput × unidad.factorBase
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticuloDialog(
    inicial: Articulo? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        nombre: String,
        dimension: DimensionUnidad,
        unidadCompra: String,
        factorCompra: Double,
        costoCompra: Double,
        rendimiento: Double,
        stockBase: Double,
        parLevel: Double,
        perecible: Boolean,
        vidaUtilDias: Int,
        esVendible: Boolean,
        seleccionableEnPos: Boolean,
        cantidadPos: Double
    ) -> Unit
) {
    val esEdicion = inicial != null

    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var dimension by remember { mutableStateOf(inicial?.dimension ?: DimensionUnidad.MASA) }

    // Unidad de compra seleccionada (se deriva de la abreviatura guardada al editar)
    val unidadInicial = inicial?.let { UnidadMedida.porAbreviatura(it.unidadCompra) }
        ?: UnidadMedida.deDimension(inicial?.dimension ?: DimensionUnidad.MASA).first()
    var unidadCompra by remember { mutableStateOf(unidadInicial) }

    // Cantidad por compra (en la unidad elegida): factor_compra / factorBase
    val cantidadInicial = inicial?.let {
        if (unidadInicial.factorBase > 0) it.factorCompra / unidadInicial.factorBase else it.factorCompra
    }
    var cantidadText by remember { mutableStateOf(cantidadInicial?.let { formatNum(it) } ?: "") }
    var costoCompraText by remember { mutableStateOf(inicial?.costoCompra?.let { if (it == 0.0) "" else formatNum(it) } ?: "") }
    var rendText by remember { mutableStateOf(((inicial?.rendimiento ?: 1.0) * 100).let { formatNum(it) }) }

    val stockInicial = inicial?.let { if (unidadInicial.factorBase > 0) it.stockBase / unidadInicial.factorBase else it.stockBase }
    var stockText by remember { mutableStateOf(stockInicial?.let { formatNum(it) } ?: "0") }
    val parInicial = inicial?.let { if (unidadInicial.factorBase > 0) it.parLevel / unidadInicial.factorBase else it.parLevel }
    var parText by remember { mutableStateOf(parInicial?.let { formatNum(it) } ?: "0") }

    var perecible by remember { mutableStateOf(inicial?.perecible ?: false) }
    var vidaText by remember { mutableStateOf(inicial?.vidaUtilDias?.toString() ?: "0") }
    var esVendible by remember { mutableStateOf(inicial?.esVendible ?: false) }
    var seleccionablePos by remember { mutableStateOf(inicial?.seleccionableEnPos ?: false) }
    var cantidadPosText by remember { mutableStateOf(inicial?.cantidadPos?.let { if (it == 0.0) "" else formatNum(it) } ?: "") }
    var expandedDim by remember { mutableStateOf(false) }
    var expandedUnidad by remember { mutableStateOf(false) }

    // Si cambia la dimensión, resetear la unidad a la primera de esa dimensión
    LaunchedEffect(dimension) {
        if (unidadCompra.dimension != dimension) {
            unidadCompra = UnidadMedida.deDimension(dimension).first()
        }
    }

    val cantidad = cantidadText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val costoCompra = costoCompraText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val rendPct = rendText.replace(",", ".").toDoubleOrNull() ?: 100.0
    val rendimiento = (rendPct / 100.0).coerceIn(0.0001, 1.0)
    val factorCompra = cantidad * unidadCompra.factorBase
    val costoBase = Articulo.calcularCostoBase(costoCompra, factorCompra, rendimiento)

    val valido = nombre.isNotBlank() && cantidad > 0 && costoCompra >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (esEdicion) "Editar Artículo" else "Nuevo Artículo") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = nombre, onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        supportingText = { Text("Ej: Carne molida, Lechuga, Coca-Cola lata") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                // Dimensión
                item {
                    ExposedDropdownMenuBox(expanded = expandedDim, onExpandedChange = { expandedDim = !expandedDim }) {
                        OutlinedTextField(
                            value = dimension.label, onValueChange = {}, readOnly = true,
                            label = { Text("Tipo de medida") },
                            supportingText = { Text("Define la unidad base interna (g / ml / un)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDim) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedDim, onDismissRequest = { expandedDim = false }) {
                            DimensionUnidad.entries.forEach { dim ->
                                DropdownMenuItem(text = { Text(dim.label) }, onClick = { dimension = dim; expandedDim = false })
                            }
                        }
                    }
                }
                // Unidad de compra (seleccionable, filtrada por dimensión)
                item {
                    val opciones = UnidadMedida.deDimension(dimension)
                    ExposedDropdownMenuBox(expanded = expandedUnidad, onExpandedChange = { expandedUnidad = !expandedUnidad }) {
                        OutlinedTextField(
                            value = "${unidadCompra.label} (${unidadCompra.abreviatura})", onValueChange = {}, readOnly = true,
                            label = { Text("Unidad de compra") },
                            supportingText = { Text("¿En qué unidad viene lo que compras?") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidad) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedUnidad, onDismissRequest = { expandedUnidad = false }) {
                            opciones.forEach { u ->
                                DropdownMenuItem(text = { Text("${u.label} (${u.abreviatura})") }, onClick = {
                                    unidadCompra = u; expandedUnidad = false
                                })
                            }
                        }
                    }
                }
                // Cantidad por compra
                item {
                    OutlinedTextField(
                        value = cantidadText, onValueChange = { cantidadText = it },
                        label = { Text("Cantidad por compra (${unidadCompra.abreviatura})") },
                        supportingText = { Text("Cuánto trae UNA compra. Saco 25 kg → 25. Pack 6 → 6.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                // Costo de compra
                item {
                    OutlinedTextField(
                        value = costoCompraText, onValueChange = { costoCompraText = it },
                        label = { Text("Costo de compra (CLP)") },
                        supportingText = { Text("Lo que pagas por esa compra completa (el saco/pack/bidón)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                // Rendimiento
                item {
                    OutlinedTextField(
                        value = rendText, onValueChange = { rendText = it },
                        label = { Text("Rendimiento %") },
                        supportingText = { Text("Cuánto queda usable tras merma de proceso. 100 = sin pérdida. Papa ≈ 55.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                // Resumen costo
                item {
                    if (cantidad > 0 && costoCompra > 0) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(12.dp)) {
                                val money = DecimalFormat("$#,##0.######")
                                Text("Equivale a ${formatNum(factorCompra)} ${dimension.unidadBase} por compra",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("Costo por ${dimension.unidadBase}: ${money.format(costoBase)}",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                // Stock
                item {
                    OutlinedTextField(
                        value = stockText, onValueChange = { stockText = it },
                        label = { Text("Stock actual (${unidadCompra.abreviatura})") },
                        supportingText = { Text("Cuánto tenés ahora, en ${unidadCompra.abreviatura}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                // Par level
                item {
                    OutlinedTextField(
                        value = parText, onValueChange = { parText = it },
                        label = { Text("Stock mínimo / par (${unidadCompra.abreviatura})") },
                        supportingText = { Text("Cuando el stock baja de esto, te avisa para comprar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = perecible, onCheckedChange = { perecible = it })
                        Text("Perecible (se puede echar a perder)")
                    }
                }
                if (perecible) {
                    item {
                        OutlinedTextField(
                            value = vidaText, onValueChange = { vidaText = it },
                            label = { Text("Vida útil (días)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = esVendible, onCheckedChange = { esVendible = it })
                        Text("Se vende directo (ej: bebida en lata)")
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = seleccionablePos, onCheckedChange = { seleccionablePos = it })
                        Text("Seleccionable en POS (salsa/agregado)")
                    }
                }
                if (seleccionablePos) {
                    item {
                        OutlinedTextField(
                            value = cantidadPosText, onValueChange = { cantidadPosText = it },
                            label = { Text("Cantidad que se gasta (${dimension.unidadBase})") },
                            supportingText = { Text("Cuánto se consume cada vez que se elige como salsa/agregado. Ej: 15 g de ketchup.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val factorBase = unidadCompra.factorBase
                    onConfirm(
                        nombre, dimension, unidadCompra.abreviatura, factorCompra, costoCompra, rendimiento,
                        (stockText.replace(",", ".").toDoubleOrNull() ?: 0.0) * factorBase,
                        (parText.replace(",", ".").toDoubleOrNull() ?: 0.0) * factorBase,
                        perecible, vidaText.toIntOrNull() ?: 0, esVendible, seleccionablePos,
                        cantidadPosText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = valido
            ) { Text(if (esEdicion) "Guardar" else "Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun formatNum(v: Double): String {
    return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}
