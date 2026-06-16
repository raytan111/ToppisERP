package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Registro de pesos de papa por etapa (tabla "papa_rendimientos").
 * Formulario para cargar mediciones; rendimiento = peso_frito / peso_crudo.
 */
@Serializable
data class PapaRendimiento(
    val id: Int = 0,
    @SerialName("articulo_id")
    val articuloId: Int,
    val fecha: String? = null,
    @SerialName("peso_crudo")
    val pesoCrudo: Double = 0.0,
    @SerialName("peso_pelado")
    val pesoPelado: Double = 0.0,
    @SerialName("peso_prefrito")
    val pesoPrefrito: Double = 0.0,
    @SerialName("peso_frito")
    val pesoFrito: Double = 0.0,
    val rendimiento: Double = 0.0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
