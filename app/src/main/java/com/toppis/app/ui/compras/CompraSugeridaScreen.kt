package com.toppis.app.ui.compras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompraSugeridaScreen(
    viewModel: CompraSugeridaViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val sugerencias by viewModel.sugerencias.collectAsState()
    val costoTotal by viewModel.costoTotal.collectAsState()
    val money = DecimalFormat("$#,##0")
    val num = DecimalFormat("#,##0.##")

    Scaffold(
        topBar = { ToppisTopBar(titulo = "Compra Sugerida", onBack = onNavigateBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Costo estimado de la compra", style = MaterialTheme.typography.labelMedium)
                    Text(money.format(costoTotal), style = MaterialTheme.typography.headlineSmall)
                    Text("${sugerencias.size} artículo(s) bajo el stock mínimo",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            if (sugerencias.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Todo el stock está sobre el mínimo. 👍", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sugerencias) { s ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(s.articulo.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text("Comprar ~${num.format(s.faltanteCompra)} ${s.unidadCompra}",
                                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    Text("para llegar al mínimo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Text(money.format(s.costoEstimado), style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}
