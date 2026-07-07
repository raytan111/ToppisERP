package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.TipoPromocion
import com.toppis.app.data.models.AnalisisPromocion
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Promocion
import com.toppis.app.data.models.PromocionItem
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Promoción con sus items resueltos. */
data class PromocionDetalle(
    val promocion: Promocion,
    val items: List<PromocionItemDetalle>,
    val analisis: AnalisisPromocion
)

data class PromocionItemDetalle(
    val promocionItem: PromocionItem,
    val item: ItemMenu
)

/**
 * Repositorio de Promociones (creador manual con análisis de costo/ganancia).
 */
class PromocionRepository {

    private val client = SupabaseClient.client

    suspend fun getPromociones(): List<Promocion> = try {
        client.postgrest.from("promociones").select().decodeList<Promocion>().sortedBy { it.nombre }
    } catch (e: Exception) {
        Log.e("PromocionRepository", "Error getPromociones: ${e.message}", e)
        emptyList()
    }

    suspend fun crearPromocion(
        nombre: String,
        tipo: TipoPromocion,
        precio: Double,
        descuentoPct: Double,
        fechaInicio: String? = null,
        fechaFin: String? = null
    ): Int? = try {
        client.postgrest.from("promociones").insert(
            buildJsonObject {
                put("nombre", nombre)
                put("tipo", tipo.name)
                put("precio", precio)
                put("descuento_pct", descuentoPct)
                if (fechaInicio == null) put("fecha_inicio", JsonNull) else put("fecha_inicio", fechaInicio)
                if (fechaFin == null) put("fecha_fin", JsonNull) else put("fecha_fin", fechaFin)
                put("activo", true)
            }
        ) { select() }.decodeSingle<Promocion>().id
    } catch (e: Exception) {
        Log.e("PromocionRepository", "Error crearPromocion: ${e.message}", e)
        null
    }

    suspend fun actualizarPromocion(promo: Promocion) {
        client.postgrest.from("promociones").update(
            buildJsonObject {
                put("nombre", promo.nombre)
                put("tipo", promo.tipo.name)
                put("precio", promo.precio)
                put("descuento_pct", promo.descuentoPct)
                if (promo.fechaInicio == null) put("fecha_inicio", JsonNull) else put("fecha_inicio", promo.fechaInicio)
                if (promo.fechaFin == null) put("fecha_fin", JsonNull) else put("fecha_fin", promo.fechaFin)
                put("activo", promo.activo)
            }
        ) {
            filter { eq("id", promo.id) }
        }
    }

    suspend fun eliminarPromocion(id: Int) {
        client.postgrest.from("promociones").delete { filter { eq("id", id) } }
    }

    /** Actualiza solo la imagen de una promoción. */
    suspend fun actualizarImagen(id: Int, url: String) {
        client.postgrest.from("promociones").update(
            buildJsonObject { put("imagen_url", url) }
        ) { filter { eq("id", id) } }
    }

    // ── Items de la promoción ────────────────────────────────────────────────

    suspend fun getItems(promocionId: Int): List<PromocionItem> = try {
        client.postgrest.from("promocion_items").select {
            filter { eq("promocion_id", promocionId) }
        }.decodeList<PromocionItem>()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun agregarItem(promocionId: Int, itemMenuId: Int, cantidad: Int) {
        client.postgrest.from("promocion_items").insert(
            buildJsonObject {
                put("promocion_id", promocionId)
                put("item_menu_id", itemMenuId)
                put("cantidad", cantidad)
            }
        )
    }

    suspend fun eliminarItem(id: Int) {
        client.postgrest.from("promocion_items").delete { filter { eq("id", id) } }
    }

    // ── Análisis ───────────────────────────────────────────────────────────────

    /** Calcula el análisis de una promoción a partir de sus items y los precios/costos del menú. */
    fun analizar(promocion: Promocion, items: List<PromocionItemDetalle>): AnalisisPromocion {
        val costoPromo = items.sumOf { it.item.costoTeorico * it.promocionItem.cantidad }
        val precioNormal = items.sumOf { it.item.precio * it.promocionItem.cantidad }
        val precioPromo = when (promocion.tipo) {
            TipoPromocion.COMBO -> promocion.precio
            TipoPromocion.DESCUENTO_PORCENTAJE -> precioNormal * (1.0 - promocion.descuentoPct / 100.0)
        }
        val ganancia = precioPromo - costoPromo
        val gananciaPct = if (precioPromo > 0) ganancia / precioPromo * 100.0 else 0.0
        val foodCostPct = if (precioPromo > 0) costoPromo / precioPromo * 100.0 else 0.0
        val ahorroCliente = precioNormal - precioPromo
        return AnalisisPromocion(costoPromo, precioNormal, precioPromo, ganancia, gananciaPct, foodCostPct, ahorroCliente)
    }

    /** Catálogo de items del menú para armar promociones. */
    suspend fun getItemsMenu(): List<ItemMenu> = try {
        client.postgrest.from("items_menu").select().decodeList<ItemMenu>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) {
        emptyList()
    }
}
