package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Objetivo/parámetro configurable de control de costos (tabla "config_costos").
 * Clave/valor por local. Ej: "pct_food_objetivo" = 0.32.
 */
@Serializable
data class ConfigCosto(
    val clave: String,
    val valor: Double,
    @SerialName("local_id")
    val localId: Int = 0
)
