package com.toppis.app.data.db.entities

import kotlinx.serialization.Serializable

@Serializable
enum class Rol { ADMIN, CAJERO }

@Serializable
enum class TipoMovimiento { INGRESO, EGRESO, TRANSFERENCIA }

@Serializable
enum class MetodoPago { EFECTIVO, DEBITO }

@Serializable
enum class EstadoVenta { COMPLETADA, ANULADA }

@Serializable
enum class TipoComponente { INGREDIENTE, INSUMO, SALSA }

@Serializable
enum class EstadoComanda { PENDIENTE, ENTREGADA }

enum class ZonaEnvio(val label: String, val precio: Double) {
    SIN_ENVIO("Sin envío", 0.0),
    ZONA_1("Zona 1", 500.0),
    ZONA_2("Zona 2", 1000.0),
    ZONA_3("Zona 3", 1500.0),
    ZONA_4("Zona 4", 2000.0)
}

@Serializable
enum class CategoriaGasto(val label: String) {
    INSUMOS("Insumos"),
    SUELDOS("Sueldos"),
    SERVICIOS("Servicios"),
    ARRIENDO("Arriendo"),
    TRANSPORTE("Transporte"),
    ENVIOS("Envíos"),
    PACKAGING("Packaging / Stickers"),
    OTROS("Otros")
}

