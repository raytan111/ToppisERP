package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.TipoLineaPedido
import com.toppis.app.data.models.Pedido
import com.toppis.app.data.models.PedidoItem
import com.toppis.app.data.models.PedidoUnidad
import com.toppis.app.data.models.PedidoUnidadMod
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

/**
 * Repositorio de Pedidos (carritos). Capa operativa del POS: CRUD de
 * pedidos/ítems/unidades/modificadores + observador Realtime. El pago materializa
 * la venta vía RPC (se agrega en la fase de pago).
 */
class PedidoRepository {

    private val client = SupabaseClient.client

    // ── Lectura ─────────────────────────────────────────────────────────────

    /** Pedidos activos (no pagados o no entregados), más recientes primero. */
    suspend fun getPedidosActivos(): List<Pedido> = try {
        client.postgrest.from("pedidos").select().decodeList<Pedido>()
            .filter { it.activo }
            .sortedByDescending { it.id }
    } catch (e: Exception) {
        Log.e("PedidoRepository", "Error getPedidosActivos: ${e.message}", e)
        emptyList()
    }

    suspend fun getPedido(id: Int): Pedido? = try {
        client.postgrest.from("pedidos").select { filter { eq("id", id) } }
            .decodeSingleOrNull<Pedido>()
    } catch (e: Exception) {
        Log.e("PedidoRepository", "Error getPedido: ${e.message}", e)
        null
    }

    suspend fun getItems(pedidoId: Int): List<PedidoItem> = try {
        client.postgrest.from("pedido_items").select { filter { eq("pedido_id", pedidoId) } }
            .decodeList<PedidoItem>().sortedBy { it.id }
    } catch (e: Exception) { emptyList() }

    suspend fun getUnidades(pedidoItemId: Int): List<PedidoUnidad> = try {
        client.postgrest.from("pedido_unidades").select { filter { eq("pedido_item_id", pedidoItemId) } }
            .decodeList<PedidoUnidad>().sortedBy { it.id }
    } catch (e: Exception) { emptyList() }

    suspend fun getMods(pedidoUnidadId: Int): List<PedidoUnidadMod> = try {
        client.postgrest.from("pedido_unidad_mods").select { filter { eq("pedido_unidad_id", pedidoUnidadId) } }
            .decodeList<PedidoUnidadMod>()
    } catch (e: Exception) { emptyList() }

    // ── Escritura ─────────────────────────────────────────────────────────────

    /** Crea un pedido ABIERTO para un cliente y lo devuelve. */
    suspend fun crearPedido(clienteId: Int): Pedido =
        client.postgrest.from("pedidos").insert(
            buildJsonObject {
                put("cliente_id", clienteId)
                put("estado", "ABIERTO")
                LocalSession.activoId.value?.let { put("local_id", it) }
            }
        ) { select() }.decodeSingle<Pedido>()

    /** Inserta una línea de cobro y devuelve su id. */
    suspend fun agregarItem(
        pedidoId: Int,
        tipo: TipoLineaPedido,
        itemMenuId: Int?,
        promocionId: Int?,
        cantidad: Int,
        precioUnitario: Double,
        subtotal: Double,
        esRegalo: Boolean = false
    ): Int =
        client.postgrest.from("pedido_items").insert(
            buildJsonObject {
                put("pedido_id", pedidoId)
                put("tipo", tipo.name)
                itemMenuId?.let { put("item_menu_id", it) }
                promocionId?.let { put("promocion_id", it) }
                put("cantidad", cantidad)
                put("precio_unitario", precioUnitario)
                put("subtotal", subtotal)
                put("es_regalo", esRegalo)
            }
        ) { select() }.decodeSingle<PedidoItem>().id

    /** Inserta una unidad a preparar y devuelve su id. */
    suspend fun agregarUnidad(pedidoItemId: Int, itemMenuId: Int, comentario: String?): Int =
        client.postgrest.from("pedido_unidades").insert(
            buildJsonObject {
                put("pedido_item_id", pedidoItemId)
                put("item_menu_id", itemMenuId)
                if (!comentario.isNullOrBlank()) put("comentario", comentario.trim())
            }
        ) { select() }.decodeSingle<PedidoUnidad>().id

    /** Asocia un modificador a una unidad. */
    suspend fun agregarMod(pedidoUnidadId: Int, modificadorId: Int) {
        client.postgrest.from("pedido_unidad_mods").insert(
            buildJsonObject {
                put("pedido_unidad_id", pedidoUnidadId)
                put("modificador_id", modificadorId)
            }
        )
    }

    /** Elimina una línea (y en cascada sus unidades/mods). */
    suspend fun quitarItem(pedidoItemId: Int) {
        client.postgrest.from("pedido_items").delete { filter { eq("id", pedidoItemId) } }
    }

    /** Recalcula y guarda el total del pedido (suma de líneas + envío). */
    suspend fun actualizarTotales(pedidoId: Int, total: Double, zonaEnvio: String, montoEnvio: Double) {
        client.postgrest.from("pedidos").update(
            buildJsonObject {
                put("total", total)
                put("zona_envio", zonaEnvio)
                put("monto_envio", montoEnvio)
                put("updated_at", java.time.Instant.now().toString())
            }
        ) { filter { eq("id", pedidoId) } }
    }

    // ── Observador Realtime ─────────────────────────────────────────────────

    /** Emite cada vez que cambia la tabla "pedidos". */
    fun observeCambios(): Flow<Unit> = channelFlow {
        val channel = client.channel("pedidos-changes-${java.util.UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "pedidos" }
        val job = launch { changes.collect { send(Unit) } }
        channel.subscribe()
        awaitClose {
            job.cancel()
            launch { channel.unsubscribe() }
        }
    }
}
