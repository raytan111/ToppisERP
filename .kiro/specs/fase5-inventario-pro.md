# Fase 5 — Inventario Profesional — Spec Técnica

## Estado: Spec / en implementación
## Base: roadmap-erp-franquicia.md + Fase 4 (artículos, recetas, food cost)

> Objetivo: frenar fugas de dinero (merma, robo, sobre-porción) y nunca quedarse
> sin stock. Construye sobre el modelo de artículos/preparaciones de Fase 4.

---

## 1. Módulos de Fase 5

### 5.1 Mermas con motivo (waste log) — PRIMERO
Registro formal de pérdidas que descuentan stock.
- Tabla `mermas`: artículo o preparación, cantidad (unidad base), **motivo**, fecha, usuario, costo (snapshot).
- Motivos: VENCIDO, ESTROPEADO, VINO_MALO, ERROR_COCINA, CORTESIA, ROBO, OTRO.
- Al registrar: descuenta `stock_base` y guarda el costo perdido (costo_base × cantidad).
- Pantalla: registrar merma + historial + total de merma del período por motivo.

### 5.2 Conteos de inventario (stock take)
Conteo físico periódico que ajusta el stock del sistema a la realidad.
- Tabla `conteos` (cabecera: fecha, usuario, estado) + `conteo_detalle` (artículo, stock_sistema, stock_contado, diferencia).
- Al cerrar el conteo: ajusta `stock_base` al valor contado; la diferencia queda registrada (es la "merma no registrada"/variance).
- Pantalla: nuevo conteo (lista artículos con stock sistema, input contado), guardar/cerrar.

### 5.3 Variance: teórico vs real
- **Teórico**: lo que las recetas dicen que se consumió según lo vendido (de items_venta_menu + recetas + mods_comp).
- **Real**: stock inicial + ingresos − stock final (del conteo).
- **Variance = Real − Teórico**; revela mermas no registradas/robo/sobre-porción.
- Reporte por período y por artículo (en pantalla de Reportes o Inventario).

### 5.4 Par levels + lista de compra sugerida
- `par_level` ya existe en artículos.
- Pantalla "Compra sugerida": lista de artículos con `stock_base < par_level`, mostrando cuánto comprar para llegar al par (en unidad de compra) y costo estimado.

---

## 2. Esquema SQL (nuevo)

### tabla `mermas`
```
id SERIAL PK
tipo_componente tipo_componente   -- ARTICULO | PREPARACION
componente_id INTEGER
cantidad_base NUMERIC(14,4)        -- en unidad base
motivo TEXT (CHECK en lista)
costo NUMERIC(12,2)                -- costo perdido (snapshot)
fecha TIMESTAMPTZ DEFAULT now()
nota TEXT DEFAULT ''
+ auditoría (created_by)
```

### tabla `conteos`
```
id SERIAL PK
fecha TIMESTAMPTZ DEFAULT now()
estado TEXT ('ABIERTO'|'CERRADO') DEFAULT 'ABIERTO'
nota TEXT DEFAULT ''
+ auditoría
```
### tabla `conteo_detalle`
```
id SERIAL PK
conteo_id INTEGER FK conteos ON DELETE CASCADE
articulo_id INTEGER FK articulos
stock_sistema NUMERIC(14,4)
stock_contado NUMERIC(14,4)
diferencia NUMERIC(14,4)           -- contado - sistema
+ auditoría
```

### RPC
- `registrar_merma(tipo, id, cantidad, motivo, costo, nota)` → inserta merma + descuenta stock (atómico).
- `cerrar_conteo(conteo_id)` → por cada detalle, ajusta articulos.stock_base = stock_contado; marca conteo CERRADO.

---

## 3. Modelos Kotlin
- `Merma`, enum `MotivoMerma`
- `Conteo`, `ConteoDetalle`
- DTOs: `VarianceItem`, `CompraSugerida`

## 4. Repositorios
- `MermaRepository` (CRUD + registrar vía RPC + totales por motivo)
- `ConteoRepository` (crear, agregar detalle, cerrar vía RPC)
- `InventarioReporteRepository` o ampliar: variance + compra sugerida

## 5. UI (Admin, en menú "Más")
- **Mermas**: registrar (artículo/prep, cantidad, motivo, nota) + historial + total período.
- **Conteo de inventario**: nuevo conteo con lista de artículos, ingresar contado, cerrar.
- **Compra sugerida**: lista bajo par + cantidad a comprar.
- **Variance**: en Reportes o Inventario.

## 6. Orden de implementación
1. Mermas (SQL + repo + pantalla)  ← empezamos aquí
2. Conteos (SQL + repo + pantalla)
3. Compra sugerida (repo + pantalla, sin SQL nuevo)
4. Variance (reporte)

Cada paso con build verde + commit.

---

**Fecha**: 2026-06-16
**Estado**: Fase 5 completa — Mermas, Conteos, Compra Sugerida y Variance implementados.
