package com.toppis.app.ui.costos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toppis.app.ui.components.ToppisTopBar

/**
 * Ayuda / tutorial del módulo de Control de Costos. Contenido estático (sin ViewModel):
 * explica la configuración inicial y la rutina semanal para operar y probar.
 */
@Composable
fun AyudaCostosScreen(onNavigateBack: () -> Unit = {}) {
    Scaffold(
        topBar = { ToppisTopBar(titulo = "Cómo usar los Costos", onBack = onNavigateBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IntroCard()

            SeccionTitulo("Parte 1 · Configuración inicial", "Se hace una sola vez")

            PasoCard(
                icono = Icons.Filled.Category,
                titulo = "1. Categorizá tus artículos",
                cuerpo = "En Inventario, al crear o editar cada artículo elegí su categoría:\n" +
                    "• Ingredientes: lo que va en las recetas (pan, carne, tocino).\n" +
                    "• Packaging: cajas, bolsas, servilletas.\n" +
                    "• Insumos: limpieza, gas de cocina, etc.\n\n" +
                    "Así el sistema sabe qué es costo variable de comida."
            )
            PasoCard(
                icono = Icons.Filled.Flag,
                titulo = "2. Definí tus objetivos",
                cuerpo = "En Costos → Objetivos y semáforos (ya vienen por defecto):\n" +
                    "• Food cost: 32%\n" +
                    "• Mano de obra: 30%\n" +
                    "• Arriendo (techo): 10%\n" +
                    "• Umbral para contratar: dejalo en 0 por ahora.\n\n" +
                    "Estos porcentajes prenden los semáforos (verde = vas bien, rojo = te pasaste)."
            )
            PasoCard(
                icono = Icons.Filled.Receipt,
                titulo = "3. Cargá tus costos fijos",
                cuerpo = "En Costos → Costos fijos, agregá cada uno con nombre, monto y periodicidad:\n" +
                    "• Luz, gas, arriendo → Mensual.\n" +
                    "• Sueldos fijos → Mensual.\n\n" +
                    "El sistema prorratea solo la parte de la semana. Ej: luz $60.000/mes ≈ $13.850/semana.\n\n" +
                    "⚠ No dupliques: los pagos por turno/hora van por Jornadas, no acá."
            )
            PasoCard(
                icono = Icons.Filled.AccountBalance,
                titulo = "4. Preparate los sobres",
                cuerpo = "Confirmá tus cuentas Efectivo y Tarjeta. El sobre FONDO para apartar la plata de los fijos lo podés crear después, directo desde la rutina de cierre (paso 3)."
            )

            SeccionTitulo("Parte 2 · Rutina semanal", "Lunes a sábado se opera, se cierra el sábado/domingo")

            PasoCard(
                icono = Icons.Filled.ShoppingCart,
                titulo = "Durante la semana",
                cuerpo = "• Ventas: por el POS, como siempre.\n" +
                    "• Compras: poné cuánto pagaste de verdad. Si un precio cambió (te subió el tocino), el costo del artículo se actualiza solo y recalcula las recetas. Si es el mismo precio, no cambia nada.\n" +
                    "• Costos variables: bencina o algo suelto, en Costos → Costos variables."
            )
            PasoCard(
                icono = Icons.Filled.Checklist,
                titulo = "Al cerrar la semana: Rutina de cierre",
                cuerpo = "Checklist de 4 pasos:\n" +
                    "1. Conteo de inventario: contás el stock real.\n" +
                    "2. Registro de mermas: lo que se botó o echó a perder.\n" +
                    "3. Provisión de fijos: el sistema te dice cuánto apartar. Elegís de qué cuenta sale y hacia qué sobre FONDO va, y tocás Provisionar (mueve la plata de verdad para no gastarla).\n" +
                    "4. Ver resultado semanal."
            )
            PasoCard(
                icono = Icons.Filled.Assessment,
                titulo = "Leer el Resultado semanal",
                cuerpo = "La foto de la semana:\n" +
                    "• Lo que queda = ventas − variables − mano de obra − fijos.\n" +
                    "• Semáforos de food / mano de obra / arriendo vs tus objetivos.\n" +
                    "• Break-even: cuánto vender para no perder y cuánto te falta.\n" +
                    "• Mano de obra disponible: cuánto podés gastar en sueldos (te dice si podés contratar).\n\n" +
                    "Cuando la semana terminó, tocá Confirmar cierre: congela los números (snapshot) para que el historial no se descuadre aunque cambien precios."
            )

            ResumenCard()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("💰 Control de Costos", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "Cada semana (lunes a sábado) el sistema calcula cuánto te queda:\n" +
                    "ventas − costos variables − mano de obra − costos fijos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SeccionTitulo(titulo: String, subtitulo: String) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitulo, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PasoCard(icono: ImageVector, titulo: String, cuerpo: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(cuerpo, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResumenCard() {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tu semana en resumen", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                "Mar–Sáb: vender · comprar (precio real) · costos variables\n" +
                    "Sáb/Dom: Rutina → conteo → mermas → provisión → resultado → confirmar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
