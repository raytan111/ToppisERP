package com.toppis.app.ui.exportacion

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.repository.ExportacionRepository
import com.toppis.app.util.ExportacionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Estado de exportación ─────────────────────────────────────────────────────

sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val uri: Uri, val mimeType: String) : ExportState()
    data class Error(val mensaje: String) : ExportState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ExportacionViewModel(
    private val repository: ExportacionRepository
) : ViewModel() {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun resetState() { _exportState.value = ExportState.Idle }

    private val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun exportarVentas(context: Context, desde: Long = 0L) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            runCatching {
                val ventas = repository.getVentas(desde)
                withContext(Dispatchers.IO) { ExportacionUtil.exportarVentasExcel(context, ventas) }
            }.onSuccess { uri ->
                _exportState.value = ExportState.Success(uri, MIME_XLSX)
            }.onFailure { e ->
                _exportState.value = ExportState.Error(e.message ?: "Error al exportar ventas")
            }
        }
    }

    fun exportarGastos(context: Context, desde: Long = 0L) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            runCatching {
                val gastos = repository.getGastos(desde)
                withContext(Dispatchers.IO) { ExportacionUtil.exportarGastosExcel(context, gastos) }
            }.onSuccess { uri ->
                _exportState.value = ExportState.Success(uri, MIME_XLSX)
            }.onFailure { e ->
                _exportState.value = ExportState.Error(e.message ?: "Error al exportar gastos")
            }
        }
    }

    fun exportarSobres(context: Context) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            runCatching {
                val sobres = repository.getSobres()
                val movimientos = repository.getMovimientos()
                withContext(Dispatchers.IO) { ExportacionUtil.exportarSobresExcel(context, sobres, movimientos) }
            }.onSuccess { uri ->
                _exportState.value = ExportState.Success(uri, MIME_XLSX)
            }.onFailure { e ->
                _exportState.value = ExportState.Error(e.message ?: "Error al exportar sobres")
            }
        }
    }

    fun exportarInventario(context: Context) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            runCatching {
                val articulos = repository.getArticulos()
                val datos = articulos.map { p ->
                    mapOf(
                        "ID" to p.id.toString(),
                        "Nombre" to p.nombre,
                        "Dimension" to p.dimension.name,
                        "UnidadBase" to p.unidadBase,
                        "StockBase" to p.stockBase.toString(),
                        "CostoBase" to p.costoBase.toString(),
                        "UnidadCompra" to p.unidadCompra,
                        "Activo" to p.activo.toString()
                    )
                }
                withContext(Dispatchers.IO) { ExportacionUtil.exportarCSV(context, datos, "inventario") }
            }.onSuccess { uri ->
                _exportState.value = ExportState.Success(uri, "text/csv")
            }.onFailure { e ->
                _exportState.value = ExportState.Error(e.message ?: "Error al exportar inventario")
            }
        }
    }

    fun exportarTodo(context: Context, desde: Long = 0L) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            runCatching {
                val ventas = repository.getVentas(desde)
                val gastos = repository.getGastos(desde)
                val sobres = repository.getSobres()
                val movimientos = repository.getMovimientos()
                val articulos = repository.getArticulos()
                withContext(Dispatchers.IO) {
                    ExportacionUtil.exportarTodoZip(context, ventas, gastos, sobres, movimientos, articulos)
                }
            }.onSuccess { uri ->
                _exportState.value = ExportState.Success(uri, "application/zip")
            }.onFailure { e ->
                _exportState.value = ExportState.Error(e.message ?: "Error al generar ZIP")
            }
        }
    }
}
