# ToppisERP - Contexto del Proyecto

**Última Actualización**: 2026-07-09
**Versión Actual**: 3.4 (Supabase Cloud + ERP Franquicia + Sistema de Diseño + Control de Costos)
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
- **Backend**: Supabase (`supabase-kt` 3.1.4) — Postgrest, Auth, Realtime, **Functions** (Edge Functions), Storage
- **Tema**: MaterialKolor 2.0.0 (esquema dinámico) · **Imágenes**: Coil 2.7.0 · **Splash**: androidx.core:core-splashscreen 1.0.1
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
- `supabase-control-costos.sql` (v3.4): categoría de artículo, costos fijos, config de objetivos, pasos de rutina semanal, cierres semanales con snapshot; RPCs `registrar_compra` (último precio) + `recalcular_recetas_articulo` + `confirmar_cierre_semanal`
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
enum class MetodoPago { EFECTIVO, TARJETA, TRANSFERENCIA }
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

## 5b. Tema / Diseño (Sistema de Diseño v3.3)

- **Tema de marca dinámico**: `com.toppis.erp.ui.theme.ThemeManager` guarda el
  color semilla (hex) y el modo (sistema/claro/oscuro) en SharedPreferences.
  `ToppisERPTheme` genera todo el `ColorScheme` Material 3 desde ese seed con
  **MaterialKolor** (`rememberDynamicColorScheme`, estilo Vibrant). En **modo
  oscuro** se aplica `contrastLevel = 0.4` para superficies más profundas y texto
  legible. Shapes redondeadas en `AppShapes`. Default seed `#E63946`.
- **Tipografía** (`ui/theme/Type.kt`): jerarquía completa sin fuentes externas
  (familia del sistema) con pesos marcados (displays en Black/ExtraBold,
  letter-spacing negativo en titulares) para "alto impacto".
- **Splash screen** (`androidx.core:core-splashscreen`): tema `Theme.ToppisERP.Splash`
  en `res/values/themes.xml` (fondo rojo de marca + logo con inset para que la
  máscara circular de Android 12+ no lo recorte; `windowSplashScreenIconBackgroundColor`
  igual al fondo). `MainActivity.onCreate` llama `installSplashScreen()` antes de
  `super.onCreate`.
- **Ícono de app**: adaptativo (`mipmap-anydpi-v26`), fondo blanco + logo real
  (`drawable/ic_launcher_fg.xml` con inset). Como minSdk 26, todos los equipos
  usan el adaptativo.
- **Login** (`ui/auth/LoginScreen.kt`): header de marca con gradiente edge-to-edge
  (dibuja detrás de la barra de estado), formulario en tarjeta elevada, y una
  **moneda de oro 3D giratoria** hecha a mano en `Canvas` (proyección por
  rebanadas en profundidad → canto/espesor real al girar; cara frontal con el
  logo y foreshortening; cara trasera con emblema; giro lento de frente y rápido
  por detrás con easing simétrico; sombra proyectada).
- **Home** (`ui/home/HomeScreen.kt`): hero POS con gradiente + sombra, tarjetas de
  categoría con ícono en contenedor de color de acento (`accentDeCategoria` en
  `HomeMenu.kt`). `CategoriaMenuScreen` usa el mismo lenguaje.
- **Navegación**: transiciones globales en el `NavHost` (slide + fade). `MainActivity`
  ya NO envuelve en un Scaffold con padding global; cada pantalla maneja sus
  propios insets con su TopBar (evita doble inset y permite el header edge-to-edge).
- **Componentes compartidos** (`ui/components/`):
  - `SearchField` — campo de búsqueda reutilizable (lupa, limpiar, redondeado).
  - `StateComponents` — `EmptyState` (ícono en círculo tenue + título/subtítulo),
    `Modifier.shimmer()`, `SkeletonList`/`SkeletonCard` (placeholders de carga).
  - `ToppisDialogs` — `ToppisErrorDialog`, `ToppisConfirmDialog`, `ToppisDeleteDialog`
    (estilo/íconos/botones unificados; el de borrado en color de error).
  - `ImagePickerField` — subir foto desde galería a Supabase Storage (bucket `menu`).
- **Patrón de listas**: cada ViewModel de lista expone `cargandoInicial: StateFlow<Boolean>`
  (true hasta la primera carga). La pantalla muestra: **skeleton** si carga inicial,
  **EmptyState** si vacío de verdad, o la **lista** (con buscador donde aplica y
  estado "sin resultados").
  - **Con buscador**: Inventario, Proveedores, Compras, Comprobantes, Historial de
    Ventas, Menú.
  - **Con skeleton + EmptyState**: Inventario, Gastos, Sobres, Proveedores, Compras,
    Comprobantes, Historial, Menú, Mermas, Modificadores, Preparaciones, Conteos,
    Empleados, Locales.
- **Imágenes de productos/promos**: Coil + Supabase Storage; columnas `imagen_url`
  en `items_menu` y `promociones` (`supabase-imagenes.sql`). Se ven en POS/menú/promos.
- **Configurar Colores** (Administración, solo ADMIN): `ui/ajustes/ConfiguracionColorScreen`
  — input hex `#`, presets, vista previa en vivo, modo claro/oscuro.
- **Logo real**: `res/drawable/toppis_logo.png` (usado en Login, Home, ícono y splash).
- Librerías de diseño: **MaterialKolor** 2.0.0, **Coil** 2.7.0, **core-splashscreen** 1.0.1.

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
- **Fondos**: sobres CUENTA/FONDO, costos puntuales, arqueo de caja, flujo de caja, contabilidad, reportes (filtrables por local).
- **Control de Costos** (v3.4, solo ADMIN): semana operativa lunes–sábado; costo de artículo por último precio (histórico congelado por snapshot); costos fijos prorrateados + variables; resultado semanal ("lo que queda") con semáforos vs objetivos, break-even y mano de obra disponible; objetivos configurables; rutina de cierre guiada (conteo → mermas → provisión de fijos en sobre FONDO → resultado). Capa pura `domain/costos/CostosCalculos.kt` con 22 tests de propiedad.
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

