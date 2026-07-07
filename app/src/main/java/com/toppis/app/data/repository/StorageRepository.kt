package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.util.UUID

/**
 * Sube imágenes (productos/promos) al bucket público "menu" de Supabase Storage
 * y devuelve su URL pública.
 */
class StorageRepository {

    private val client = SupabaseClient.client
    private val bucketId = "menu"

    /**
     * Sube los bytes de una imagen y retorna la URL pública.
     * @param carpeta subcarpeta lógica dentro del bucket (ej. "items", "promos").
     */
    suspend fun subirImagen(bytes: ByteArray, carpeta: String = "items", ext: String = "jpg"): String {
        val bucket = client.storage.from(bucketId)
        val path = "$carpeta/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext"
        bucket.upload(path, bytes) { upsert = true }
        return bucket.publicUrl(path)
    }

    /** Borra una imagen por su URL pública (best-effort). */
    suspend fun borrarPorUrl(url: String) {
        try {
            val marcador = "/object/public/$bucketId/"
            val path = url.substringAfter(marcador, "")
            if (path.isNotBlank()) client.storage.from(bucketId).delete(path)
        } catch (e: Exception) {
            Log.e("StorageRepository", "No se pudo borrar imagen: ${e.message}")
        }
    }
}
