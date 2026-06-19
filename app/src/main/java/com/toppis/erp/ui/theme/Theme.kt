package com.toppis.erp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * Tema de la app basado en el color de marca configurable (ThemeManager).
 *
 * A partir de un único color semilla (elegido por el admin en hex "#"), se
 * genera todo el esquema Material 3 (primary, secondary, tertiary, containers,
 * superficies) tanto en claro como en oscuro, usando MaterialKolor.
 */
@Composable
fun ToppisERPTheme(
    content: @Composable () -> Unit
) {
    val seedHex by ThemeManager.seedHex.collectAsState()
    val modo by ThemeManager.modoOscuro.collectAsState()

    val systemDark = isSystemInDarkTheme()
    val dark = when (modo) {
        1 -> false
        2 -> true
        else -> systemDark
    }

    val seed: Color = ThemeManager.hexToColor(seedHex)
        ?: ThemeManager.hexToColor(ThemeManager.SEED_POR_DEFECTO)!!

    val colorScheme = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = dark,
        isAmoled = false,
        style = PaletteStyle.Vibrant
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
