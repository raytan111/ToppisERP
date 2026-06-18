# Refactorización de Navegación, Roles y Performance

## Estado: En implementación por fases

### Progreso
- [x] **Fase 1 — Roles v2**: enum Kotlin `Rol { ADMIN, ADMIN_LOCAL, SUPERVISOR, CAJERO }` actualizado; dropdown de usuarios muestra los 4; SQL en `supabase-roles-v2.sql` (ejecutar en Supabase).
- [x] **Fase 2 — Sesión persistente**: `AuthViewModel.init` restaura sesión al arrancar; `AuthRepository.getCurrentUser()` espera `auth.awaitInitialization()` antes de leer el usuario, para no devolver null mientras Supabase restaura la sesión persistida.
- [x] **Fase 3 — Performance / cache en memoria**: los ViewModels se scopean al Activity (`viewModelStoreOwner = activityOwner` en NavGraph) en vez de al NavBackStackEntry. Sobreviven a la navegación, conservan su data en StateFlow (no se reconsulta la DB al abrir cada pantalla) y Realtime los mantiene frescos. Equivale al CacheManager planeado con menos código. (Nota: los VM no se limpian al logout; Fase 5 maneja el scope por local.)
- [x] **Fase 4 — Navegación nueva** (sin bottom bar): `HomeScreen` con botón grande de POS + tarjetas de categoría; `CategoriaMenuScreen` (sub-menú pantalla completa); `BackScaffold` para envolver pantallas que dependían del bottom bar; NavGraph reescrito a un solo NavHost. Definiciones de menú en `ui/home/HomeMenu.kt`. Logout en TopBar del Home. (`MainScaffold` queda muerto → se borra en Fase 6.)
- [x] **Fase 5 — Permisos por rol + scope local**: modelo `Permisos` (ui/auth/Permisos.kt) por rol con `rutas`, `puedeEditar`, `puedeBorrar`, `scopeLocal`. Home/CategoríaMenu filtran opciones por permisos; NavGraph gatea cada ruta con `permisos.puedeAbrir(...)`. Borrado oculto para SUPERVISOR en inventario/preparaciones/modificadores/promociones/conteos (CAJERO no las alcanza). Scope local: al iniciar sesión, no-admins quedan fijados a su local asignado (`AuthRepository.getLocalAsignado` + `aplicarScopeLocal` en AuthViewModel); ADMIN cambia de local libremente.
- [x] **Fase 6 — Quitar Rendimiento Papa + bottom bar + código muerto**: eliminados `PapaRendimiento*` (screen/VM/factory/repo/modelo), `MainScaffold` (bottom bar), y `Dashboard*` (reemplazado por KPIs Ejecutivos). Limpiadas referencias en MainActivity y NavGraph.
- [x] **Fase 7 — Clean slate SQL**: `supabase-clean-slate.sql` creado (lo ejecuta el usuario en Supabase). Trunca transaccionales (DO block con `to_regclass` para tablas existentes), resetea stocks/saldos a 0, conserva usuarios/locales/configs.

---

## 1. Navegación Nueva

### Estructura
- **Sin bottom bar** — se elimina completamente.
- **Pantalla principal (Home)**: POS como botón grande + 5 tarjetas en grilla (Cocina, Inventario, Fondos, Personal, Administración).
- **Sub-menú de categoría**: pantalla completa con lista de opciones de esa categoría.
- **Pantalla específica**: cada función (POS, Artículos, etc.) con botón "volver" al sub-menú.

### Categorías y contenido

**🛒 Venta** → acceso directo al POS (sin sub-menú)

**🍔 Cocina**
- Configurar Menú
- Preparaciones
- Modificadores
- Food Cost & Menú

**📦 Inventario**
- Artículos (stock)
- Mermas
- Conteo de Inventario
- Compra Sugerida
- Análisis de Inventario
- Compras
- Proveedores

**💰 Fondos**
- Sobres
- Gastos
- Arqueo de Caja
- Flujo de Caja
- Contabilidad
- Reportes

