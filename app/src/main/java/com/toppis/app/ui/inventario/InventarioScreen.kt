package com.toppis.app.ui.inventario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.DimensionUnidad
import com.toppis.app.data.models.Articulo
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val articulos by viewModel.articulos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCrear by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Articulo?>(null) }
    var aEliminar by remember { mutableStateOf<Articulo?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is InventarioUiState.Error) {
            snackbarHostState.showSnackbar((uiState as InventarioUiState.Error).message)
            viewModel.resetState()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (articulos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin artículos. Usá el botón + para agregar.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(articulos) { art ->
                    ArticuloCard(
                        articulo = art,
                        onEditar = { enEdicion = art },
                        onEliminar = { aEliminar = art }
                    )
                }
            }
        }

        if (isAdmin) {
            FloatingActionButton(
                onClick = { showCrear = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Crear artículo")
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showCrear) {
        ArticuloDialog(
            onDismiss = { showCrear = false },
            onConfirm = { nombre, dim, uCompra, factor, costo, rend, stock, par, perecible, vida, vendible, posSel ->
                viewModel.crearArticulo(nombre, dim, uCompra, factor, costo, rend, stock, par, perecible, vida, vendible, posSel)
                showCrear = false
            }
        )
    }

    enEdicion?.let { art ->
        ArticuloDialog(
            inicial = art,
            onDismiss = { enEdicion = null },
            onConfirm = { nombre, dim, uCompra, factor, costo, rend, stock, par, perecible, vida, vendible, posSel ->
                viewModel.editarArticulo(
                    art.copy(
                        nombre = nombre, dimension = dim, unidadBase = dim.unidadBase,
                        unidadCompra = uCompra, factorCompra = factor, costoCompra = costo,
                        rendimiento = rend, stockBase = stock, parLevel = par,
                        perecible = perecible, vidaUtilDias = vida, esVendible = vendible,
                        seleccionableEnPos = posSel
                    )
                )
                enEdicion = null
            }
        )
    }

    aEliminar?.let { art ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("Eliminar artículo") },
            text = { Text("¿Eliminar \"${art.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = { viewModel.eliminarArticulo(art.id); aEliminar = null }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticuloCard(
    articulo: Articulo,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val enAlerta = articulo.parLevel > 0 && articulo.stockBase < articulo.parLevel
    val stockColor = if (enAlerta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val money = DecimalFormat("$#,##0.######")
    val num = DecimalFormat("#,##0.##")

    // Mostrar el stock en la unidad de compra (ej: kg) para legibilidad
    val unidad = com.toppis.app.data.db.entities.UnidadMedida.porAbreviatura(articulo.unidadCompra)
    val factor = unidad?.factorBase ?: 1.0
    val stockMostrado = if (factor > 0) articulo.stockBase / factor else articulo.stockBase
    val unidadLabel = unidad?.abreviatura ?: articulo.unidadBase

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEditar,
        colors = CardDefaults.cardColors(
            containerColor = if (enAlerta)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(articulo.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Stock: ${num.format(stockMostrado)} $unidadLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = stockColor
                )
                Text(
                    text = "Costo: ${money.format(articulo.costoBase)}/${articulo.unidadBase}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (articulo.rendimiento < 1.0) {
                    Text(
                        text = "Rendimiento: ${(articulo.rendimiento * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (enAlerta) {
                    Text("⚠ Bajo el stock mínimo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
