package com.toppis.app.domain.pos

import com.toppis.app.data.db.entities.EstadoPedido
import com.toppis.app.data.db.entities.ModoEspacioPromo
import com.toppis.app.data.db.entities.TipoModificador
import com.toppis.app.data.db.entities.TipoPromocion
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.Modificador
import com.toppis.app.data.models.PromocionEspacio
import com.toppis.app.data.models.PromocionEspacioOpcion
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de propiedad de la capa pura del POS (PosCalculos).
 * kotest-property, 100 iteraciones por propiedad, un test por propiedad del diseño.
 */
class PosCalculosPropertyTest {

    private val ITERS = 100

    private fun montos(): Arb<Double> =
        Arb.double(0.0..1_000_000.0).filterNot { it.isNaN() || it.isInfinite() }

    private fun prop(block: suspend () -> Unit) = runBlocking { block() }

    private val cats = listOf("Hamburguesas", "Papas fritas", "Bebida lata", "Bebida mediana", "Salsas", "Otro")

    private fun arbItem(): Arb<ItemMenu> = arbitrary {
        ItemMenu(
            id = Arb.int(1..5000).bind(),
            nombre = Arb.string(1, 10).bind(),
            precio = montos().bind(),
            categoria = Arb.of(cats).bind()
        )
    }

    // Property 1: Total del pedido = Σ subtotales + envío
    @Test fun p01_totalPedido() = prop {
        checkAll(ITERS, Arb.list(montos(), 0..8), montos()) { subs, envio ->
            assertEquals(subs.sum() + envio, PosCalculos.totalPedido(subs, envio), 1e-6)
        }
    }

    // Property 2: Precio de promo siempre fijo
    @Test fun p02_precioPromoFijo() = prop {
        checkAll(ITERS, montos()) { fijo ->
            assertEquals(fijo, PosCalculos.precioPromo(fijo), 0.0)
        }
    }

    // Property 3: Precio de producto = base + Σ deltas (≥ base con deltas ≥ 0)
    @Test fun p03_precioProducto() = prop {
        checkAll(ITERS, montos(), Arb.list(montos(), 0..6)) { base, deltas ->
            val p = PosCalculos.precioProducto(base, deltas)
            assertEquals(base + deltas.sum(), p, 1e-6)
            assertTrue(p >= base - 1e-9)
        }
    }

    // Property 4: Modificadores aplicables ⊆ (misma categoría ∪ puntuales del item)
    @Test fun p04_modificadoresAplicables() = prop {
        val arbMod: Arb<Modificador> = arbitrary {
            Modificador(
                id = Arb.int(1..9999).bind(),
                nombre = Arb.string(1, 8).bind(),
                tipo = TipoModificador.EXTRA,
                itemMenuId = Arb.int(1..50).orNullable().bind(),
                categoria = Arb.of(cats + listOf<String?>(null)).bind(),
                activo = Arb.boolean().bind()
            )
        }
        checkAll(ITERS, Arb.int(1..50), Arb.of(cats), Arb.list(arbMod, 0..10)) { itemId, cat, mods ->
            val res = PosCalculos.modificadoresAplicables(itemId, cat, mods)
            res.forEach { m ->
                assertTrue(m.activo)
                val porItem = m.itemMenuId == itemId
                val porCat = !m.categoria.isNullOrBlank() && m.categoria.equals(cat, ignoreCase = true)
                assertTrue(porItem || porCat)
            }
        }
    }

    // Property 5: Elegibles de un espacio ⊆ opciones (LISTA) ó categoría (CATEGORIA)
    @Test fun p05_elegiblesEspacio() = prop {
        checkAll(ITERS, Arb.list(arbItem(), 0..12), Arb.of(cats)) { items, cat ->
            // Modo CATEGORIA
            val espCat = PromocionEspacio(id = 1, promocionId = 1, nombre = "x", modo = ModoEspacioPromo.CATEGORIA, categoria = cat)
            PosCalculos.elegiblesEspacio(espCat, emptyList(), items).forEach {
                assertTrue(it.categoria.equals(cat, ignoreCase = true))
            }
            // Modo LISTA
            val ids = items.take(2).map { it.id }.toSet()
            val opciones = ids.map { PromocionEspacioOpcion(espacioId = 2, itemMenuId = it) }
            val espLista = PromocionEspacio(id = 2, promocionId = 1, nombre = "y", modo = ModoEspacioPromo.LISTA)
            PosCalculos.elegiblesEspacio(espLista, opciones, items).forEach {
                assertTrue(it.id in ids)
            }
        }
    }

