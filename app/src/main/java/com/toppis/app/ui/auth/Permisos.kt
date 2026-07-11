package com.toppis.app.ui.auth

import com.toppis.app.data.db.entities.Rol

/**
 * Permisos efectivos de un usuario según su [Rol].
 *
 * - [rutas]: rutas de navegación que el rol puede abrir. ADMIN puede abrir todo.
 * - [puedeEditar]: puede crear/editar registros (no aplica al POS, que siempre
 *   puede registrar ventas si tiene acceso).
 * - [puedeBorrar]: puede eliminar registros.
 * - [scopeLocal]: true si el rol está restringido a su(s) local(es) asignado(s).
 *
 * Resumen:
 * - ADMIN: todo, global.
 * - ADMIN_LOCAL: todo de su local excepto administración global (usuarios,
 *   locales, asignaciones, exportación). Puede borrar.
 * - SUPERVISOR: operación de su local (POS, inventario, mermas, preparaciones,
 *   modificadores, promos, historial, comprobantes). Crea/edita, NO borra.
 * - CAJERO: POS, historial y comprobantes de su local. Solo registra ventas.
 */
data class Permisos(
    val rol: Rol,
    val rutas: Set<String>,
    val puedeEditar: Boolean,
    val puedeBorrar: Boolean,
    val scopeLocal: Boolean
) {
    val esAdminGlobal: Boolean get() = rol == Rol.ADMIN

    /** ¿Puede abrir esta ruta de navegación? */
    fun puedeAbrir(ruta: String): Boolean = rol == Rol.ADMIN || ruta in rutas

    /**
     * Roles que este usuario puede asignar al crear/editar usuarios.
     * - ADMIN: todos.
     * - ADMIN_LOCAL: solo SUPERVISOR y CAJERO (no puede crear admins).
     * - SUPERVISOR / CAJERO: ninguno (no gestionan usuarios).
     */
    val rolesAsignables: List<Rol>
        get() = when (rol) {
            Rol.ADMIN -> listOf(Rol.ADMIN, Rol.ADMIN_LOCAL, Rol.SUPERVISOR, Rol.CAJERO)
            Rol.ADMIN_LOCAL -> listOf(Rol.SUPERVISOR, Rol.CAJERO)
            else -> emptyList()
        }

    companion object {
        // Rutas operativas de un local (las usa ADMIN_LOCAL completas).
        private val RUTAS_ADMIN_LOCAL = setOf(
            "pos",
            // Fondos
            "sobres", "gastos", "arqueo", "flujo_caja", "contabilidad", "reportes",
            // Personal
            "empleados", "mano_obra",
            // Inventario
            "inventario", "mermas", "conteos", "compra_sugerida", "variance", "compras", "proveedores",
            // Cocina
            "menu_config", "preparaciones", "modificadores", "food_cost", "comandas",
            // Análisis / otros
            "kpis", "ventas_historial", "comprobantes", "promociones", "clientes"
        )

        private val RUTAS_SUPERVISOR = setOf(
            "pos", "ventas_historial", "comprobantes", "promociones", "comandas",
            "inventario", "mermas", "conteos", "preparaciones", "modificadores"
        )

        private val RUTAS_CAJERO = setOf(
            "pos", "ventas_historial", "comprobantes", "comandas"
        )

        fun de(rol: Rol?): Permisos = when (rol) {
            Rol.ADMIN -> Permisos(Rol.ADMIN, emptySet(), puedeEditar = true, puedeBorrar = true, scopeLocal = false)
            Rol.ADMIN_LOCAL -> Permisos(Rol.ADMIN_LOCAL, RUTAS_ADMIN_LOCAL, puedeEditar = true, puedeBorrar = true, scopeLocal = true)
            Rol.SUPERVISOR -> Permisos(Rol.SUPERVISOR, RUTAS_SUPERVISOR, puedeEditar = true, puedeBorrar = false, scopeLocal = true)
            Rol.CAJERO -> Permisos(Rol.CAJERO, RUTAS_CAJERO, puedeEditar = false, puedeBorrar = false, scopeLocal = true)
            null -> Permisos(Rol.CAJERO, emptySet(), puedeEditar = false, puedeBorrar = false, scopeLocal = true)
        }
    }
}
