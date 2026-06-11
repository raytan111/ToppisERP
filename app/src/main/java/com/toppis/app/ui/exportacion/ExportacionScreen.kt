package com.toppis.app.ui.exportacion

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toppis.app.ui.components.ToppisTopBar

// ── Períodos disponibles ──────────────────────────────────────────────────────

private enum class Periodo(val label: String, val dias: Int) {
    HOY("Hoy", 0),
    SEMANA("Esta semana", 7),
    MES("Este mes", 30),
    TODO("Todo", -1)
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ExportacionScreen(
    viewModel: ExportacionViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var periodoSeleccionado by remember { mutableStateOf(Periodo.TODO) }

    // Calcular timestamp "desde" según período
    val desdeMs: Long = remember(periodoSeleccionado) {
        when (periodoSeleccionado) {
            Periodo.TODO -> 0L
            else -> {
                val ahora = System.currentTimeMillis()
                val msEnDia = 24L * 60 * 60 * 1000
                when (periodoSeleccionado) {
                    Periodo.HOY -> {
                        val cal = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        cal.timeInMillis
                    }
                    Periodo.SEMANA -> ahora - 7 * msEnDia
                    Periodo.MES   -> ahora - 30 * msEnDia
                    else          -> 0L
                }
            }
        }
    }

    // Reaccionar al estado
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                val result = snackbarHostState.showSnackbar(
                    message = "✅ Archivo listo",
                    actionLabel = "Abrir",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(state.uri, state.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                }
                viewModel.resetState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar("❌ ${state.mensaje}")
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ToppisTopBar(
                titulo = "📤 Exportar datos",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        val isExporting = exportState is ExportState.Exporting

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {

            // ── Selector de período ──────────────────────────────────────────
            item {
                Text(
                    text = "Período de exportación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Periodo.entries) { p ->
                        FilterChip(
                            selected = periodoSeleccionado == p,
                            onClick = { periodoSeleccionado = p },
                            label = { Text(p.label) }
                        )
                    }
                }
            }

            // ── Indicador de progreso ────────────────────────────────────────
            if (isExporting) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text("Generando archivo…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // ── Sección: Exportaciones individuales ──────────────────────────
            item {
                Text(
                    text = "Exportaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                ExportCard(
                    emoji = "📊",
                    titulo = "Exportar Ventas",
                    subtitulo = "Historial de ventas en Excel (.xlsx)",
                    mimeHint = "Excel",
                    enabled = !isExporting,
                    onClick = { viewModel.exportarVentas(context, desdeMs) }
                )
            }

            item {
                ExportCard(
                    emoji = "💸",
                    titulo = "Exportar Gastos",
                    subtitulo = "Gastos y totales por categoría en Excel (.xlsx)",
                    mimeHint = "Excel",
                    enabled = !isExporting,
                    onClick = { viewModel.exportarGastos(context, desdeMs) }
                )
            }

            item {
                ExportCard(
                    emoji = "💰",
                    titulo = "Exportar Sobres",
                    subtitulo = "Saldos y movimientos en Excel (.xlsx)",
                    mimeHint = "Excel",
                    enabled = !isExporting,
                    onClick = { viewModel.exportarSobres(context) }
                )
            }

            item {
                ExportCard(
                    emoji = "📦",
                    titulo = "Exportar Inventario",
                    subtitulo = "Lista de insumos en CSV (.csv)",
                    mimeHint = "CSV",
                    enabled = !isExporting,
                    onClick = { viewModel.exportarInventario(context) }
                )
            }

            // ── Sección: Exportar todo ───────────────────────────────────────
            item {
                Text(
                    text = "Exportación completa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🗂️", style = MaterialTheme.typography.headlineMedium)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exportar Todo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Ventas + Gastos + Sobres + Inventario en un ZIP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(
                            onClick = { viewModel.exportarTodo(context, desdeMs) },
                            enabled = !isExporting
                        ) {
                            Icon(
                                Icons.Filled.FolderZip,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ZIP")
                        }
                    }
                }
            }
        }
    }
}

// ── Card de exportación ───────────────────────────────────────────────────────

@Composable
private fun ExportCard(
    emoji: String,
    titulo: String,
    subtitulo: String,
    mimeHint: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onClick, enabled = enabled) {
                Icon(
                    Icons.Filled.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(mimeHint)
            }
        }
    }
}

