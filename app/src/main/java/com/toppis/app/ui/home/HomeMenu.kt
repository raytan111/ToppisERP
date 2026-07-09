package com.toppis.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Definición declarativa de la navegación tipo menú (sin bottom bar).
 *
 * - [MenuOpcion]: una pantalla concreta (ruta de navegación).
 * - [MenuCategoria]: agrupa opciones bajo una tarjeta del Home.
 *
 * El POS ("Venta") es un acceso directo destacado del Home y no vive en una
 * categoría.
 */
data class MenuOpcion(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector,
    val soloAdmin: Boolean = false
)

data class MenuCategoria(
    val id: String,            // ruta de la categoría, ej. "cat_cocina"
    val titulo: String,
    val emoji: String,
    val icono: ImageVector,
    val soloAdmin: Boolean = false,
    val opciones: List<MenuOpcion>
)

/** Ruta del acceso directo al POS. */
const val RUTA_POS = "pos"

/** Todas las categorías del menú principal, en orden de presentación. */
val CATEGORIAS_MENU: List<MenuCategoria> = listOf(
    MenuCategoria(
        id = "cat_cocina",
        titulo = "Cocina",
        emoji = "🍔",
        icono = Icons.Filled.Restaurant,
        opciones = listOf(
            MenuOpcion("menu_config", "Configurar Menú", Icons.Filled.Restaurant),
            MenuOpcion("preparaciones", "Preparaciones", Icons.Filled.Blender),
            MenuOpcion("modificadores", "Modificadores", Icons.Filled.Tune),
            MenuOpcion("promociones", "Promociones", Icons.Filled.LocalOffer, soloAdmin = true),
            MenuOpcion("food_cost", "Food Cost & Menú", Icons.Filled.PieChart)
        )
    ),
    MenuCategoria(
        id = "cat_inventario",
        titulo = "Inventario",
        emoji = "📦",
        icono = Icons.Filled.Inventory,
        opciones = listOf(
            MenuOpcion("inventario", "Artículos (stock)", Icons.AutoMirrored.Filled.List),
            MenuOpcion("mermas", "Mermas", Icons.Filled.DeleteSweep),
            MenuOpcion("conteos", "Conteo de Inventario", Icons.Filled.Inventory),
            MenuOpcion("compra_sugerida", "Compra Sugerida", Icons.Filled.ShoppingCart),
            MenuOpcion("variance", "Análisis de Inventario", Icons.Filled.Analytics),
            MenuOpcion("compras", "Compras", Icons.Filled.AddShoppingCart),
            MenuOpcion("proveedores", "Proveedores", Icons.Filled.LocalShipping)
        )
    ),
    MenuCategoria(
        id = "cat_fondos",
        titulo = "Fondos",
        emoji = "💰",
        icono = Icons.Filled.AccountBalance,
        opciones = listOf(
            MenuOpcion("sobres", "Sobres", Icons.Filled.AccountBalance),
            MenuOpcion("arqueo", "Arqueo de Caja", Icons.Filled.PointOfSale),
            MenuOpcion("flujo_caja", "Flujo de Caja", Icons.AutoMirrored.Filled.ShowChart),
            MenuOpcion("contabilidad", "Contabilidad", Icons.Filled.Calculate),
            MenuOpcion("reportes", "Reportes", Icons.Filled.BarChart)
        )
    ),
    MenuCategoria(
        id = "cat_personal",
        titulo = "Personal",
        emoji = "👥",
        icono = Icons.Filled.Groups,
        opciones = listOf(
            MenuOpcion("empleados", "Empleados", Icons.Filled.Badge),
            MenuOpcion("mano_obra", "Mano de Obra / Prime Cost", Icons.Filled.Groups)
        )
    ),
    MenuCategoria(
        id = "cat_costos",
        titulo = "Costos",
        emoji = "📊",
        icono = Icons.Filled.Savings,
        soloAdmin = true,
        opciones = listOf(
            MenuOpcion("rutina_semanal", "Rutina de cierre", Icons.Filled.Checklist, soloAdmin = true),
            MenuOpcion("cierre_semanal", "Resultado semanal", Icons.Filled.CalendarMonth, soloAdmin = true),
            MenuOpcion("costos_fijos", "Costos fijos", Icons.Filled.Receipt, soloAdmin = true),
            MenuOpcion("objetivos_costos", "Objetivos y semáforos", Icons.Filled.Flag, soloAdmin = true),
            MenuOpcion("ayuda_costos", "Cómo usar / Ayuda", Icons.Filled.HelpOutline, soloAdmin = true),
            MenuOpcion("gastos", "Costos puntuales", Icons.Filled.AttachMoney, soloAdmin = true),
            MenuOpcion("sobres", "Sobres", Icons.Filled.AccountBalance, soloAdmin = true),
            MenuOpcion("flujo_caja", "Flujo de caja", Icons.AutoMirrored.Filled.ShowChart, soloAdmin = true)
        )
    ),
    MenuCategoria(
        id = "cat_admin",
        titulo = "Administración",
        emoji = "⚙️",
        icono = Icons.Filled.Settings,
        soloAdmin = true,
        opciones = listOf(
            MenuOpcion("usuarios", "Usuarios", Icons.Filled.Group, soloAdmin = true),
            MenuOpcion("locales", "Locales", Icons.Filled.Store, soloAdmin = true),
            MenuOpcion("asignaciones_local", "Usuarios por Local", Icons.Filled.SupervisedUserCircle, soloAdmin = true),
            MenuOpcion("kpis", "KPIs Ejecutivos", Icons.Filled.Dashboard, soloAdmin = true),
            MenuOpcion("ventas_historial", "Historial de Ventas", Icons.Filled.History),
            MenuOpcion("comprobantes", "Comprobantes", Icons.Filled.Receipt),
            MenuOpcion("config_color", "Configurar Colores", Icons.Filled.Palette, soloAdmin = true),
            MenuOpcion("exportacion", "Exportación", Icons.Filled.FileDownload, soloAdmin = true)
        )
    )
)

/** Busca una categoría por su id de ruta. */
fun categoriaPorId(id: String?): MenuCategoria? = CATEGORIAS_MENU.firstOrNull { it.id == id }

/** Color de acento por categoría (paleta apetitosa y contrastada). */
fun accentDeCategoria(id: String): Color = when (id) {
    "cat_cocina" -> Color(0xFFF4511E)      // naranja brasa
    "cat_inventario" -> Color(0xFF00897B)  // teal
    "cat_fondos" -> Color(0xFF2E7D32)      // verde dinero
    "cat_personal" -> Color(0xFF5E35B1)    // violeta
    "cat_costos" -> Color(0xFF1565C0)      // azul control
    "cat_admin" -> Color(0xFF455A64)       // slate
    else -> Color(0xFFE63946)              // rojo marca
}
