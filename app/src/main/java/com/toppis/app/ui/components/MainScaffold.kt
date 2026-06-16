package com.toppis.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/**
 * Scaffold principal que envuelve todas las pantallas de la app.
 * Provee NavigationBar consistente y ModalBottomSheet "Más".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    currentRoute: String?,
    isAdmin: Boolean,
    navController: NavHostController,
    onLogout: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    var showMoreSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // TopBar solo en pantallas principales
            when (currentRoute) {
                "dashboard" -> ToppisTopBar(titulo = "Dashboard")
                "sobres" -> ToppisTopBar(titulo = "💰 Sobres")
                "pos" -> ToppisTopBar(titulo = "Punto de Venta")
                "inventario" -> ToppisTopBar(titulo = "Inventario")
                "gastos" -> ToppisTopBar(titulo = "Gastos")
                "reportes" -> ToppisTopBar(titulo = "Reportes")
            }
        },
        bottomBar = {
            NavigationBar {
                // 1. Home / Dashboard
                NavigationBarItem(
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )

                // 2. Sobres
                NavigationBarItem(
                    selected = currentRoute == "sobres",
                    onClick = {
                        navController.navigate("sobres") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.AccountBalance, contentDescription = "Sobres") },
                    label = { Text("Sobres") }
                )

                // 3. Venta / POS
                NavigationBarItem(
                    selected = currentRoute == "pos",
                    onClick = {
                        navController.navigate("pos") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Venta") },
                    label = { Text("Venta") }
                )

                // 4. Inventario
                NavigationBarItem(
                    selected = currentRoute == "inventario",
                    onClick = {
                        navController.navigate("inventario") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Inventario") },
                    label = { Text("Inventario") }
                )

                // 5. Más (abre sheet)
                NavigationBarItem(
                    selected = currentRoute in listOf("gastos", "reportes", "usuarios", "menu_config", "flujo_caja", "exportacion"),
                    onClick = { showMoreSheet = true },
                    icon = { Icon(Icons.Filled.MoreVert, contentDescription = "Más") },
                    label = { Text("Más") }
                )
            }
        }
    ) { padding ->
        content(padding)

        // ModalBottomSheet para opciones adicionales
        if (showMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Más opciones",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Opciones para todos los usuarios
                    MoreMenuItem(
                        icon = Icons.Filled.AttachMoney,
                        label = "Gastos",
                        onClick = {
                            showMoreSheet = false
                            navController.navigate("gastos") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    MoreMenuItem(
                        icon = Icons.Filled.BarChart,
                        label = "Reportes",
                        onClick = {
                            showMoreSheet = false
                            navController.navigate("reportes") {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    MoreMenuItem(
                        icon = Icons.Filled.Receipt,
                        label = "Comprobantes",
                        onClick = {
                            showMoreSheet = false
                            navController.navigate("comprobantes") {
                                launchSingleTop = true
                            }
                        }
                    )

                    // Opciones solo para Admin
                    if (isAdmin) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        MoreMenuItem(
                            icon = Icons.Filled.Group,
                            label = "Usuarios",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("usuarios") {
                                    launchSingleTop = true
                                }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.Restaurant,
                            label = "Configurar Menú",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("menu_config") {
                                    launchSingleTop = true
                                }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.Blender,
                            label = "Preparaciones",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("preparaciones") { launchSingleTop = true }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.Tune,
                            label = "Modificadores",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("modificadores") { launchSingleTop = true }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.LocalOffer,
                            label = "Promociones",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("promociones") { launchSingleTop = true }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.Scale,
                            label = "Rendimiento Papa",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("papa_rendimientos") { launchSingleTop = true }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.PieChart,
                            label = "Food Cost & Menú",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("food_cost") { launchSingleTop = true }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.DeleteSweep,
                            label = "Mermas",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("mermas") { launchSingleTop = true }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.ShowChart,
                            label = "Flujo de Caja",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("flujo_caja") {
                                    launchSingleTop = true
                                }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.Calculate,
                            label = "Contabilidad",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("contabilidad") {
                                    launchSingleTop = true
                                }
                            }
                        )

                        MoreMenuItem(
                            icon = Icons.Filled.FileDownload,
                            label = "Exportación",
                            onClick = {
                                showMoreSheet = false
                                navController.navigate("exportacion") {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Cerrar sesión (siempre visible)
                    MoreMenuItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        label = "Cerrar Sesión",
                        onClick = {
                            showMoreSheet = false
                            onLogout()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Item del menú "Más" en el ModalBottomSheet.
 */
@Composable
private fun MoreMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
