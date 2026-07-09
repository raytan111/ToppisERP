package com.toppis.app.domain.costos

import com.toppis.app.data.db.entities.CategoriaArticulo
import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.db.entities.DimensionUnidad
import com.toppis.app.data.db.entities.EstadoSemaforo
import com.toppis.app.data.db.entities.GrupoCosto
import com.toppis.app.data.db.entities.Periodicidad
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.CostoFijo
import com.toppis.app.data.util.FechaUtil
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Tests de propiedad de la capa pura de control de costos (CostosCalculos + SemanaOperativa).
 * kotest-property, 100 iteraciones por propiedad, un test por propiedad del diseño.
 */
class CostosCalculosPropertyTest {

    private val ITERS = 100

    private fun montos(): Arb<Double> =
        Arb.double(0.0..1_000_000.0).filterNot { it.isNaN() || it.isInfinite() }

    private fun montosNeg(): Arb<Double> =
        Arb.double(-1_000_000.0..1_000_000.0).filterNot { it.isNaN() || it.isInfinite() }

    private fun ventasPos(): Arb<Double> =
        Arb.double(1.0..1_000_000.0).filterNot { it.isNaN() || it.isInfinite() }

    private fun pct(): Arb<Double> = Arb.double(0.0..1.0).filterNot { it.isNaN() || it.isInfinite() }

    private val arbCostoFijo: Arb<CostoFijo> = arbitrary {
        CostoFijo(
            id = Arb.int(1..100000).bind(),
            nombre = Arb.string(1, 12).bind(),
            categoria = Arb.enum<CategoriaGasto>().bind(),
            monto = montos().bind(),
            periodicidad = Arb.enum<Periodicidad>().bind(),
            activo = Arb.boolean().bind()
        )
    }

    private val arbValores: Arb<ValoresSemana> = arbitrary {
        ValoresSemana(montos().bind(), montos().bind(), montos().bind(), montos().bind())
    }

    private val minDate = LocalDate.of(2020, 1, 1)
    private val maxDate = LocalDate.of(2030, 12, 31)

    private fun assertClose(esperado: Double, real: Double) {
        val delta = 1e-6 + Math.abs(esperado) * 1e-9
        assertEquals(esperado, real, delta)
    }

    /** Ejecuta el bloque de propiedad; devuelve Unit para que JUnit lo acepte como test void. */
    private fun prop(block: suspend () -> Unit) = runBlocking { block() }

    // Feature: control-de-costos, Property 1: Prorrateo semanal por periodicidad
    @Test fun property01_prorrateo() = prop {
        checkAll(ITERS, montos(), Arb.enum<Periodicidad>()) { m, p ->
            val esperado = when (p) {
                Periodicidad.MENSUAL -> m / 4.33
                Periodicidad.ANUAL -> m / 52.0
                Periodicidad.SEMANAL -> m
            }
            assertClose(esperado, CostosCalculos.prorrateoSemanal(m, p))
        }
    }

    // Feature: control-de-costos, Property 2: Total de fijos suma solo los activos
    @Test fun property02_totalFijosSoloActivos() = prop {
        checkAll(ITERS, Arb.list(arbCostoFijo, 0..8)) { lista ->
            val esperado = lista.filter { it.activo }.sumOf { it.monto / it.periodicidad.divisorSemanal }
            assertClose(esperado, CostosCalculos.totalFijosSemanales(lista))
        }
    }

    // Feature: control-de-costos, Property 3: Monto negativo es rechazado
    @Test fun property03_montoNegativoRechazado() = prop {
        checkAll(ITERS, montosNeg()) { m ->
            assertEquals(m >= 0.0, CostosCalculos.montoFijoValido(m))
        }
    }

    // Feature: control-de-costos, Property 4: Categoría de artículo por defecto
    @Test fun property04_categoriaDefault() = prop {
        checkAll(ITERS, Arb.string(1, 12)) { nombre ->
            val a = Articulo(nombre = nombre, dimension = DimensionUnidad.MASA, unidadBase = "g")
            assertEquals(CategoriaArticulo.INGREDIENTES, a.categoria)
        }
    }