### Auditoría de base de datos (2026-07-09) — ✅ base sana
Scripts: `.kiro/database/supabase-auditoria.sql` (solo lectura), `supabase-limpieza.sql`, `supabase-indices.sql` (todos corridos).
- **Funciones**: 15 RPC, sin duplicados. Se eliminó el único overload: versión vieja de 6 args de `registrar_gasto` (quedó solo la de 8 args con `p_tiene_iva`/`p_local_id`).
- **Seguridad**: 36/36 tablas con RLS activo + políticas (event trigger `rls_auto_enable` lo mantiene automático).
- **Legacy**: sin tablas del modelo viejo (`ingredientes`/`insumos`/`salsas` ya eliminadas por Fase 4).
- **Índices**: agregados en FKs de JOIN (compra_detalle, conteo_detalle, compras, promocion_items) y `local_id`; `ANALYZE` corrido. No se indexan `created_by` (auditoría, no se filtran).
- Volúmenes reales: ventas 113, articulos 53, gastos 2, compras 1.
- Pendiente opcional (no urgente): consolidar los ~40 scripts de `.kiro/database/` en un único `schema-completo.sql` para recrear la base desde cero sin depender del orden.

### v3.4 — Control de Costos y Resultado Semanal (2026-07-09)
Spec: `.kiro/specs/control-de-costos/`. SQL: `.kiro/database/supabase-control-costos.sql`.
- **Semana operativa lunes → sábado** (domingo cerrado); rango medio-abierto `[lunes, lunes siguiente)`. `SemanaOperativa` + `FechaUtil.semanaActual/semanaDe/semanaOffset`.
- **Costo de artículo por último precio**: al registrar una compra el costo del artículo se reemplaza por el último precio (RPC `registrar_compra` + `recalcular_recetas_articulo`); solo recalcula recetas si el precio cambió. El histórico queda **congelado** vía snapshot al cerrar la semana. Inventario pasa a manejar **solo stock** (los costos ya no se editan ahí).
- **Categoría de artículo**: Ingredientes / Packaging / Insumos (campo `categoria` en `Articulo`).
- **Dos grupos de costo**: Variables y Fijos. **Costos fijos** con periodicidad (semanal/mensual/…) prorrateados a semana. CRUD en `CostosFijosScreen`.
- **Resultado semanal** (`CierreSemanalScreen`): ventas − variables − mano de obra − fijos = "lo que queda"; semáforos vs objetivos (food ≤32%, mano de obra 30%, arriendo ≤10%), break-even semanal + cuánto falta vender, mano de obra disponible (30%×ventas ÷ empleados) para decidir contrataciones, alerta de arriendo. Cierre atómico con snapshot (RPC `confirmar_cierre_semanal`).
- **Objetivos configurables** (`ObjetivosScreen`, `ConfigCostosRepository`).
- **Rutina de cierre** (`RutinaSemanalScreen`): checklist guiado de 4 pasos (conteo → mermas → provisión de fijos en sobre FONDO → ver resultado) por semana, sin duplicar módulos. La **provisión** transfiere el prorrateo de fijos desde una cuenta a un sobre FONDO (reutiliza `SobreRepository.transferir`).
- **Capa pura** `domain/costos/CostosCalculos.kt` con **22 tests de propiedad** (kotest-property, 100 iter, verdes).
- **Sección de menú "Costos"** (`cat_costos`, azul `0xFF1565C0`, soloAdmin): Rutina de cierre, Resultado semanal, Costos fijos, Objetivos y semáforos, Costos puntuales (antes "gastos"), Sobres, Flujo de caja.
- Terminología unificada **"gastos" → "costos"** en rótulos visibles de la sección.
- Fuera de alcance por ahora: utilidad/reparto entre socios.

### v3.3 — Sistema de Diseño / Glow-up (2026-07-08)
- **Splash screen** con logo (core-splashscreen) e **ícono de app** propio (fondo blanco + logo, adaptativo).
- **Login** rediseñado: header con gradiente edge-to-edge + **moneda de oro 3D giratoria** dibujada en Canvas (espesor real por rebanadas, cara con logo, giro lento-de-frente/rápido-por-detrás).
- **Home** con hero POS en gradiente y tarjetas de categoría con acento de color; `CategoriaMenuScreen` a juego.
- **Tipografía** de alto impacto (pesos/letter-spacing) sin fuentes externas.
- **Modo oscuro** con mayor contraste (`contrastLevel = 0.4`).
- **Transiciones** de navegación (slide + fade). `MainActivity` sin Scaffold/padding global (cada pantalla maneja insets).
- **Componentes reutilizables**: `SearchField`, `StateComponents` (EmptyState + shimmer + SkeletonList), `ToppisDialogs` (Error/Confirm/Delete unificados).
- **Buscador** en Inventario, Proveedores, Compras, Comprobantes, Historial de Ventas y Menú.
- **Skeleton de carga + estados vacíos** en 14 pantallas de lista (patrón `cargandoInicial` en cada ViewModel).
- **Diálogos unificados** aplicados en Inventario, Proveedores, Sobres, Mermas, Modificadores, Preparaciones, Conteos, Empleados, Locales y Usuarios.
- Pendiente explícito: **rediseño del POS con nuevas funcionalidades** (se dejó para el final).

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
