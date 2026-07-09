package com.toppis.app.ui.costos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.EstadoCierre
import com.toppis.app.data.db.entities.EstadoSemaforo
import com.toppis.app.domain.costos.ManoObraDisponible
import com.toppis.app.domain.costos.ResultadoSemanal
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val money = DecimalFormat("$#,##0")
private val pct = DecimalFormat("0.#")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreSemanalScreen(
    viewModel: CierreSemanalViewModel,
    usuarioId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val semana by viewModel.semana.collectAsState()
    val resultado by viewModel.resultado.collectAsState()
    val manoObra by viewModel.manoObra.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(uiState) {
        when (val st = uiState) {
            is CierreUiState.Error -> { errorMsg = st.message; viewModel.resetState() }
            CierreUiState.Success -> { snackbarHostState.showSnackbar("Semana cerrada"); viewModel.resetState() }
            else -> {}
        }
    }
    errorMsg?.let { msg ->
        com.toppis.app.ui.components.ToppisErrorDialog(mensaje = msg, onDismiss = { errorMsg = null })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Resultado semanal", onBack = onNavigateBack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Selector de semana
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.cambiarSemana(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior")
                }
                Text(semana.etiqueta, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { viewModel.cambiarSemana(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Semana siguiente")
                }
            }
            HorizontalDivider()

            if (cargando && resultado == null) {
                com.toppis.app.ui.components.SkeletonList()
            } else {
                val r = resultado
                if (r == null) {
                    com.toppis.app.ui.components.EmptyState(
                        icon = Icons.Filled.Lock,
                        titulo = "Sin datos",
                        subtitulo = "No se pudo cargar el resultado de la semana."
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LoQueQuedaCard(r)
                        DesgloseCard(r)
                        ManoObraCard(manoObra)
                        BreakEvenCard(r)
                        SemaforosCard(r)

                        if (r.estado == EstadoCierre.CERRADO) {
                            AssistChip(onClick = {}, label = { Text("Semana cerrada (valores congelados)") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) })
                        } else {
                            Button(
                                onClick = { confirmar = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("CERRAR SEMANA") }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (confirmar) {
        com.toppis.app.ui.components.ToppisConfirmDialog(
            titulo = "Cerrar semana",
            mensaje = "Se guardará una foto de esta semana (${semana.etiqueta}). Los valores quedan congelados y no cambian si después modificás precios. ¿Confirmás?",
            textoConfirmar = "Cerrar semana",
            onConfirm = { confirmar = false; viewModel.confirmarCierre(usuarioId) },
            onDismiss = { confirmar = false }
        )
    }
}

@Composable
private fun LoQueQuedaCard(r: ResultadoSemanal) {
    val positivo = r.resultado >= 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (positivo) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Lo que queda esta semana", style = MaterialTheme.typography.labelLarge)
            Text(
                money.format(r.resultado),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (positivo) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer
            )
            Text("Ventas cobradas: ${money.format(r.ventasCobradas)}",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DesgloseCard(r: ResultadoSemanal) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Costos de la semana", style = MaterialTheme.typography.titleSmall)
            Fila("Variables (insumos/packaging/bencina)", money.format(r.costoVariable))
            Fila("Mano de obra pagada", money.format(r.manoObraPagada))
            Fila("Fijos (prorrateados)", money.format(r.fijosProrrateados))
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Fila("Food cost (teórico)", "${pct.format(r.foodPct)}%")
            Fila("Mano de obra", "${pct.format(r.laborPct)}%")
        }
    }
}

@Composable
private fun ManoObraCard(mo: ManoObraDisponible?) {
    if (mo == null) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Mano de obra disponible", style = MaterialTheme.typography.titleSmall)
            Fila("Presupuesto de la semana", money.format(mo.total))
            if (mo.esPresupuestoParaContratar) {
                Text("Aún no hay empleados cargados: este es el presupuesto para contratar.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Fila("Por persona (${mo.empleadosActivos})", money.format(mo.porPersona))
            }
            if (mo.alcanzaParaContratar) {
                Text("✔ Ya alcanza para contratar a alguien.",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BreakEvenCard(r: ResultadoSemanal) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Punto de equilibrio (break-even)", style = MaterialTheme.typography.titleSmall)
            if (r.breakEven == null) {
                Text("No calculable con los costos variables actuales.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Fila("Necesitás vender", money.format(r.breakEven))
                if (r.bajoBreakEven && r.faltaVender != null) {
                    Text("Te falta vender ${money.format(r.faltaVender)} para no perder.",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Estás por sobre el break-even 🎉",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SemaforosCard(r: ResultadoSemanal) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Objetivos", style = MaterialTheme.typography.titleSmall)
            SemaforoFila("Food cost", r.foodPct, r.semaforoFood)
            SemaforoFila("Mano de obra", r.laborPct, r.semaforoLabor)
            SemaforoFila("Arriendo", r.arriendoPct, r.semaforoArriendo)
            if (r.alertaArriendo) {
                Text("⚠ El arriendo se pasa del techo objetivo.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            if (r.bajoBreakEven) {
                Text("⚠ Estás bajo el break-even.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SemaforoFila(etiqueta: String, valorPct: Double, estado: EstadoSemaforo) {
    val color = if (estado == EstadoSemaforo.ALERTA) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (estado == EstadoSemaforo.ALERTA) "🔴" else "🟢")
            Spacer(Modifier.width(8.dp))
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        }
        Text("${pct.format(valorPct)}%", style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}
