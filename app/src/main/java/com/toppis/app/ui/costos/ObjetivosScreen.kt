package com.toppis.app.ui.costos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjetivosScreen(
    viewModel: ObjetivosViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val objetivos by viewModel.objetivos.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val guardado by viewModel.guardado.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val num = DecimalFormat("0.#")

    // Campos en porcentaje visible (32, 30, 10). Se inicializan al cargar.
    var foodTxt by remember(objetivos) { mutableStateOf(num.format(objetivos.pctFood * 100)) }
    var moTxt by remember(objetivos) { mutableStateOf(num.format(objetivos.pctManoObra * 100)) }
    var arriendoTxt by remember(objetivos) { mutableStateOf(num.format(objetivos.pctArriendoTecho * 100)) }
    var umbralTxt by remember(objetivos) { mutableStateOf(if (objetivos.umbralContratarMo == 0.0) "" else objetivos.umbralContratarMo.toLong().toString()) }

    LaunchedEffect(guardado) {
        if (guardado) { snackbarHostState.showSnackbar("Objetivos guardados"); viewModel.resetGuardado() }
    }

    fun pct(t: String): Double = ((t.replace(",", ".").toDoubleOrNull() ?: 0.0) / 100.0).coerceIn(0.0, 1.0)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Objetivos y semáforos", onBack = onNavigateBack) }
    ) { padding ->
        if (cargando) {
            com.toppis.app.ui.components.SkeletonList()
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Definí los porcentajes objetivo. Los semáforos del resultado semanal se ponen en rojo cuando se superan.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CampoPct("Food cost máximo (%)", foodTxt) { foodTxt = it }
            CampoPct("Mano de obra máxima (%)", moTxt) { moTxt = it }
            CampoPct("Arriendo máximo (%)", arriendoTxt) { arriendoTxt = it }
            OutlinedTextField(
                value = umbralTxt, onValueChange = { umbralTxt = it },
                label = { Text("Umbral por persona para contratar (CLP)") },
                supportingText = { Text("Cuando la mano de obra por persona alcanza esto, avisa que se puede contratar. 0 = siempre.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.guardar(
                        pct(foodTxt), pct(moTxt), pct(arriendoTxt),
                        umbralTxt.replace(",", ".").toDoubleOrNull() ?: 0.0
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("GUARDAR") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoPct(label: String, valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor, onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
}
