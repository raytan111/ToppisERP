# ToppisERP - Contexto del Proyecto

**Última Actualización**: 2026-06-20
**Versión Actual**: 3.2 (Supabase Cloud + ERP Franquicia)
**Ubicación**: Chile (CLP, español chileno, integración SII futura)

---

## 1. Descripción General

ToppisERP es una app Android para gestionar una hamburguesería / dark kitchen. Ya no es solo POS: es un ERP de operación con food cost, inventario profesional, compras, dinero real (sobres + arqueo), mano de obra, multi-local, roles y KPIs ejecutivos.

**Estado actual**: App funcional sobre **Supabase** (PostgreSQL + Auth + Realtime). Autenticación en la nube, datos centralizados, operaciones atómicas vía RPC.

**Propietario**: raytan111 / andreslh — Repo: `https://github.com/raytan111/ToppisERP` (rama `main`).

**Visión futura**: emisión de boletas electrónicas (SII), módulo contable/tributario completo, multi-app modular, IA.

---

## 2. Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Min SDK**: 26 · Target 35 · Compile 36
- **Patrón**: MVVM (StateFlow + ViewModel + Factory + Screen)
- **Navegación**: Jetpack Navigation Compose (un solo NavHost, sin bottom bar)
- **DI**: Manual (sin Hilt) — cableado en `MainActivity.onCreate`
- **Backend**: Supabase (`supabase-kt` 3.1.4) — Postgrest, Auth, Realtime, **Functions** (Edge Functions)
- **Exportación**: Apache POI (Excel/XLSX)

### Build / pruebas
- Build: `./gradlew :app:assembleDebug`
- El usuario corre los SQL **manualmente** en el SQL Editor de Supabase; los scripts viven en `.kiro/database/`.
- Pruebas en Samsung físico + emulador.

---

## 3. Backend (Supabase)

- Cliente singleton: `data/supabase/SupabaseClient.kt` (`SupabaseClient.client`).
- Modelos en `data/models/` son `@Serializable` con `@SerialName` en snake_case.
- Enums en `data/db/entities/Enums.kt`.
- Repositorios usan `client.postgrest` / `client.auth`.
- **Operaciones atómicas** vía funciones RPC PostgreSQL (venta, gasto, arqueo, merma, compra, etc.). Errores del RPC se extraen con regex `"message"\s*:\s*"([^"]+)"`.
- **Edge Function** `admin-usuarios` (`supabase/functions/`): operaciones que requieren `service_role` — borrado total de cuenta Auth y reset de contraseña. Valida JWT + rol ADMIN. Se invoca con `client.functions.invoke(...)`. La `service_role key` NUNCA está en la app.
- **Realtime**: canales con nombre único (UUID) para evitar el crash "cannot call postgresChangeFlow after joining".
- **Sellado por local**: las RPCs/inserts agregan `local_id` desde `LocalSession.activoId`.

### Scripts SQL clave (`.kiro/database/`)
- `supabase-schema.sql`, `supabase-rls.sql`, `supabase-realtime.sql`
- `supabase-fase4-*` (food cost / clean slate de cocina), `supabase-fase5-*` (mermas, conteos), `supabase-fase6-*` (proveedores, compras), `supabase-fase7-dinero.sql` (sobres/arqueo), `supabase-fase8-manoobra.sql`, `supabase-fase9-*` (locales, roles-local)
- `supabase-roles-v2.sql` — amplía enum `rol` a 4 valores
- `supabase-clean-slate.sql` — borra transaccionales y resetea stocks/saldos (puesta en marcha)

---

## 4. Modelo de Datos (resumen)

Tablas principales en Postgres:

- **Identidad / multi-local**: `usuarios`, `locales`, `usuarios_locales`
- **Cocina / food cost**: `articulos` (unifica ingredientes+insumos; dimensión MASA/VOLUMEN/UNIDAD, unidad de compra + factor, costo_base, rendimiento, stock_base, cantidad_pos, seleccionable_en_pos), `preparaciones` (+ `preparacion_componentes`), `items_menu`, `recetas_menu`, `modificadores` (+ `modificador_componentes`), `promociones` (+ `promocion_items`)
- **Inventario pro**: `mermas`, `conteos` (+ `conteo_detalle`)
- **Compras**: `proveedores`, `compras` (+ `compra_detalle`) — recepción suma stock + costo promedio ponderado + caducidad por lote
- **Dinero**: `sobres` (tipo CUENTA = dinero real / FONDO = provisión), `movimientos_sobre`, `arqueos`, `gastos`, `presupuestos`, `comprobantes`, `cierres_mensuales`
- **Ventas**: `ventas` (incluye `descripcion`, `canal`, `modo_entrega`, `origen`='APP'|'IMPORT_HISTORICO'; `metodo_pago` y `sobre_id` son NULL-ables para ventas históricas importadas), `items_venta_menu`, `comandas`
- **Mano de obra**: `empleados`, `jornadas`, `propinas`

