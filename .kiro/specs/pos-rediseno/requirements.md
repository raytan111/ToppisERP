# Requirements Document

## Introduction

Rediseño completo del Punto de Venta (POS) de ToppisERP. El POS actual asume una
sola venta a la vez y un flujo lineal "arma carrito → cobra". La operación real de
la dark kitchen necesita **atender a varios clientes en paralelo** (varios carritos
abiertos), separar el momento de **cerrar/mandar a cocina**, **pagar** y **entregar**
(que no siempre ocurren en ese orden), promociones **configurables** (el cliente
elige qué producto va en cada espacio del combo), registro de **clientes** con
**cuponera**, y una **pantalla de cocina** para ver comandas desde otro dispositivo.

Todo en CLP, español chileno, montos con IVA incluido. Local único (id 1). Diseño
acorde al sistema visual actual (Material 3, tarjetas redondeadas, imágenes de
menú/promos, acentos de color).

## Glossary

- **Pedido / carrito**: una orden de un cliente, persistida en la nube, con estado.
- **Estados del pedido**: `ABIERTO` → `CERRADO` → (`PAGADO` y `ENTREGADO`, en cualquier orden).
- **Cerrar**: emitir la comanda de cocina. Tras cerrar, el pedido queda fijado y se
  puede consultar su comanda; ya no es la pantalla de armado.
- **Comanda**: el detalle para cocina, queda adherido al pedido y al cliente y se guarda.
- **Modificador**: agregado que se aplica a los productos de una **categoría** del
  menú (ej: "Doble carne" para Hamburguesas). Solo se usan para **agregar** (suman
  stock/costo/precio). Los "sin esto/sin aquello" son **comentario** a la comanda.
- **Promo configurable**: promoción de precio fijo con **espacios** que el cajero
  completa eligiendo entre una **lista específica** de productos o una **categoría**.
- **Cliente**: identificado por los **3 últimos dígitos del WhatsApp** + nombre
  (opcional al vender, editable después).
- **Cupón/cuponera**: al acumular 6 pedidos con hamburguesa, el cliente gana una
  Cheese gratis en el siguiente pedido. El regalo se registra como **REGALO** (no merma).

## Decisiones acordadas (contexto)

1. Carritos **persistidos en la nube** (Realtime), no solo en memoria del teléfono.
2. Nombre del cliente **opcional** al vender (basta 3 dígitos); se completa luego en Clientes.
3. `CERRADO` = se emite la comanda; queda adherida al pedido/cliente y consultable.
4. Stock e ingreso al sobre se registran al marcar **PAGADO** (no al abrir).
5. Al pagar se elige método (Efectivo/Tarjeta/Transferencia) → va al sobre correspondiente.
6. Aviso visual (rojo) de pedidos **no pagados**; en Clientes se ve si tiene deuda.
7. Modificadores por **categoría**; el "sin …" es comentario que **no** descuenta stock.
8. Espacios de promo elegibles por **lista específica** o por **categoría** (ambos).
9. Los productos elegidos dentro de una promo **pueden llevar modificadores**.
10. Promo de **precio fijo**: no se pueden elegir productos fuera de la promo.
11. Bebidas: cada sabor es un producto del menú con categoría **Bebida lata / Bebida
    mediana**; además el inventario separa las bebidas de ingredientes/insumos.
12. Cuponera: 6 pedidos con hamburguesa → 1 Cheese gratis; poder **cargar cupones
    existentes**; el regalo se marca como **REGALO**.
13. **Pantalla de cocina** (comandas) para usar en otro dispositivo.
14. Envío por zonas fijas: **1000, 1500, 2000, 2500, 3000**.

---

## Requirements

### Requisito 1 — Lista de pedidos (pantalla principal del POS)

**Historia**: Como cajero, quiero ver todos los pedidos activos y crear nuevos, para
atender a varios clientes al mismo tiempo sin perder ninguno.

**Criterios de aceptación**:
1. CUANDO abro el POS, el sistema DEBE mostrar la lista de pedidos activos (no
   entregados/cerrados del todo) con: identificación del cliente (3 dígitos + nombre
   si tiene), estado, total y hora.
2. CUANDO creo un pedido nuevo, el sistema DEBE pedir los **3 últimos dígitos del
   WhatsApp** (nombre opcional) y crear el pedido en estado `ABIERTO`.
3. CUANDO se crea el pedido, el sistema DEBE abrir la pantalla del carrito de ese pedido.
4. El sistema DEBE permitir **múltiples pedidos abiertos simultáneamente**.
5. CUANDO un pedido está `ENTREGADO` y `PAGADO`, el sistema DEBE sacarlo de la lista
   activa (queda en historial).
6. SI un pedido está `ENTREGADO` pero **no** `PAGADO`, el sistema DEBE mostrarlo con
   una **marca roja de deuda** y mantenerlo visible.
