package com.toppis.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pantalla principal (Home) — menú de la app, sin bottom bar.
 *
 * - Hero POS con gradiente de marca: entrada veloz y de alto impacto.
 * - Grilla de tarjetas de categoría, cada una con su color de acento.
 * - Logout en la TopBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    permisos: com.toppis.app.ui.auth.Permisos,
    onAbrirPos: () -> Unit,
    onAbrirCategoria: (String) -> Unit,
    onAbrirSobres: () -> Unit,
    onLogout: () -> Unit
) {
    val localNombre by com.toppis.app.data.repository.LocalSession.activoNombre.collectAsState()
    // Solo categorías que tengan al menos una opción visible para este rol.
    val categorias = remember(permisos) {
        CATEGORIAS_MENU.filter { cat ->
            cat.opciones.any { permisos.puedeAbrir(it.ruta) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.toppis.erp.R.drawable.toppis_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "ToppisERP",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (localNombre != null) {
                                Text(
                                    text = localNombre!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
            Spacer(Modifier.height(8.dp))

            // ── Hero POS con gradiente ────────────────────────────────────
            HeroPos(onClick = onAbrirPos)

            // ── Atajo a Sobres (dinero en caja) ───────────────────────────
            if (permisos.puedeAbrir("sobres")) {
                Spacer(Modifier.height(12.dp))
                AtajoSobres(onClick = onAbrirSobres)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Gestión",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            // ── Tarjetas de categoría ─────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(categorias, key = { it.id }) { cat ->
                    val visibles = cat.opciones.count { permisos.puedeAbrir(it.ruta) }
                    CategoriaCard(
                        cat = cat,
                        visibles = visibles,
                        accent = accentDeCategoria(cat.id),
                        onClick = { onAbrirCategoria(cat.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroPos(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val gradiente = Brush.linearGradient(
        colors = listOf(cs.primary, cs.tertiary)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = cs.primary
            )
            .clip(RoundedCornerShape(28.dp))
            .background(gradiente)
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                "VENTA",
                style = MaterialTheme.typography.displaySmall,
                color = cs.onPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Punto de venta · entrada rápida",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onPrimary.copy(alpha = 0.85f)
            )
        }
        // Ícono grande semitransparente a la derecha
        Icon(
            imageVector = Icons.Filled.PointOfSale,
            contentDescription = null,
            tint = cs.onPrimary.copy(alpha = 0.28f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(96.dp)
        )
        // Chip de flecha "ir"
        Surface(
            shape = CircleShape,
            color = cs.onPrimary.copy(alpha = 0.22f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = cs.onPrimary
                )
            }
        }
    }
}

@Composable
private fun AtajoSobres(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2E7D32).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Sobres", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Dinero en caja · acceso rápido",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoriaCard(
    cat: MenuCategoria,
    visibles: Int,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Contenedor de ícono con acento de color
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = cat.icono,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    cat.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$visibles opciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


