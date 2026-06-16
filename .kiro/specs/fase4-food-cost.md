# Fase 4 — Costos y Rentabilidad (Food Cost) — Spec Técnica

## Estado: Spec / sin implementar (esperando aprobación)
## Base: roadmap-erp-franquicia.md (v3, decisiones D1-D4, Q1-Q7, QF1-QF4 confirmadas)

> Esta fase es **fundacional**: rehace el modelo de datos de cocina (clean slate, Q4)
> para soportar unidades reales, artículos unificados, sub-recetas, modificadores y
> promociones, y "enciende" el food cost. El backend (Supabase, repos, DI manual,
> Realtime) se reutiliza; el modelo de datos se rehace.

---

## 1. Objetivo y Alcance

### Entra en Fase 4
1. **Sistema de unidades** (MASA→g, VOLUMEN→ml, UNIDAD→un) con conversión compra→base.
2. **Artículos** unificados (reemplazan ingredientes + insumos): stock y costo en unidad base.
3. **Sub-recetas / preparaciones** (bechamel, cheddar): se producen por lote y generan stock con costo por gramo derivado.
4. **Recetas de venta** referidas a artículos y/o preparaciones, todo en unidad base.
5. **Modificadores** (doble, quitar, reemplazar, extra) con delta de receta y delta de precio.
6. **Promociones** (creador manual) con cálculo de costo/ganancia/food cost %.
7. **Food cost**: costo teórico por plato, food cost % por item y por venta.
8. **Menu engineering** (estrellas/vacas/puzzles/perros) y **KPI food cost %** en dashboard.
9. **Rendimiento de papa**: formulario para cargar pesos por etapa (datos opcionales, QF1).

### NO entra (fases siguientes)
- Conteos de inventario y variance teórico vs real → Fase 5
- Waste log formal y par levels → Fase 5 (en Fase 4 solo el modelo de merma por % básico que ya existe migrado)
- Compras/proveedores y costo promedio ponderado, caducidad por lote → Fase 6
- Mano de obra / prime cost → Fase 8
- location_id / multi-local → Fase 9

---

## 2. Modelo de Datos Nuevo

### 2.1 Enums nuevos
```
dimension_unidad : MASA | VOLUMEN | UNIDAD
tipo_articulo    : INGREDIENTE | PREPARACION   (PREPARACION = sub-receta producida)
tipo_componente  : ARTICULO | PREPARACION       (reemplaza INGREDIENTE/INSUMO/SALSA)
tipo_modificador : DOBLE | QUITAR | REEMPLAZAR | EXTRA
tipo_promocion   : COMBO | DESCUENTO_PORCENTAJE  (más adelante: 2X1 | PRECIO_ESPECIAL)
```

### 2.2 Tabla `articulos` (reemplaza `ingredientes` + `insumos`)
Concepto único. Bebidas, lechuga, carne, pan, packaging: todo es artículo.
```
id              SERIAL PK
nombre          TEXT
dimension       dimension_unidad      -- MASA / VOLUMEN / UNIDAD
unidad_base     TEXT                  -- 'g' | 'ml' | 'un' (derivado de dimension)
unidad_compra   TEXT                  -- ej 'saco 25kg', 'pack 6', 'bidón 5L'
factor_compra   NUMERIC(14,4)         -- cuántas unidades base trae 1 unidad de compra
                                      --   saco 25kg → 25000 (g); pack 6 → 6 (un); bidón 5L → 5000 (ml)
costo_compra    NUMERIC(12,2)         -- lo que cuesta 1 unidad de compra
costo_base      NUMERIC(14,6)         -- costo por unidad base = costo_compra / factor_compra / rendimiento
rendimiento     NUMERIC(6,4) DEFAULT 1 -- AP→EP (1 = sin pérdida). Papa usa <1 (QF1)
stock_base      NUMERIC(14,4) DEFAULT 0 -- stock en unidad base
par_level       NUMERIC(14,4) DEFAULT 0 -- usado en Fase 5 (se deja el campo)
perecible       BOOLEAN DEFAULT false
vida_util_dias  INTEGER DEFAULT 0
es_vendible     BOOLEAN DEFAULT false  -- true para bebidas que se venden directo
activo          BOOLEAN DEFAULT true
+ auditoría (created_at, updated_at, created_by)
```
- `costo_base` se recalcula al editar costo_compra/factor/rendimiento (cálculo en repo o trigger).
- Para bebidas: dimension=UNIDAD, unidad_compra='pack 6', factor_compra=6, vende por unidad.

