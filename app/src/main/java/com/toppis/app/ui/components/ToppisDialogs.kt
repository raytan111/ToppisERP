package com.toppis.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Diálogos unificados de la app: mismo estilo, íconos y botones en todas las
 * pantallas. Evita que cada pantalla arme su propio AlertDialog a mano.
 */

/** Diálogo de error (operación que falló). Un solo botón para cerrar. */
@Composable
fun ToppisErrorDialog(
    mensaje: String,
    onDismiss: () -> Unit,
    titulo: String = "No se pudo completar"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido") }
        }
    )
}

/** Diálogo de confirmación genérico (acción reversible o neutra). */
@Composable
fun ToppisConfirmDialog(
    titulo: String,
    mensaje: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    textoConfirmar: String = "Confirmar",
    textoCancelar: String = "Cancelar",
    icono: ImageVector? = null,
    confirmarHabilitado: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icono?.let { { Icon(it, contentDescription = null) } },
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmarHabilitado) { Text(textoConfirmar) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(textoCancelar) }
        }
    )
}

/**
 * Diálogo de confirmación de borrado: ícono de basurero y botón destacado en
 * color de error. Para eliminaciones (acción destructiva).
 */
@Composable
fun ToppisDeleteDialog(
    nombre: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    titulo: String = "Eliminar",
    mensaje: String? = null,
    confirmarHabilitado: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(titulo) },
        text = { Text(mensaje ?: "¿Eliminar \"$nombre\"? Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmarHabilitado
            ) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
