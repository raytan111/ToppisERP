# Implementation Plan

## Overview

Plan de implementación del rediseño del POS. Sigue las fases A–J del diseño. Cada tarea
es incremental y termina con `./gradlew :app:assembleDebug` verde; al cerrar cada fase se
hace commit + push. El SQL lo ejecuta el usuario en Supabase (el agente solo escribe el
script). Convenciones del proyecto: Kotlin/Compose MVVM (ViewModel + Factory + Screen),
DI manual en `MainActivity`, rutas en `NavGraph`, opción de menú en `HomeMenu`; modelos
`@Serializable` snake_case; enums en `Enums.kt`; componentes compartidos y patrón
`cargandoInicial`; errores en `ToppisErrorDialog`.

## Tasks

### Fase A — Modelo de datos y base

- [x] 1. Script SQL `.kiro/database/supabase-pos-rediseno.sql` (idempotente): tablas
  `clientes`, `pedidos`, `pedido_items`, `pedido_unidades`, `pedido_unidad_mods`,
  `promocion_espacios`, `promocion_espacio_opciones`; columna `modificadores.categoria`;
  ampliar `chk_articulo_categoria` con `BEBIDA_LATA`/`BEBIDA_MEDIANA`; RLS + realtime de
  `pedidos` y `comandas`.
  - _Requisitos: 1.7, 4.1, 5.1, 6.2, 7.1, 10.1_

- [x] 2. Enums en `Enums.kt`: `EstadoPedido`, `TipoLineaPedido`, `ModoEspacioPromo`,
  `CategoriaMenu`; actualizar `ZonaEnvio` (1000–3000, mantener SIN_ENVIO) y
  `CategoriaArticulo` (+ BEBIDA_LATA, BEBIDA_MEDIANA).
  - _Requisitos: 6.1, 6.2, 12.1_

- [x] 3. Modelos `@Serializable`: `Cliente`, `Pedido`, `PedidoItem`, `PedidoUnidad`,
  `PedidoUnidadMod`, `PromocionEspacio`, `PromocionEspacioOpcion`; campo `categoria` en `Modificador`.
  - _Requisitos: 1.1, 4.1, 5.1, 10.1_

- [x] 4. Repos base: `PedidoRepository` (CRUD + Realtime), `ClienteRepository`
  (get-or-create, editar, sellos); extender `PromocionRepository` (espacios/opciones) y
  `ModificadorRepository` (categoría).
  - _Requisitos: 1.7, 4.1, 5.1, 10.1, 10.2_

## Fase B — Dominio puro + tests

- [x] 5. `domain/pos/PosCalculos.kt`: totales, precio de producto (base + mods), precio
  de promo (fijo), modificadores aplicables, elegibles de espacio, promo completa,
  deuda, activo-en-lista, sellos/regalo.
  - _Requisitos: 2.6, 3.4, 5.2, 5.6, 7.3, 11.1, 11.2_

- [x] 6. Tests de propiedad `PosCalculosPropertyTest` (kotest-property, 100 iter) para
  las 16 propiedades del diseño. Build/test verdes.
  - _Requisitos: 5.6, 7.3, 8.4, 11.2, 11.3_

## Fase C — Configuración (menú, modificadores, promos)

- [x] 7. Categorías fijas de menú (incl. Bebida lata/mediana) en `MenuConfigScreen`;
  categorías de bebida en el formulario de artículo (Inventario).
  - _Requisitos: 6.1, 6.2_

- [x] 8. Modificadores por categoría en `ModificadoresScreen` (selector de categoría del
  menú, opcional item puntual).
  - _Requisitos: 4.1, 4.2, 4.3_

- [x] 9. Armado de promos con espacios en `PromocionesScreen`: agregar/editar espacios
  (modo LISTA con opciones, o CATEGORIA), precio fijo.
  - _Requisitos: 5.1, 5.2_

## Fase D — Lista de pedidos + clientes

- [ ] 10. `crear_pedido` (RPC get-or-create cliente + pedido ABIERTO).
  - _Requisitos: 1.2, 10.1, 10.4_

- [ ] 11. `PosPedidosScreen` + `PedidosViewModel`: lista activa realtime, tarjetas con
  estado/total/chips y **marca roja de deuda**; FAB "Nuevo pedido" (3 dígitos + nombre opcional).
  - _Requisitos: 1.1, 1.2, 1.3, 1.4, 1.6, 1.7_

- [ ] 12. Cableado navegación/DI: ruta `pos` (lista), `pedido/{id}`, en `NavGraph` +
  `MainActivity` + `HomeMenu`; permisos.
  - _Requisitos: 1.1, 14.1_

## Fase E — Carrito (armado)