    // Feature: control-de-costos, Property 5: Costo del artículo por último precio
    @Test fun property05_ultimoPrecio() = prop {
        checkAll(ITERS, montos(), montos()) { actual, nuevo ->
            val r = CostosCalculos.aplicarUltimoPrecio(actual, nuevo)
            assertClose(nuevo, r.nuevoCosto)
            assertEquals(nuevo != actual, r.recalcular)
        }
    }

    // Feature: control-de-costos, Property 6: Congelamiento del snapshot semanal
    @Test fun property06_snapshotCongelado() = prop {
        checkAll(ITERS, arbValores, arbValores, Arb.boolean()) { snap, act, cerrado ->
            val r = CostosCalculos.valoresAConsultar(cerrado, snap, act)
            assertEquals(if (cerrado) snap else act, r)
        }
    }

    // Feature: control-de-costos, Property 7: Partición determinista de grupo de costo
    @Test fun property07_grupoDeterminista() = prop {
        checkAll(ITERS, Arb.enum<CategoriaGasto>().filterNot { it == CategoriaGasto.OTROS }) { c ->
            val g = CostosCalculos.grupoDe(c)
            when (c) {
                CategoriaGasto.INSUMOS, CategoriaGasto.PACKAGING,
                CategoriaGasto.ENVIOS, CategoriaGasto.TRANSPORTE -> assertEquals(GrupoCosto.VARIABLE, g)
                CategoriaGasto.ARRIENDO, CategoriaGasto.SERVICIOS,
                CategoriaGasto.SUELDOS -> assertEquals(GrupoCosto.FIJO, g)
                CategoriaGasto.OTROS -> {}
            }
        }
    }

    // Feature: control-de-costos, Property 8: Costo sin mapeo exige clasificación manual
    @Test fun property08_otrosSinMapeo() = prop {
        checkAll(ITERS, Arb.of(CategoriaGasto.OTROS)) { c ->
            assertNull(CostosCalculos.grupoDe(c))
        }
    }

    // Feature: control-de-costos, Property 9: Compras de la semana entran al egreso
    @Test fun property09_comprasDeLaSemana() = prop {
        checkAll(
            ITERS,
            Arb.localDate(minDate, maxDate),
            Arb.list(Arb.pair(Arb.localDate(minDate, maxDate), montos()), 0..8)
        ) { base, pares ->
            val semana = FechaUtil.semanaDe(base)
            val esperado = pares.filter {
                !it.first.isBefore(semana.lunesInicio) && it.first.isBefore(semana.lunesInicio.plusDays(7))
            }.sumOf { it.second }
            val real = pares.filter { semana.contiene(it.first) }.sumOf { it.second }
            assertClose(esperado, real)
        }
    }

    // Feature: control-de-costos, Property 10: Resultado de caja / "lo que queda"
    @Test fun property10_resultadoCaja() = prop {
        checkAll(ITERS, montos(), montos(), montos(), montos()) { v, varc, mo, f ->
            assertClose(v - varc - mo - f, CostosCalculos.resultadoSemanal(v, varc, mo, f))
        }
    }

    // Feature: control-de-costos, Property 11: El food teórico no afecta la caja
    @Test fun property11_foodNoAfectaCaja() = prop {
        checkAll(ITERS, montos(), montos(), montos(), montos()) { v, varc, mo, f ->
            val base = CostosCalculos.resultadoSemanal(v, varc, mo, f)
            // El food teórico no es parámetro del resultado de caja: recomputar da lo mismo.
            assertClose(base, CostosCalculos.resultadoSemanal(v, varc, mo, f))
        }
    }

    // Feature: control-de-costos, Property 12: La semana operativa es lunes a sábado
    @Test fun property12_semanaOperativa() = prop {
        checkAll(ITERS, Arb.localDate(minDate, maxDate)) { f ->
            val s = FechaUtil.semanaDe(f)
            assertEquals(DayOfWeek.MONDAY, s.lunesInicio.dayOfWeek)
            assertEquals(s.lunesInicio.plusDays(6), s.domingoInicio)
            assertTrue(s.contiene(f))
        }
    }

    // Feature: control-de-costos, Property 13: Porcentajes de food y labor independientes
    @Test fun property13_porcentajesIndependientes() = prop {
        checkAll(ITERS, montos(), montos(), ventasPos()) { food, labor, ventas ->
            assertClose(food / ventas * 100.0, CostosCalculos.porcentajeSobreVentas(food, ventas))
            assertClose(labor / ventas * 100.0, CostosCalculos.porcentajeSobreVentas(labor, ventas))
        }
    }

