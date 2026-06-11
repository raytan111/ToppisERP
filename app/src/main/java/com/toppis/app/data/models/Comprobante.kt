package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Comprobante de venta (tabla "comprobantes").
 *
 * Fase 2A: documento de control interno (estado INTERNO), NO tributario.
 * Preparado para emisión real ante el SII en Fase 2B (campos tipo_dte,
 * folio_sii, track_id, pdf_url, etc.).
 */
@Serializable
data class Comprobante(
    val id: Int = 0,
    @SerialName("venta_id")
    val ventaId: Int? = null,
    val folio: Int,
    val tipo: String = "COMPROBANTE_INTERNO",
    val neto: Double,
    val iva: Double,
    val total: Double,
    val estado: String = "INTERNO",
    @SerialName("tipo_dte")
    val tipoDte: Int? = null,
    @SerialName("folio_sii")
    val folioSii: Int? = null,
    @SerialName("rut_receptor")
    val rutReceptor: String? = null,
    @SerialName("track_id")
    val trackId: String? = null,
    @SerialName("pdf_url")
    val pdfUrl: String? = null,
    @SerialName("xml_url")
    val xmlUrl: String? = null,
    @SerialName("fecha_emision")
    val fechaEmision: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null
)