### 2.3 Tabla `preparaciones` (sub-recetas, Q1)
```
id                 SERIAL PK
nombre             TEXT                 -- 'Salsa bechamel', 'Salsa cheddar'
dimension          dimension_unidad     -- normalmente MASA o VOLUMEN
rendimiento_lote   NUMERIC(14,4)        -- cuánta unidad base produce un lote (ej 2000 g)
costo_lote         NUMERIC(12,2)        -- costo total del lote (suma de su receta) — calculado
costo_base         NUMERIC(14,6)        -- costo_lote / rendimiento_lote (costo por g/ml)
stock_base         NUMERIC(14,4) DEFAULT 0
activo             BOOLEAN DEFAULT true
+ auditoría
```

### 2.4 Tabla `preparacion_componentes` (receta de una preparación)
```
id              SERIAL PK
preparacion_id  INTEGER FK preparaciones ON DELETE CASCADE
tipo_componente tipo_componente          -- ARTICULO | PREPARACION (anidación permitida)
componente_id   INTEGER
cantidad_base   NUMERIC(14,4)            -- en unidad base
+ auditoría
```

### 2.5 Tabla `recetas_menu` (rehecha)
```
id              SERIAL PK
item_menu_id    INTEGER FK items_menu ON DELETE CASCADE
tipo_componente tipo_componente          -- ARTICULO | PREPARACION
componente_id   INTEGER
cantidad_base   NUMERIC(14,4)            -- en unidad base (g/ml/un)
+ auditoría
```
> Las salsas dejan de ser un tipo aparte: una salsa es un ARTICULO (comprada) o una PREPARACION (hecha en casa). El POS sigue mostrando "salsas" como artículos/preparaciones marcados para selección (ver 2.9).

### 2.6 `items_menu` (se mantiene, + costo cacheado)
```
+ costo_teorico  NUMERIC(12,2) DEFAULT 0   -- suma de costos de la receta (cache, recalculado)
+ categoria      TEXT DEFAULT ''           -- 'Hamburguesas','Papas','Bebidas','Completos'...
```
- `precio` ya existe. food_cost_% = costo_teorico / precio.

### 2.7 Tabla `modificadores` (Q2)
Catálogo de modificadores aplicables.
```
id              SERIAL PK
nombre          TEXT                     -- 'Doble', 'Sin cebolla', 'Extra queso', 'Cambiar lechuga→rúcula'
tipo            tipo_modificador         -- DOBLE | QUITAR | REEMPLAZAR | EXTRA
item_menu_id    INTEGER NULL FK          -- NULL = aplica a cualquiera; o restringido a un item
delta_precio    NUMERIC(12,2) DEFAULT 0  -- lo que suma/resta al precio
activo          BOOLEAN DEFAULT true
+ auditoría
```

### 2.8 Tabla `modificador_componentes` (delta de receta del modificador)
```
id               SERIAL PK
modificador_id   INTEGER FK ON DELETE CASCADE
accion           TEXT          -- 'AGREGAR' | 'QUITAR'
tipo_componente  tipo_componente
componente_id    INTEGER
cantidad_base    NUMERIC(14,4)
```
- DOBLE = agregar los componentes principales otra vez (o un set definido).
- QUITAR = quita el componente (resta consumo/costo).
- REEMPLAZAR = un QUITAR + un AGREGAR.
- EXTRA = un AGREGAR + delta_precio>0.

### 2.9 Selección tipo "salsa" en POS
- Se agrega flag `seleccionable_en_pos BOOLEAN` a `articulos` y `preparaciones`, o una tabla `opciones_pos` que lista artículos/preparaciones ofrecibles como agregado al armar el pedido. Decisión de implementación: **flag en cada tabla** (más simple).

