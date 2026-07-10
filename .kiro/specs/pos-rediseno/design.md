# Design Document

## Overview

El nuevo POS agrega una **capa operativa de pedidos** encima de las ventas.

```
Cliente pide  →  PEDIDO (carrito, estado ABIERTO)     ← se edita en la nube, realtime
                    │  cerrar → comanda + estado CERRADO
                    │  pagar  → materializa una VENTA (atómica: sobre + stock + cupón)
                    │  entregar
                    ▼
              VENTA (tabla `ventas`, ya existente)     ← fuente de verdad para reportes/costos
```

- **`pedidos`** es la capa operativa (carritos vivos). Persisten en Supabase y se
  sincronizan por Realtime entre dispositivos (celular caja + tablet cocina).
- **`ventas`** sigue siendo la fuente de verdad para KPIs, reportes y Control de Costos.
  Una venta se crea **al pagar** (no antes), reutilizando la lógica atómica actual
  (ingreso a sobre + descuento de stock por receta + modificadores).
- Patrón MVVM habitual: repos con `SupabaseClient.client`, VMs con `MutableStateFlow`
  privados + `StateFlow` públicos, `cargandoInicial`, refresh-after-write, errores en
  `ToppisErrorDialog`. Realtime con canales de nombre único (UUID).
- Toda la lógica de dinero/reglas se concentra en una **capa pura** `domain/pos/`
  para poder cubrirla con tests de propiedad (como en Control de Costos).

## Architecture

La arquitectura sigue el patrón del proyecto: capa de datos (repos + RPC Supabase),
capa de dominio pura (`domain/pos/`), capa de presentación (Compose MVVM). La novedad
es la **capa operativa de pedidos** descrita arriba, que se materializa en `ventas` al
pagar. Realtime mantiene sincronizados los pedidos (caja) y las comandas (cocina).

## Data Models

### 2.1 Clientes y cuponera

```
clientes
  id                 SERIAL PK
  telefono3          TEXT NOT NULL          -- 3 últimos dígitos del WhatsApp
  nombre             TEXT                   -- opcional (se completa después)
  sellos_hamburguesa INT  NOT NULL DEFAULT 0-- avance de la cuponera
  local_id           INT  REFERENCES locales(id)
  created_at, updated_at
```
- No es único por `telefono3` (puede repetirse): al crear un pedido, si hay varias
  coincidencias se elige entre ellas o se crea uno nuevo (Req 10.4).
- **Cuponera**: `sellos_hamburguesa` avanza +1 por cada pedido pagado que incluya al
  menos una hamburguesa. Al llegar a 6, el pedido siguiente puede agregar una **Cheese
  gratis**; al aplicarla se descuentan 6 sellos. Cargar cupones existentes = fijar
  `sellos_hamburguesa` a mano en la pantalla de Clientes.

### 2.2 Pedidos (carritos)

```
pedidos
  id           SERIAL PK
  cliente_id   INT REFERENCES clientes(id)
  estado       TEXT NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO','CERRADO'))
  pagado       BOOLEAN NOT NULL DEFAULT FALSE
  entregado    BOOLEAN NOT NULL DEFAULT FALSE
  metodo_pago  TEXT                       -- al pagar (EFECTIVO|TARJETA|TRANSFERENCIA)
  sobre_id     INT  REFERENCES sobres(id) -- al pagar
  venta_id     INT  REFERENCES ventas(id) -- venta materializada al pagar
  zona_envio   TEXT NOT NULL DEFAULT 'SIN_ENVIO'
  monto_envio  NUMERIC NOT NULL DEFAULT 0
  total        NUMERIC NOT NULL DEFAULT 0
  comanda_texto TEXT                      -- se llena al cerrar; queda adherido
  local_id     INT REFERENCES locales(id)
  created_by   UUID
  created_at, updated_at, closed_at, paid_at, delivered_at
```
- `estado` fija si el carrito se edita (`ABIERTO`) o ya salió a cocina (`CERRADO`).
- `pagado` y `entregado` son **banderas independientes** (Req 7.3).
- **Deuda** = `entregado = TRUE AND pagado = FALSE` (marca roja).
- Activo en la lista del POS = no (`pagado` y `entregado`). Es decir, sigue visible
  mientras falte pagar o entregar.

