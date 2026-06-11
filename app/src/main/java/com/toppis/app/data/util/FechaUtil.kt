package com.toppis.app.data.util

import java.time.Instant
import java.time.OffsetDateTime

/**
 * Utilidades para convertir entre epoch millis (que usa la lógica de la app)
 * y timestamps ISO 8601 (que usa Supabase / PostgreSQL TIMESTAMPTZ).
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
}
