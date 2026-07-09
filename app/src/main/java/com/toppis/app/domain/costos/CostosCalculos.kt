package com.toppis.app.domain.costos

import com.toppis.app.data.db.entities.CategoriaGasto
import com.toppis.app.data.db.entities.EstadoSemaforo
import com.toppis.app.data.db.entities.GrupoCosto
import com.toppis.app.data.db.entities.Periodicidad
import com.toppis.app.data.models.CostoFijo

/** Resultado de aplicar el "último precio" a un artículo. */
data class ResultadoUltimoPrecio(val nuevoCosto: Double, val recalcular: Boolean)

/** Valores de caja de una semana (para el congelamiento de snapshot). */
data class ValoresSemana(
    val ventas: Double,
    val variable: Double,
    val manoObra: Double,
    val fijos: Double
)

/**
 * Capa de cálculo PURA del control de costos: sin dependencias de red ni Android.
 * Todo el dinero en CLP con IVA incluido. Es la base de los tests de propiedad.
 */
object CostosCalculos {

    // ── Prorrateo de fijos ──────────────────────────────────────────────────

    /** Prorratea el monto de un fijo a su porción semanal según periodicidad. */
    fun prorrateoSemanal(monto: Double, periodicidad: Periodicidad): Double =
        monto / periodicidad.divisorSemanal

    /** Suma de prorrateos semanales de los fijos ACTIVOS (ignora inactivos). */
    fun totalFijosSemanales(fijos: List<CostoFijo>): Double =
        fijos.filter { it.activo }.sumOf { prorrateoSemanal(it.monto, it.periodicidad) }

    // ── Resultado de caja ("lo que queda") ──────────────────────────────────

    fun resultadoSemanal(ventas: Double, variable: Double, manoObra: Double, fijos: Double): Double =
        ventas - variable - manoObra - fijos

    /** Consulta congelada: si la semana está cerrada devuelve el snapshot, si no el cálculo en vivo. */
    fun valoresAConsultar(cerrado: Boolean, snapshot: ValoresSemana, actual: ValoresSemana): ValoresSemana =
        if (cerrado) snapshot else actual

    // ── Márgenes y break-even ─────────────────────────────────────────────────

    /** Margen de contribución = 1 − (%variable). 0 si no hay ventas. */
    fun margenContribucion(ventas: Double, costoVariable: Double): Double =
        if (ventas <= 0.0) 0.0 else 1.0 - (costoVariable / ventas)

    /** Break-even semanal; null si el margen es ≤ 0 (no calculable). */
    fun breakEven(fijos: Double, margen: Double): Double? =
        if (margen <= 0.0) null else fijos / margen

    /** Cuánto falta vender para no perder; null si el break-even no es calculable. */
    fun faltaVender(ventas: Double, breakEven: Double?): Double? =
        if (breakEven == null) null else (breakEven - ventas).coerceAtLeast(0.0)

    /** true si las ventas están bajo un break-even calculable. */
    fun bajoBreakEven(ventas: Double, breakEven: Double?): Boolean =
        breakEven != null && ventas < breakEven

    // ── Mano de obra ─────────────────────────────────────────────────────────

    fun manoObraDisponible(pctObjetivo: Double, ventas: Double): Double = pctObjetivo * ventas

    /** Monto por persona; si no hay empleados devuelve el disponible total (presupuesto para contratar). */
    fun manoObraPorPersona(disponible: Double, empleadosActivos: Int): Double =
        if (empleadosActivos <= 0) disponible else disponible / empleadosActivos

    fun alcanzaParaContratar(porPersona: Double, disponibleTotal: Double, umbral: Double): Boolean =
        disponibleTotal > 0.0 && porPersona >= umbral

    // ── Porcentajes y semáforos ───────────────────────────────────────────────

    fun porcentajeSobreVentas(monto: Double, ventas: Double): Double =
        if (ventas <= 0.0) 0.0 else monto / ventas * 100.0

    /** ALERTA si el valor supera el objetivo, FAVORABLE en caso contrario. */
    fun semaforo(valorPct: Double, objetivoPct: Double): EstadoSemaforo =
        if (valorPct > objetivoPct) EstadoSemaforo.ALERTA else EstadoSemaforo.FAVORABLE

    /** Alerta de arriendo: dispara si el arriendo prorrateado supera techo × ventas. */
    fun alertaArriendo(arriendoProrrateado: Double, ventas: Double, techoPct: Double): Boolean =
        arriendoProrrateado > techoPct * ventas

    // ── Costo del artículo por último precio ──────────────────────────────────

    /** Aplica el último precio: el nuevo costo reemplaza al actual; recalcula solo si difiere. */
    fun aplicarUltimoPrecio(costoActual: Double, nuevoPrecio: Double): ResultadoUltimoPrecio =
        ResultadoUltimoPrecio(nuevoCosto = nuevoPrecio, recalcular = nuevoPrecio != costoActual)

    // ── Validaciones / provisión / cierre ─────────────────────────────────────

    /** El monto de un costo fijo es válido si es ≥ 0. */
    fun montoFijoValido(monto: Double): Boolean = monto >= 0.0

    /** Una sugerencia de provisión es válida (se propone) solo si su monto es > 0. */
    fun sugerenciaProvisionValida(montoSemanal: Double): Boolean = montoSemanal > 0.0

    /** Advertir saldo insuficiente solo si hay fijos por provisionar y el saldo no alcanza. */
    fun advertirSaldoInsuficiente(hayFijosPorProvisionar: Boolean, saldo: Double, montoApartar: Double): Boolean =
        hayFijosPorProvisionar && saldo < montoApartar

    /** Se puede confirmar el cierre solo si todos los pasos están completos y las validaciones pasan. */
    fun puedeConfirmarCierre(todosPasosCompletos: Boolean, validacionesOk: Boolean): Boolean =
        todosPasosCompletos && validacionesOk

    // ── Clasificación de grupo de costo ───────────────────────────────────────

    /** Grupo automático de una categoría de costo; null si requiere clasificación manual. */
    fun grupoDe(categoria: CategoriaGasto): GrupoCosto? = when (categoria) {
        CategoriaGasto.INSUMOS,
        CategoriaGasto.PACKAGING,
        CategoriaGasto.ENVIOS,
        CategoriaGasto.TRANSPORTE -> GrupoCosto.VARIABLE
        CategoriaGasto.ARRIENDO,
        CategoriaGasto.SERVICIOS,
        CategoriaGasto.SUELDOS -> GrupoCosto.FIJO
        CategoriaGasto.OTROS -> null
    }
}
