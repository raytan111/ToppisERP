package com.toppis.app.ui.comprobantes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.Comprobante
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprobantesScreen(
    viewModel: ComprobantesViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val comprobantes by viewModel.comprobantes.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()
    val money = DecimalFormat("$#,##0")
    var query by remember { mutableStateOf("") }
    val filtrados = remember(comprobantes, query) {
        if (query.isBlank()) comprobantes
        else comprobantes.filter {
            "#${it.folio}".contains(query.trim()) ||
                (it.ventaId?.let { v -> "#$v".contains(query.trim()) } ?: false)
        }
    }

    // Recargar al abrir (refleja cambios hechos fuera de la app).
    LaunchedEffect(Unit) { viewModel.recargar() }

    Scaffold(
        topBar = {
            ToppisTopBar(titulo = "Comprobantes", onBack = onNavigateBack)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                cargandoInicial && comprobantes.isEmpty() -> {
                    com.toppis.app.ui.components.SkeletonList()
                }
                comprobantes.isEmpty() -> {
                    com.toppis.app.ui.components.EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        titulo = "Sin comprobantes",
                        subtitulo = "Los comprobantes emitidos aparecerán acá."
                    )
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        com.toppis.app.ui.components.SearchField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "Buscar por folio o #venta…",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        if (filtrados.isEmpty()) {
                            com.toppis.app.ui.components.EmptyState(
                                icon = Icons.Filled.SearchOff,
                                titulo = "Sin resultados",
                                subtitulo = "No hay comprobantes que coincidan con \"$query\"."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(filtrados) { c ->
                                    ComprobanteCard(c, money)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComprobanteCard(c: Comprobante, money: DecimalFormat) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Comprobante #${c.folio}", style = MaterialTheme.typography.titleMedium)
                Text(
                    money.format(c.total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Neto ${money.format(c.neto)} · IVA ${money.format(c.iva)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            c.ventaId?.let {
                Text(
                    "Venta #$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatFechaIso(c.fechaEmision),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