### 2.10 Tabla `promociones` (Q3 / QF2)
```
id              SERIAL PK
nombre          TEXT
tipo            tipo_promocion           -- COMBO | DESCUENTO_PORCENTAJE
precio          NUMERIC(12,2)            -- precio final de la promo (COMBO) o se calcula (DESCUENTO)
descuento_pct   NUMERIC(5,2) DEFAULT 0   -- para DESCUENTO_PORCENTAJE
activo          BOOLEAN DEFAULT true
fecha_inicio    DATE NULL
fecha_fin       DATE NULL
+ auditoría
```

### 2.11 Tabla `promocion_items`
```
id              SERIAL PK
promocion_id    INTEGER FK ON DELETE CASCADE
item_menu_id    INTEGER FK items_menu
cantidad        INTEGER DEFAULT 1
```
- Cálculo de la promo (en repo/vista):
  - costo_promo = Σ (item.costo_teorico × cantidad)
  - precio_normal = Σ (item.precio × cantidad)
  - precio_promo = `precio` (COMBO) o precio_normal × (1 − descuento_pct/100)
  - ganancia = precio_promo − costo_promo ; ganancia_% = ganancia / precio_promo
  - food_cost_% = costo_promo / precio_promo
  - ahorro_cliente = precio_normal − precio_promo

### 2.12 Tabla `papa_rendimientos` (QF1 — formulario para cargar después)
```
id              SERIAL PK
articulo_id     INTEGER FK articulos      -- el artículo 'Papa cruda'
fecha           DATE DEFAULT today
peso_crudo      NUMERIC(12,2)
peso_pelado     NUMERIC(12,2)
peso_prefrito   NUMERIC(12,2)
peso_frito      NUMERIC(12,2)
rendimiento     NUMERIC(6,4)              -- peso_frito / peso_crudo (calculado)
+ auditoría
```
- Mientras no haya registros, el artículo papa usa `rendimiento` por defecto (campo en `articulos`).
- Al guardar un registro, opcionalmente actualiza `articulos.rendimiento` (promedio o último).

### 2.13 `items_venta_menu` (+ trazabilidad de costo y modificadores)
```
+ costo_unitario  NUMERIC(12,2) DEFAULT 0  -- costo teórico del item al momento de la venta (snapshot)
+ modificadores   TEXT DEFAULT ''          -- JSON de modificadores aplicados (id + deltas)
+ promocion_id    INTEGER NULL FK promociones
```
> `salsas_seleccionadas` existente se mantiene por compatibilidad o se migra a `modificadores`.

---

## 3. Cálculo de Food Cost (lógica)

### 3.1 Costo de un artículo por unidad base
```
costo_base = (costo_compra / factor_compra) / rendimiento
```
Ej papa: saco 25 kg ($8.000), factor 25000 g, rendimiento 0.55 →
costo_base = (8000/25000)/0.55 = 0.32/0.55 = 0.5818 $/g servible.

### 3.2 Costo de una preparación por unidad base
```
costo_lote  = Σ (componente.costo_base × cantidad_base)
costo_base  = costo_lote / rendimiento_lote
```

### 3.3 Costo teórico de un item de menú
```
costo_teorico = Σ (componente.costo_base × cantidad_base)
```
(componente = artículo o preparación). Se cachea en `items_menu.costo_teorico` y se recalcula al editar receta o costos.

### 3.4 Con modificadores (en venta)
```
costo_item_venta = costo_teorico
                 + Σ(agregados.costo_base × cantidad)
                 − Σ(quitados.costo_base  × cantidad)
precio_item_venta = precio + Σ delta_precio
```

### 3.5 Food cost %
```
food_cost_%_item  = costo_teorico / precio
food_cost_%_venta = Σ costo_item / Σ precio_item
food_cost_%_periodo = Σ costo de items vendidos / Σ ventas (neto)
```
Benchmark mostrado: meta ~28-30% (QSR).

