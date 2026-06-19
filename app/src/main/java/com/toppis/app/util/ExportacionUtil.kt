package com.toppis.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.toppis.app.data.models.Gasto
import com.toppis.app.data.models.MovimientoSobre
import com.toppis.app.data.models.Articulo
import com.toppis.app.data.models.Sobre
import com.toppis.app.data.models.Venta
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportacionUtil {

    private val sdfTs     get() = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
    private val sdfDisplay get() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    /** Formatea un timestamp ISO (Supabase) a "dd/MM/yyyy HH:mm" en hora local. */
    private fun fmtIso(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            OffsetDateTime.parse(iso)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } catch (e: Exception) {
            iso.take(16).replace("T", " ")
        }
    }

    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_CSV  = "text/csv"
    const val MIME_ZIP  = "application/zip"

    // ── Ventas Excel ──────────────────────────────────────────────────────────

    fun exportarVentasExcel(context: Context, ventas: List<Venta>): Uri {
        val wb = XSSFWorkbook()

        // Hoja "Ventas"
        val sheetV = wb.createSheet("Ventas")
        sheetV.createRow(0).also { r ->
            listOf("ID", "Fecha", "Total", "Método Pago", "Estado")
                .forEachIndexed { i, h -> r.createCell(i).setCellValue(h) }
        }
        ventas.forEachIndexed { idx, v ->
            sheetV.createRow(idx + 1).also { r ->
                r.createCell(0).setCellValue(v.id.toDouble())
                r.createCell(1).setCellValue(fmtIso(v.fecha))
                r.createCell(2).setCellValue(v.total)
                r.createCell(3).setCellValue(v.metodoPago?.name ?: "-")
                r.createCell(4).setCellValue(v.estado.name)
            }
        }

        // Hoja "Resumen"
        val sheetR = wb.createSheet("Resumen")
        val totalIngresos = ventas.sumOf { it.total }
        val promedio = if (ventas.isNotEmpty()) totalIngresos / ventas.size else 0.0
        listOf("Total ventas" to ventas.size.toDouble(),
               "Total ingresos" to totalIngresos,
               "Venta promedio" to promedio)
            .forEachIndexed { idx, (label, valor) ->
                sheetR.createRow(idx).also { r ->
                    r.createCell(0).setCellValue(label)
                    r.createCell(1).setCellValue(valor)
                }
            }

        val fileName = "toppis_ventas_${sdfTs.format(Date())}.xlsx"
        return guardar(context, fileName, MIME_XLSX) { stream -> wb.use { it.write(stream) } }
    }

    // ── Gastos Excel ──────────────────────────────────────────────────────────

    fun exportarGastosExcel(context: Context, gastos: List<Gasto>): Uri {
        val wb = XSSFWorkbook()

        // Hoja "Gastos"
        val sheetG = wb.createSheet("Gastos")
        sheetG.createRow(0).also { r ->
            listOf("ID", "Fecha", "Descripción", "Categoría", "Monto", "SobreID")
                .forEachIndexed { i, h -> r.createCell(i).setCellValue(h) }
        }
        gastos.forEachIndexed { idx, g ->
            sheetG.createRow(idx + 1).also { r ->
                r.createCell(0).setCellValue(g.id.toDouble())
                r.createCell(1).setCellValue(fmtIso(g.fecha))
                r.createCell(2).setCellValue(g.descripcion)
                r.createCell(3).setCellValue(g.categoria.label)
                r.createCell(4).setCellValue(g.monto)
                r.createCell(5).setCellValue(g.sobreId?.toDouble() ?: 0.0)
            }
        }

        // Hoja "Por Categoría"
        val sheetC = wb.createSheet("Por Categoría")
        sheetC.createRow(0).also { r ->
            r.createCell(0).setCellValue("Categoría")
            r.createCell(1).setCellValue("Total")
            r.createCell(2).setCellValue("Cantidad")
        }
        gastos.groupBy { it.categoria }.entries.forEachIndexed { idx, (cat, lista) ->
            sheetC.createRow(idx + 1).also { r ->
                r.createCell(0).setCellValue(cat.label)
                r.createCell(1).setCellValue(lista.sumOf { it.monto })
                r.createCell(2).setCellValue(lista.size.toDouble())
            }
        }

        val fileName = "toppis_gastos_${sdfTs.format(Date())}.xlsx"
        return guardar(context, fileName, MIME_XLSX) { stream -> wb.use { it.write(stream) } }
    }

    // ── Sobres Excel ──────────────────────────────────────────────────────────

    fun exportarSobresExcel(
        context: Context,
        sobres: List<Sobre>,
        movimientos: List<MovimientoSobre>
    ): Uri {
        val wb = XSSFWorkbook()

        // Hoja "Sobres"
        val sheetS = wb.createSheet("Sobres")
        sheetS.createRow(0).also { r ->
            listOf("ID", "Nombre", "Descripción", "Saldo", "Fecha Creación")
                .forEachIndexed { i, h -> r.createCell(i).setCellValue(h) }
        }
        sobres.forEachIndexed { idx, s ->
            sheetS.createRow(idx + 1).also { r ->
                r.createCell(0).setCellValue(s.id.toDouble())
                r.createCell(1).setCellValue(s.nombre)
                r.createCell(2).setCellValue(s.descripcion)
                r.createCell(3).setCellValue(s.saldo)
                r.createCell(4).setCellValue(fmtIso(s.fechaCreacion))
            }
        }

        // Hoja "Movimientos"
        val sheetM = wb.createSheet("Movimientos")
        sheetM.createRow(0).also { r ->
            listOf("ID", "Fecha", "Tipo", "Descripción", "Monto", "Origen ID", "Destino ID")
                .forEachIndexed { i, h -> r.createCell(i).setCellValue(h) }
        }
        movimientos.forEachIndexed { idx, m ->
            sheetM.createRow(idx + 1).also { r ->
                r.createCell(0).setCellValue(m.id.toDouble())
                r.createCell(1).setCellValue(fmtIso(m.fecha))
                r.createCell(2).setCellValue(m.tipo.name)
                r.createCell(3).setCellValue(m.descripcion)
                r.createCell(4).setCellValue(m.monto)
                r.createCell(5).setCellValue(m.origenId?.toDouble() ?: 0.0)
                r.createCell(6).setCellValue(m.destinoId?.toDouble() ?: 0.0)
            }
        }

        val fileName = "toppis_sobres_${sdfTs.format(Date())}.xlsx"
        return guardar(context, fileName, MIME_XLSX) { stream -> wb.use { it.write(stream) } }
    }

    // ── CSV genérico ──────────────────────────────────────────────────────────

    fun exportarCSV(
        context: Context,
        datos: List<Map<String, String>>,
        nombreArchivo: String
    ): Uri {
        if (datos.isEmpty()) throw IllegalArgumentException("Sin datos para exportar")
        val headers = datos.first().keys.toList()
        val csv = buildString {
            appendLine(headers.joinToString(",") { "\"$it\"" })
            datos.forEach { row ->
                appendLine(headers.joinToString(",") { key ->
                    "\"${row[key]?.replace("\"", "\"\"") ?: ""}\""
                })
            }
        }
        val fileName = "toppis_${nombreArchivo}_${sdfTs.format(Date())}.csv"
        return guardar(context, fileName, MIME_CSV) { stream ->
            stream.write(csv.toByteArray(Charsets.UTF_8))
        }
    }

    // ── ZIP con todo ──────────────────────────────────────────────────────────

    fun exportarTodoZip(
        context: Context,
        ventas: List<Venta>,
        gastos: List<Gasto>,
        sobres: List<Sobre>,
        movimientos: List<MovimientoSobre>,
        insumos: List<Articulo>
    ): Uri {
        val fileName = "toppis_export_${sdfTs.format(Date())}.zip"
        return guardar(context, fileName, MIME_ZIP) { stream ->
            ZipOutputStream(stream).use { zip ->

                // ventas.xlsx
                zip.putNextEntry(ZipEntry("ventas.xlsx"))
                XSSFWorkbook().also { wb ->
                    buildVentasSheet(wb, ventas)
                    wb.write(zip)
                    wb.close()
                }
                zip.closeEntry()

                // gastos.xlsx
                zip.putNextEntry(ZipEntry("gastos.xlsx"))
                XSSFWorkbook().also { wb ->
                    buildGastosSheet(wb, gastos)
                    wb.write(zip)
                    wb.close()
                }
                zip.closeEntry()

                // sobres.xlsx
                zip.putNextEntry(ZipEntry("sobres.xlsx"))
                XSSFWorkbook().also { wb ->
                    buildSobresSheet(wb, sobres, movimientos)
                    wb.write(zip)
                    wb.close()
                }
                zip.closeEntry()

                // inventario.csv
                zip.putNextEntry(ZipEntry("inventario.csv"))
                val headers = listOf("ID", "Nombre", "Dimension", "UnidadBase", "StockBase", "CostoBase", "UnidadCompra", "Activo")
                val csvContent = buildString {
                    appendLine(headers.joinToString(",") { "\"$it\"" })
                    insumos.forEach { p ->
                        val row = listOf(p.id, p.nombre, p.dimension.name, p.unidadBase, p.stockBase, p.costoBase, p.unidadCompra, p.activo)
                        appendLine(row.joinToString(",") { "\"$it\"" })
                    }
                }
                zip.write(csvContent.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    // ── Helper central de guardado ────────────────────────────────────────────

    /**
     * Crea el archivo y entrega un [OutputStream] abierto al [writer].
     * - API 29+: MediaStore Downloads (sin subcarpeta).
     * - API 26-28: Downloads público + FileProvider para URI compartible.
     */
    private fun guardar(
        context: Context,
        fileName: String,
        mimeType: String,
        writer: (OutputStream) -> Unit
    ): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ── Android 10+ ───────────────────────────────────────────────────
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
            ) ?: throw IllegalStateException("MediaStore no pudo crear el archivo")
            context.contentResolver.openOutputStream(uri)
                ?.use { writer(it) }
                ?: throw IllegalStateException("No se pudo abrir el stream de escritura")
            uri
        } else {
            // ── Android 8-9 ───────────────────────────────────────────────────
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { writer(it) }
            // FileProvider para que el Intent ACTION_VIEW funcione sin FileUriExposedException
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    // ── Builders de hojas (reutilizados en ZIP) ───────────────────────────────

    private fun buildVentasSheet(wb: XSSFWorkbook, ventas: List<Venta>) {
        val sheet = wb.createSheet("Ventas")
        sheet.createRow(0).also { r ->
            listOf("ID", "Fecha", "Total", "Método Pago", "Estado")
                .forEachIndexed { i, h -> r.createCell(i).setCellValue(h) }
        }
        ventas.forEachIndexed { idx, v ->
            sheet.createRow(idx + 1).also { r ->
                r.createCell(0).setCellValue(v.id.toDouble())
                r.createCell(1).setCellValue(fmtIso(v.fecha))
                r.createCell(2).setCellValue(v.total)
                r.createCell(3).setCellValue(v.metodoPago?.name ?: "-")
                r.createCell(4).setCellValue(v.estado.name)
            }
        }
    }

    private fun buildGastosSheet(wb: XSSFWorkbook, gastos: List<Gasto>) {
        val sheet = wb.createSheet("Gastos")
        sheet.createRow(0).also { r ->
            listOf("ID", "Fecha", "Descripción", "Categoría", "Monto", "SobreID")
                .forEachIndexed { i, h -> r.createCell(i).setCellValue(h) }
        }
        gastos.forEachIndexed { idx, g ->
            sheet.createRow(idx + 1).also { r ->
                r.createCell(0).setCellValue(g.id.toDouble())
                r.createCell(1).setCellValue(fmtIso(g.fecha))
                r.createCell(2).setCellValue(g.descripcion)
                r.createCell(3).setCellValue(g.categoria.label)
                r.createCell(4).setCellValue(g.monto)
                r.createCell(5).setCellValue(g.sobreId?.toDouble() ?: 0.0)
            }
        }
    }

    private fun buildSobresSheet(wb: XSSFWorkbook, sobres: List<Sobre>, movimientos: List<MovimientoSobre>) {
        val sheetS = wb.createSheet("Sobres")
        sheetS.createRow(0).also { r ->
            listOf("ID", "Nombre", "Descripción", "Saldo", "Fecha Creación")
                .forEachIndexed { i, h -> r.createCell(i).setCellValue(h) }
        }
        sobres.forEachIndexed { idx, s ->
            sheetS.createRow(idx + 1).also { r ->
                r.createCell(0).setCellValue(s.id.toDouble())
                r.createCell(1).setCellValue(s.nombre)
                r.createCell(2).setCellValue(s.descripcion)
                r.createCell(3).setCellValue(s.saldo)
                r.createCell(4).setCellValue(fmtIso(s.fechaCreacion))
            }
        }
        val sheetM = wb.createSheet("Movimientos")
        sheetM.createRow(0).also { r ->
            listOf("ID", "Fecha", "Tipo", "Descripción", "Monto", "Origen ID", "Destino ID")
                .forEachIndexed { i, h -> r.createCell(i).setCellValue(h) }
        }
        movimientos.forEachIndexed { idx, m ->
            sheetM.createRow(idx + 1).also { r ->
                r.createCell(0).setCellValue(m.id.toDouble())
                r.createCell(1).setCellValue(fmtIso(m.fecha))
                r.createCell(2).setCellValue(m.tipo.name)
                r.createCell(3).setCellValue(m.descripcion)
                r.createCell(4).setCellValue(m.monto)
                r.createCell(5).setCellValue(m.origenId?.toDouble() ?: 0.0)
                r.createCell(6).setCellValue(m.destinoId?.toDouble() ?: 0.0)
            }
        }
    }
}
