package com.toppis.app.ui.ventas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.ItemVentaMenu
import com.toppis.app.data.models.Venta
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.ui.components.ToppisTopBar
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasHistorialScreen(
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var ventas by remember { mutableStateOf<List<Venta>>(emptyList()) }
    var detalle by remember { mutableStateOf<List<ItemVentaMenu>>(emptyList()) }
    var ventaSel by remember { mutableStateOf<Venta?>(null) }
    val money = DecimalFormat("$#,##0")

    LaunchedEffect(Unit) {
        ventas = try {
            SupabaseClient.client.postgrest.from("ventas").select()
                .decodeList<Venta>().sortedByDescending { it.fecha }
        } catch (_: Exception) { emptyList() }
    }

    Scaffold(
        topBar = { ToppisTopBar(titulo = "Historial de Ventas", onBack = onNavigateBack) }
    ) { padding ->
        if (ventas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Sin ventas registradas.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(ventas) { v ->
                    Card(modifier = Modifier.fillMaxWidth(), onClick = {
                        ventaSel = v
                        scope.launch {
                            detalle = try {
                                SupabaseClient.client.postgrest.from("items_venta_menu").select {
                                    filter { eq("venta_id", v.id) }
                                }.decodeList<ItemVentaMenu>()
                            } catch (_: Exception) { emptyList() }
                        }
                    }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Venta #${v.id}", style = MaterialTheme.typography.titleSmall)
                                Text(v.fecha?.take(16)?.replace("T", " ") ?: "",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("${v.metodoPago.name} · ${v.estado.name}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(money.format(v.total), style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    ventaSel?.let { v ->
        AlertDialog(
            onDismissRequest = { ventaSel = null; detalle = emptyList() },
            title = { Text("Venta #${v.id} — ${money.format(v.total)}") },
            text = {
                if (detalle.isEmpty()) {
                    Text("Sin detalle.", color = MaterialTheme.colorScheme.outline)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(detalle) { d ->
                            Column {
                                Text("${d.cantidad}× item #${d.itemMenuId}  ${money.format(d.subtotal)}",
                                    style = MaterialTheme.typography.bodyMedium)
                                if (d.modificadores.isNotBlank()) Text(d.modificadores,
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                if (d.salsasSeleccionadas.isNotBlank()) Text(d.salsasSeleccionadas,
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        item {
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            Text("Fecha: ${v.fecha?.take(16)?.replace("T", " ")}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("Método: ${v.metodoPago.name} · Estado: ${v.estado.name}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { ventaSel = null; detalle = emptyList() }) { Text("Cerrar") } }
        )
    }
}
