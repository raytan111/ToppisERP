package com.toppis.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.toppis.app.data.models.Comprobante
import com.toppis.app.data.repository.LineaComanda
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera un PDF simple del comprobante interno usando PdfDocument (nativo Android).
 * No requiere librerías externas.
 */
object ComprobantePdfUtil {

    private val money = DecimalFormat("$#,##0")

    /**
     * Genera el PDF del comprobante y retorna un Uri compartible (FileProvider).
     */
    fun generarPdf(
        context: Context,
        comprobante: Comprobante,
        lineas: List<LineaComanda>,
        nombreNegocio: String = "Toppis"
    ): android.net.Uri {
        val pageWidth = 380
        val pageHeight = 600
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val pTitle = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val pNormal = Paint().apply { textSize = 12f }
        val pSmall = Paint().apply { textSize = 10f; color = android.graphics.Color.GRAY }
        val pBold = Paint().apply { textSize = 14f; isFakeBoldText = true }

        var y = 40f
        val left = 24f
        val right = pageWidth - 24f

        canvas.drawText(nombreNegocio, left, y, pTitle)
        y += 24f
        canvas.drawText("Comprobante interno #${comprobante.folio}", left, y, pBold)
        y += 18f
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL")).format(Date())
        canvas.drawText(fecha, left, y, pSmall)
        y += 24f
        canvas.drawLine(left, y, right, y, pSmall)
        y += 20f

        // Detalle
        for (l in lineas) {
            canvas.drawText("${l.cantidad}x ${l.nombre}", left, y, pNormal)
            canvas.drawText(money.format(l.subtotal), right - 70f, y, pNormal)
            y += 16f
            if (l.salsas.isNotBlank()) {
                canvas.drawText("   ${l.salsas}", left, y, pSmall)
                y += 14f
            }
        }

        y += 8f
        canvas.drawLine(left, y, right, y, pSmall)
        y += 20f

        canvas.drawText("Neto", left, y, pNormal)
        canvas.drawText(money.format(comprobante.neto), right - 70f, y, pNormal)
        y += 16f
        canvas.drawText("IVA (19%)", left, y, pNormal)
        canvas.drawText(money.format(comprobante.iva), right - 70f, y, pNormal)
        y += 18f
        canvas.drawText("TOTAL", left, y, pBold)
        canvas.drawText(money.format(comprobante.total), right - 70f, y, pBold)
        y += 30f

        canvas.drawText("Documento de control interno", left, y, pSmall)
        y += 12f
        canvas.drawText("No constituye documento tributario", left, y, pSmall)

        doc.finishPage(page)

        val dir = File(context.cacheDir, "comprobantes")
        dir.mkdirs()
        val file = File(dir, "comprobante_${comprobante.folio}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Comparte el PDF generado (WhatsApp, etc.). */
    fun compartirPdf(context: Context, uri: android.net.Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir comprobante"))
    }
}