### 2.3 Líneas del pedido y unidades a preparar

Separamos la **línea de cobro** (lo que se cobra) de la **unidad a preparar** (lo que
va a la comanda y descuenta stock). Un producto simple = 1 línea + 1 unidad. Una promo
= 1 línea (precio fijo) + N unidades (los productos elegidos en cada espacio).

```
pedido_items                      -- línea de cobro
  id            SERIAL PK
  pedido_id     INT REFERENCES pedidos(id) ON DELETE CASCADE
  tipo          TEXT CHECK (tipo IN ('PRODUCTO','PROMO'))
  item_menu_id  INT REFERENCES items_menu(id)   -- si PRODUCTO
  promocion_id  INT REFERENCES promociones(id)  -- si PROMO
  cantidad      INT NOT NULL DEFAULT 1
  precio_unitario NUMERIC NOT NULL DEFAULT 0     -- precio fijo (promo) o precio del producto
  subtotal      NUMERIC NOT NULL DEFAULT 0
  es_regalo     BOOLEAN NOT NULL DEFAULT FALSE   -- cupón: no se cobra

pedido_unidades                   -- producto físico a preparar (comanda + stock)
  id             SERIAL PK
  pedido_item_id INT REFERENCES pedido_items(id) ON DELETE CASCADE
  item_menu_id   INT REFERENCES items_menu(id)
  comentario     TEXT             -- "sin tomate" (no descuenta stock)

pedido_unidad_mods                -- modificadores (agregados) de una unidad
  id             SERIAL PK
  pedido_unidad_id INT REFERENCES pedido_unidades(id) ON DELETE CASCADE
  modificador_id INT REFERENCES modificadores(id)
```
- El **comentario** ("sin …") viaja a la comanda pero **no** toca stock (Req 3.3, 4.4).
- Los **modificadores** solo agregan; suman a precio y descuentan su receta al pagar.

### 2.4 Promociones configurables (espacios)

```
promocion_espacios                -- un "slot" a elegir dentro de la promo
  id            SERIAL PK
  promocion_id  INT REFERENCES promociones(id) ON DELETE CASCADE
  nombre        TEXT              -- ej "Hamburguesa", "Bebida"
  cantidad      INT NOT NULL DEFAULT 1
  modo          TEXT CHECK (modo IN ('LISTA','CATEGORIA'))
  categoria     TEXT              -- si modo = CATEGORIA (ej "Bebida lata")

promocion_espacio_opciones        -- opciones si modo = LISTA
  id            SERIAL PK
  espacio_id    INT REFERENCES promocion_espacios(id) ON DELETE CASCADE
  item_menu_id  INT REFERENCES items_menu(id)
```
- Elegibles de un espacio = las `opciones` (modo LISTA) o todos los `items_menu` de la
  `categoria` (modo CATEGORIA). Precio de la promo **fijo** (Req 5.2, 5.6).

### 2.5 Modificadores por categoría

`modificadores` gana una columna:
```
ALTER TABLE modificadores ADD COLUMN categoria TEXT;   -- categoría del menú a la que aplica
```
- **Resolución**: los modificadores aplicables a un producto = los que tienen
  `categoria = item.categoria` **o** `item_menu_id = item.id` (puntual). Se mantiene la
  compatibilidad con los modificadores por item ya existentes.

### 2.6 Categorías fijas de bebida

- **Menú** (`items_menu.categoria`, texto): set fijo pasa a ser
  `Hamburguesas · Papas fritas · Bebida lata · Bebida mediana · Salsas · Otro`.
- **Inventario** (`CategoriaArticulo`): se agregan `BEBIDA_LATA` y `BEBIDA_MEDIANA`
  para separar las bebidas de ingredientes/packaging/insumos (Req 6.2). Constraint
  `chk_articulo_categoria` se amplía a esos valores.

### 2.7 Enums (Kotlin, `data/db/entities/Enums.kt`)

