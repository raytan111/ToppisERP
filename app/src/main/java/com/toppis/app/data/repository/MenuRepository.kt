package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.TipoComponente
import com.toppis.app.data.models.Ingrediente
import com.toppis.app.data.models.Insumo
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.RecetaMenu
import com.toppis.app.data.models.Salsa
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

/** Componente resuelto de una receta: incluye el nombre y unidad del ingrediente o insumo. */
data class ComponenteReceta(
    val recetaMenu: RecetaMenu,
    val nombre: String,
    val unidad: String
)

/**
 * Repositorio del módulo Menú (items, recetas, salsas) basado en Supabase.
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

    suspend fun crearItemMenu(nombre: String, descripcion: String, precio: Double) {
        client.postgrest.from("items_menu").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("descripcion", descripcion)
                put("precio", precio)
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
                TipoComponente.INGREDIENTE -> {
                    val ing = getIngredientePorId(receta.componenteId)
                    ing?.let { ComponenteReceta(receta, it.nombre, "gr") }
                }
                TipoComponente.INSUMO -> {
                    val insumo = getInsumoPorId(receta.componenteId)
                    insumo?.let { ComponenteReceta(receta, it.nombre, "gr") }
                }
                TipoComponente.SALSA -> {
                    val salsa = getSalsaPorId(receta.componenteId)
                    salsa?.let { ComponenteReceta(receta, it.nombre, "gr") }
                }
            }
        }
    }

    private suspend fun getSalsaPorId(id: Int): Salsa? = try {
        client.postgrest.from("salsas").select {
            filter { eq("id", id) }
        }.decodeSingleOrNull<Salsa>()
    } catch (e: Exception) {
        null
    }

    suspend fun agregarComponente(
        itemMenuId: Int,
        tipo: TipoComponente,
        componenteId: Int,
        cantidad: Double
    ) {
        client.postgrest.from("recetas_menu").insert(
            buildJsonObject {
                put("item_menu_id", itemMenuId)
                put("tipo_componente", tipo.name)
                put("componente_id", componenteId)
                put("cantidad", cantidad)
            }
        )
    }

    suspend fun eliminarComponente(receta: RecetaMenu) {
        client.postgrest.from("recetas_menu").delete {
            filter { eq("id", receta.id) }
        }
    }

    // ── Salsas ──────────────────────────────────────────────────────────────────

    suspend fun getSalsasActivas(): List<Salsa> = try {
        client.postgrest.from("salsas").select()
            .decodeList<Salsa>()
            .filter { it.activa }
            .sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("MenuRepository", "Error getSalsasActivas: ${e.message}", e)
        emptyList()
    }

    suspend fun getAllSalsas(): List<Salsa> = try {
        client.postgrest.from("salsas").select().decodeList<Salsa>().sortedBy { it.id }
    } catch (e: Exception) {
        Log.e("MenuRepository", "Error getAllSalsas: ${e.message}", e)
        emptyList()
    }

    suspend fun crearSalsa(nombre: String, descripcion: String = "") {
        client.postgrest.from("salsas").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("descripcion", descripcion)
                put("activa", true)
            }
        )
    }

    suspend fun actualizarSalsa(salsa: Salsa) {
        client.postgrest.from("salsas").update(
            buildJsonObject {
                put("nombre", salsa.nombre)
                put("descripcion", salsa.descripcion)
                put("activa", salsa.activa)
            }
        ) {
            filter { eq("id", salsa.id) }
        }
    }

    suspend fun eliminarSalsa(salsa: Salsa) {
        client.postgrest.from("salsas").delete {
            filter { eq("id", salsa.id) }
        }
    }

    // ── Helpers: ingredientes e insumos disponibles ───────────────────────────

    suspend fun getIngredientes(): List<Ingrediente> = try {
        client.postgrest.from("ingredientes").select().decodeList<Ingrediente>().sortedBy { it.nombre }
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun getInsumos(): List<Insumo> = try {
        client.postgrest.from("insumos").select().decodeList<Insumo>().sortedBy { it.nombre }
    } catch (e: Exception) {
        emptyList()
    }

    private suspend fun getIngredientePorId(id: Int): Ingrediente? = try {
        client.postgrest.from("ingredientes").select {
            filter { eq("id", id) }
        }.decodeSingleOrNull<Ingrediente>()
    } catch (e: Exception) {
        null
    }

    private suspend fun getInsumoPorId(id: Int): Insumo? = try {
        client.postgrest.from("insumos").select {
            filter { eq("id", id) }
        }.decodeSingleOrNull<Insumo>()
    } catch (e: Exception) {
        null
    }

    // ── Realtime ─────────────────────────────────────────────────────────────────

    fun observeItemsMenu(): Flow<Unit> = observarTabla("items_menu", "items-menu-changes")
    fun observeSalsas(): Flow<Unit> = observarTabla("salsas", "salsas-changes")

    private fun observarTabla(tabla: String, canal: String): Flow<Unit> = channelFlow {
        // Nombre de canal único por suscripción para evitar reusar uno ya unido
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
