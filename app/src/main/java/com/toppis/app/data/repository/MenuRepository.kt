package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Preparacion
import com.toppis.app.data.models.RecetaMenu
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Componente resuelto de una receta: incluye nombre, unidad base y costo del componente. */
data class ComponenteReceta(
    val recetaMenu: RecetaMenu,
    val nombre: String,
    val unidad: String,
    val costoBase: Double
) {
    /** Costo de este componente dentro de la receta. */
    val costoLinea: Double get() = costoBase * recetaMenu.cantidadBase
}

/** Food cost de un item del menú. */
data class FoodCostItem(
    val item: ItemMenu,
    val costoTeorico: Double,
    val precio: Double
) {
    val margen: Double get() = precio - costoTeorico
    val foodCostPct: Double get() = if (precio > 0) costoTeorico / precio * 100.0 else 0.0
}

/**
 * Repositorio del módulo Menú (items, recetas) basado en Supabase.
 * Las recetas se componen de artículos y/o preparaciones, todo en unidad base.
 */
class MenuRepository {

    private val client = SupabaseClient.client

    // ── ItemMenu ────────────────────────────────────────────────────────────────

    suspend fun getItemsMenuActivos(): List<ItemMenu> = try {
        client.postgrest.from("items_menu").select()
            .decodeList<ItemMenu>()
            .filter { it.activo }
            .sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("MenuRepository", "Error getItemsMenuActivos: ${e.message}", e)
        emptyList()
    }

    suspend fun getAllItemsMenu(): List<ItemMenu> = try {
        client.postgrest.from("items_menu").select().decodeList<ItemMenu>().sortedBy { it.id }
    } catch (e: Exception) {
        Log.e("MenuRepository", "Error getAllItemsMenu: ${e.message}", e)
        emptyList()
    }

    suspend fun crearItemMenu(nombre: String, descripcion: String, precio: Double, categoria: String = "") {
        client.postgrest.from("items_menu").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("descripcion", descripcion)
                put("precio", precio)
                put("categoria", categoria)
                put("activo", true)
            }
        )
    }

    suspend fun actualizarItemMenu(item: ItemMenu) {
        client.postgrest.from("items_menu").update(
            buildJsonObject {
                put("nombre", item.nombre)
                put("descripcion", item.descripcion)
                put("precio", item.precio)
                put("categoria", item.categoria)
                put("activo", item.activo)
            }
        ) {
            filter { eq("id", item.id) }
        }
    }

    suspend fun eliminarItemMenu(item: ItemMenu) {
        client.postgrest.from("items_menu").delete {
            filter { eq("id", item.id) }
        }
    }

    // ── RecetaMenu ──────────────────────────────────────────────────────────────

    suspend fun getComponentesReceta(itemMenuId: Int): List<ComponenteReceta> {
        val recetas = try {
            client.postgrest.from("recetas_menu").select {
                filter { eq("item_menu_id", itemMenuId) }
            }.decodeList<RecetaMenu>()
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error getComponentesReceta: ${e.message}", e)
            emptyList()
        }

        return recetas.mapNotNull { receta ->
            when (receta.tipoComponente) {
                TipoComponente.ARTICULO -> {
                    val a = getArticuloPorId(receta.componenteId)
                    a?.let { ComponenteReceta(receta, it.nombre, it.unidadBase, it.costoBase) }
                }
                TipoComponente.PREPARACION -> {
                    val p = getPreparacionPorId(receta.componenteId)
                    p?.let { ComponenteReceta(receta, it.nombre, it.unidadBase, it.costoBase) }
                }
            }
        }
    }

    suspend fun agregarComponente(
        itemMenuId: Int,
        tipo: TipoComponente,
        componenteId: Int,
        cantidadBase: Double
    ) {
        client.postgrest.from("recetas_menu").insert(
            buildJsonObject {
                put("item_menu_id", itemMenuId)
                put("tipo_componente", tipo.name)
                put("componente_id", componenteId)
                put("cantidad_base", cantidadBase)
            }
        )
        recalcularCostoItem(itemMenuId)
    }

    suspend fun eliminarComponente(receta: RecetaMenu) {
        client.postgrest.from("recetas_menu").delete {
            filter { eq("id", receta.id) }
        }
        recalcularCostoItem(receta.itemMenuId)
    }

    /** Recalcula y persiste el costo teórico de un item del menú. */
    suspend fun recalcularCostoItem(itemMenuId: Int): Double {
        val componentes = getComponentesReceta(itemMenuId)
        val costo = componentes.sumOf { it.costoLinea }
        client.postgrest.from("items_menu").update(
            buildJsonObject { put("costo_teorico", costo) }
        ) {
            filter { eq("id", itemMenuId) }
        }
        return costo
    }

    /** Food cost de todos los items activos (para reportes/menu engineering). */
    suspend fun getFoodCostItems(): List<FoodCostItem> {
        val items = getAllItemsMenu().filter { it.activo }
        return items.map { FoodCostItem(it, it.costoTeorico, it.precio) }
    }

    // ── Catálogos para armar recetas ──────────────────────────────────────────

    suspend fun getArticulos(): List<Articulo> = try {
        client.postgrest.from("articulos").select().decodeList<Articulo>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("MenuRepository", "Error getArticulos: ${e.message}", e)
        emptyList()
    }

    suspend fun getPreparaciones(): List<Preparacion> = try {
        client.postgrest.from("preparaciones").select().decodeList<Preparacion>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("MenuRepository", "Error getPreparaciones: ${e.message}", e)
        emptyList()
    }

    /** Artículos y preparaciones marcados como seleccionables en POS (salsas, agregados). */
    suspend fun getOpcionesPos(): List<ComponenteReceta> {
        val arts = getArticulos().filter { it.seleccionableEnPos }
        val preps = getPreparaciones().filter { it.seleccionableEnPos }
        return arts.map {
            ComponenteReceta(
                RecetaMenu(0, 0, TipoComponente.ARTICULO, it.id, 0.0), it.nombre, it.unidadBase, it.costoBase
            )
        } + preps.map {
            ComponenteReceta(
                RecetaMenu(0, 0, TipoComponente.PREPARACION, it.id, 0.0), it.nombre, it.unidadBase, it.costoBase
            )
        }
    }

    private suspend fun getArticuloPorId(id: Int): Articulo? = try {
        client.postgrest.from("articulos").select { filter { eq("id", id) } }
            .decodeSingleOrNull<Articulo>()
    } catch (e: Exception) {
        null
    }

    private suspend fun getPreparacionPorId(id: Int): Preparacion? = try {
        client.postgrest.from("preparaciones").select { filter { eq("id", id) } }
            .decodeSingleOrNull<Preparacion>()
    } catch (e: Exception) {
        null
    }

    // ── Realtime ─────────────────────────────────────────────────────────────────

    fun observeItemsMenu(): Flow<Unit> = observarTabla("items_menu", "items-menu-changes")

    private fun observarTabla(tabla: String, canal: String): Flow<Unit> = channelFlow {
        val channel = client.channel("$canal-${java.util.UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = tabla
        }
        val job = launch { changes.collect { send(Unit) } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }
}