```kotlin
enum class EstadoPedido { ABIERTO, CERRADO }
enum class TipoLineaPedido { PRODUCTO, PROMO }
enum class ModoEspacioPromo { LISTA, CATEGORIA }
// ZonaEnvio: 1000, 1500, 2000, 2500, 3000 (se quita 500 y SIN_ENVIO se mantiene)
// CategoriaArticulo: + BEBIDA_LATA, BEBIDA_MEDIANA
// CategoriaMenu (nuevo, para el set fijo): HAMBURGUESAS, PAPAS, BEBIDA_LATA, BEBIDA_MEDIANA, SALSAS, OTRO
```

## 3. Capa de dominio pura (`domain/pos/PosCalculos.kt`)

Funciones sin dependencias de red/Android (base de los tests de propiedad):
- `precioLinea(precioUnit, cantidad)` y `totalPedido(lineas, envio)`.
- `precioProducto(base, modificadores)` — base + suma de `delta_precio`.
- `precioPromo(promo)` — siempre el precio fijo, sin importar lo elegido.
- `modificadoresAplicables(producto, todos)` — por categoría + puntual.
- `elegiblesEspacio(espacio, itemsMenu)` — por lista o categoría.
- `promoCompleta(espacios, elecciones)` — true si cada espacio tiene su cantidad elegida.
- `tieneDeuda(entregado, pagado)` — `entregado && !pagado`.
- `activoEnLista(pagado, entregado)` — `!(pagado && entregado)`.
- `sellosTrasPedido(sellos, incluyeHamburguesa)` y `puedeRegalar(sellos, umbral=6)`.
- `sellosTrasRegalo(sellos, umbral=6)` — resta el umbral.
- `esRegaloValido(...)`, etc.

## 4. Repositorios

- `PedidoRepository` — CRUD de pedidos/ítems/unidades/mods; `observeCambios()` Realtime;
  `crearPedido`, `agregarProducto`, `agregarPromo`, `editarLinea`, `quitarLinea`,
  `cerrarPedido`, `pagarPedido` (RPC), `marcarEntregado`.
- `ClienteRepository` — get-or-create por 3 dígitos, editar nombre, historial, deuda,
  sellos (leer/fijar).
- Se **extienden** `PromocionRepository` (espacios/opciones) y `ModificadorRepository`
  (categoría). `MenuRepository` para elegibles por categoría.

## 5. Funciones RPC (PostgreSQL, atómicas)

1. **`crear_pedido(p_telefono3, p_nombre, p_usuario, p_local_id)`** → busca/crea cliente,
   crea pedido `ABIERTO`, retorna `pedido_id` (+ `cliente_id`).
2. **`cerrar_pedido(p_pedido_id, p_comanda_texto)`** → guarda `comanda_texto`, estado
   `CERRADO`, `closed_at`. No toca stock ni dinero.
3. **`pagar_pedido(p_pedido_id, p_metodo, p_sobre_id, p_usuario)`** → **atómico**:
   - Crea la `venta` + `items_venta_menu` desde `pedido_unidades` (con costos/mods).
   - Descuenta stock por receta + modificadores (misma lógica que `registrar_venta_menu`).
   - Suma el total al `sobre` e inserta `movimientos_sobre`.
   - Marca `pagado`, `paid_at`, `venta_id`, `metodo_pago`, `sobre_id`.
   - Cuponera: si el pedido trae hamburguesa, `sellos += 1`; si hubo regalo aplicado,
     `sellos -= 6`. Los ítems `es_regalo` entran con precio 0 (no suman al cobro) pero
     **sí** descuentan stock y se marcan como REGALO (no merma).
   - Respeta el guard *safeupdate* (todos los UPDATE con WHERE).
4. **`marcar_entregado(p_pedido_id)`** → `entregado = TRUE`, `delivered_at`. Avisa en la
   app si `pagado = FALSE` (no bloquea).

> Nota: `pagar_pedido` lee las líneas ya persistidas (no recibe JSON), lo que evita
> divergencias entre lo que se ve y lo que se cobra.

## Components and Interfaces

