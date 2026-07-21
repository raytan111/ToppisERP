package com.toppis.app.ui.sobres

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.TipoSobre
import com.toppis.app.data.models.MovimientoSobre
import com.toppis.app.data.models.Sobre
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val money = DecimalFormat("$#,##0")
private val zonaCL = ZoneId.of("America/Santiago")
private val horaFmt = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "CL"))
private val diaFmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CL"))

/** Movimiento enriquecido con el saldo del sobre justo después de ocurrir. */
private data class MovStatement(
    val mov: MovimientoSobre,
    val entra: Boolean,
    val saldoDespues: Double,
    val fechaHora: LocalDateTime?
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SobreHistorialScreen(
    viewModel: SobreViewModel,
    sobreId: Int,
    onNavigateBack: () -> Unit = {}
) {
    val sobres by viewModel.sobres.collectAsState()
    val movimientosTodos by viewModel.movimientosTodos.collectAsState()
    val cargando by viewModel.cargandoMovimientos.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargarTodosMovimientos() }

    val sobresById = remember(sobres) { sobres.associateBy { it.id } }

    if (sobres.isEmpty()) {
        Scaffold(topBar = { ToppisTopBar(titulo = "Historial", onBack = onNavigateBack) }) { p ->
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return
    }

    val initialIndex = remember(sobres) { sobres.indexOfFirst { it.id == sobreId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { sobres.size }
    val currentSobre = sobres.getOrNull(pagerState.currentPage) ?: sobres.first()

    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ToppisTopBar(
                titulo = currentSobre.nombre,
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { showEdit = true }) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Indicador de páginas (puntos) para las cuentas.
            if (sobres.size > 1) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    sobres.forEachIndexed { i, _ ->
                        val sel = i == pagerState.currentPage
                        Box(
                            Modifier.padding(3.dp).size(if (sel) 9.dp else 7.dp).clip(CircleShape)
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val s = sobres[page]
                val movs = remember(movimientosTodos, s.id) {
                    movimientosTodos.filter { it.origenId == s.id || it.destinoId == s.id }
                }
                PaginaHistorial(
                    sobre = s,
                    movimientos = movs,
                    cargando = cargando && movimientosTodos.isEmpty(),
                    sobresById = sobresById,
                    onTransferir = { showTransfer = true }
                )
                // (deslizá para ver otras cuentas)
            }
        }
    }

    if (showEdit) {
        EditarSobreDialog(
            sobre = currentSobre,
            onDismiss = { showEdit = false },
            onConfirm = { nombre, descripcion, tipo ->
                viewModel.editarSobre(currentSobre.copy(nombre = nombre, descripcion = descripcion, tipo = tipo))
                showEdit = false
            }
        )
    }

    if (showDelete) {
        val conSaldo = currentSobre.saldo != 0.0
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = currentSobre.nombre,
            titulo = "Eliminar sobre",
            mensaje = if (conSaldo)
                "No se puede eliminar un sobre con saldo (${money.format(currentSobre.saldo)})."
            else
                "¿Eliminar el sobre \"${currentSobre.nombre}\"? Esta acción no se puede deshacer.",
            confirmarHabilitado = !conSaldo,
            onConfirm = { viewModel.eliminarSobre(currentSobre); showDelete = false; onNavigateBack() },
            onDismiss = { showDelete = false }
        )
    }

    if (showTransfer) {
        val destinos = sobres.filter { it.id != currentSobre.id }
        TransferDialog(
            origen = currentSobre,
            destinos = destinos,
            onDismiss = { showTransfer = false },
            onConfirm = { destinoId, monto, desc ->
                viewModel.transferir(currentSobre.id.toLong(), destinoId, monto, desc, null)
                showTransfer = false
            }
        )
    }
}

@Composable
private fun PaginaHistorial(
    sobre: Sobre,
    movimientos: List<MovimientoSobre>,
    cargando: Boolean,
    sobresById: Map<Int, Sobre>,
    onTransferir: () -> Unit
) {
    val esCuenta = sobre.tipo != TipoSobre.FONDO

    val statement = remember(movimientos, sobre.saldo) {
        var running = sobre.saldo
        movimientos.map { m ->
            val entra = m.destinoId == sobre.id
            val saldoDespues = running
            running -= if (entra) m.monto else -m.monto
            MovStatement(m, entra, saldoDespues, parseCL(m.createdAt ?: m.fecha))
        }
    }
    val porDia = remember(statement) {
        statement.groupBy { it.fechaHora?.toLocalDate() ?: LocalDate.MIN }
            .toList().sortedByDescending { it.first }
    }

    Column(Modifier.fillMaxSize()) {
        HeaderCuenta(sobre, esCuenta, onTransferir)
        when {
            cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            statement.isEmpty() -> com.toppis.app.ui.components.EmptyState(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                titulo = "Sin movimientos",
                subtitulo = "Este sobre todavía no tiene entradas ni salidas."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                porDia.forEach { (dia, movs) ->
                    stickyHeader { DiaHeader(dia, movs.firstOrNull()?.saldoDespues ?: 0.0) }
                    items(movs) { s -> MovimientoRow(s, sobresById) }
                }
            }
        }
    }
}

@Composable
private fun HeaderCuenta(sobre: Sobre, esCuenta: Boolean, onTransferir: () -> Unit) {
    val animado by animateFloatAsState(
        targetValue = sobre.saldo.toFloat(),
        animationSpec = tween(700), label = "saldo"
    )
    // El degradado usa colores de contenedor (no "primary", que ahora es blanco),
    // y el texto usa su on-container para contraste correcto.
    val bg = if (esCuenta) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val onBg = if (esCuenta) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp
    ) {
        Box(
            Modifier.background(
                Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0.6f)))
            ).fillMaxWidth().padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (esCuenta) "Cuenta · dinero real" else "Fondo · provisión",
                        style = MaterialTheme.typography.labelMedium, color = onBg.copy(alpha = 0.85f))
                    Spacer(Modifier.height(6.dp))
                    Text("Saldo actual", style = MaterialTheme.typography.labelSmall, color = onBg.copy(alpha = 0.85f))
                    Text(money.format(animado.toDouble()), style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold, color = onBg)
                    if (sobre.descripcion.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(sobre.descripcion, style = MaterialTheme.typography.bodySmall, color = onBg.copy(alpha = 0.9f))
                    }
                }
                // Botón transferir centrado a la derecha dentro de la card del saldo.
                FilledIconButton(
                    onClick = onTransferir,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = onBg.copy(alpha = 0.18f),
                        contentColor = onBg
                    )
                ) { Icon(Icons.Filled.SwapHoriz, contentDescription = "Transferir") }
            }
        }
    }
}