### 3.6 Menu engineering (clasificación)
Por período, cada item:
- **Popularidad** = unidades vendidas vs promedio (alta/baja)
- **Margen** = (precio − costo) vs promedio (alto/bajo)
- Estrella (alta+alto), Vaca/Caballo de batalla (alta+bajo), Puzzle (baja+alto), Perro (baja+bajo)

---

## 4. Modelos Kotlin (data/models)

Nuevos / modificados (`@Serializable`, `@SerialName` snake_case):
- `Articulo.kt` (reemplaza Ingrediente/Insumo en uso; se pueden eliminar los viejos)
- `Preparacion.kt`, `PreparacionComponente.kt`
- `RecetaMenu.kt` (campo `cantidadBase`, `tipoComponente` nuevo enum)
- `ItemMenu.kt` (+ `costoTeorico`, `categoria`)
- `Modificador.kt`, `ModificadorComponente.kt`
- `Promocion.kt`, `PromocionItem.kt`
- `PapaRendimiento.kt`
- `ItemVentaMenu.kt` (+ `costoUnitario`, `modificadores`, `promocionId`)
- Enums nuevos en `data/db/entities/Enums.kt`: `DimensionUnidad`, `TipoArticulo`, `TipoComponente` (rehacer), `TipoModificador`, `TipoPromocion`, `AccionModificador`

DTOs de cálculo (no tablas):
- `data class FoodCostItem(itemMenuId, nombre, precio, costoTeorico, foodCostPct, margen)`
- `data class AnalisisPromocion(costoPromo, precioNormal, precioPromo, ganancia, gananciaPct, foodCostPct, ahorroCliente)`
- `data class ClasificacionMenu(item, popularidad, margen, categoria)` // estrella/perro...

---

## 5. Repositorios (data/repository)

Patrón establecido: `SupabaseClient.client`, `getX()` suspend, `observeCambios()` Realtime con canal UUID, refresh-after-write en ViewModel.

- **`ArticuloRepository`** (reemplaza/absorbe Inventario de ingredientes+insumos): CRUD artículos, recálculo `costo_base`, observe.
- **`PreparacionRepository`**: CRUD preparaciones + componentes, recálculo `costo_lote`/`costo_base`, "producir lote" (suma stock_base, descuenta artículos) — la producción real (descuento de stock) puede quedar mínima en Fase 4 y completarse en Fase 5.
- **`MenuRepository`** (refactor): recetas con nuevo tipo_componente; calcular `costo_teorico` y persistirlo; food cost por item.
- **`ModificadorRepository`**: CRUD modificadores + componentes.
- **`PromocionRepository`**: CRUD promos + items; `analizarPromocion(id): AnalisisPromocion`.
- **`FoodCostRepository`** (o ampliar ReporteRepository): food cost % por período, menu engineering, KPIs dashboard.
- **`PapaRendimientoRepository`**: CRUD registros de pesos; actualizar rendimiento del artículo papa.

RPC PostgreSQL (atómicas, en `.kiro/database/`):
- `recalcular_costo_item(item_id)` opcional, o calcular en repo.
- `producir_preparacion(prep_id, n_lotes)` (Fase 5 completo; Fase 4 stub o cálculo de costo).

---

## 6. UI (Jetpack Compose + Material 3)

### 6.1 Artículos (Admin) — reemplaza pantalla de inventario actual
- Lista de artículos con stock_base, costo_base, par level.
- Form crear/editar: nombre, dimensión, unidad de compra + factor, costo de compra, rendimiento, perecible/vida útil, es_vendible, seleccionable_en_pos.
- Muestra costo_base calculado en vivo.

### 6.2 Preparaciones (Admin)
- Lista con costo por g/ml y rendimiento de lote.
- Form: nombre, dimensión, rendimiento_lote, receta (agregar artículos/preparaciones con cantidad base). Muestra costo_lote y costo_base.

### 6.3 Menú / Recetas (Admin) — refactor
- Editor de receta usa artículos y preparaciones (todo en g/ml/un).
- Muestra **costo teórico**, **precio**, **margen** y **food cost %** del item en vivo.

### 6.4 Modificadores (Admin)
- CRUD de modificadores (doble, quitar, reemplazar, extra) con su delta de receta y delta de precio.