7. Los pedidos DEBEN persistir en la nube y reflejarse en **tiempo real** entre dispositivos.

### Requisito 2 — Pantalla del carrito (armado del pedido)

**Historia**: Como cajero, quiero una pantalla dividida (menú arriba, carrito abajo)
para armar el pedido rápido y ver lo que va entrando.

**Criterios de aceptación**:
1. La pantalla DEBE dividirse en **dos zonas**: arriba el **catálogo** (menú/promos),
   abajo el **detalle del carrito** con líneas, cantidades y total.
2. El catálogo DEBE tener un **navegador superior** con dos pestañas: **Menú** y **Promociones**.
3. En **Menú**, cada producto DEBE mostrarse como **tarjeta con su imagen y nombre** juntos.
4. El menú DEBE poder filtrarse/agruparse por categoría (Hamburguesas, Papas fritas,
   Bebida lata, Bebida mediana, Salsas, Otro).
5. CUANDO toco una línea del carrito, el sistema DEBE permitir cambiar cantidad o quitarla.
6. El carrito DEBE mostrar el **total** actualizado (productos + promos + envío).
7. Todos los cambios del carrito DEBEN guardarse en el pedido en la nube.

### Requisito 3 — Popup protegido de producto (modificadores + comentario)

**Historia**: Como cajero, quiero un popup al elegir un producto para configurar sus
modificadores y comentarios sin cerrarlo por accidente.

**Criterios de aceptación**:
1. CUANDO toco un producto del menú, el sistema DEBE abrir un **popup protegido** que
   **no** se cierra al tocar afuera; solo se cierra con **Agregar** o con la **X/Cancelar**.
2. El popup DEBE mostrar los **modificadores de la categoría** del producto (ej: la
   Cheese muestra los de Hamburguesas, no los de Papas).
3. El popup DEBE incluir un campo de **comentario libre** ("sin tomate", "sin cebolla")
   que va a la comanda y **no** descuenta stock.
4. El popup DEBE mostrar el precio resultante (base + modificadores) antes de agregar.
5. CUANDO toco **Agregar**, el sistema DEBE sumar la línea al carrito con sus
   modificadores y comentario.

### Requisito 4 — Modificadores por categoría

**Historia**: Como administrador, quiero que un modificador aplique a una categoría
completa, para no configurarlo producto por producto.

**Criterios de aceptación**:
1. El sistema DEBE permitir asociar un modificador a una **categoría del menú**.
2. Un modificador de una categoría DEBE aparecer en **todos los productos** de esa categoría.
3. El sistema DEBE seguir permitiendo modificadores para un **producto puntual** (opcional).
4. Los modificadores DEBEN usarse solo para **agregar** (suman receta/costo/precio);
   quitar ingredientes se maneja como **comentario** en la comanda.

### Requisito 5 — Promociones configurables

**Historia**: Como administrador, quiero armar promos con espacios elegibles, para
vender combos donde el cliente elige (ej: "Dúo" = 2 hamburguesas + 2 bebidas a elección).

**Criterios de aceptación**:
1. El sistema DEBE permitir definir una promo con uno o más **espacios**, cada uno con
   una **cantidad** y un **conjunto elegible** definido por **lista específica de
   productos** o por **categoría**.
2. La promo DEBE tener un **precio fijo**.
3. CUANDO el cajero agrega una promo en el POS, el sistema DEBE pedir que **elija un
   producto por cada espacio**, solo dentro del conjunto elegible.
4. El sistema NO DEBE permitir elegir productos **fuera** de la promo.
5. Los productos elegidos dentro de un espacio DEBEN poder llevar **modificadores** y comentario.
6. El precio de la promo DEBE mantenerse fijo (no se suma el precio individual de los elegidos).

### Requisito 6 — Bebidas por categoría (menú e inventario)

**Historia**: Como administrador, quiero manejar las bebidas por tipo, para poder
elegir el sabor en el POS y separarlas en el inventario.

**Criterios de aceptación**:
1. El menú DEBE incluir las categorías **Bebida lata** y **Bebida mediana**; cada sabor
   es un producto del menú de esa categoría.
2. El inventario DEBE separar las bebidas de ingredientes/packaging/insumos con su
   propia categoría de artículo.
3. Una promo DEBE poder usar una categoría de bebida como conjunto elegible de un espacio.

### Requisito 7 — Estados del pedido y transiciones

**Historia**: Como cajero, quiero controlar el ciclo del pedido, porque pagar y
entregar no siempre pasan en el mismo orden.

**Criterios de aceptación**:
1. Un pedido DEBE tener estado `ABIERTO`, `CERRADO`, y banderas independientes de
   `PAGADO` y `ENTREGADO`.