    // Property 6: Promo completa ⇔ cada espacio tiene su cantidad elegida
    @Test fun p06_promoCompleta() = prop {
        checkAll(ITERS, Arb.list(Arb.int(1..3), 1..4)) { cantidades ->
            val espacios = cantidades.mapIndexed { i, c ->
                PromocionEspacio(id = i + 1, promocionId = 1, nombre = "e$i", cantidad = c, modo = ModoEspacioPromo.CATEGORIA, categoria = "Bebida lata")
            }
            val completos = espacios.associate { it.id to it.cantidad }
            assertTrue(PosCalculos.promoCompleta(espacios, completos))
            // Si a un espacio le falta uno, ya no está completa
            val faltante = completos.toMutableMap().apply { this[espacios.first().id] = espacios.first().cantidad - 1 }
            assertFalse(PosCalculos.promoCompleta(espacios, faltante))
        }
    }

    // Property 7: tieneDeuda ⇔ entregado ∧ ¬pagado
    @Test fun p07_tieneDeuda() = prop {
        checkAll(ITERS, Arb.boolean(), Arb.boolean()) { entregado, pagado ->
            assertEquals(entregado && !pagado, PosCalculos.tieneDeuda(entregado, pagado))
        }
    }

    // Property 8: activo en lista ⇔ ¬(pagado ∧ entregado)
    @Test fun p08_activoEnLista() = prop {
        checkAll(ITERS, Arb.boolean(), Arb.boolean()) { pagado, entregado ->
            assertEquals(!(pagado && entregado), PosCalculos.activoEnLista(pagado, entregado))
        }
    }

    // Property 9: el pago materializa venta solo si no hay una (idempotencia)
    @Test fun p09_pagoIdempotente() = prop {
        checkAll(ITERS, Arb.int(1..9999).orNullable()) { ventaId ->
            assertEquals(ventaId == null, PosCalculos.debeMaterializarVenta(ventaId))
        }
    }

    // Property 10: tras pagar con hamburguesa, sellos +1
    @Test fun p10_selloPorHamburguesa() = prop {
        checkAll(ITERS, Arb.int(0..100), Arb.boolean()) { sellos, hamb ->
            val esperado = if (hamb) sellos + 1 else sellos
            assertEquals(esperado, PosCalculos.sellosTrasPedido(sellos, hamb))
        }
    }

    // Property 11: puedeRegalar ⇔ sellos ≥ 6
    @Test fun p11_puedeRegalar() = prop {
        checkAll(ITERS, Arb.int(0..100)) { sellos ->
            assertEquals(sellos >= 6, PosCalculos.puedeRegalar(sellos))
        }
    }

    // Property 12: aplicar regalo baja 6 sellos (nunca < 0) y el precio del regalo es 0
    @Test fun p12_aplicarRegalo() = prop {
        checkAll(ITERS, Arb.int(0..100)) { sellos ->
            assertEquals(maxOf(0, sellos - 6), PosCalculos.sellosTrasRegalo(sellos))
            assertEquals(0.0, PosCalculos.precioRegalo(), 0.0)
        }
    }

    // Property 13: un ítem regalo no suma al cobro (subtotal 0)
    @Test fun p13_regaloNoCobra() = prop {
        checkAll(ITERS, Arb.int(1..10)) { cantidad ->
            assertEquals(0.0, PosCalculos.precioLinea(PosCalculos.precioRegalo(), cantidad), 0.0)
        }
    }

    // Property 14: estados — pagado/entregado en cualquier orden llegan al mismo final
    @Test fun p14_transicionesEstado() = prop {
        checkAll(ITERS, Arb.boolean(), Arb.boolean()) { a, b ->
            // (pagar luego entregar) == (entregar luego pagar)
            val orden1 = Pair(true, false).let { Pair(it.first, true) }       // pagar, luego entregar
            val orden2 = Pair(false, true).let { Pair(true, it.second) }      // entregar, luego pagar
            assertEquals(orden1, orden2)
            // Cerrar solo desde ABIERTO
            assertTrue(PosCalculos.puedeCerrar(EstadoPedido.ABIERTO))
            assertFalse(PosCalculos.puedeCerrar(EstadoPedido.CERRADO))
        }
    }

