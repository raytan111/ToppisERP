package com.toppis.app.ui.conteos

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
import com.toppis.app.data.db.entities.UnidadMedida
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Conteo
import com.toppis.app.data.repository.LineaConteo
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConteosScreen(
    viewModel: ConteoViewModel,
    usuarioId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val conteos by viewModel.conteos.collectAsState()
    val articulos by viewModel.articulos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var modoNuevo by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ConteoUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as ConteoUiState.Error).message)
                viewModel.resetState()
            }
            ConteoUiState.Success -> {
                snackbarHostState.showSnackbar("Conteo guardado")
                modoNuevo = false
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // ── Pantalla completa: NUEVO CONTEO ──────────────────────────────────────
    if (modoNuevo) {
        NuevoConteoView(
            articulos = articulos,
            snackbarHostState = snackbarHostState,
            onCancelar = { modoNuevo = false },
            onGuardar = { lineas, nota, cerrar ->
                viewModel.guardarConteo(lineas, nota, usuarioId, cerrar)
            }
        )
        return
    }

    // ── Pantalla: HISTORIAL DE CONTEOS ───────────────────────────────────────
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Conteo de Inventario", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.refrescarArticulos(); modoNuevo = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo conteo")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (conteos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin conteos. Usá + para hacer uno.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(conteos) { c ->
                        ConteoCard(c, onEliminar = { aEliminar = c.id })
                    }
                }
            }
        }
    }

    aEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("Eliminar conteo") },
            text = { Text("¿Eliminar este conteo del historial? (no revierte el ajuste de stock)") },
            confirmButton = { TextButton(onClick = { viewModel.eliminarConteo(id); aEliminar = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ConteoCard(c: Conteo, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Conteo #${c.id}", style = MaterialTheme.typography.titleMedium)
                Text(c.fecha?.take(16)?.replace("T", " ") ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                val color = if (c.estado == "CERRADO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                Text(c.estado, style = MaterialTheme.typography.labelMedium, color = color)
                if (c.nota.isNotBlank()) Text(c.nota, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoConteoView(
    articulos: List<Articulo>,
    snackbarHostState: SnackbarHostState,
    onCancelar: () -> Unit,
    onGuardar: (lineas: List<LineaConteo>, nota: String, cerrar: Boolean) -> Unit
) {
    val contados = remember { mutableStateMapOf<Int, String>() }
    var nota by remember { mutableStateOf("") }
    var cerrar by remember { mutableStateOf(true) }
    val num = DecimalFormat("#,##0.##")

    fun unidadDe(a: Articulo): UnidadMedida? = UnidadMedida.porAbreviatura(a.unidadCompra)

    fun construirLineas(): List<LineaConteo> = articulos.mapNotNull { a ->
        val txt = contados[a.id]?.replace(",", ".")?.toDoubleOrNull() ?: return@mapNotNull null
        val factor = unidadDe(a)?.factorBase ?: 1.0
        LineaConteo(articulo = a, stockSistema = a.stockBase, stockContado = txt * factor)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Nuevo conteo", onBack = onCancelar) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = cerrar, onCheckedChange = { cerrar = it })
                        Text("Cerrar y ajustar stock al valor contado", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { onGuardar(construirLineas(), nota, cerrar) },
                        enabled = articulos.isNotEmpty() && contados.values.any { it.isNotBlank() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("GUARDAR CONTEO") }
                }
            }
        }
    ) { padding ->
        if (articulos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay artículos para contar.", color = MaterialTheme.colorScheme.outline)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = nota, onValueChange = { nota = it },
                    label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Ingresá el stock real contado de cada artículo (en su unidad de compra):",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            items(articulos) { a ->
                val unidad = unidadDe(a)
                val factor = unidad?.factorBase ?: 1.0
                val sistemaEnCompra = if (factor > 0) a.stockBase / factor else a.stockBase
                val unidadLabel = unidad?.abreviatura ?: a.unidadBase
                val contadoTxt = contados[a.id]
                val contadoVal = contadoTxt?.replace(",", ".")?.toDoubleOrNull()
                val dif = if (contadoVal != null) contadoVal - sistemaEnCompra else null

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(a.nombre, style = MaterialTheme.typography.titleSmall)
                        Text("Sistema: ${num.format(sistemaEnCompra)} $unidadLabel",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        OutlinedTextField(
                            value = contadoTxt ?: "",
                            onValueChange = { contados[a.id] = it },
                            label = { Text("Contado ($unidadLabel)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        if (dif != null && dif != 0.0) {
                            val difColor = if (dif < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                            Text("Diferencia: ${if (dif > 0) "+" else ""}${num.format(dif)} $unidadLabel",
                                style = MaterialTheme.typography.labelMedium, color = difColor)
                        }
                    }
                }
            }
        }
    }
}
