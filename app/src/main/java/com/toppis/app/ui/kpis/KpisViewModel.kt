package com.toppis.app.ui.kpis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.CategoriaMenu
import com.toppis.app.data.db.entities.EstadoVenta
import com.toppis.app.data.db.entities.Periodicidad
import com.toppis.app.data.models.ItemMenu
import com.toppis.app.data.models.ItemVentaMenu
import com.toppis.app.data.models.Venta
import com.toppis.app.data.repository.CierreSemanalRepository
import com.toppis.app.data.repository.ConfigCostosRepository
import com.toppis.app.data.repository.CostoFijoRepository
import com.toppis.app.data.repository.LocalSession
import com.toppis.app.data.repository.ResultadoSemanalRepository
import com.toppis.app.data.supabase.SupabaseClient
import com.toppis.app.data.util.FechaUtil
import com.toppis.app.data.util.SemanaOperativa
import com.toppis.app.domain.costos.ResultadoSemanal
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Kpis(
    val ventasSemana: Double = 0.0,
    val ventasHoy: Double = 0.0,
    val ticketPromedio: Double = 0.0,
    val totalVentasSemana: Int = 0,
    val foodCostPct: Double = 0.0,
    val laborCostPct: Double = 0.0,
    val primeCostPct: Double = 0.0,
    val mermaCostoSemana: Double = 0.0,
    val articulosBajoPar: Int = 0,
    val lotesProxVencer: Int = 0,
    val hamburguesasVendidas: Int = 0,
    val semanaContieneHoy: Boolean = true
)

/** Delivery de un día concreto (día en formato "EEE d MMM", hora de Chile). */
data class DiaDelivery(val dia: String, val monto: Double, val pedidos: Int)

/** Resumen de delivery de una semana: total + desglose por día. */
data class DeliveryMes(
    val total: Double = 0.0,
    val pedidosConEnvio: Int = 0,
    val porDia: List<DiaDelivery> = emptyList()
)

class KpisViewModel : ViewModel() {

    private val zonaCL = ZoneId.of("America/Santiago")

    private val _kpis = MutableStateFlow(Kpis())
    val kpis: StateFlow<Kpis> = _kpis.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    // Semana operativa seleccionada (lun–sáb); controla KPIs y delivery.
    private val _semana = MutableStateFlow(FechaUtil.semanaActual())
    val semana: StateFlow<SemanaOperativa> = _semana.asStateFlow()

    private val _delivery = MutableStateFlow(DeliveryMes())
    val delivery: StateFlow<DeliveryMes> = _delivery.asStateFlow()

    // Resultado semanal completo (variables + mano de obra + fijos + resultado).
    private val _resultado = MutableStateFlow<ResultadoSemanal?>(null)
    val resultado: StateFlow<ResultadoSemanal?> = _resultado.asStateFlow()

    private val resultadoRepo = ResultadoSemanalRepository(
        ConfigCostosRepository(), CostoFijoRepository(), CierreSemanalRepository()
    )

    init { cargar() }

    /** Cambia la semana (delta = -1 anterior, +1 siguiente) y recarga todo. */
    fun cambiarSemana(delta: Long) {
        _semana.value = FechaUtil.semanaOffset(_semana.value, delta)
        cargar()
    }

    /** Convierte un timestamp ISO de Supabase (UTC) a fecha local de Chile. */
    private fun fechaCL(iso: String?): LocalDate? = try {
        if (iso.isNullOrBlank()) null
        else OffsetDateTime.parse(iso).atZoneSameInstant(zonaCL).toLocalDate()
    } catch (_: Exception) { null }

