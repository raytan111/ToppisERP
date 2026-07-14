# Requirements Document

## Introduction

Rediseño del módulo de **Promociones** de ToppisERP para que crear y vender promos sea
más interactivo y visual, respetando el diseño y el flujo del POS ya rediseñado.

La base ya existe: una promo se compone de **grupos de elección** (tabla
`promocion_espacios`), cada grupo con una **cantidad a elegir** y una **fuente** que
puede ser una **lista fija de productos** o una **categoría del menú** (ej. "Bebida
lata"), y la venta ya registra el **detalle real elegido** y su **costo real**
(food cost exacto por venta). Este rediseño agrega lo que falta:

1. Un grupo puede permitir o no **repetir el mismo producto** (`permite_repetir`).
2. La **creación/edición** de una promo se hace en una **pantalla dedicada** (no un
   popup), interactiva y con **imágenes de los productos**.
3. La **selección en el POS** se hace tocando **tarjetas con imagen**, con contador por
   grupo, soportando grupos de cantidad > 1 (elegir iguales o distintos).
4. Las bebidas (categorías "Bebida lata" / "Bebida mediana") pueden tener **imagen** y
   se usan en el selector con foto.

Todo en CLP, español chileno. Local único. Se mantiene el precio fijo de la promo y el
cálculo de food cost real por venta que ya funciona.

## Glossary

- **Grupo de elección** (tabla `promocion_espacios`): un "slot" configurable de la promo
  (ej. "Elige tu hamburguesa"), con cantidad a elegir y fuente.
- **Fuente lista fija** (`LISTA`): el cliente elige entre una lista específica de
  productos (`promocion_espacio_opciones`).
- **Fuente categoría** (`CATEGORIA`): el cliente elige entre todos los productos activos
  del menú de esa categoría (ej. "Bebida lata"); las bebidas nuevas quedan disponibles
  automáticamente sin editar la promo.
- **permite_repetir**: si un grupo con cantidad > 1 permite elegir el mismo producto más
  de una vez (ej. 2 Cheese iguales) o exige productos distintos.
- **Precio fijo**: el cliente paga siempre el precio de la promo, sin importar la elección.

## Requirements

### Requisito 1 — Repetición configurable por grupo

**Historia**: Como administrador, quiero definir si un grupo permite elegir el mismo
producto varias veces, para modelar bien los Duo (2 iguales o 2 distintos).

**Criterios de aceptación**:
1. Cada grupo DEBE tener una bandera `permite_repetir`.
2. CUANDO `permite_repetir = true`, el POS DEBE permitir elegir el mismo producto hasta
   completar la cantidad del grupo.
3. CUANDO `permite_repetir = false`, el POS DEBE exigir que las unidades del grupo sean
   productos distintos.
4. La validación de "grupo completo" DEBE basarse en la **suma de unidades elegidas**
   igual a la cantidad del grupo, sin exigir que sean iguales.

### Requisito 2 — Pantalla de creación/edición de promo (view, no popup)

**Historia**: Como administrador, quiero crear/editar promos en una pantalla completa e
interactiva, para hacerlo cómodo y visual.

**Criterios de aceptación**:
1. La creación/edición DEBE ser una **pantalla dedicada** (no un AlertDialog).
2. La pantalla DEBE permitir definir **nombre**, **precio fijo** e **imagen** de la promo.
3. La pantalla DEBE permitir agregar/editar/quitar **grupos**, cada uno con: nombre,
   cantidad a elegir, fuente (lista fija o categoría), y `permite_repetir`.
4. En grupos de **lista fija**, la selección de productos DEBE mostrarse con **tarjetas
   con imagen** del producto (tocar para incluir/excluir).
5. En grupos de **categoría**, DEBE elegirse la categoría (Bebida lata / Bebida mediana
   u otra) y mostrarse una vista previa de los productos que quedan incluidos.
6. La pantalla DEBE mostrar un resumen claro de la promo antes de guardar.
7. Guardar DEBE persistir la promo y sus grupos/opciones; editar DEBE cargar lo existente.

### Requisito 3 — Selección interactiva en el POS con imágenes

**Historia**: Como cajero, quiero armar la promo tocando fotos de los productos, para que
sea rápido y claro.

**Criterios de aceptación**:
1. CUANDO el cajero agrega una promo, el sistema DEBE mostrar, **por grupo**, las opciones
   como **tarjetas con imagen** (foto + nombre).
2. Cada grupo DEBE mostrar un **contador** "elegidas / cantidad" (ej. "Bebidas 1/2").
3. En grupos con cantidad > 1, tocar un producto DEBE **sumar una unidad**; se puede
   tocar el mismo (si `permite_repetir`) o distintos.
4. DEBE poder **quitar** una unidad elegida.
5. El botón **Agregar** DEBE habilitarse solo cuando **todos los grupos** están completos.
6. La promo DEBE agregarse al carrito a su **precio fijo**.
7. El diseño DEBE ser consistente con el POS (tarjetas, popup protegido, acentos).

### Requisito 4 — Imágenes en bebidas

**Historia**: Como administrador, quiero que las bebidas tengan foto, para que se vean en
el selector de promos y en el menú.

**Criterios de aceptación**:
1. Los productos de categoría "Bebida lata" / "Bebida mediana" DEBEN poder tener imagen
   (ya son ítems de menú con `imagen_url`).
2. El selector de promos DEBE mostrar la imagen de la bebida cuando exista, y un
   placeholder cuando no.

### Requisito 5 — Detalle real y food cost por venta (ya vigente, no romper)

**Historia**: Como dueño, quiero que la comanda muestre lo elegido y que el food cost sea
real por venta.

**Criterios de aceptación**:
1. La comanda DEBE listar los productos reales elegidos de la promo.
2. El costo de la venta DEBE sumar el costo real de los productos elegidos (no un
   promedio fijo).
3. El precio de venta DEBE mantenerse fijo (el de la promo).

### Requisito 6 — Compatibilidad

**Historia**: Como administrador, quiero que las promos existentes sigan funcionando.

**Criterios de aceptación**:
1. Las promos y grupos ya creados DEBEN seguir funcionando (default `permite_repetir`).
2. El flujo de pago/stock/cupón del POS NO DEBE romperse.
