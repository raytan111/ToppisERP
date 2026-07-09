package com.toppis.app.data.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Semana operativa lunes → sábado (domingo cerrado). El rango de filtrado es
 * medio-abierto `[lunesInicio, finExclusivo)` con `finExclusivo` = lunes siguiente,
 * de modo que toda fecha pertenece a exactamente una semana.
 */
data class SemanaOperativa(val lunesInicio: LocalDate) {
    /** Sábado, último día operativo. */
    val sabadoFin: LocalDate get() = lunesInicio.plusDays(5)
    /** Domingo (día de cierre, no operativo). */
    val domingoInicio: LocalDate get() = lunesInicio.plusDays(6)
    /** Lunes siguiente (fin exclusivo del rango de la semana). */
    val finExclusivo: LocalDate get() = lunesInicio.plusDays(7)

    /** true si [fecha] pertenece a esta semana operativa. */
    fun contiene(fecha: LocalDate): Boolean =
        !fecha.isBefore(lunesInicio) && fecha.isBefore(finExclusivo)

    /** ISO de inicio (lunes 00:00) para filtrar en Supabase. */
    val isoDesde: String get() = lunesInicio.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    /** ISO de fin exclusivo (lunes siguiente 00:00). */
    val isoHasta: String get() = finExclusivo.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    /** Etiqueta legible, ej "Lun 12 – Sáb 17 ago". */
    val etiqueta: String
        get() {
            val locale = Locale("es", "CL")
            val dia = DateTimeFormatter.ofPattern("d", locale)
            val diaMes = DateTimeFormatter.ofPattern("d MMM", locale)
            return "Lun ${lunesInicio.format(dia)} – Sáb ${sabadoFin.format(diaMes)}"
        }
}

/**
 * Utilidades para convertir entre epoch millis (que usa la lógica de la app)
 * y timestamps ISO 8601 (que usa Supabase / PostgreSQL TIMESTAMPTZ), más el
 * cálculo de la semana operativa (lunes–sábado).
 */
object FechaUtil {

    /** Convierte epoch millis a un timestamp ISO 8601 (UTC). */
    fun millisToIso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    /** Convierte un timestamp ISO de Supabase a epoch millis. Retorna 0 si falla. */
    fun isoToMillis(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        return try {
            OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }

    /** Semana operativa (lunes–sábado) que contiene [fecha]. */
    fun semanaDe(fecha: LocalDate): SemanaOperativa {
        val lunes = fecha.minusDays((fecha.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        return SemanaOperativa(lunes)
    }

    /** Semana operativa actual. */
    fun semanaActual(): SemanaOperativa = semanaDe(LocalDate.now())

    /** Desplaza [base] en [delta] semanas (-1 anterior, +1 siguiente). */
    fun semanaOffset(base: SemanaOperativa, delta: Long): SemanaOperativa =
        SemanaOperativa(base.lunesInicio.plusWeeks(delta))
}
