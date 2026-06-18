package com.toppis.app.ui.kpis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.db.entities.EstadoVenta
import com.toppis.app.data.models.Venta
import com.toppis.app.data.repository.LocalSession
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class Kpis(
    val ventasMes: Double = 0.0,
    val ventasHoy: Double = 0.0,
    val ticketPromedio: Double = 0.0,
    val totalVentasMes: Int = 0,
    val foodCostPct: Double = 0.0,
    val laborCostPct: Double = 0.0,
    val primeCostPct: Double = 0.0,
    val mermaCostoMes: Double = 0.0,
    val articulosBajoPar: Int = 0,
    val lotesProxVencer: Int = 0
)

class KpisViewModel : ViewModel() {

    private val _kpis = MutableStateFlow(Kpis())
    val kpis: StateFlow<Kpis> = _kpis.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _cargando.value = true
            val client = SupabaseClient.client
            val ym = YearMonth.now()
            val desdeIso = ym.atDay(1).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val hastaIso = ym.plusMonths(1).atDay(1).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val hoyIso = LocalDate.now().atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val lid = LocalSession.activoId.value

            // Ventas del mes
            val ventas = try {
                client.postgrest.from("ventas").select {
                    filter {
                        gte("fecha", desdeIso)
                        if (lid != null) eq("local_id", lid)
                    }
                }.decodeList<Venta>().filter { it.estado == EstadoVenta.COMPLETADA }
            } catch (_: Exception) { emptyList() }

            val ventasMes = ventas.sumOf { it.total }
            val ventasHoy = ventas.filter { (it.fecha ?: "") >= hoyIso }.sumOf { it.total }
            val ticket = if (ventas.isNotEmpty()) ventasMes / ventas.size else 0.0

            // Food cost teórico
            val food = try {
                client.postgrest.rpc("consumo_teorico_periodo", buildJsonObject {
                    put("p_desde", desdeIso); put("p_hasta", hastaIso)
                }).decodeList<com.toppis.app.data.repository.ConsumoTeoricoRow>().sumOf { it.costo }
            } catch (_: Exception) { 0.0 }

            // Labor: jornadas + sueldos fijos
            val jornadasCosto = try {
                client.postgrest.from("jornadas").select().decodeList<com.toppis.app.data.models.Jornada>()
                    .filter { (it.fecha ?: "") >= desdeIso.take(10) && (it.fecha ?: "") < hastaIso.take(10) }
                    .sumOf { it.costo }
            } catch (_: Exception) { 0.0 }
            val sueldosFijos = try {
                client.postgrest.from("empleados").select().decodeList<com.toppis.app.data.models.Empleado>()
                    .filter { it.activo && it.tipoPago == com.toppis.app.data.db.entities.TipoPago.SUELDO_FIJO }
                    .sumOf { it.monto }
            } catch (_: Exception) { 0.0 }
            val labor = jornadasCosto + sueldosFijos

            val foodPct = if (ventasMes > 0) food / ventasMes * 100 else 0.0
            val laborPct = if (ventasMes > 0) labor / ventasMes * 100 else 0.0
            val primePct = if (ventasMes > 0) (food + labor) / ventasMes * 100 else 0.0

            // Merma costo mes
            val mermaCosto = try {
                client.postgrest.from("mermas").select().decodeList<com.toppis.app.data.models.Merma>()
                    .filter { (it.fecha ?: "") >= desdeIso }
                    .sumOf { it.costo }
            } catch (_: Exception) { 0.0 }

            // Artículos bajo par
            val bajoPar = try {
                client.postgrest.from("articulos").select().decodeList<com.toppis.app.data.models.Articulo>()
                    .count { it.activo && it.parLevel > 0 && it.stockBase < it.parLevel }
            } catch (_: Exception) { 0 }

            // Lotes próximos a vencer (< 7 días)
            val enUnaSemana = LocalDate.now().plusDays(7).toString()
            val hoyStr = LocalDate.now().toString()
            val lotesVencer = try {
                client.postgrest.from("compra_detalle").select().decodeList<com.toppis.app.data.models.CompraDetalle>()
                    .count { !it.vencimiento.isNullOrBlank() && it.vencimiento >= hoyStr && it.vencimiento <= enUnaSemana }
            } catch (_: Exception) { 0 }

            _kpis.value = Kpis(
                ventasMes = ventasMes,
                ventasHoy = ventasHoy,
                ticketPromedio = ticket,
                totalVentasMes = ventas.size,
                foodCostPct = foodPct,
                laborCostPct = laborPct,
                primeCostPct = primePct,
                mermaCostoMes = mermaCosto,
                articulosBajoPar = bajoPar,
                lotesProxVencer = lotesVencer
            )
            _cargando.value = false
        }
    }
}
