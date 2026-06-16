package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.AccionModificador
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.db.entities.TipoModificador
import com.toppis.app.data.models.Modificador
import com.toppis.app.data.models.ModificadorComponente
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositorio de Modificadores (doble, quitar, reemplazar, extra) y sus deltas de receta.
 */
class ModificadorRepository {

    private val client = SupabaseClient.client

    suspend fun getModificadores(): List<Modificador> = try {
        client.postgrest.from("modificadores").select().decodeList<Modificador>().sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("ModificadorRepository", "Error getModificadores: ${e.message}", e)
        emptyList()
    }

    /** Modificadores aplicables a un item (los específicos del item + los globales). */
    suspend fun getModificadoresParaItem(itemMenuId: Int): List<Modificador> =
        getModificadores().filter { it.activo && (it.itemMenuId == null || it.itemMenuId == itemMenuId) }

    suspend fun crearModificador(
        nombre: String,
        tipo: TipoModificador,
        itemMenuId: Int?,
        deltaPrecio: Double
    ): Int? = try {
        client.postgrest.from("modificadores").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("tipo", tipo.name)
                if (itemMenuId == null) put("item_menu_id", JsonNull) else put("item_menu_id", itemMenuId)
                put("delta_precio", deltaPrecio)
                put("activo", true)
            }
        ) { select() }.decodeSingle<Modificador>().id
    } catch (e: Exception) {
        Log.e("ModificadorRepository", "Error crearModificador: ${e.message}", e)
        null
    }

    suspend fun actualizarModificador(mod: Modificador) {
        client.postgrest.from("modificadores").update(
            buildJsonObject {
                put("nombre", mod.nombre)
                put("tipo", mod.tipo.name)
                if (mod.itemMenuId == null) put("item_menu_id", JsonNull) else put("item_menu_id", mod.itemMenuId)
                put("delta_precio", mod.deltaPrecio)
                put("activo", mod.activo)
            }
        ) {
            filter { eq("id", mod.id) }
        }
    }

    suspend fun eliminarModificador(id: Int) {
        client.postgrest.from("modificadores").delete { filter { eq("id", id) } }
    }

    // ── Componentes ───────────────────────────────────────────────────────────

    suspend fun getComponentes(modificadorId: Int): List<ModificadorComponente> = try {
        client.postgrest.from("modificador_componentes").select {
            filter { eq("modificador_id", modificadorId) }
        }.decodeList<ModificadorComponente>()
    } catch (e: Exception) {
        Log.e("ModificadorRepository", "Error getComponentes: ${e.message}", e)
        emptyList()
    }

    suspend fun agregarComponente(
        modificadorId: Int,
        accion: AccionModificador,
        tipo: TipoComponente,
        componenteId: Int,
        cantidadBase: Double
    ) {
        client.postgrest.from("modificador_componentes").insert(
            buildJsonObject {
                put("modificador_id", modificadorId)
                put("accion", accion.name)
                put("tipo_componente", tipo.name)
                put("componente_id", componenteId)
                put("cantidad_base", cantidadBase)
            }
        )
    }

    suspend fun eliminarComponente(id: Int) {
        client.postgrest.from("modificador_componentes").delete { filter { eq("id", id) } }
    }
}
