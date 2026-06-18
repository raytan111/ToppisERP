package com.toppis.app.data.db.entities

import kotlinx.serialization.Serializable

@Serializable
enum class Rol { ADMIN, ADMIN_LOCAL, SUPERVISOR, CAJERO }

@Serializable
enum class TipoMovimiento { INGRESO, EGRESO, TRANSFERENCIA }

@Serializable
enum class MetodoPago { EFECTIVO, DEBITO }

@Serializable
enum class EstadoVenta { COMPLETADA, ANULADA }

@Serializable
enum class TipoComponente { ARTICULO, PREPARACION }

/** Dimensión de unidad de medida. Cada una tiene su unidad base. */
@Serializable
enum class DimensionUnidad(val unidadBase: String, val label: String) {
    MASA("g", "Masa (g)"),
    VOLUMEN("ml", "Volumen (ml)"),
    UNIDAD("un", "Unidad (un)")
}

/** Tipo de artículo: comprado (INGREDIENTE) o producido en casa (PREPARACION). */
@Serializable
enum class TipoArticulo { INGREDIENTE, PREPARACION }

/**
 * Unidad de medida seleccionable, asociada a una dimensión, con su factor de
 * conversión a la unidad base de esa dimensión.
 * Ej: 1 kg = 1000 g; 1 L = 1000 ml; 1 un = 1 un.
 */
enum class UnidadMedida(
    val label: String,
    val abreviatura: String,
    val dimension: DimensionUnidad,
    val factorBase: Double
) {
    GRAMO("Gramo", "g", DimensionUnidad.MASA, 1.0),
    KILOGRAMO("Kilogramo", "kg", DimensionUnidad.MASA, 1000.0),
    MILILITRO("Mililitro", "ml", DimensionUnidad.VOLUMEN, 1.0),
    LITRO("Litro", "L", DimensionUnidad.VOLUMEN, 1000.0),
    UNIDAD("Unidad", "un", DimensionUnidad.UNIDAD, 1.0);

    companion object {
        /** Unidades disponibles para una dimensión. */
        fun deDimension(dim: DimensionUnidad): List<UnidadMedida> = entries.filter { it.dimension == dim }
        /** Busca por abreviatura (para edición). */
        fun porAbreviatura(abrev: String): UnidadMedida? = entries.firstOrNull { it.abreviatura == abrev }
    }
}

/** Tipo de modificador aplicable a un item del menú en el POS. */
@Serializable
enum class TipoModificador(val label: String) {
    DOBLE("Doble"),
    QUITAR("Quitar"),
    REEMPLAZAR("Reemplazar"),
    EXTRA("Extra")
}

/** Acción de un componente de modificador sobre la receta. */
@Serializable
enum class AccionModificador { AGREGAR, QUITAR }

/** Tipo de promoción. */
@Serializable
enum class TipoPromocion(val label: String) {
    COMBO("Combo (precio fijo)"),
    DESCUENTO_PORCENTAJE("Descuento %")
}

/** Motivo de merma (pérdida de stock). */
@Serializable
enum class MotivoMerma(val label: String) {
    VENCIDO("Vencido"),
    ESTROPEADO("Estropeado"),
    VINO_MALO("Vino malo"),
    ERROR_COCINA("Error de cocina"),
    CORTESIA("Cortesía/regalo"),
    ROBO("Robo"),
    OTRO("Otro")
}

/** Tipo de sobre: CUENTA = dinero real; FONDO = provisión/aparte. */
@Serializable
enum class TipoSobre(val label: String) {
    CUENTA("Cuenta (dinero real)"),
    FONDO("Fondo (provisión)")
}

/** Forma de pago de un empleado. */
@Serializable
enum class TipoPago(val label: String) {
    SUELDO_FIJO("Sueldo fijo mensual"),
    POR_TURNO("Por turno"),
    POR_HORA("Por hora")
}

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

