package com.toppis.app.ui.costos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toppis.app.data.db.entities.PasoRutina
import com.toppis.app.data.models.Sobre
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val money = DecimalFormat("$#,##0")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinaSemanalScreen(
    viewModel: RutinaSemanalViewModel,
    usuarioId: String? = null,
    onIrAConteo: () -> Unit = {},
    onIrAMermas: () -> Unit = {},
    onIrAResultado: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val semana by viewModel.semana.collectAsState()
    val pasos by viewModel.pasos.collectAsState()
    val fijos by viewModel.fijos.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(uiState) {
        when (val st = uiState) {
            is RutinaUiState.Error -> { errorMsg = st.message; viewModel.resetState() }
            is RutinaUiState.Ok -> { snackbarHostState.showSnackbar(st.message); viewModel.resetState() }
            else -> {}
        }
    }
    errorMsg?.let { msg -> com.toppis.app.ui.components.ToppisErrorDialog(mensaje = msg, onDismiss = { errorMsg = null }) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Rutina de cierre", onBack = onNavigateBack) }
    ) { padding ->
        if (cargando) { com.toppis.app.ui.components.SkeletonList(); return@Scaffold }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Semana ${semana.etiqueta}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Completá los pasos del cierre semanal.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            PasoCard(
                numero = 1, titulo = "Conteo de inventario",
                hecho = PasoRutina.CONTEO in pasos,
                accionTexto = "Ir a conteo", onAccion = onIrAConteo,
                onToggle = { viewModel.marcarPaso(PasoRutina.CONTEO, it) }
            )
            PasoCard(
                numero = 2, titulo = "Registro de mermas",
                hecho = PasoRutina.MERMAS in pasos,
                accionTexto = "Ir a mermas", onAccion = onIrAMermas,
                onToggle = { viewModel.marcarPaso(PasoRutina.MERMAS, it) }
            )

            // Paso 3: provisión inline
            ProvisionCard(viewModel, fijos.isNotEmpty(), PasoRutina.PROVISION in pasos, usuarioId) {
                viewModel.marcarPaso(PasoRutina.PROVISION, it)
            }

            PasoCard(
                numero = 4, titulo = "Ver resultado semanal",
                hecho = PasoRutina.RESULTADO in pasos,
                accionTexto = "Ver resultado", onAccion = onIrAResultado,
                onToggle = { viewModel.marcarPaso(PasoRutina.RESULTADO, it) }
            )

            val completos = pasos.size >= 4
            if (completos) {
                Text("✔ Rutina completa. Ya podés cerrar la semana en Resultado semanal.",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PasoCard(
    numero: Int, titulo: String, hecho: Boolean,
    accionTexto: String, onAccion: () -> Unit, onToggle: (Boolean) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onToggle(!hecho) }) {
                Icon(
                    if (hecho) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (hecho) "Hecho" else "Pendiente",
                    tint = if (hecho) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            Column(Modifier.weight(1f)) {
                Text("$numero. $titulo", style = MaterialTheme.typography.titleSmall)
            }
            TextButton(onClick = onAccion) { Text(accionTexto) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProvisionCard(
    viewModel: RutinaSemanalViewModel,
    hayFijos: Boolean,
    hecho: Boolean,
    usuarioId: String?,
    onToggle: (Boolean) -> Unit
) {
    val total = viewModel.totalProvision
    val cuentas = viewModel.sobresCuenta
    val fondos = viewModel.sobresFondo
    var origen by remember(cuentas) { mutableStateOf(cuentas.firstOrNull()) }
    var destino by remember(fondos) { mutableStateOf(fondos.firstOrNull()) }
    var expOrigen by remember { mutableStateOf(false) }
    var expDestino by remember { mutableStateOf(false) }
    var crearFondo by remember { mutableStateOf(false) }
    var nombreFondo by remember { mutableStateOf("Provisión fijos") }

    val saldoInsuf = origen != null && origen!!.saldo < total

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (hecho) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (hecho) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(8.dp))
                Text("3. Provisión de fijos", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { onToggle(!hecho) }) { Text(if (hecho) "Desmarcar" else "Marcar") }
            }

            if (!hayFijos) {
                Text("No tenés costos fijos activos. Cargalos en Costos fijos.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            Text("Apartar esta semana: ${money.format(total)}", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

            SobreSelector("Desde (cuenta)", origen, cuentas, expOrigen, { expOrigen = it }) { origen = it }

            if (fondos.isEmpty()) {
                Text("No hay sobre de fondo. Creá uno para apartar la plata:",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = nombreFondo, onValueChange = { nombreFondo = it },
                    label = { Text("Nombre del sobre fondo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.crearFondo(nombreFondo) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Crear sobre fondo")
                }
            } else {
                SobreSelector("Hacia (fondo)", destino, fondos, expDestino, { expDestino = it }) { destino = it }
                if (saldoInsuf) {
                    Text("⚠ El saldo de la cuenta ($${money.format(origen!!.saldo)}) no alcanza para $${money.format(total)}.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        val o = origen ?: return@Button
                        val d = destino ?: return@Button
                        viewModel.provisionar(o.id, d.id, usuarioId)
                    },
                    enabled = origen != null && destino != null && total > 0,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Provisionar ${money.format(total)}") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SobreSelector(
    label: String, seleccionado: Sobre?, opciones: List<Sobre>,
    expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onSelect: (Sobre) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = seleccionado?.let { "${it.nombre} (${money.format(it.saldo)})" } ?: "",
            onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            opciones.forEach { s ->
                DropdownMenuItem(text = { Text("${s.nombre} (${money.format(s.saldo)})") },
                    onClick = { onSelect(s); onExpandedChange(false) })
            }
        }
    }
}
