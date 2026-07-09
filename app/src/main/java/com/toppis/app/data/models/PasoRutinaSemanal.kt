package com.toppis.app.data.models

import com.toppis.app.data.db.entities.PasoRutina
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Estado de un paso de la rutina semanal de cierre (tabla "pasos_rutina_semanal").
 */
@Serializable
data class PasoRutinaSemanal(
    val id: Int = 0,
    @SerialName("semana_inicio")
    val semanaInicio: String,          // yyyy-MM-dd (lunes)
    val paso: PasoRutina,
    val completado: Boolean = false,
    @SerialName("completado_at")
    val completadoAt: String? = null,
    @SerialName("local_id")
    val localId: Int? = null
)
