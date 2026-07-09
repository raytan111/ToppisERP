package com.toppis.app.domain.costos

import com.toppis.app.data.db.entities.EstadoCierre
import com.toppis.app.data.db.entities.EstadoSemaforo
import com.toppis.app.data.util.SemanaOperativa

/**
 * Resultado de caja de una semana (no persistido; se calcula o se lee del snapshot).
 */
data class ResultadoSemanal(
    val semana: SemanaOperativa,
    val ventasCobradas: Double,
    val costoVariable: Double,
    val foodTeorico: Double,
    val manoObraPagada: Double,
    val fijosProrrateados: Double,
    val resultado: Double,            // lo que queda
    val foodPct: Double,
    val laborPct: Double,
    val arriendoPct: Double,
    val margenContribucion: Double,
    val breakEven: Double?,           // null si no calculable
    val faltaVender: Double?,
    val estado: EstadoCierre,
    val semaforoFood: EstadoSemaforo,
    val semaforoLabor: EstadoSemaforo,
    val semaforoArriendo: EstadoSemaforo,
    val bajoBreakEven: Boolean,
    val alertaArriendo: Boolean
)

/** Mano de obra que el negocio puede pagar en la semana. */
data class ManoObraDisponible(
    val total: Double,
    val empleadosActivos: Int,
    val porPersona: Double,
    val alcanzaParaContratar: Boolean,
    val esPresupuestoParaContratar: Boolean
)