1. **PosPedidosScreen** (reemplaza la entrada actual del POS): lista de pedidos activos
   (tarjetas con cliente, estado, total, chips de PAGADO/ENTREGADO, **marca roja de
   deuda**). FAB "Nuevo pedido" → diálogo 3 dígitos + nombre opcional.
2. **PedidoCarritoScreen**: dividida — arriba catálogo con **pestañas Menú / Promociones**
   (tarjetas con imagen + nombre, filtro por categoría); abajo el carrito (líneas,
   cantidades, total, envío). Botones **Cerrar** / **Pagar** / **Entregar** según estado.
   Si `CERRADO`, al abrir muestra la **comanda**.
3. **ProductoModsDialog** (popup **protegido**: `properties = DialogProperties(
   dismissOnClickOutside = false)`): modificadores de la categoría + comentario libre +
   precio resultante; botones **Agregar** y **X/Cancelar**.
4. **PromoConfigDialog**: un selector por espacio (solo elegibles); permite abrir el
   popup de mods/comentario para cada elegido; **Agregar** cuando la promo está completa.
5. **PagarDialog**: método (Efectivo/Tarjeta/Transferencia) → sobre correspondiente.
6. **ComandasScreen** (cocina/KDS): lista realtime de pedidos `CERRADO` no entregados,
   con productos/mods/comentarios; botón "Entregar".
7. **ClientesScreen**: lista con búsqueda por 3 dígitos/nombre; muestra **deuda** y
   **sellos**; editar nombre; **cargar cupón** (fijar sellos).
8. **Config** (extensiones): Promociones (armar espacios), Modificadores (elegir
   categoría), Inventario/Menú (categorías de bebida fijas).

Diseño acorde al sistema actual (Material 3, tarjetas redondeadas, imágenes, acentos,
EmptyState/Skeleton, errores en `ToppisErrorDialog`).

## 7. Navegación / DI

- Rutas nuevas: `pos` (pasa a ser la **lista de pedidos**), `pedido/{id}` (carrito),
  `comandas` (cocina), `clientes`. Se cablean en `NavGraph` + `MainActivity` + `HomeMenu`.
- `comandas` y `clientes` visibles según rol (cocina puede ver comandas).
- Atajo POS del Home ya existe (abre la lista de pedidos).

## Correctness Properties

### Property 1: Total del pedido
El total = Σ subtotales de líneas + envío.
**Validates: Requirements 2.6**

### Property 2: Precio de promo fijo
El precio de una promo es siempre su precio fijo (independiente de lo elegido).
**Validates: Requirements 5.2, 5.6**

### Property 3: Precio de producto con modificadores
El precio de un producto = base + Σ delta de sus modificadores (≥ base).
**Validates: Requirements 3.4**

### Property 4: Modificadores aplicables
Modificadores aplicables ⊆ (misma categoría ∪ puntuales del item).
**Validates: Requirements 4.1, 4.2**

### Property 5: Elegibles de un espacio
Elegibles de un espacio ⊆ (opciones de lista) ó (items de la categoría).
**Validates: Requirements 5.3, 5.4**

### Property 6: Promo completa
Una promo solo se agrega si todos los espacios están completos.
**Validates: Requirements 5.3**

### Property 7: Deuda
`tieneDeuda` ⇔ entregado ∧ ¬pagado.
**Validates: Requirements 1.6, 7.5**

### Property 8: Pedido activo en lista
Un pedido sale de la lista activa ⇔ pagado ∧ entregado.
**Validates: Requirements 1.5**

### Property 9: Pago idempotente
`pagar` es idempotente respecto a `venta_id` (no crea dos ventas para un pedido).
**Validates: Requirements 8.4**

### Property 10: Sello por hamburguesa
Tras pagar con hamburguesa, sellos aumenta exactamente 1.
**Validates: Requirements 11.1**

### Property 11: Puede regalar
`puedeRegalar` ⇔ sellos ≥ 6.
**Validates: Requirements 11.2**

### Property 12: Aplicar regalo
Aplicar regalo baja sellos en 6 y el ítem regalado queda a precio 0.
**Validates: Requirements 11.2, 11.3**

