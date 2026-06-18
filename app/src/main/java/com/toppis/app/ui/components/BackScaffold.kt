package com.toppis.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Scaffold simple con TopAppBar (título + botón volver) para envolver pantallas
 * que antes dependían del bottom bar / MainScaffold para su barra superior.
 *
 * Si hay un local activo, lo muestra como subtítulo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackScaffold(
    titulo: String,
    onNavigateBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val localNombre by com.toppis.app.data.repository.LocalSession.activoNombre.collectAsState()
    val tituloFinal = if (localNombre != null) "$titulo · $localNombre" else titulo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tituloFinal) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        content = content
    )
}
