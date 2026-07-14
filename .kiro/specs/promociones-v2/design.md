# Design Document

## Overview

Este rediseño **extiende** el modelo de promos ya existente (grupos de elección) en vez
de reemplazarlo. Los cambios son incrementales:

- Agregar `permite_repetir` a los grupos.
- Reemplazar la creación por **AlertDialog** (`CrearPromocionDialog` + `ArmarPromoDialog`
  + `EspaciosPromoDialog`) por una **pantalla dedicada** `PromocionEditorScreen`.
- Reemplazar el `PromoConfigDialog` de dropdowns del POS por una selección **con
  tarjetas de imagen** y contadores por grupo.

Lo que **NO cambia** (ya funciona y no se toca): el pago atómico (`pagar_pedido`)
materializa la venta con el detalle real de cada unidad y su `costo_teorico`
(food cost real por venta); la comanda lista los productos elegidos; el precio es fijo.

## Architecture

Capa de datos: `PromocionRepository` (ya tiene CRUD de promos, grupos y opciones); se
agrega `permite_repetir`. Capa de dominio pura `domain/pos/PosCalculos.kt` gana la
validación de repetición/completitud. Presentación Compose MVVM: nuevo
`PromocionEditorScreen` + `PromocionEditorViewModel`; el POS usa el `CarritoViewModel`
existente con un selector nuevo.

## Data Models

### Cambio en `promocion_espacios`
```
ALTER TABLE promocion_espacios ADD COLUMN permite_repetir BOOLEAN NOT NULL DEFAULT TRUE;
```
- Modelo `PromocionEspacio` gana `permiteRepetir: Boolean = true`.
- No se crean tablas nuevas. `promocion_espacio_opciones` se mantiene igual.
- Las bebidas siguen siendo `items_menu` de categoría "Bebida lata"/"Bebida mediana"
  con su `imagen_url`; la fuente `CATEGORIA` ya las incluye automáticamente.

## Components and Interfaces

### Creación/edición — `PromocionEditorScreen` (ruta `promo_editor` / `promo_editor/{id}`)
- Encabezado: `ImagePickerField` (imagen de la promo), campo **nombre**, campo **precio**.
- Sección **Grupos** (lista editable): cada grupo es una tarjeta con nombre, cantidad,
  fuente y `permite_repetir`; botón para agregar grupo.
- Editor de grupo (inline o sub-sheet):
  - **Lista fija**: grilla de productos del menú con **tarjetas de imagen**; tocar
    marca/desmarca la opción (check visual).
  - **Categoría**: selector de categoría (Bebida lata / Bebida mediana / Hamburguesas /
    etc.) + vista previa de los productos incluidos.
  - Switch `permite_repetir` y campo cantidad.
- Botón **Guardar** (crea o actualiza promo + grupos + opciones).
- `PromocionesScreen` cambia: el FAB y el tap en una promo abren esta pantalla; se quitan
  los diálogos `CrearPromocionDialog`, `ArmarPromoDialog` y `EspaciosPromoDialog`.

### Venta — selector interactivo en el POS
- Reemplaza `PromoConfigDialog` (dropdowns) por una vista de selección con **tarjetas de
  imagen** por grupo:
  - Encabezado del grupo: "nombre (elegidas/cantidad)".
  - Grilla de opciones (foto + nombre); tocar suma una unidad.
  - Lista de unidades elegidas con opción de quitar.
  - Respeta `permite_repetir` (si false, deshabilita/omite productos ya elegidos).
  - **Agregar** habilitado solo cuando todos los grupos están completos.
- Puede seguir siendo un diálogo protegido a pantalla casi completa, o una vista; se
  mantiene el estilo del POS.

### Repositorio
- `PromocionRepository.crearEspacio(...)` y modelo ganan `permiteRepetir`.
- Se agrega guardado "en bloque" opcional para el editor (crear promo + grupos + opciones
  en secuencia); si falla algo se informa con `ToppisErrorDialog`.

## Correctness Properties

### Property 1: Completitud por suma de unidades
Un grupo está completo si y solo si la suma de unidades elegidas es igual a su cantidad,
sin exigir que los productos sean iguales.
**Validates: Requirements 1.4, 3.5**

### Property 2: Repetición permitida
Si `permite_repetir = true`, un mismo producto puede aparecer varias veces hasta
completar la cantidad del grupo.
**Validates: Requirements 1.2**

### Property 3: Repetición prohibida
Si `permite_repetir = false`, no puede haber dos unidades del mismo producto en un grupo;
las unidades del grupo son todas distintas.
**Validates: Requirements 1.3**

### Property 4: Promo completa
La promo se puede agregar si y solo si todos sus grupos están completos.
**Validates: Requirements 3.5**

### Property 5: Precio fijo
El precio de la promo agregada es siempre su precio fijo, independiente de lo elegido.
**Validates: Requirements 3.6, 5.3**

## Error Handling
- Errores de guardado/venta en `ToppisErrorDialog` (popup).
- El editor valida: nombre no vacío, precio > 0, cada grupo con cantidad ≥ 1 y (si lista
  fija) al menos una opción.
- Guardar es tolerante: si una parte falla, se informa y no se deja la promo a medias
  (se valida antes de persistir).

## Testing Strategy
- Tests de propiedad (kotest-property) para las 5 propiedades (completitud, repetición,
  precio fijo) sobre funciones puras nuevas en `PosCalculos`.
- Verificación manual del flujo: crear promo Duo con 2 hamburguesas a elección + 2
  bebidas por categoría; venderla en el POS eligiendo iguales y distintas; revisar
  comanda y food cost.
- Build verde al cerrar cada fase.

## Plan por fases (cada fase: build verde → commit + push)
- **Fase A — Datos + dominio**: `permite_repetir` (SQL + modelo + repo); funciones puras
  en `PosCalculos` (validación repetición/completitud) + tests de propiedad.
- **Fase B — Pantalla de creación**: `PromocionEditorScreen` + `PromocionEditorViewModel`,
  con imágenes; reemplaza los diálogos de creación en `PromocionesScreen`; ruta + DI.
- **Fase C — Selección en POS**: selector interactivo con imágenes y contadores por grupo
  (reemplaza `PromoConfigDialog`), respeta `permite_repetir`.
- **Fase D — Verificación + docs**: build + tests, actualizar README y PROYECTO-CONTEXTO.

## Compatibilidad
- `permite_repetir` default TRUE → promos existentes siguen igual.
- `pagar_pedido`, comanda, stock y cupón no se tocan.