- [ ] 13. `PedidoCarritoScreen` dividida (catálogo arriba con **pestañas Menú/Promos** +
  filtro por categoría, tarjetas con imagen+nombre; carrito abajo con total y envío).
  - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.6_

- [ ] 14. `ProductoModsDialog` (popup **protegido**): modificadores de la categoría +
  comentario libre + precio resultante; Agregar / X.
  - _Requisitos: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 15. Editar/quitar líneas y persistir cambios del carrito (pedido_items/unidades/mods).
  - _Requisitos: 2.5, 2.7_

## Fase F — Promos en el POS

- [ ] 16. `PromoConfigDialog`: un selector por espacio (solo elegibles), permite
  mods/comentario por elegido; Agregar cuando la promo está completa; precio fijo.
  - _Requisitos: 5.3, 5.4, 5.5, 5.6_

## Fase G — Estados y pago

- [ ] 17. `cerrar_pedido` (RPC): genera/guarda comanda, estado CERRADO; al abrir un
  pedido CERRADO se muestra la comanda.
  - _Requisitos: 7.2, 7.6, 9.4_

- [ ] 18. `pagar_pedido` (RPC atómica): materializa venta desde `pedido_unidades`,
  descuenta stock + mods, ingresa al sobre, marca pagado/venta_id; idempotente por venta_id.
  - _Requisitos: 7.4, 8.1, 8.2, 8.3, 8.4_

- [ ] 19. `marcar_entregado` (RPC) + `PagarDialog` (método → sobre) + avisos de deuda
  (rojo en lista; aviso al entregar sin pagar, no bloquea).
  - _Requisitos: 7.3, 7.5, 8.1, 1.6_

## Fase H — Cocina (KDS)

- [ ] 20. `ComandasScreen` + `ComandasViewModel`: lista realtime de pedidos CERRADO no
  entregados con productos/mods/comentarios; botón Entregar; ruta/DI/permisos.
  - _Requisitos: 9.1, 9.2, 9.3_

## Fase I — Cuponera y clientes

- [ ] 21. Cuponera en `pagar_pedido`: +1 sello si trae hamburguesa; habilitar Cheese
  gratis a los 6; aplicar regalo (precio 0, −6 sellos, marca REGALO, no merma).
  - _Requisitos: 11.1, 11.2, 11.3_

- [ ] 22. `ClientesScreen` + `ClientesViewModel`: lista con búsqueda, editar nombre,
  ver **deuda** e **historial**, ver/fijar **sellos** (cargar cupones existentes).
  - _Requisitos: 10.2, 10.3, 11.4, 11.5_

## Fase J — Envío, diseño final y verificación

- [ ] 23. Envío por zonas (1000–3000) en el carrito, sumado al total y registrado en la venta.
  - _Requisitos: 12.1, 12.2, 12.3_

- [ ] 24. Pulido visual (imágenes, acentos, EmptyState/Skeleton, errores en
  `ToppisErrorDialog`) y consistencia con el sistema de diseño.
  - _Requisitos: 13.1, 13.2, 13.3, 13.4_

- [ ] 25. Verificación final: build verde, correr tests de propiedad, revisar flujo
  completo (crear→armar→cerrar→pagar→entregar + cocina + cupón); actualizar `README.md`
  y `.kiro/PROYECTO-CONTEXTO.md`.
  - _Requisitos: 14.1, 14.2, 14.3_

---

## Task Dependency Graph

```
A(1,2,3,4) → B(5,6) → C(7,8,9)
A → D(10,11,12) → E(13,14,15) → F(16) → G(17,18,19) → H(20) → I(21,22) → J(23,24,25)
C alimenta E/F (categorías, mods, promos)
```

```json
{
  "waves": [
    { "wave": 1, "tasks": [1, 2, 3] },
    { "wave": 2, "tasks": [4, 5] },
    { "wave": 3, "tasks": [6, 7, 8, 9, 10] },
    { "wave": 4, "tasks": [11, 12] },
    { "wave": 5, "tasks": [13, 14, 15] },
    { "wave": 6, "tasks": [16, 17, 18] },
    { "wave": 7, "tasks": [19, 20] },
    { "wave": 8, "tasks": [21, 22] },
    { "wave": 9, "tasks": [23, 24, 25] }
  ]
}
```

## Notes

- SQL: el usuario corre `supabase-pos-rediseno.sql`. Todas las RPC nuevas con UPDATE
  llevan WHERE (guard *safeupdate*).
- `ventas`/`items_venta_menu` no cambian de forma; el pago las alimenta.
- Realtime: canales UUID nuevos para `pedidos` y `comandas`.
- POS actual se reemplaza por el flujo de pedidos; ventas históricas intactas.
