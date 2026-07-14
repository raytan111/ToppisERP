# Implementation Plan

## Overview

Rediseño del módulo de Promociones sobre el modelo de grupos ya existente. Cada tarea es
incremental y termina con `./gradlew :app:assembleDebug` verde; al cerrar cada fase,
commit + push. El SQL lo corre el usuario en Supabase. Convenciones del proyecto:
Kotlin/Compose MVVM, DI en `MainActivity`, rutas en `NavGraph`, componentes compartidos
(ToppisTopBar, ImagePickerField, StateComponents, ToppisDialogs), errores en
`ToppisErrorDialog`. No se toca `pagar_pedido`, comanda, stock ni cupón.

## Tasks

### Fase A — Datos + dominio

- [x] 1. SQL `supabase-promos-v2.sql`: `ALTER TABLE promocion_espacios ADD COLUMN
  permite_repetir BOOLEAN NOT NULL DEFAULT TRUE`. Modelo `PromocionEspacio` +
  `permiteRepetir`; `PromocionRepository.crearEspacio` guarda `permite_repetir`.
  - _Requisitos: 1.1, 6.1_

- [x] 2. Funciones puras en `domain/pos/PosCalculos.kt`: `grupoCompleto(cantidad, elegidas)`,
  `puedeAgregarAlGrupo(permiteRepetir, yaElegidos, productoId, cantidad)`,
  `promoCompletaPorGrupo(...)`. Tests de propiedad (kotest-property) para las 5 propiedades.
  - _Requisitos: 1.2, 1.3, 1.4, 3.5, 5.3_

### Fase B — Pantalla de creación/edición (view)

- [x] 3. `PromocionEditorViewModel` (+ Factory): cargar promo/grupos/opciones para editar;
  guardar nombre/precio/imagen + grupos (nombre, cantidad, fuente, permite_repetir) +
  opciones de lista fija.
  - _Requisitos: 2.7, 6.2_

- [x] 4. `PromocionEditorScreen` (view, no popup): encabezado (imagen/nombre/precio),
  lista de grupos editable, editor de grupo con **tarjetas de imagen** para lista fija y
  selector + preview para categoría, switch `permite_repetir`, resumen y guardar.
  - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 4.1, 4.2_

- [x] 5. Integrar en `PromocionesScreen`: FAB y tap abren `PromocionEditorScreen`; quitar
  `CrearPromocionDialog`/`ArmarPromoDialog`/`EspaciosPromoDialog`. Ruta + DI en NavGraph/MainActivity.
  - _Requisitos: 2.1_

### Fase C — Selección interactiva en el POS

- [x] 6. Nuevo selector de promo en el carrito con **tarjetas de imagen** por grupo,
  contador "elegidas/cantidad", sumar/quitar unidades, respeta `permite_repetir`;
  reemplaza `PromoConfigDialog`. Habilita Agregar solo con todos los grupos completos;
  agrega a precio fijo.
  - _Requisitos: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 4.2_

### Fase D — Verificación + docs

- [ ] 7. Build verde, correr tests de propiedad, verificar flujo (crear Duo → vender
  eligiendo iguales/distintas → comanda + food cost); actualizar `README.md` y
  `.kiro/PROYECTO-CONTEXTO.md`.
  - _Requisitos: 5.1, 5.2, 5.3, 6.2_

## Task Dependency Graph

```
A(1,2) → B(3,4,5) → C(6) → D(7)
```

```json
{
  "waves": [
    { "wave": 1, "tasks": [1, 2] },
    { "wave": 2, "tasks": [3, 4, 5] },
    { "wave": 3, "tasks": [6] },
    { "wave": 4, "tasks": [7] }
  ]
}
```

## Notes
- `permite_repetir` default TRUE preserva las promos existentes.
- Las bebidas por categoría ya quedan disponibles automáticamente (fuente CATEGORIA).
- El food cost real por venta y el detalle en comanda ya funcionan desde el POS actual.
