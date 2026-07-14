package com.toppis.app.ui.promociones

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.toppis.app.data.db.entities.CategoriaMenu
import com.toppis.app.data.db.entities.ModoEspacioPromo
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.ui.components.ImagePickerField
import com.toppis.app.ui.components.ToppisTopBar
import java.text.DecimalFormat

private val money = DecimalFormat("$#,##0")

/** Estado de edición de un grupo (index = null → grupo nuevo). */
private data class GrupoEnEdicion(val index: Int?, val draft: GrupoDraft)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromocionEditorScreen(
    viewModel: PromocionEditorViewModel,
    promocionId: Int?,
    onGuardado: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val cargando by viewModel.cargando.collectAsState()
    val nombre by viewModel.nombre.collectAsState()
    val precioText by viewModel.precioText.collectAsState()
    val imagenUrl by viewModel.imagenUrl.collectAsState()
    val grupos by viewModel.grupos.collectAsState()
    val itemsMenu by viewModel.itemsMenu.collectAsState()
    val error by viewModel.error.collectAsState()

    var grupoEditor by remember { mutableStateOf<GrupoEnEdicion?>(null) }

    LaunchedEffect(promocionId) { viewModel.cargar(promocionId) }
    error?.let { msg -> com.toppis.app.ui.components.ToppisErrorDialog(mensaje = msg, onDismiss = { viewModel.resetError() }) }

    Scaffold(
        topBar = { ToppisTopBar(titulo = if (promocionId == null) "Nueva promoción" else "Editar promoción", onBack = onNavigateBack) }
    ) { padding ->
        if (cargando) { com.toppis.app.ui.components.SkeletonList(); return@Scaffold }

        val editor = grupoEditor
        if (editor != null) {
            GrupoEditor(
                inicial = editor.draft,
                itemsMenu = itemsMenu,
                modifier = Modifier.padding(padding),
                onCancelar = { grupoEditor = null },
                onGuardar = { g ->
                    if (editor.index == null) viewModel.agregarGrupo(g) else viewModel.actualizarGrupo(editor.index, g)
                    grupoEditor = null
                }
            )
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ImagePickerField(
                imagenUrl = imagenUrl, carpeta = "promos",
                onImagenSubida = { viewModel.setImagen(it) }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nombre, onValueChange = { viewModel.setNombre(it) },
                label = { Text("Nombre de la promo") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = precioText, onValueChange = { viewModel.setPrecio(it) },
                label = { Text("Precio fijo (CLP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Grupos de elección", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    grupoEditor = GrupoEnEdicion(null, GrupoDraft("", 1, ModoEspacioPromo.LISTA, null, true, emptyList()))
                }) { Icon(Icons.Filled.Add, contentDescription = null); Text(" Agregar") }
            }
            if (grupos.isEmpty()) {
                Text("Agregá al menos un grupo (ej: \"Elige tu hamburguesa\").",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            grupos.forEachIndexed { i, g ->
                GrupoResumenCard(
                    g = g, itemsMenu = itemsMenu,
                    onEditar = { grupoEditor = GrupoEnEdicion(i, g) },
                    onQuitar = { viewModel.quitarGrupo(i) }
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.guardar { onGuardado() } },
                enabled = viewModel.esValido,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar promoción") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GrupoResumenCard(
    g: GrupoDraft,
    itemsMenu: List<ItemMenu>,
    onEditar: () -> Unit,
    onQuitar: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${g.cantidad}x ${g.nombre.ifBlank { "Grupo" }}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val fuente = when (g.modo) {
                    ModoEspacioPromo.CATEGORIA -> "Categoría: ${g.categoria ?: "-"}"
                    ModoEspacioPromo.LISTA -> {
                        val nombres = itemsMenu.filter { it.id in g.opciones }.joinToString(", ") { it.nombre }
                        "Opciones: ${nombres.ifBlank { "sin opciones" }}"
                    }
                }
                Text(fuente, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (g.permiteRepetir) "Permite repetir" else "Deben ser distintos",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onQuitar) { Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrupoEditor(
    inicial: GrupoDraft,
    itemsMenu: List<ItemMenu>,
    modifier: Modifier = Modifier,
    onCancelar: () -> Unit,
    onGuardar: (GrupoDraft) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial.nombre) }
    var cantidadText by remember { mutableStateOf(inicial.cantidad.toString()) }
    var permiteRepetir by remember { mutableStateOf(inicial.permiteRepetir) }
    var modo by remember { mutableStateOf(inicial.modo) }
    var categoria by remember { mutableStateOf(inicial.categoria ?: CategoriaMenu.BEBIDA_LATA.label) }
    val opciones = remember { mutableStateListOf<Int>().apply { addAll(inicial.opciones) } }
    var expCat by remember { mutableStateOf(false) }

    val cantidad = cantidadText.toIntOrNull() ?: 1
    val valido = cantidad >= 1 && (modo == ModoEspacioPromo.CATEGORIA || opciones.isNotEmpty())

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Grupo de elección", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nombre, onValueChange = { nombre = it },
            label = { Text("Nombre (ej: Elige tu hamburguesa)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = cantidadText, onValueChange = { cantidadText = it.filter { c -> c.isDigit() } },
            label = { Text("Cantidad a elegir") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Permite elegir el mismo producto repetido", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = permiteRepetir, onCheckedChange = { permiteRepetir = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = modo == ModoEspacioPromo.LISTA, onClick = { modo = ModoEspacioPromo.LISTA })
            Text("Lista de productos", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
            RadioButton(selected = modo == ModoEspacioPromo.CATEGORIA, onClick = { modo = ModoEspacioPromo.CATEGORIA })
            Text("Categoría", style = MaterialTheme.typography.bodySmall)
        }

        if (modo == ModoEspacioPromo.CATEGORIA) {
            ExposedDropdownMenuBox(expanded = expCat, onExpandedChange = { expCat = !expCat }) {
                OutlinedTextField(
                    value = categoria, onValueChange = {}, readOnly = true,
                    label = { Text("Categoría elegible") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCat) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expCat, onDismissRequest = { expCat = false }) {
                    CategoriaMenu.entries.forEach { c ->
                        DropdownMenuItem(text = { Text(c.label) }, onClick = { categoria = c.label; expCat = false })
                    }
                }
            }
            val incluidos = itemsMenu.filter { CategoriaMenu.porLabel(it.categoria) == CategoriaMenu.porLabel(categoria) }
            Text("Incluye ${incluidos.size} producto(s) de esa categoría (se actualiza solo).",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(incluidos, key = { it.id }) { item -> ItemMiniCard(item, seleccionado = false, onClick = {}) }
            }
        } else {
            Text("Tocá los productos que entran en este grupo:", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(itemsMenu, key = { it.id }) { item ->
                    ItemMiniCard(item, seleccionado = item.id in opciones, onClick = {
                        if (item.id in opciones) opciones.remove(item.id) else opciones.add(item.id)
                    })
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(
                onClick = {
                    onGuardar(GrupoDraft(
                        nombre = nombre, cantidad = cantidad, modo = modo,
                        categoria = if (modo == ModoEspacioPromo.CATEGORIA) categoria else null,
                        permiteRepetir = permiteRepetir,
                        opciones = if (modo == ModoEspacioPromo.LISTA) opciones.toList() else emptyList()
                    ))
                },
                enabled = valido, modifier = Modifier.weight(1f)
            ) { Text("Listo") }
        }
    }
}

@Composable
private fun ItemMiniCard(item: ItemMenu, seleccionado: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .then(if (seleccionado) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(64.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (item.imagenUrl != null) {
                    AsyncImage(model = item.imagenUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (seleccionado) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Elegido",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface))
                }
            }
            Text(item.nombre, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        }
    }
}
