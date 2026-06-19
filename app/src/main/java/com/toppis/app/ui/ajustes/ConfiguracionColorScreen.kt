package com.toppis.app.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toppis.erp.ui.theme.ThemeManager

/**
 * Configurar Colores de la empresa: el admin elige un color de marca (hex "#")
 * y todo el tema Material 3 se regenera en vivo. Modo claro/oscuro/sistema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionColorScreen(
    onNavigateBack: () -> Unit = {}
) {
    val seedHex by ThemeManager.seedHex.collectAsState()
    val modo by ThemeManager.modoOscuro.collectAsState()

    var texto by remember(seedHex) { mutableStateOf(seedHex.removePrefix("#")) }
    val hexValido = ThemeManager.normalizarHex(texto) != null

    val presets = listOf(
        "#E63946" to "Rojo Toppis",
        "#F4511E" to "Naranja",
        "#FF7043" to "Mandarina",
        "#FBC02D" to "Amarillo",
        "#2E7D32" to "Verde",
        "#00897B" to "Teal",
        "#1E88E5" to "Azul",
        "#5E35B1" to "Violeta",
        "#D81B60" to "Magenta",
        "#6D4C41" to "Café",
        "#37474F" to "Pizarra",
        "#000000" to "Negro"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Colores") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Vista previa en vivo ──────────────────────────────────────
            Text("Vista previa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            PreviewCard()

            // ── Color de marca (hex) ──────────────────────────────────────
            Text("Color de marca", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ThemeManager.hexToColor(texto) ?: MaterialTheme.colorScheme.primary)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = { nuevo ->
                        texto = nuevo.removePrefix("#").take(8)
                        ThemeManager.normalizarHex(texto)?.let { ThemeManager.setSeed(it) }
                    },
                    label = { Text("Código hex") },
                    prefix = { Text("#") },
                    isError = texto.isNotEmpty() && !hexValido,
                    supportingText = {
                        Text(if (texto.isNotEmpty() && !hexValido) "Hex inválido (ej: E63946)" else "Formato: RRGGBB")
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Presets ───────────────────────────────────────────────────
            Text("Sugerencias", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRowSwatches(
                presets = presets,
                seleccionadoHex = ThemeManager.normalizarHex(texto),
                onPick = { hex ->
                    texto = hex.removePrefix("#")
                    ThemeManager.setSeed(hex)
                }
            )

            // ── Apariencia ────────────────────────────────────────────────
            Text("Apariencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val opciones = listOf("Sistema", "Claro", "Oscuro")
                opciones.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = modo == index,
                        onClick = { ThemeManager.setModo(index) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = opciones.size)
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    ThemeManager.reset()
                    texto = ThemeManager.SEED_POR_DEFECTO.removePrefix("#")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restablecer al color por defecto") }

            Text(
                "Los cambios se aplican y guardan al instante en este dispositivo.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreviewCard() {
    val cs = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Swatch("Primary", cs.primary, cs.onPrimary, Modifier.weight(1f))
                Swatch("Secondary", cs.secondary, cs.onSecondary, Modifier.weight(1f))
                Swatch("Tertiary", cs.tertiary, cs.onTertiary, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Botón") }
                FilledTonalButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("Tonal") }
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("Outline") }
            }
            Surface(color = cs.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                Text(
                    "Tarjeta de ejemplo · ToppisERP",
                    color = cs.onPrimaryContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun Swatch(label: String, color: Color, onColor: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", color = onColor, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FlowRowSwatches(
    presets: List<Pair<String, String>>,
    seleccionadoHex: String?,
    onPick: (String) -> Unit
) {
    // Grilla simple de 6 por fila usando Column/Row (evita dependencias extra).
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        presets.chunked(6).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEach { (hex, _) ->
                    val color = ThemeManager.hexToColor(hex) ?: MaterialTheme.colorScheme.primary
                    val sel = seleccionadoHex == ThemeManager.normalizarHex(hex)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (sel) 3.dp else 1.dp,
                                color = if (sel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onPick(hex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (sel) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}
