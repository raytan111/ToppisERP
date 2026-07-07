package com.toppis.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.toppis.app.data.repository.StorageRepository
import kotlinx.coroutines.launch

/**
 * Campo para elegir/subir una imagen (desde la galería) a Supabase Storage.
 * Muestra la imagen actual o un placeholder; al elegir una, la sube y devuelve
 * la URL pública en [onImagenSubida].
 */
@Composable
fun ImagePickerField(
    imagenUrl: String?,
    carpeta: String = "items",
    onImagenSubida: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { StorageRepository() }
    var subiendo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                subiendo = true; error = null
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes == null) {
                        error = "No se pudo leer la imagen."
                    } else if (bytes.size > 5_000_000) {
                        error = "La imagen es muy pesada (máx. 5 MB)."
                    } else {
                        val url = repo.subirImagen(bytes, carpeta = carpeta)
                        onImagenSubida(url)
                    }
                } catch (e: Exception) {
                    error = e.message ?: "Error al subir la imagen."
                } finally {
                    subiendo = false
                }
            }
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(Modifier),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.matchParentSize()
            ) {}
            if (imagenUrl != null) {
                AsyncImage(
                    model = imagenUrl,
                    contentDescription = "Imagen",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp))
                )
            } else {
                Icon(
                    Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (subiendo) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = { launcher.launch("image/*") }, enabled = !subiendo) {
            Text(if (imagenUrl == null) "Agregar foto" else "Cambiar foto")
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