@Composable
private fun DiaHeader(dia: LocalDate, cierre: Double) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (dia == LocalDate.MIN) "Sin fecha" else dia.format(diaFmt).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold
            )
            Text("Cerró en ${money.format(cierre)}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MovimientoRow(s: MovStatement, sobresById: Map<Int, Sobre>) {
    val m = s.mov
    val entra = s.entra
    val color = if (entra) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val contraparteId = if (entra) m.origenId else m.destinoId
    val contraparte = contraparteId?.let { sobresById[it]?.nombre }
    val icono: ImageVector = when {
        m.origenId != null && m.destinoId != null -> Icons.Filled.SwapHoriz
        entra -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.AutoMirrored.Filled.TrendingDown
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(m.descripcion.ifBlank { m.tipo.name }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            val sub = buildString {
                s.fechaHora?.let { append(it.format(horaFmt)); append(" h") }
                if (contraparte != null) append(if (entra) "  ·  desde $contraparte" else "  ·  hacia $contraparte")
            }
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text((if (entra) "+" else "−") + money.format(m.monto),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text("Saldo ${money.format(s.saldoDespues)}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

private fun parseCL(iso: String?): LocalDateTime? = try {
    if (iso.isNullOrBlank()) null
    else OffsetDateTime.parse(iso).atZoneSameInstant(zonaCL).toLocalDateTime()
} catch (_: Exception) {
    runCatching { LocalDate.parse(iso!!.take(10)).atStartOfDay() }.getOrNull()
}