### Enums (`data/db/entities/Enums.kt`)
```kotlin
enum class Rol { ADMIN, ADMIN_LOCAL, SUPERVISOR, CAJERO }
enum class TipoMovimiento { INGRESO, EGRESO, TRANSFERENCIA }
enum class MetodoPago { EFECTIVO, DEBITO }
enum class EstadoVenta { COMPLETADA, ANULADA }
enum class TipoComponente { ARTICULO, PREPARACION }
enum class DimensionUnidad { MASA, VOLUMEN, UNIDAD }   // con unidadBase
enum class TipoArticulo { INGREDIENTE, PREPARACION }
enum class UnidadMedida { GRAMO, KILOGRAMO, MILILITRO, LITRO, UNIDAD } // factorBase
enum class TipoModificador { DOBLE, QUITAR, REEMPLAZAR, EXTRA }
enum class AccionModificador { AGREGAR, QUITAR }
enum class TipoPromocion { COMBO, DESCUENTO_PORCENTAJE }
enum class MotivoMerma { VENCIDO, ESTROPEADO, VINO_MALO, ERROR_COCINA, CORTESIA, ROBO, OTRO }
enum class TipoSobre { CUENTA, FONDO }
enum class TipoPago { SUELDO_FIJO, POR_TURNO, POR_HORA }
enum class EstadoComanda { PENDIENTE, ENTREGADA }
enum class ZonaEnvio { SIN_ENVIO, ZONA_1..ZONA_4 }
enum class CategoriaGasto { INSUMOS, SUELDOS, SERVICIOS, ARRIENDO, TRANSPORTE, ENVIOS, PACKAGING, OTROS }
```

---

## 5. Navegación (refactor 2026-06)

**Sin bottom bar.** Estructura:
- **Login** → auto-login si hay sesión Supabase activa (`AuthViewModel.init` + `getCurrentUser()` espera `auth.awaitInitialization()`).
- **Home** (`ui/home/HomeScreen.kt`): botón grande de **Venta (POS)** + tarjetas de categoría. Logout en la TopBar.
- **Sub-menú de categoría** (`CategoriaMenuScreen`): lista a pantalla completa.
- **Pantalla específica**: cada función con su botón volver (`BackScaffold` para las que no traen TopBar propia).

Definiciones del menú en `ui/home/HomeMenu.kt`. Categorías:
- 🛒 **Venta** → POS directo
- 🍔 **Cocina**: Configurar Menú, Preparaciones, Modificadores, Food Cost & Menú
- 📦 **Inventario**: Artículos, Mermas, Conteo, Compra Sugerida, Análisis, Compras, Proveedores
- 💰 **Fondos**: Sobres, Gastos, Arqueo, Flujo de Caja, Contabilidad, Reportes
- 👥 **Personal**: Empleados, Mano de Obra / Prime Cost
- ⚙️ **Administración**: Usuarios, Locales, Usuarios por Local, KPIs, Historial de Ventas, Comprobantes, Promociones, Exportación

**Performance/cache**: los ViewModels se scopean al **Activity** (`viewModelStoreOwner = activityOwner` en `NavGraph`), así sobreviven a la navegación y no reconsultan la DB cada vez; Realtime los mantiene frescos.

## 5b. Tema / Diseño

- **Tema de marca dinámico**: `com.toppis.erp.ui.theme.ThemeManager` guarda el
  color semilla (hex) y el modo (sistema/claro/oscuro) en SharedPreferences.
  `ToppisERPTheme` genera todo el `ColorScheme` Material 3 desde ese seed con
  **MaterialKolor** (`rememberDynamicColorScheme`, estilo Vibrant). Shapes
  redondeadas en `AppShapes`. Default seed `#E63946`.
