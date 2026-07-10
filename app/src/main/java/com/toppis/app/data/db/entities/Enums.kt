package com.toppis.app.data.db.entities

import kotlinx.serialization.Serializable

@Serializable
enum class Rol { ADMIN, ADMIN_LOCAL, SUPERVISOR, CAJERO }

@Serializable
enum class TipoMovimiento { INGRESO, EGRESO, TRANSFERENCIA }

@Serializable
enum class MetodoPago(val label: String) {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia")
}

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
    ZONA_1("Zona 1", 1000.0),
    ZONA_2("Zona 2", 1500.0),
    ZONA_3("Zona 3", 2000.0),
    ZONA_4("Zona 4", 2500.0),
    ZONA_5("Zona 5", 3000.0)
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

// ── Control de Costos ──────────────────────────────────────────────────────

/** Periodicidad de un costo fijo recurrente, con su divisor a semana. */
@Serializable
enum class Periodicidad(val label: String, val divisorSemanal: Double) {
    SEMANAL("Semanal", 1.0),
    MENSUAL("Mensual", 4.33),
    ANUAL("Anual", 52.0)
}

/** Categoría de un artículo para agrupar costos variables. */
@Serializable
enum class CategoriaArticulo(val label: String) {
    INGREDIENTES("Ingredientes"),
    PACKAGING("Packaging"),
    BEBIDA_LATA("Bebida lata"),
    BEBIDA_MEDIANA("Bebida mediana"),
    INSUMOS("Insumos")
}

/** Estado de un cierre semanal. */
@Serializable
enum class EstadoCierre { ABIERTO, CERRADO }

/** Grupo de costo: cambia con las ventas (variable) o no (fijo). */
@Serializable
enum class GrupoCosto(val label: String) { VARIABLE("Variable"), FIJO("Fijo") }

/** Estado visual de un semáforo de objetivo. */
enum class EstadoSemaforo { FAVORABLE, ALERTA }

/** Paso de la rutina semanal de cierre. */
@Serializable
enum class PasoRutina(val label: String) {
    CONTEO("Conteo de inventario"),
    MERMAS("Registro de mermas"),
    PROVISION("Provisión de fijos"),
    RESULTADO("Resultado semanal")
}


// ── Rediseño del POS ───────────────────────────────────────────────────────

/** Estado operativo de un pedido/carrito. */
@Serializable
enum class EstadoPedido { ABIERTO, CERRADO }

/** Tipo de línea de cobro en un pedido. */
@Serializable
enum class TipoLineaPedido { PRODUCTO, PROMO }

/** Cómo se definen los elegibles de un espacio de promo. */
@Serializable
enum class ModoEspacioPromo { LISTA, CATEGORIA }

/** Categorías fijas del menú (para el POS y la config del menú). */
enum class CategoriaMenu(val label: String) {
    HAMBURGUESAS("Hamburguesas"),
    PAPAS("Papas fritas"),
    BEBIDA_LATA("Bebida lata"),
    BEBIDA_MEDIANA("Bebida mediana"),
    SALSAS("Salsas"),
    OTRO("Otro");

    companion object {
        /** Busca por label (como se guarda en items_menu.categoria); OTRO por defecto. */
        fun porLabel(label: String?): CategoriaMenu =
            entries.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) } ?: OTRO
    }
}
