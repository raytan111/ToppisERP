package com.toppis.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla principal (Home) — menú de la app, sin bottom bar.
 *
 * - Botón grande y destacado para "Venta" (POS): entrada veloz.
 * - Grilla de tarjetas pequeñas: una por categoría.
 * - Logout en la TopBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isAdmin: Boolean,
    onAbrirPos: () -> Unit,
    onAbrirCategoria: (String) -> Unit,
    onLogout: () -> Unit
) {
    val localNombre by com.toppis.app.data.repository.LocalSession.activoNombre.collectAsState()
    val categorias = remember(isAdmin) {
        CATEGORIAS_MENU.filter { isAdmin || !it.soloAdmin }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ToppisERP", fontWeight = FontWeight.Bold)
                        if (localNombre != null) {
                            Text(
                                text = localNombre!!,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Acceso directo destacado: POS ─────────────────────────────
            ElevatedButton(
                onClick = onAbrirPos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Venta", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Punto de Venta · entrada rápida", fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Gestión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            // ── Tarjetas de categoría (más pequeñas) ──────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(categorias, key = { it.id }) { cat ->
                    CategoriaCard(cat = cat, onClick = { onAbrirCategoria(cat.id) })
                }
            }
        }
    }
}

@Composable
private fun CategoriaCard(cat: MenuCategoria, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(cat.emoji, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                cat.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${cat.opciones.size} opciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
