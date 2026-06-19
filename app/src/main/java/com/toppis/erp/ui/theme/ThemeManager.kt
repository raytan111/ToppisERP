package com.toppis.erp.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestiona el tema de marca: color semilla (en hex "#RRGGBB") y modo de
 * apariencia. Persiste en SharedPreferences y expone StateFlow para que el
 * tema reaccione en vivo a los cambios.
 *
 * Modo: 0 = según el sistema, 1 = claro, 2 = oscuro.
 */
object ThemeManager {
    private const val PREFS = "toppis_theme"
    private const val KEY_SEED = "seed_hex"
    private const val KEY_MODO = "modo_oscuro"

    /** Color de marca por defecto (rojo apetitoso para hamburguesería). */
    const val SEED_POR_DEFECTO = "#E63946"

    private var prefs: SharedPreferences? = null

    private val _seedHex = MutableStateFlow(SEED_POR_DEFECTO)
    val seedHex: StateFlow<String> = _seedHex.asStateFlow()

    private val _modoOscuro = MutableStateFlow(0)
    val modoOscuro: StateFlow<Int> = _modoOscuro.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _seedHex.value = p.getString(KEY_SEED, SEED_POR_DEFECTO) ?: SEED_POR_DEFECTO
        _modoOscuro.value = p.getInt(KEY_MODO, 0)
    }

    /** Guarda el color semilla. Acepta "#RGB", "#RRGGBB" o "#AARRGGBB". */
    fun setSeed(hex: String) {
        val normalizado = normalizarHex(hex) ?: return
        _seedHex.value = normalizado
        prefs?.edit()?.putString(KEY_SEED, normalizado)?.apply()
    }

    fun setModo(modo: Int) {
        _modoOscuro.value = modo
        prefs?.edit()?.putInt(KEY_MODO, modo)?.apply()
    }

    fun reset() {
        setSeed(SEED_POR_DEFECTO)
        setModo(0)
    }

    /** Normaliza un hex a la forma "#RRGGBB" en mayúsculas, o null si es inválido. */
    fun normalizarHex(input: String): String? {
        var h = input.trim().removePrefix("#").uppercase()
        if (!h.matches(Regex("^[0-9A-F]{3}$|^[0-9A-F]{6}$|^[0-9A-F]{8}$"))) return null
        if (h.length == 3) h = h.map { "$it$it" }.joinToString("") // #RGB -> #RRGGBB
        if (h.length == 8) h = h.substring(2) // descartar alpha
        return "#$h"
    }

    /** Convierte un hex válido a Color, o null. */
    fun hexToColor(input: String): Color? {
        val norm = normalizarHex(input) ?: return null
        val rgb = norm.removePrefix("#")
        return try {
            Color(android.graphics.Color.parseColor("#$rgb"))
        } catch (_: Exception) { null }
    }
}