### 6.5 Promociones (Admin) — **creador manual (QF2)**
- Pantalla: nombre, tipo (combo / % descuento), agregar items del menú con cantidad, precio (combo) o % descuento.
- Panel de análisis en vivo: costo total, precio, **ganancia $ y %**, **food cost %**, ahorro al cliente vs por separado.
- Vigencia opcional.

### 6.6 POS (refactor moderado)
- Al agregar item: permitir **modificadores** (doble/quitar/reemplazar/extra) y selección de salsas (artículos/preparaciones seleccionables).
- Permitir agregar **promociones** como línea.
- Calcula costo snapshot por línea (para food cost de la venta).

### 6.7 Reportes / Dashboard
- **KPI food cost %** del período (con meta 28-30%).
- **Menu engineering**: tabla con clasificación estrella/vaca/puzzle/perro.
- Food cost % por item (ordenable).

### 6.8 Rendimiento de Papa (Admin) — **formulario QF1**
- Form para ingresar pesos (crudo/pelado/prefrito/frito) por fecha. Calcula y muestra rendimiento. Historial. Botón "usar como rendimiento del artículo papa".

---

## 7. Clean Slate / Migración (Q4)

Como no hay datos reales:
- Script `supabase-fase4-schema.sql`: `DROP` de tablas viejas de cocina (ingredientes, insumos, salsas, recetas_menu vieja) y `CREATE` del modelo nuevo (articulos, preparaciones, preparacion_componentes, recetas_menu nueva, modificadores, modificador_componentes, promociones, promocion_items, papa_rendimientos) + enums + triggers updated_at + índices.
- Script `supabase-fase4-rls.sql`: RLS por tabla (Admin escribe, Cajero lee lo necesario).
- Script `supabase-fase4-realtime.sql`: publicación realtime de tablas relevantes.
- Mantener: usuarios, sobres, movimientos_sobre, ventas, items_venta_menu (alterada), comandas, gastos, presupuestos, comprobantes, contabilidad.
- Eliminar modelos Kotlin obsoletos (Ingrediente, Insumo, Salsa) tras refactor, o conservarlos temporalmente si el POS los referencia hasta migrar.

Usuario ejecuta los scripts manualmente en Supabase SQL Editor (orden documentado).

---

## 8. Plan de Tareas (orden de implementación)

1. **SQL**: enums + tablas nuevas + RLS + realtime (clean slate). Usuario ejecuta.
2. **Modelos Kotlin** nuevos + enums.
3. **ArticuloRepository** + UI Artículos (CRUD + costo_base en vivo). Build + commit.
4. **PreparacionRepository** + UI Preparaciones. Build + commit.
5. **MenuRepository refactor** (recetas con artículos/preparaciones + costo_teorico + food cost por item) + UI receta. Build + commit.
6. **Modificadores** repo + UI. Build + commit.
7. **Promociones** repo + UI creador con análisis. Build + commit.
8. **POS refactor** (modificadores, salsas, promos, costo snapshot). Build + commit.
9. **Reportes**: food cost %, menu engineering + KPI dashboard. Build + commit.
10. **Papa rendimientos** form + repo. Build + commit.
11. DI manual en MainActivity para todos los repos nuevos; verificación final `:app:compileDebugKotlin` + `:app:assembleDebug`.

Cada paso con build verde se commitea a GitHub (raytan111/ToppisERP).

---

## 9. Riesgos y Notas
- El POS es lo más sensible (refactor de selección de salsas → artículos/preparaciones + modificadores). Mantener compatibilidad de `items_venta_menu` durante la transición.
- `costo_base` cacheado debe recalcularse de forma consistente (al editar costo/factor/rendimiento y al producir preparaciones). Considerar trigger o recálculo explícito en repo.
- No es asesoría contable/legal; food cost es gestión interna.
- Realtime: usar canales con nombre único (UUID) como ya está establecido.

---

**Fecha**: 2026-06-15
**Autor**: Kiro + andreslh
**Estado**: Spec lista para revisión. No implementar hasta aprobación del usuario.
