package com.toppis.app.ui.promociones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.toppis.app.data.db.entities.CategoriaMenu
import com.toppis.app.data.db.entities.ModoEspacioPromo
import com.toppis.app.data.models.PromocionEspacio
import com.toppis.app.data.models.PromocionEspacioOpcion
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.toppis.app.data.db.entities.TipoPromocion
import com.toppis.app.data.models.AnalisisPromocion
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Promocion
import com.toppis.app.data.repository.PromocionItemDetalle
import com.toppis.app.ui.components.ImagePickerField
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

@Composable
private fun PromoThumb(url: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(56.dp)
    ) {
        if (url != null) {
            AsyncImage(
                model = url, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Agregar foto", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val money = DecimalFormat("$#,##0")
private val pct = DecimalFormat("0.#")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromocionesScreen(
    viewModel: PromocionViewModel,
    puedeBorrar: Boolean = true,
    onNuevaPromo: () -> Unit = {},
    onEditarPromo: (Int) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val promociones by viewModel.promociones.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var promoAEliminar by remember { mutableStateOf<Promocion?>(null) }
    var fotoDe by remember { mutableStateOf<Promocion?>(null) }

    // Recargar al volver del editor.
    LaunchedEffect(Unit) { viewModel.recargar() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PromocionUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as PromocionUiState.Error).message)
                viewModel.resetState()
            }
            PromocionUiState.Success -> viewModel.resetState()
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ToppisTopBar(titulo = "Promociones", onBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaPromo) {
                Icon(Icons.Filled.Add, contentDescription = "Crear promoción")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (promociones.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin promociones.\nUsá el botón + para agregar.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(promociones) { promo ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onEditarPromo(promo.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PromoThumb(promo.imagenUrl, onClick = { fotoDe = promo })
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(promo.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        promo.tipo.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    val detalle = when (promo.tipo) {
                                        TipoPromocion.COMBO -> "Precio ${money.format(promo.precio)}"
                                        TipoPromocion.DESCUENTO_PORCENTAJE -> "Descuento ${pct.format(promo.descuentoPct)}%"
                                    }
                                    Text(
                                        detalle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onEditarPromo(promo.id) }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (puedeBorrar) {
                                    IconButton(onClick = { promoAEliminar = promo }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fotoDe?.let { promo ->
        AlertDialog(
            onDismissRequest = { fotoDe = null },
            title = { Text("Foto de ${promo.nombre}") },
            text = {
                ImagePickerField(
                    imagenUrl = promo.imagenUrl,
                    carpeta = "promos",
                    onImagenSubida = { url ->
                        viewModel.actualizarImagen(promo.id, url)
                        fotoDe = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { fotoDe = null }) { Text("Cerrar") } }
        )
    }

    promoAEliminar?.let { promo ->
        com.toppis.app.ui.components.ToppisDeleteDialog(
            nombre = promo.nombre,
            titulo = "Eliminar promoción",
            onConfirm = { viewModel.eliminarPromocion(promo.id); promoAEliminar = null },
            onDismiss = { promoAEliminar = null }
        )
    }
}
