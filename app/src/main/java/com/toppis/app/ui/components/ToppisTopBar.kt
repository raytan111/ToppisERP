package com.toppis.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * TopAppBar reutilizable para todo el proyecto.
 *
 * @param titulo  Título que se muestra en la barra.
 * @param onBack  Si no es null se muestra el ícono de volver; omitir en pantallas raíz.
 * @param actions Bloque opcional para botones adicionales a la derecha (RowScope).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToppisTopBar(
    titulo: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(titulo) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            }
        },
        actions = actions
    )
}