- **Configurar Colores** (Administración, solo ADMIN): `ui/ajustes/ConfiguracionColorScreen`
  — input hex `#`, presets, vista previa en vivo, modo claro/oscuro. Aplica y
  persiste al instante.
- **Logo**: `res/drawable/toppis_logo.xml` (placeholder) usado en Login y Home.
  Reemplazo de assets documentado en `.kiro/ASSETS-DISENO.md`.
- Librería de diseño: **MaterialKolor** `com.materialkolor:material-kolor:2.0.0`.

---

## 6. Roles y Permisos (`ui/auth/Permisos.kt`)

| Rol | Ve | Crea/Edita | Borra | Scope |
|---|---|---|---|---|
| ADMIN | Todo | Sí | Sí | Global |
| ADMIN_LOCAL | Todo de su local (no admin global) | Sí | Sí | Su local |
| SUPERVISOR | POS, inventario, mermas, conteos, preparaciones, modificadores, promos, historial, comprobantes | Sí | **No** | Su local |
| CAJERO | POS, historial, comprobantes | Solo ventas | No | Su local |

- `Permisos.de(rol)` expone `rutas`, `puedeEditar`, `puedeBorrar`, `scopeLocal` y `rolesAsignables` (qué roles puede crear/asignar: ADMIN todos; ADMIN_LOCAL solo SUPERVISOR/CAJERO).
- Home/CategoríaMenu filtran opciones; NavGraph gatea cada ruta con `permisos.puedeAbrir(...)`.
- Borrado oculto para SUPERVISOR en inventario/preparaciones/modificadores/promociones/conteos.
- **Scope local**: al iniciar sesión, no-admins quedan fijados a su local asignado (`AuthRepository.getLocalAsignado` + `aplicarScopeLocal`). ADMIN cambia de local desde la pantalla Locales.

---

## 7. Módulos / Funcionalidades

- **POS**: venta veloz, modificadores (extra/doble agregan; quitar/cambiar se editan sobre la receta real con botón lápiz por línea), salsas (artículos/preparaciones con `seleccionable_en_pos` + `cantidad_pos`), promos, comandas, descuento de stock, comprobante.
- **Food Cost & Menú**: food cost % por plato, menu engineering.
- **Inventario**: artículos unificados, mermas (waste log), conteos (pantalla completa), compra sugerida, análisis/variance.
- **Compras**: proveedores, recepción (pide "total pagado" por línea → costo unitario; costo promedio ponderado; caducidad por lote; gasto/sobre opcional).
- **Fondos**: sobres CUENTA/FONDO, gastos, arqueo de caja, flujo de caja, contabilidad, reportes (filtrables por local).
- **Personal**: empleados (sueldo fijo/turno/hora), jornadas, propinas, Prime Cost.
- **Multi-local**: locales, asignaciones usuario-local, reportes por local.
- **Usuarios** (solo ADMIN): crear, editar (nombre/rol/activo), eliminar (borrado total vía Edge Function, con confirmación, no a sí mismo), resetear contraseña. Roles asignables limitados por `Permisos.rolesAsignables`.
- **KPIs Ejecutivos**: ventas, ticket, food/labor/prime cost %, merma, alertas.
- **Historial de Ventas**: lista con detalle en popup; muestra `descripcion`/delivery/modo de entrega para ventas históricas (sin ítems).
- **Exportación**: Excel/CSV/ZIP (Apache POI).

---

## 8. Convenciones

- **UI/código/comentarios/commits**: español (chileno).
- Tablas plural snake_case; modelos singular.
- VM: `MutableStateFlow` privado + `StateFlow` público, `viewModelScope.launch`, sealed UiState, refresh-after-write.
- Errores de operaciones normales → mostrar en **popup (AlertDialog)**, no solo snackbar.
- **Inputs de fecha**: usar `ui/components/DatePickerField` (calendario Material 3), no texto libre.
- **Parseo numérico**: siempre `toDoubleOrNull()` / `toIntOrNull()` con guarda (`?: return@...`), nunca `toDouble()`/`toInt()` directo sobre texto de usuario.
- Mensajes de error de Auth/Supabase se traducen a español (ver helpers en `AuthRepository`).
- Cada pantalla nueva: ViewModel + Factory + Screen, cableada en `MainActivity` (DI), `NavGraph` (composable + guarda de permiso) y `ui/home/HomeMenu.kt` (opción en su categoría).
- Material 3 estricto; content descriptions obligatorios.
- Cada módulo con build verde → commit + push a GitHub.
- Los "Unresolved reference" del IDE suelen ser falsos positivos de caché (Invalidate Caches) si Gradle compila bien.

