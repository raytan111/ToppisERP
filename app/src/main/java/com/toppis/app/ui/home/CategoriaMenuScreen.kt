package com.toppis.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Sub-menú de una categoría a pantalla completa: lista las opciones de la
 * categoría. Cada opción navega a su pantalla. Botón "volver" al Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaMenuScreen(
    categoria: MenuCategoria,
    permisos: com.toppis.app.ui.auth.Permisos,
    onAbrirOpcion: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val opciones = remember(categoria, permisos) {
        categoria.opciones.filter { permisos.puedeAbrir(it.ruta) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${categoria.emoji}  ${categoria.titulo}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(opciones, key = { it.ruta }) { opcion ->
                ElevatedCard(
                    onClick = { onAbrirOpcion(opcion.ruta) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(opcion.icono, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            opcion.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
