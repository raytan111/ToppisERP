package com.toppis.app.data.repository

import com.toppis.app.data.db.entities.TipoSobre
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Modificador
import com.toppis.app.data.models.Promocion
import com.toppis.app.data.models.Sobre
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caché en memoria del catálogo del POS (menú, promos, modificadores, sobres).
 *
 * Evita repetir peticiones a Supabase cada vez que se abre un pedido o se cambia de
 * pantalla del POS. El catálogo cambia poco durante un turno, así que se guarda con un
 * TTL corto y se puede invalidar manualmente al editar menú/promos/modificadores/sobres.
 */
object PosCache {

    private const val TTL_MS = 5 * 60 * 1000L  // 5 minutos

    private val menuRepo = MenuRepository()
    private val promoRepo = PromocionRepository()
    private val modRepo = ModificadorRepository()
    private val sobreRepo = SobreRepository()

    private val mutex = Mutex()

    private var menuCache: List<ItemMenu>? = null
    private var menuTs = 0L
    private var promosCache: List<Promocion>? = null
    private var promosTs = 0L
    private var modsCache: List<Modificador>? = null
    private var modsTs = 0L
    private var sobresCache: List<Sobre>? = null
    private var sobresTs = 0L

    private fun fresco(ts: Long) = System.currentTimeMillis() - ts < TTL_MS

    suspend fun menu(force: Boolean = false): List<ItemMenu> = mutex.withLock {
        if (force || menuCache == null || !fresco(menuTs)) {
            menuCache = menuRepo.getItemsMenuActivos().sortedBy { it.nombre }
            menuTs = System.currentTimeMillis()
        }
        menuCache!!
    }

    suspend fun promos(force: Boolean = false): List<Promocion> = mutex.withLock {
        if (force || promosCache == null || !fresco(promosTs)) {
            promosCache = promoRepo.getPromociones().filter { it.activo }
            promosTs = System.currentTimeMillis()
        }
        promosCache!!
    }

    suspend fun modificadores(force: Boolean = false): List<Modificador> = mutex.withLock {
        if (force || modsCache == null || !fresco(modsTs)) {
            modsCache = modRepo.getModificadores().filter { it.activo }
            modsTs = System.currentTimeMillis()
        }
        modsCache!!
    }

    suspend fun sobresCuenta(force: Boolean = false): List<Sobre> = mutex.withLock {
        if (force || sobresCache == null || !fresco(sobresTs)) {
            sobresCache = sobreRepo.getSobres().filter { it.tipo == TipoSobre.CUENTA }
            sobresTs = System.currentTimeMillis()
        }
        sobresCache!!
    }

    /** Invalida todo el catálogo (llamar al editar menú/promos/modificadores). */
    fun invalidarCatalogo() {
        menuCache = null; promosCache = null; modsCache = null
    }

    fun invalidarSobres() { sobresCache = null }
}