**👥 Personal**
- Empleados
- Mano de Obra / Prime Cost

**⚙️ Administración** (solo Admin)
- Usuarios
- Locales
- Usuarios por Local
- KPIs Ejecutivos
- Historial de Ventas
- Comprobantes
- Promociones
- Exportación

### Eliminado
- Rendimiento Papa (se quita; la merma se ingresa manualmente en el artículo)
- Bottom navigation bar

---

## 2. Roles (4 niveles)

Enum: `ADMIN | ADMIN_LOCAL | SUPERVISOR | CAJERO`

| Rol | Ve | Crea/Edita | Borra | Scope |
|---|---|---|---|---|
| ADMIN | Todo | Todo | Todo | Global |
| ADMIN_LOCAL | Fondos, Personal, Inventario, Cocina, Venta, Análisis | Todo de su(s) local(es) | Sí (de su local, excepto usuarios/locales) | Su(s) local(es) |
| SUPERVISOR | POS, Historial, Comprobantes, Promos, Inventario, Mermas, Preparaciones, Modificadores | Crear/editar | NO borrar | Su local |
| CAJERO | POS, Historial propio, Comprobantes | Solo crear ventas | NO | Su local |

**Restricción por local**: CAJERO y SUPERVISOR solo operan y ven datos de su local asignado. ADMIN_LOCAL ve solo su(s) local(es). ADMIN ve todo.

---

## 3. Sesión Persistente

- Al abrir la app, verificar si Supabase tiene sesión activa (refresh token).
- Si sí → ir directo a Home (sin login).
- Si no → mostrar login.
- El refresh token de Supabase dura días; en la práctica se mantiene todo el día.

---

## 4. Cache en Memoria (performance)

**Problema**: cada vez que se abre una pantalla, se consulta la DB → se ve un delay/loading.

**Solución**: patrón **cache-first + refresh**:
- Los ViewModels guardan la data en StateFlow (ya lo hacen).
- El tema es que los VMs se recrean al navegar porque usamos `viewModel()` por pantalla.
- **Fix**: usar ViewModels compartidos a nivel de actividad (o un DataStore singleton) para datos frecuentes (artículos, items menú, sobres, empleados) → se cargan UNA VEZ al iniciar y se refrescan con Realtime.
- Pantallas que abren ven la data cacheada al instante; si hay un cambio Realtime, se actualiza en vivo.

Implementación práctica:
- **CacheManager** singleton con `StateFlow` de: artículos, preparaciones, items menú, sobres, empleados, locales.
- Se carga al login y se mantiene con suscripciones Realtime.
- Los ViewModels leen del CacheManager en vez de consultar directo a Supabase.
- Así al navegar entre pantallas, todo aparece instantáneo.

---

## 5. Clean Slate (al final)

Script SQL que borra datos transaccionales y resetea stocks a 0:
- Borra: ventas, items_venta_menu, comandas, gastos, movimientos_sobre, mermas, conteos, conteo_detalle, compras, compra_detalle, arqueos, jornadas, propinas, comprobantes, cierres_mensuales, papa_rendimientos, presupuestos.
- Resetea: articulos.stock_base = 0, preparaciones.stock_base = 0, sobres.saldo = 0.
- Conserva: usuarios, locales, usuarios_locales, artículos (configs), preparaciones (configs), items_menu, recetas_menu, modificadores, modificador_componentes, promociones, promocion_items, proveedores.

---

## 6. Orden de implementación

1. SQL: nuevo enum de roles (`ADMIN | ADMIN_LOCAL | SUPERVISOR | CAJERO`)
2. Sesión persistente (auto-login)
3. CacheManager singleton (carga inicial + Realtime)
4. Nueva navegación: Home tarjetas → sub-menús → pantallas (sin bottom bar)
5. Restricciones de visibilidad por rol + scope por local
6. Eliminar pantalla Rendimiento Papa + bottom bar + código muerto
7. Clean slate SQL (usuario ejecuta al final)
8. Build + commit

---

**Fecha**: 2026-06-16
**Estado**: Plan aprobado, por implementar.