---

## 9. Historial de Cambios

### v3.2 — Ajustes operativos (2026-06-20)
- Método de pago: **EFECTIVO / TARJETA / TRANSFERENCIA** (`supabase-metodo-pago-v2.sql`).
- KPIs: sección **Delivery** con selector de mes, total y desglose por día.
- POS: rediseño de legibilidad (tarjetas con chip de precio, total destacado, COBRAR con monto).
- Borrado limpio: borrar un **artículo** solo lo quita de recetas/preparaciones/modificadores y de sus líneas de compra (conserva la compra); borrar un **ítem de menú** lo quita de promos y su receta cae.
- Gastos/Comprobantes recargan al abrir (evita cache vieja tras cambios por SQL).
- Import histórico verificado: 99 ventas = $1.003.545 (el resumen del Excel tenía +$7.990 por error propio en Sem 3 May).

### v3.1 — Usuarios, datos históricos, robustez (2026-06-19)
- Gestión de usuarios completa (solo ADMIN): crear, editar, eliminar (borrado
  total vía Edge Function `admin-usuarios`), resetear contraseña; roles asignables
  por rol; mensajes de error traducidos.
- Sesión: confirmación de email documentada (`NOTAS-SUPABASE-AUTH.md`).
- Import de ventas históricas (Mar–Jun 2026, 99 ventas en texto libre): columnas
  nuevas en `ventas` + scripts (`supabase-import-01/02`, `supabase-clean-slate`).
- UX/robustez: inputs de fecha con calendario (`DatePickerField`); parseo numérico
  seguro; limpieza de código muerto.
- Seguridad: revisión en `NOTAS-SEGURIDAD.md` + fix RLS de gastos para roles nuevos.

### v3.0 — Refactor de navegación, roles, performance (2026-06-18)
Spec: `.kiro/specs/refactorizacion-navegacion.md`
- Roles ampliados a 4 (ADMIN / ADMIN_LOCAL / SUPERVISOR / CAJERO).
- Sesión persistente (auto-login).
- Cache en memoria: ViewModels scopeados al Activity.
- Navegación tipo menú sin bottom bar (Home + categorías + sub-menús).
- Permisos por rol + scope por local.
- Eliminados: Rendimiento Papa, bottom bar (MainScaffold), Dashboard (reemplazado por KPIs).
- Clean slate SQL para puesta en marcha.

### v2.x — ERP Franquicia (Fases 4-10)
Spec: `.kiro/specs/roadmap-erp-franquicia.md`
- Fase 4 Food Cost, Fase 5 Inventario Pro, Fase 6 Compras, Fase 7 Dinero real (sobres + arqueo), Fase 8 Mano de obra, Fase 9 Multi-local, Fase 10 KPIs.

### v2.0 — Migración a Supabase
- De Room/SQLite local + auth local → Supabase (PostgreSQL + Auth + Realtime).

---

## 10. Roadmap Futuro

- **Comprobantes electrónicos (SII)**: boletas/facturas, envío al SII, XML firmados.
- **Contabilidad/tributario**: libro ventas/compras, cierre mensual, IVA, estados financieros.
- **Multi-app modular** y **preparación IA** (predicción demanda, optimización inventario, etc.).

---

## 11. Mantenimiento

**Propietario**: andreslh · Chile · CLP · hamburguesería/dark kitchen.

Archivos de contexto:
- Este archivo: `.kiro/PROYECTO-CONTEXTO.md`
- Specs: `.kiro/specs/refactorizacion-navegacion.md`, `roadmap-erp-franquicia.md`, `import-ventas-historicas.md`, `fase4-food-cost.md`, `fase5-inventario-pro.md`, etc.
- SQL y notas: `.kiro/database/` (incluye `NOTAS-SUPABASE-AUTH.md`, `NOTAS-SEGURIDAD.md`)
- Edge Functions: `supabase/functions/admin-usuarios/`

Actualizar este documento al completar cada fase mayor o cambio estructural.