    fun cargar() {
        viewModelScope.launch {
            _cargando.value = true
            val client = SupabaseClient.client
            val sem = _semana.value
            val hoyCL = LocalDate.now(zonaCL)
            val lid = LocalSession.activoId.value

            // Ventana ISO con 1 día de margen a cada lado (para no perder ventas de
            // borde por diferencia horaria); luego se filtra con precisión por fecha CL.
            val desdeIso = sem.lunesInicio.minusDays(1).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val hastaIso = sem.finExclusivo.plusDays(1).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            // Rango exacto de la semana para las RPC/consultas por fecha.
            val semDesdeIso = sem.isoDesde
            val semHastaIso = sem.isoHasta

            // Consultas independientes en paralelo.
            val ventasDef = async {
                try {
                    client.postgrest.from("ventas").select {
                        filter {
                            gte("fecha", desdeIso); lt("fecha", hastaIso)
                            if (lid != null) eq("local_id", lid)
                        }
                    }.decodeList<Venta>()
                        .filter { it.estado == EstadoVenta.COMPLETADA }
                        .filter { v -> fechaCL(v.fecha)?.let { sem.contiene(it) } ?: false }
                } catch (_: Exception) { emptyList() }
            }
            val foodDef = async {
                try {
                    client.postgrest.rpc("consumo_teorico_periodo", buildJsonObject {
                        put("p_desde", semDesdeIso); put("p_hasta", semHastaIso)
                    }).decodeList<com.toppis.app.data.repository.ConsumoTeoricoRow>().sumOf { it.costo }
                } catch (_: Exception) { 0.0 }
            }
            val jornadasDef = async {
                try {
                    client.postgrest.from("jornadas").select().decodeList<com.toppis.app.data.models.Jornada>()
                        .filter {
                            val f = runCatching { LocalDate.parse((it.fecha ?: "").take(10)) }.getOrNull()
                            f != null && sem.contiene(f)
                        }.sumOf { it.costo }
                } catch (_: Exception) { 0.0 }
            }
            val sueldosDef = async {
                try {
                    client.postgrest.from("empleados").select().decodeList<com.toppis.app.data.models.Empleado>()
                        .filter { it.activo && it.tipoPago == com.toppis.app.data.db.entities.TipoPago.SUELDO_FIJO }
                        .sumOf { it.monto }
                } catch (_: Exception) { 0.0 }
            }
            val mermaDef = async {
                try {
                    client.postgrest.from("mermas").select().decodeList<com.toppis.app.data.models.Merma>()
                        .filter { m -> fechaCL(m.fecha)?.let { sem.contiene(it) } ?: false }
                        .sumOf { it.costo }
                } catch (_: Exception) { 0.0 }
            }
            val bajoParDef = async {
                try {
                    client.postgrest.from("articulos").select().decodeList<com.toppis.app.data.models.Articulo>()
                        .count { it.activo && it.parLevel > 0 && it.stockBase < it.parLevel }
                } catch (_: Exception) { 0 }
            }
            val lotesDef = async {
                val hoyStr = hoyCL.toString()
                val enUnaSemana = hoyCL.plusDays(7).toString()
                try {
                    client.postgrest.from("compra_detalle").select().decodeList<com.toppis.app.data.models.CompraDetalle>()
                        .count { !it.vencimiento.isNullOrBlank() && it.vencimiento >= hoyStr && it.vencimiento <= enUnaSemana }
                } catch (_: Exception) { 0 }
            }
            val menuDef = async {
                try { client.postgrest.from("items_menu").select().decodeList<ItemMenu>() } catch (_: Exception) { emptyList() }
            }
            val resultadoDef = async { runCatching { resultadoRepo.getResultado(sem) }.getOrNull() }

            val ventas = ventasDef.await()

            // Hamburguesas vendidas en la semana (incluye las de promos, que se
            // materializan como filas de items_venta_menu con su item de menú).
            val menuById = menuDef.await().associateBy { it.id }
            val ventaIds = ventas.map { it.id }.toSet()
            val hamburguesas = if (ventaIds.isEmpty()) 0 else try {
                client.postgrest.from("items_venta_menu").select().decodeList<ItemVentaMenu>()
                    .filter { it.ventaId in ventaIds }
                    .filter { CategoriaMenu.porLabel(menuById[it.itemMenuId]?.categoria) == CategoriaMenu.HAMBURGUESAS }
                    .sumOf { it.cantidad }
            } catch (_: Exception) { 0 }
            val food = foodDef.await()
            // Sueldo fijo mensual prorrateado a la semana (÷ 4.33).
            val labor = jornadasDef.await() + (sueldosDef.await() / Periodicidad.MENSUAL.divisorSemanal)

            val ventasSemana = ventas.sumOf { it.total }
            val ventasHoy = ventas.filter { fechaCL(it.fecha) == hoyCL }.sumOf { it.total }
            val ticket = if (ventas.isNotEmpty()) ventasSemana / ventas.size else 0.0

            val foodPct = if (ventasSemana > 0) food / ventasSemana * 100 else 0.0
            val laborPct = if (ventasSemana > 0) labor / ventasSemana * 100 else 0.0
            val primePct = if (ventasSemana > 0) (food + labor) / ventasSemana * 100 else 0.0

            _kpis.value = Kpis(
                ventasSemana = ventasSemana,
                ventasHoy = ventasHoy,
                ticketPromedio = ticket,
                totalVentasSemana = ventas.size,
                foodCostPct = foodPct,
                laborCostPct = laborPct,
                primeCostPct = primePct,
                mermaCostoSemana = mermaDef.await(),
                articulosBajoPar = bajoParDef.await(),
                lotesProxVencer = lotesDef.await(),
                hamburguesasVendidas = hamburguesas,
                semanaContieneHoy = sem.contiene(hoyCL)
            )
            _resultado.value = resultadoDef.await()

            // Delivery de la semana (por día, hora de Chile).
            val diaFmt = DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale("es", "CL"))
            val conEnvio = ventas.filter { it.montoEnvio > 0 }
            val porDia = conEnvio
                .mapNotNull { v -> fechaCL(v.fecha)?.let { it to v } }
                .groupBy { it.first }
                .toSortedMap()
                .map { (dia, lst) ->
                    DiaDelivery(
                        dia = dia.format(diaFmt).replaceFirstChar { it.uppercase() },
                        monto = lst.sumOf { it.second.montoEnvio },
                        pedidos = lst.size
                    )
                }
            _delivery.value = DeliveryMes(
                total = conEnvio.sumOf { it.montoEnvio },
                pedidosConEnvio = conEnvio.size,
                porDia = porDia
            )

            _cargando.value = false
        }
    }
}
