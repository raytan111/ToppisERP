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
        deltaPrecio: Double,
        categoria: String? = null
    ): Int? = try {
        client.postgrest.from("modificadores").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("tipo", tipo.name)
                if (itemMenuId == null) put("item_menu_id", JsonNull) else put("item_menu_id", itemMenuId)
                if (categoria.isNullOrBlank()) put("categoria", JsonNull) else put("categoria", categoria)
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
                if (mod.categoria.isNullOrBlank()) put("categoria", JsonNull) else put("categoria", mod.categoria)
                put("delta_precio", mod.deltaPrecio)
                put("activo", mod.activo)
            }
        ) {
            filter { eq("id", mod.id) }
        }
    }

    /** Modificadores aplicables a un producto: su categoría del menú + los puntuales del item. */
    suspend fun getModificadoresParaItemYCategoria(itemMenuId: Int, categoria: String?): List<Modificador> =
        com.toppis.app.domain.pos.PosCalculos.modificadoresAplicables(itemMenuId, categoria, getModificadores())

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

    // ── Resolución de costo para el POS ─────────────────────────────────────────

    /**
     * Modificadores aplicables a un item, con su delta de costo resuelto y sus componentes.
     * deltaCosto = Σ(signo × costoBase × cantidad), donde AGREGAR suma y QUITAR resta.
     */
    suspend fun getModificadoresConCosto(itemMenuId: Int): List<ModificadorConCosto> {
        val mods = getModificadoresParaItem(itemMenuId)
        val costoArticulos = mutableMapOf<Int, Double>()
        val costoPreps = mutableMapOf<Int, Double>()

        return mods.map { mod ->
            val comps = getComponentes(mod.id)
            var deltaCosto = 0.0
            for (c in comps) {
                val costoBase = when (c.tipoComponente) {
                    TipoComponente.ARTICULO -> costoArticulos.getOrPut(c.componenteId) { costoArticulo(c.componenteId) }
                    TipoComponente.PREPARACION -> costoPreps.getOrPut(c.componenteId) { costoPreparacion(c.componenteId) }
                }
                val signo = if (c.accion == AccionModificador.QUITAR) -1.0 else 1.0
                deltaCosto += signo * costoBase * c.cantidadBase
            }
            ModificadorConCosto(mod, deltaCosto, comps)
        }
    }

    suspend fun getArticulos(): List<com.toppis.app.data.models.Articulo> = try {
        client.postgrest.from("articulos").select().decodeList<com.toppis.app.data.models.Articulo>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) { emptyList() }

    suspend fun getPreparaciones(): List<com.toppis.app.data.models.Preparacion> = try {
        client.postgrest.from("preparaciones").select().decodeList<com.toppis.app.data.models.Preparacion>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) { emptyList() }

    private suspend fun costoArticulo(id: Int): Double = try {
        client.postgrest.from("articulos").select { filter { eq("id", id) } }
            .decodeSingleOrNull<com.toppis.app.data.models.Articulo>()?.costoBase ?: 0.0
    } catch (e: Exception) { 0.0 }

    private suspend fun costoPreparacion(id: Int): Double = try {
        client.postgrest.from("preparaciones").select { filter { eq("id", id) } }
            .decodeSingleOrNull<com.toppis.app.data.models.Preparacion>()?.costoBase ?: 0.0
    } catch (e: Exception) { 0.0 }
}

/** Modificador con su delta de costo ya resuelto y sus componentes (para el POS). */
data class ModificadorConCosto(
    val modificador: com.toppis.app.data.models.Modificador,
    val deltaCosto: Double,
    val componentes: List<ModificadorComponente> = emptyList()
) {
    val deltaPrecio: Double get() = modificador.deltaPrecio
}
