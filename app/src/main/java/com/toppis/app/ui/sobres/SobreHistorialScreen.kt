package com.toppis.app.ui.sobres

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
    val movimientos by viewModel.movimientos.collectAsState()
    val cargando by viewModel.cargandoMovimientos.collectAsState()

    LaunchedEffect(sobreId) { viewModel.cargarMovimientos(sobreId) }
    DisposableEffect(Unit) { onDispose { viewModel.limpiarMovimientos() } }

    val sobre = sobres.firstOrNull { it.id == sobreId }
    val sobresById = remember(sobres) { sobres.associateBy { it.id } }
    val esCuenta = sobre?.tipo != TipoSobre.FONDO
    val acento = if (esCuenta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

    // Enriquecer con saldo corriente (desde el saldo actual hacia atrás).
    val statement = remember(movimientos, sobre?.saldo) {
        var running = sobre?.saldo ?: 0.0
        movimientos.map { m ->
            val entra = m.destinoId == sobreId
            val saldoDespues = running
            running -= if (entra) m.monto else -m.monto
            MovStatement(m, entra, saldoDespues, parseCL(m.createdAt ?: m.fecha))
        }
    }
    val porDia = remember(statement) {
        statement.groupBy { it.fechaHora?.toLocalDate() ?: LocalDate.MIN }
            .toList().sortedByDescending { it.first }
    }

    Scaffold(
        topBar = { ToppisTopBar(titulo = sobre?.nombre ?: "Historial", onBack = onNavigateBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (sobre != null) HeaderCuenta(sobre, acento, esCuenta)

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
}

@Composable
private fun HeaderCuenta(sobre: Sobre, acento: Color, esCuenta: Boolean) {
    // Animación de aparición del saldo (cuenta suave hasta el valor real).
    val animado by animateFloatAsState(
        targetValue = sobre.saldo.toFloat(),
        animationSpec = tween(700), label = "saldo"
    )
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp
    ) {
        Box(
            Modifier.background(
                Brush.verticalGradient(listOf(acento.copy(alpha = 0.85f), acento.copy(alpha = 0.55f)))
            ).fillMaxWidth().padding(20.dp)
        ) {
            Column {
                Text(if (esCuenta) "Cuenta · dinero real" else "Fondo · provisión",
                    style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(6.dp))
                Text("Saldo actual", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                Text(money.format(animado.toDouble()), style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                if (sobre.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(sobre.descripcion, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                }
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
