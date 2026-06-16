# Roadmap — ToppisERP nivel Franquicia

## Estado: Plan v3 — decisiones confirmadas, listo para spec de Fase 4
## Objetivo: Llevar el negocio a control total (ingredientes, inventario, dinero
## real) y crecimiento multi-local / franquicia, usando métodos probados de la
## industria gastronómica.

> Investigación basada en benchmarks y prácticas de la industria de restaurantes
> (fuentes: completecontroller, sage, supy, crunchtime, restaurantowner, etc.).
> Contenido reformulado para cumplimiento de licencias.
> Nota: apoyo de software, no asesoría contable/legal formal.

---

## 1. Métodos Probados de la Industria (qué hacen los exitosos)

### 1.1 Prime Cost — el número más importante del P&L
- **Prime Cost = Costo de comida + Costo de mano de obra**
- Representa >50% de los costos y casi todo lo que el dueño puede controlar a corto plazo
- **Meta saludable: ≤ 60-65% de las ventas**. Las cadenas lo calculan **semanal y por local**

### 1.2 Food Cost % (costo de comida sobre ventas)
- Benchmark: **QSR 28-30%, servicio completo 34-36%**
- Top performers bajan mermas para llegar a ~28%

### 1.3 Food Cost Teórico vs Real (variance) — el mejor diagnóstico de cocina
- **Teórico**: lo que las recetas dicen que se debió consumir según lo vendido
- **Real**: inventario inicial + compras − inventario final (lo que de verdad se consumió)
- **Variance = Real − Teórico**. Revela mermas, robos, sobre-porcionado, cambios de precio
- Una variación de 8% semanal en un grupo puede costar cientos de miles al año

### 1.4 Inventario profesional
- **Par levels** (stock mínimo para operar el ciclo de pedido + colchón de seguridad)
- **Conteos periódicos** (stock takes), FIFO, registro de **mermas con motivo**
- Multi-local: **compras centralizadas, transferencias entre locales, par por local, reportes consolidados**

### 1.5 Dinero a nivel real
- Separar **cuentas reales** (dónde está la plata) de **fondos/provisiones** (para qué)
- **Arqueo de caja** por turno (apertura/cierre, diferencias)
- Conciliación con banco

### 1.6 Menu Engineering (rentabilidad por plato)
- Clasificar platos por popularidad + margen: estrellas, vacas, puzzles, perros
- Decisiones de precio/promoción/eliminación basadas en datos

---

## 2. Diagnóstico de ToppisERP Hoy

### Ya tenemos ✅
- POS, recetas (ingredientes/insumos/salsas), inventario con stock y costo/merma
- Sobres (caja), gastos con IVA, comprobantes internos, reportes, dashboard
- Flujo de caja, contabilidad básica (IVA débito/crédito, libros, cierre)
- Multiusuario (Admin/Cajero), Realtime, RLS, operaciones atómicas

### Nos falta para nivel franquicia ❌
- Costo teórico por plato y **food cost %**
- **Food cost teórico vs real** (variance) y **conteos de inventario**
- **Mermas** formales con motivo
- **Par levels** + punto de reorden + alertas de compra
- **Compras/proveedores** y costo promedio ponderado
- Dinero "real": **cuentas vs fondos**, **arqueo de caja**
- **Mano de obra / Prime Cost**
- **Multi-local / franquicia** (todo con location_id)
- KPIs ejecutivos con benchmarks

---

## 3. Roadmap Propuesto (fases incrementales)

### Fase 4 — Costos y Rentabilidad (Food Cost) 🍔
**Por qué primero**: ya tienes recetas con costos; es "activar" el valor sin datos nuevos.
- Sistema de **unidades** (MASA/VOLUMEN/UNIDAD) + **artículos** unificados
- **Sub-recetas/preparaciones** (bechamel, cheddar) que generan stock
- **Modificadores** (doble, quitar, reemplazar, extra) y **promociones** (creador manual)
- Costo teórico por plato y food cost % por item y por venta
- Reporte de **menu engineering** (estrellas/perros)
- KPI food cost % en dashboard
- **Impacto**: sabes qué platos te dan plata y cuáles te la quitan

### Fase 5 — Inventario Profesional 📦
- **Conteos de inventario** (stock take) con fecha
- **Food cost teórico vs real** (variance) por período
- **Mermas** con motivo (vencido, error, regalo, robo) + rendimiento por etapa (papa)
- **Par levels** + punto de reorden + **lista de compra sugerida**
- **Impacto**: detectas fugas de dinero (merma/robo/porción) y nunca te quedas sin stock

### Fase 6 — Compras y Proveedores 🚚
- Proveedores + órdenes de compra + recepción de mercadería
- Entrada de stock actualiza **costo promedio ponderado** (food cost real preciso)
- **Caducidad por lote** (cada compra con su fecha de vencimiento)
- **Impacto**: compras ordenadas y costos reales actualizados