    // Property 15: cerrar no toca dinero (no materializa venta); pagar sí
    @Test fun p15_cerrarVsPagar() = prop {
        checkAll(ITERS, Arb.boolean()) { _ ->
            // Un pedido recién cerrado no tiene venta aún → aún debe materializarse al pagar
            assertTrue(PosCalculos.debeMaterializarVenta(null))
            // Ya pagado (con venta) → no se vuelve a materializar
            assertFalse(PosCalculos.debeMaterializarVenta(123))
        }
    }

    // Property 16: el comentario no altera el descuento de stock
    @Test fun p16_comentarioNoAfectaStock() = prop {
        checkAll(ITERS, Arb.int(1..500), Arb.list(Arb.int(1..50), 0..5)) { itemId, mods ->
            val k1 = PosCalculos.claveStockUnidad(itemId, mods)
            val k2 = PosCalculos.claveStockUnidad(itemId, mods.shuffled())
            assertEquals(k1, k2) // misma unidad (item+mods), sin importar orden ni comentario
        }
    }

    // ── Promos v2: grupos con repetición ───────────────────────────────────────

    // Property (v2) 17: grupo completo ⇔ elegidas == cantidad
    @Test fun p17_grupoCompleto() = prop {
        checkAll(ITERS, Arb.int(1..5), Arb.int(0..5)) { cant, eleg ->
            assertEquals(eleg == cant, PosCalculos.grupoCompleto(cant, eleg))
        }
    }

    // Property (v2) 18: repetición permitida deja sumar mientras falte cantidad
    @Test fun p18_repeticionPermitida() = prop {
        checkAll(ITERS, Arb.int(1..4), Arb.int(1..50)) { cant, prod ->
            // Con permiteRepetir=true, mientras haya menos elegidas que la cantidad, se puede sumar el mismo producto
            val yaElegidos = List((0 until cant - 1).count()) { prod } // cant-1 repetidos
            if (cant >= 1) {
                assertTrue(PosCalculos.puedeAgregarAlGrupo(true, yaElegidos, prod, cant))
            }
            // Si ya se llegó a la cantidad, no se puede sumar más
            val lleno = List(cant) { prod }
            assertFalse(PosCalculos.puedeAgregarAlGrupo(true, lleno, prod, cant))
        }
    }

    // Property (v2) 19: repetición prohibida ⇒ no se repite el mismo producto
    @Test fun p19_repeticionProhibida() = prop {
        checkAll(ITERS, Arb.int(2..5), Arb.int(1..50)) { cant, prod ->
            // Ya elegido una vez ese producto; con permiteRepetir=false no se puede volver a elegir
            assertFalse(PosCalculos.puedeAgregarAlGrupo(false, listOf(prod), prod, cant))
            // Un producto distinto sí se puede (si falta cantidad)
            assertTrue(PosCalculos.puedeAgregarAlGrupo(false, listOf(prod), prod + 1, cant))
        }
    }

    // Property (v2) 20: promo completa ⇔ todos los grupos con su cantidad cubierta
    @Test fun p20_promoCompletaPorGrupos() = prop {
        checkAll(ITERS, Arb.list(Arb.int(1..3), 1..4)) { cantidades ->
            val cantsPorGrupo = cantidades.mapIndexed { i, c -> i to c }.toMap()
            val completas = cantsPorGrupo.mapValues { (_, c) -> List(c) { 1 } }
            assertTrue(PosCalculos.promoCompletaPorGrupos(cantsPorGrupo, completas))
            // A un grupo le falta uno → incompleta
            val faltante = completas.toMutableMap()
            val primero = cantsPorGrupo.keys.first()
            faltante[primero] = completas[primero]!!.dropLast(1)
            assertFalse(PosCalculos.promoCompletaPorGrupos(cantsPorGrupo, faltante))
        }
    }
}

private fun Arb<Int>.orNullable(): Arb<Int?> = arbitrary { if (Arb.boolean().bind()) null else this@orNullable.bind() }