### Property 13: Regalo y stock
Un ítem regalo descuenta stock pero no suma al cobro ni cuenta como merma.
**Validates: Requirements 11.3**

### Property 14: Transiciones de estado
Estados válidos: ABIERTO→CERRADO; pagado/entregado en cualquier orden.
**Validates: Requirements 7.1, 7.3**

### Property 15: Cerrar vs pagar
Cerrar no modifica stock ni saldos; pagar sí (atómico).
**Validates: Requirements 7.2, 7.4**

### Property 16: Comentario no afecta stock
El comentario "sin …" no altera el descuento de stock.
**Validates: Requirements 3.3, 4.4**

## Error Handling

- Errores de operación (saldo insuficiente, stock, pago fallido) se muestran en
  **`ToppisErrorDialog`** (popup), no solo snackbar.
- Las RPC lanzan `RAISE EXCEPTION` con mensaje legible; los repos lo extraen con el
  regex `"message"\s*:\s*"([^"]+)"` (patrón ya usado).
- `pagar_pedido` es **atómica**: si algo falla (stock/sobre), no se crea la venta ni se
  marca pagado. Idempotente por `venta_id` (no duplica venta si se reintenta).
- Al marcar entregado sin pagar: **aviso** (no bloquea) y queda registrada la deuda.
- Realtime que falla no rompe la app (se sigue refrescando tras cada operación).

## Testing Strategy

- **Tests de propiedad** (kotest-property, 100 iter) sobre `PosCalculos.kt` para las
  16 propiedades listadas (totales, precio fijo de promo, deuda, sellos/cupón, etc.).
- Verificación manual del flujo completo en dispositivo (crear → armar → cerrar →
  pagar → entregar + cocina + cupón), como cierre de la fase J.
- Build `./gradlew :app:assembleDebug` verde al cerrar cada fase.

## Plan por fases (cada fase: build verde → commit + push)

- **Fase A — Datos**: SQL (tablas `clientes`, `pedidos`, `pedido_items`,
  `pedido_unidades`, `pedido_unidad_mods`, `promocion_espacios`,
  `promocion_espacio_opciones`, columna `modificadores.categoria`, categorías de bebida,
  zonas de envío) + enums + modelos + repos base. Sin UI.
- **Fase B — Dominio + tests**: `PosCalculos.kt` + tests de propiedad (kotest-property).
- **Fase C — Config**: categorías fijas (incl. bebidas) en menú e inventario;
  modificadores por categoría; promos con espacios (armado en Promociones).
- **Fase D — Lista de pedidos + clientes**: `PosPedidosScreen`, crear pedido (3 dígitos),
  `ClienteRepository` get-or-create, Realtime.
- **Fase E — Carrito**: `PedidoCarritoScreen` (split + pestañas), `ProductoModsDialog`
  (popup protegido), agregar/editar/quitar líneas.
- **Fase F — Promos en POS**: `PromoConfigDialog` (elegir por espacio + mods).
- **Fase G — Estados/pago**: `cerrar_pedido`, `pagar_pedido` (RPC atómica), `marcar_entregado`,
  `PagarDialog`, avisos de deuda.
- **Fase H — Cocina (KDS)**: `ComandasScreen` realtime + entregar.
- **Fase I — Cuponera + Clientes**: sellos, regalo (REGALO no merma), cargar cupones,
  deuda/historial en `ClientesScreen`.
- **Fase J — Envío + diseño final + verificación**: zonas 1000–3000, pulido visual,
  build verde, tests, actualizar README y PROYECTO-CONTEXTO.

## 10. Compatibilidad y migración

- `ventas` / `items_venta_menu` **no cambian de forma**: el nuevo POS las alimenta al
  pagar, así KPIs, reportes y Control de Costos siguen funcionando igual.
- El POS actual se reemplaza por el flujo de pedidos; las ventas históricas quedan intactas.
- Realtime: nuevos canales UUID para `pedidos` y `comandas` (no rompe los existentes).
- Todos los UPDATE de las RPC nuevas llevan WHERE (guard *safeupdate*).
```