### Fase 7 — Dinero a Nivel Real 💰
- Clasificar sobres: **CUENTA** (real) vs **FONDO** (provisión)
- Método de pago → cuenta; **arqueo de caja** por turno (apertura/cierre, diferencia)
- Conciliación
- **Impacto**: el dinero del sistema cuadra con la realidad, control anti-robo

### Fase 8 — Personas y Prime Cost 👥
- Empleados, turnos, costo de mano de obra (sueldo fijo / por turno / por hora)
- **Propinas**: total diario
- **Prime Cost** (food + labor) y KPIs (prime %, labor %, food %) con benchmarks
- **Impacto**: ves el número más importante del negocio, por local y semana

### Fase 9 — Multi-Local / Franquicia 🏪
- `location_id` en todas las tablas (multi-tenant)
- Recetas estándar compartidas; precios por local
- **Transferencias entre locales** (opcional, no hay cocina central)
- Reportes **consolidados + por local**; roles por local (franquiciado/encargado)
- (Opcional franquicia) royalties/fees por local
- **Impacto**: escalas a N locales con control central

### Fase 10 — Inteligencia y KPIs Ejecutivos 📈
- Dashboard ejecutivo: food cost %, prime cost %, ticket promedio, ventas por canal/hora/día
- Alertas (variance alto, stock bajo, margen bajo), proyecciones
- **Promociones data-driven**: recomendar promos según ventas/márgenes/demanda
- Base para IA (predicción de demanda, compras óptimas)

---

## 4. Orden Recomendado y Razonamiento

1. **Fase 4** (food cost) — valor inmediato, sin datos nuevos
2. **Fase 5** (inventario pro) — frena fugas de dinero (el mayor ahorro)
3. **Fase 7** (dinero real) — control de caja antes de crecer
4. **Fase 6** (compras) — costos reales precisos
5. **Fase 8** (prime cost) — visión financiera completa
6. **Fase 9** (multi-local) — cuando vayas a abrir el 2º local/franquicia
7. **Fase 10** (KPIs/IA) — capa ejecutiva sobre todo lo anterior

> Multi-local (Fase 9) idealmente **antes** de abrir el segundo local. El resto
> se puede hacer en un solo local y "viaja" automáticamente al multiplicarse.

---

## 5. Decisiones Confirmadas (D1-D4)
- **D1 = Fase 4 (food cost) primero** ✅
- **D2 = cada local tiene su propia cocina** (NO cocina central / commissary). Las transferencias entre locales quedan como opción futura.
- **D3 = locales propios primero, luego franquicia a inversionistas**. El sistema soporta ambos (roles por local + royalties opcionales en Fase 9).
- **D4 = mano de obra DENTRO de la app** (Fase 8); el contador debe poder ver toda la info.

---

## 6. Modelo de Producto: Artículos, Recetas, Modificadores y Promociones

### 6.1 Sistema de Unidades (base de todo)
Tres dimensiones, cada una con **unidad base** para guardar stock y costo:
- **MASA** → base **gramo (g)**
- **VOLUMEN** → base **mililitro (ml)**
- **UNIDAD** → base **unidad (un)**

Reglas:
- El **stock** y el **costo por unidad base** se guardan siempre en la unidad base.
- Las **unidades de compra** tienen un **factor de conversión** a la base (ej: saco 25 kg → 25.000 g; pack bebida 6 un → 6 un; bidón aceite 5 L → 5.000 ml).
- Todo cálculo de receta usa unidad base → costo exacto por plato.

### 6.2 Artículos (insumos/ingredientes unificados)
Un solo concepto "Artículo" con: nombre, dimensión, unidad base, unidad de compra + factor, costo por unidad base (promedio ponderado en Fase 6), stock actual, par level, perecible (sí/no), vida útil (días).

### 6.3 Recetas y Sub-recetas (preparaciones) — **Q1 = SÍ**
- **Receta de venta**: ItemMenu (hamburguesa, papas, completo) compuesto por artículos y/o sub-recetas, todo en gramos/ml/un.
- **Sub-receta / preparación**: se produce **por lote** y genera stock propio (ej: **salsa bechamel, salsa cheddar**, mezcla de aliño). Tiene su receta de ingredientes, rendimiento (cuánto produce el lote) y **costo por gramo** derivado.
- Una receta de venta puede consumir una preparación como si fuera un artículo (ej: hamburguesa lleva 40 g de cheddar).

### 6.4 Modificadores y Variantes — **Q2 = SÍ**
Tipos de modificación sobre un ItemMenu en el POS:
- **Doble** (duplicar componente principal: 2 medallones) → suma costo e impacta inventario.
- **Quitar ingrediente** (sin cebolla) → resta costo/consumo.
- **Reemplazar ingrediente** (cambiar lechuga por rúcula) → ajusta costo/consumo según el sustituto.
- **Extra / agregado** (extra queso, extra palta) → suma costo + precio extra.