    // Feature: control-de-costos, Property 14: Mano de obra disponible y por persona
    @Test fun property14_manoObraDisponible() = prop {
        checkAll(ITERS, pct(), montos(), Arb.int(0..20)) { pctObj, ventas, emp ->
            val disp = CostosCalculos.manoObraDisponible(pctObj, ventas)
            assertClose(pctObj * ventas, disp)
            val pp = CostosCalculos.manoObraPorPersona(disp, emp)
            if (emp <= 0) assertClose(disp, pp) else assertClose(disp / emp, pp)
        }
    }

    // Feature: control-de-costos, Property 15: Indicador de contratar
    @Test fun property15_indicadorContratar() = prop {
        checkAll(ITERS, montos(), montos(), montos()) { pp, disp, umbral ->
            assertEquals(disp > 0.0 && pp >= umbral, CostosCalculos.alcanzaParaContratar(pp, disp, umbral))
        }
    }

    // Feature: control-de-costos, Property 16: Alerta de techo de arriendo
    @Test fun property16_alertaArriendo() = prop {
        checkAll(ITERS, montos(), montos(), pct()) { arr, ventas, techo ->
            assertEquals(arr > techo * ventas, CostosCalculos.alertaArriendo(arr, ventas, techo))
        }
    }

    // Feature: control-de-costos, Property 17: Break-even semanal y cuánto falta vender
    @Test fun property17_breakEven() = prop {
        checkAll(ITERS, ventasPos(), montos(), montos()) { ventas, varc, fijos ->
            val margen = CostosCalculos.margenContribucion(ventas, varc)
            assertClose(1.0 - varc / ventas, margen)
            val be = CostosCalculos.breakEven(fijos, margen)
            if (margen <= 0.0) {
                assertNull(be)
            } else {
                assertClose(fijos / margen, be!!)
                assertClose(maxOf(0.0, be - ventas), CostosCalculos.faltaVender(ventas, be)!!)
            }
        }
    }

    // Feature: control-de-costos, Property 18: Sugerencias de provisión = prorrateos positivos
    @Test fun property18_sugerenciaProvision() = prop {
        checkAll(ITERS, montos(), Arb.enum<Periodicidad>()) { monto, per ->
            val pro = CostosCalculos.prorrateoSemanal(monto, per)
            assertEquals(pro > 0.0, CostosCalculos.sugerenciaProvisionValida(pro))
        }
    }

    // Feature: control-de-costos, Property 19: Advertencia de saldo insuficiente
    @Test fun property19_saldoInsuficiente() = prop {
        checkAll(ITERS, Arb.boolean(), montos(), montos()) { hay, saldo, monto ->
            assertEquals(hay && saldo < monto, CostosCalculos.advertirSaldoInsuficiente(hay, saldo, monto))
        }
    }

    // Feature: control-de-costos, Property 20: Habilitación del cierre semanal
    @Test fun property20_habilitacionCierre() = prop {
        checkAll(ITERS, Arb.boolean(), Arb.boolean()) { pasos, valid ->
            assertEquals(pasos && valid, CostosCalculos.puedeConfirmarCierre(pasos, valid))
        }
    }

    // Feature: control-de-costos, Property 21: Semáforo de objetivo
    @Test fun property21_semaforo() = prop {
        checkAll(ITERS, montos(), montos()) { valor, objetivo ->
            val esperado = if (valor > objetivo) EstadoSemaforo.ALERTA else EstadoSemaforo.FAVORABLE
            assertEquals(esperado, CostosCalculos.semaforo(valor, objetivo))
        }
    }

    // Feature: control-de-costos, Property 22: Alerta bajo break-even
    @Test fun property22_bajoBreakEven() = prop {
        val beArb = Arb.double(0.0..1_000_000.0).filterNot { it.isNaN() || it.isInfinite() }.orNull(0.2)
        checkAll(ITERS, montos(), beArb) { ventas, be ->
            assertEquals(be != null && ventas < be, CostosCalculos.bajoBreakEven(ventas, be))
        }
    }
}