2. CUANDO **cierro** un pedido, el sistema DEBE generar la **comanda** y fijar el pedido
   (deja de editarse el carrito).
3. `PAGADO` y `ENTREGADO` DEBEN poder marcarse en **cualquier orden**.
4. CUANDO marco **PAGADO**, el sistema DEBE registrar la venta (ingreso al sobre según
   método) y **descontar stock** de forma atómica.
5. SI intento marcar **ENTREGADO** un pedido no pagado, el sistema DEBE **avisar** (no
   bloquear) y dejar constancia de la deuda.
6. CUANDO un pedido está `CERRADO`, al tocarlo el sistema DEBE **mostrar su comanda**.

### Requisito 8 — Pago del pedido

**Historia**: Como cajero, quiero cobrar un pedido eligiendo el método, para que la
plata entre al sobre correcto.

**Criterios de aceptación**:
1. CUANDO pago un pedido, el sistema DEBE pedir el **método** (Efectivo/Tarjeta/Transferencia).
2. El sistema DEBE registrar el ingreso en el **sobre** correspondiente al método.
3. El sistema DEBE registrar la venta con sus líneas, costos y promociones para reportes/costos.
4. El pago DEBE ser **atómico** (venta + movimiento de sobre + descuento de stock).

### Requisito 9 — Pantalla de cocina (comandas)

**Historia**: Como cocina, quiero una pantalla con las comandas pendientes, para
prepararlas desde otro dispositivo.

**Criterios de aceptación**:
1. El sistema DEBE ofrecer una **pantalla de comandas** que lista las comandas de
   pedidos `CERRADO` no entregados, en tiempo real.
2. Cada comanda DEBE mostrar cliente, productos, modificadores y comentarios.
3. CUANDO cocina marca una comanda como lista/entregada, el sistema DEBE actualizar el
   estado del pedido en tiempo real.
4. La comanda DEBE quedar **guardada y adherida** al pedido y al cliente (para consulta futura).

### Requisito 10 — Clientes

**Historia**: Como administrador, quiero un registro de clientes por sus 3 dígitos,
para seguir su historial y su cuponera.

**Criterios de aceptación**:
1. CUANDO se crea un pedido con 3 dígitos que no existen, el sistema DEBE crear el cliente.
2. El sistema DEBE permitir **editar** el nombre del cliente después.
3. El sistema DEBE mostrar una **lista de clientes** con su historial de pedidos y si
   tiene **deuda** (pedidos entregados sin pagar).
4. SI dos clientes comparten los 3 dígitos, el sistema DEBE diferenciarlos por nombre
   (o permitir elegir entre coincidencias al crear el pedido).

### Requisito 11 — Cuponera

**Historia**: Como administrador, quiero premiar la fidelidad, para regalar una Cheese
cada 6 pedidos con hamburguesa.

**Criterios de aceptación**:
1. El sistema DEBE contar los **pedidos con hamburguesa** por cliente.
2. CUANDO un cliente llega a **6** pedidos con hamburguesa, el sistema DEBE habilitar
   una **Cheese gratis** en el pedido siguiente.
3. CUANDO se aplica el cupón, el producto regalado DEBE registrarse como **REGALO** (no
   como merma) y no cobrarse.
4. El sistema DEBE permitir **cargar cupones existentes** (clientes que ya acumularon sellos).
5. El sistema DEBE mostrar cuántos sellos lleva cada cliente.

### Requisito 12 — Envío por zonas

**Historia**: Como cajero, quiero cobrar el envío por zona, para sumar el delivery al total.

**Criterios de aceptación**:
1. El pedido DEBE poder incluir un **envío** con monto por zona: 1000, 1500, 2000, 2500, 3000.
2. El monto de envío DEBE sumarse al total del pedido.
3. El envío DEBE quedar registrado en la venta.

### Requisito 13 — Diseño y consistencia visual

**Historia**: Como usuario, quiero un POS lindo y consistente con el resto de la app.

**Criterios de aceptación**:
1. El POS DEBE usar el sistema de diseño actual (Material 3, tarjetas redondeadas,
   acentos de color, tipografía de impacto).
2. El catálogo DEBE aprovechar las **imágenes** de productos y promos.
3. Estados vacíos y de carga DEBEN usar los componentes compartidos (EmptyState/Skeleton).
4. Errores de operación DEBEN mostrarse en **popup** (ToppisErrorDialog), no solo snackbar.

### Requisito 14 — Permisos y compatibilidad

**Historia**: Como administrador, quiero que el nuevo POS respete roles y no rompa lo existente.

**Criterios de aceptación**:
1. El POS DEBE respetar los permisos por rol actuales (cajero puede vender, etc.).
2. Los datos históricos de ventas DEBEN seguir siendo válidos (no se rompe `ventas`).
3. El control de costos y reportes DEBEN seguir recibiendo las ventas del nuevo POS.
```
