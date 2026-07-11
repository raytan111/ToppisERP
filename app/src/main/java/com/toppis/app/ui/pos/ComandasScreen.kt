package com.toppis.app.ui.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toppis.app.data.models.Pedido
import com.toppis.app.ui.components.ToppisTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComandasScreen(
    viewModel: ComandasViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val comandas by viewModel.comandas.collectAsState()
    val cargandoInicial by viewModel.cargandoInicial.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        topBar = { ToppisTopBar(titulo = "Cocina · Comandas", onBack = onNavigateBack) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                cargandoInicial && comandas.isEmpty() -> com.toppis.app.ui.components.SkeletonList()
                comandas.isEmpty() -> com.toppis.app.ui.components.EmptyState(
                    icon = Icons.Filled.Restaurant,
                    titulo = "Sin comandas pendientes",
                    subtitulo = "Las comandas aparecen acá cuando se cierra un pedido."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(comandas, key = { it.id }) { pedido ->
                        ComandaCard(
                            pedido = pedido,
                            clienteEtiqueta = viewModel.clienteDe(pedido)?.etiqueta ?: "Cliente",
                            onEntregar = { viewModel.marcarEntregado(pedido.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComandaCard(pedido: Pedido, clienteEtiqueta: String, onEntregar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(clienteEtiqueta, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (pedido.pagado) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Text(
                        if (pedido.pagado) "Pagado" else "No pagado",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (pedido.pagado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                pedido.comandaTexto ?: "(sin detalle)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(onClick = onEntregar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Marcar entregado")
            }
        }
    }
}
