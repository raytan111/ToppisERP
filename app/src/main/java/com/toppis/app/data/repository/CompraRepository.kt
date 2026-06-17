package com.toppis.app.data.repository

import android.util.Log
import com.toppis.app.data.db.entities.UnidadMedida
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Compra
import com.toppis.app.data.models.CompraDetalle
import com.toppis.app.data.models.Proveedor
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Línea de compra ingresada por el usuario (en unidad de compra del artículo). */
data class LineaCompra(
    val articulo: Articulo,
    val cantidadCompra: Double,        // en unidad de compra (kg/L/un)
    val costoUnitarioCompra: Double,   // precio por unidad de compra
    val vencimiento: String? = null    // yyyy-MM-dd opcional
) {
    private val factor: Double get() = UnidadMedida.porAbreviatura(articulo.unidadCompra)?.factorBase ?: 1.0
    val cantidadBase: Double get() = cantidadCompra * factor
    val costoPorBase: Double get() = if (factor > 0) costoUnitarioCompra / factor else costoUnitarioCompra
    val subtotal: Double get() = cantidadCompra * costoUnitarioCompra
}

/** Detalle de caducidad con nombre del artículo. */
data class LoteVencimiento(
    val detalle: CompraDetalle,
    val nombreArticulo: String
)

/**
 * Repositorio de Compras. El registro es atómico vía RPC registrar_compra.
 */
class CompraRepository {

    private val client = SupabaseClient.client

    suspend fun getCompras(): List<Compra> = try {
        client.postgrest.from("compras").select().decodeList<Compra>().sortedByDescending { it.fecha }
    } catch (e: Exception) {
        Log.e("CompraRepository", "Error getCompras: ${e.message}", e)
        emptyList()
    }

    suspend fun getArticulos(): List<Articulo> = try {
        client.postgrest.from("articulos").select().decodeList<Articulo>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) { emptyList() }

    suspend fun getProveedores(): List<Proveedor> = try {
        client.postgrest.from("proveedores").select().decodeList<Proveedor>()
            .filter { it.activo }.sortedBy { it.nombre }
    } catch (e: Exception) { emptyList() }

    suspend fun getSobres(): List<Sobre> = try {
        client.postgrest.from("sobres").select().decodeList<Sobre>().sortedBy { it.nombre }
    } catch (e: Exception) { emptyList() }

    /** Lotes con vencimiento, ordenados por fecha más próxima. */
    suspend fun getProximosAVencer(): List<LoteVencimiento> {
        val detalles = try {
            client.postgrest.from("compra_detalle").select().decodeList<CompraDetalle>()
                .filter { !it.vencimiento.isNullOrBlank() }
        } catch (e: Exception) { emptyList() }
        val articulos = getArticulos().associateBy { it.id }
        return detalles.map { d ->
            LoteVencimiento(d, articulos[d.articuloId]?.nombre ?: "Artículo #${d.articuloId}")
        }.sortedBy { it.detalle.vencimiento }
    }

    suspend fun registrarCompra(
        proveedorId: Int?,
        tieneIva: Boolean,
        nota: String,
        lineas: List<LineaCompra>,
        sobreId: Int?,
        usuarioId: String?
    ): Int {
        require(lineas.isNotEmpty()) { "No hay items en la compra" }
        val itemsJson = buildJsonArray {
            lineas.forEach { l ->
                add(buildJsonObject {
                    put("articulo_id", l.articulo.id)
                    put("cantidad_base", l.cantidadBase)
                    put("costo_por_base", l.costoPorBase)
                    put("vencimiento", l.vencimiento ?: "")
                })
            }
        }
        val params = buildJsonObject {
            if (proveedorId == null) put("p_proveedor_id", JsonNull) else put("p_proveedor_id", proveedorId)
            put("p_tiene_iva", tieneIva)
            put("p_nota", nota)
            put("p_items", itemsJson)
            if (sobreId == null) put("p_sobre_id", JsonNull) else put("p_sobre_id", sobreId)
            if (usuarioId == null) put("p_usuario", JsonNull) else put("p_usuario", usuarioId)
            LocalSession.activoId.value?.let { put("p_local_id", it) }
        }
        return try {
            client.postgrest.rpc("registrar_compra", params).decodeAs<Int>()
        } catch (e: Exception) {
            Log.e("CompraRepository", "Error registrarCompra: ${e.message}", e)
            val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(e.message ?: "")?.groupValues?.get(1)
            throw Exception(msg ?: "Error al registrar la compra")
        }
    }
}