Modelo: cada modificador define **delta de receta** (artículos que agrega/quita) y **delta de precio**. Así el costo y el food cost se recalculan exactos en cada venta modificada.

### 6.5 Promociones — **Q3 = SÍ (creador manual)**
- **Creador de promociones manual** con su propia pantalla.
- Flujo: **nombre de la promo** + seleccionar **lo que incluye del menú** (items + cantidades) + **precio de la promo** → el sistema **calcula todo**: costo total (suma de costos de las recetas), **ganancia ($ y %)**, **food cost %**, y comparación contra vender los items por separado.
- Vigencia (fechas/horarios/canales) y registro de ventas por promo.
- **Objetivo futuro (Fase 10)**: usar datos históricos para **recomendar promociones** según análisis de mercado.

---

## 7. Manejo de Merma (dos métodos según tipo de artículo)

### 7.1 Merma por Rendimiento / Yield (papa y similares) — multi-etapa
La papa pierde peso en cada etapa de proceso. La app tendrá un **formulario para ingresar los pesos** de cada estación, para llenarlos **más adelante** cuando se midan en cocina:
- **Crudo (AP, as-purchased)** → peso al comprar
- **Pelado** → peso tras pelar
- **Prefrito** → peso tras prefritura
- **Frito (EP, edible portion / listo a servir)** → peso final servible

**Rendimiento total = peso final / peso inicial**. El **costo del producto servible** se ajusta: `costo EP = costo AP / rendimiento`. Mientras no haya datos cargados, se usa un rendimiento por defecto. Se registran rendimientos por etapa para detectar dónde se pierde más.

### 7.2 Merma por Deterioro / Waste Log (perecibles)
Para artículos que se estropean o vienen malos (**lechuga, tomate, palta, cebolla morada, cebolla normal, leche, etc.**):
- **Registro de merma (waste log)** con: artículo, cantidad, **motivo** (vencido, estropeado, vino malo, error de cocina, regalo/cortesía, robo), fecha y responsable.
- **Control de vida útil / caducidad**: cada perecible tiene vida útil (días); el sistema **alerta** lo próximo a vencer (FIFO sugerido). Caducidad **por lote** se hace en Fase 6.
- La merma registrada **descuenta stock** y alimenta el reporte de **variance** y el costo de merma del período.

> Método recomendado e implementado en la industria: combinar **yield-based costing** (rendimiento) para procesos con pérdida sistemática, y **waste tracking con motivo + shelf-life** para deterioro.

---

## 8. Mano de Obra (Fase 8) — **Q7**
Modelos de pago a soportar:
- **Sueldo fijo mensual** (prorrateado al período/semana para prime cost)
- **Por turno** (monto por turno trabajado)
- **Por hora** (horas × valor hora)
- **Propinas**: se cuenta solo el **total por día** (monto total diario, sin reparto por persona en el sistema).

Con esto se calcula **costo de mano de obra** → **Prime Cost = food + labor** por local y por semana, con benchmarks. El contador podrá ver toda la info.

---

## 9. Bebidas y Otros — **Q6**
- **Compra por pack, venta por unidad**: el artículo bebida se compra en pack (ej: 6 un, 24 un) con factor de conversión a unidad. Stock y costo se llevan **por unidad**; se vende por unidad.

---

## 10. Clean Slate — **Q4**
- **No se conserva nada**: aún no hay datos reales en la base de datos. Podemos **borrar y recrear** el esquema completo para implementar bien el nuevo modelo (unidades, artículos, recetas, sub-recetas, modificadores, promociones, merma). El backend reutilizable se mantiene; el modelo de datos se rehace.

---

## 11. Preguntas Finales (QF1-QF4) — RESPONDIDAS ✅

- **QF1 — Papa / rendimiento**: La app tendrá un **formulario para ingresar los pesos de las papas** (crudo, pelado, prefrito, frito) para llenarlos **más adelante**. Estructura lista para las 4 etapas; el costeo usa el rendimiento cargado o uno por defecto.
- **QF2 — Promociones**: **Creador de promociones manual** con su propia pantalla. Nombre + items del menú incluidos + precio → calcula costo, ganancia y food cost %.
- **QF3 — Caducidad por lote**: se hace en **Fase 6**. Por ahora solo vida útil por artículo + alerta simple.
- **QF4 — Propinas**: solo **total por día** (sin reparto por persona en el sistema).

---

**Fecha**: 2026-06-15
**Autor**: Kiro + andreslh
**Estado**: Plan v3 — decisiones D1-D4, Q1-Q7 y QF1-QF4 confirmadas. Listo para escribir la spec técnica de Fase 4.
