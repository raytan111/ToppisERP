# Requirements Document

## Introduction

Esta funcionalidad le da al dueño de ToppisERP una mirada **semanal de caja**: cuánto entró, cuánto se fue en costos y **cuánto le queda** al final de la semana. El foco es responder tres preguntas: cuánto queda después de costos, dónde se fuga la plata y cómo controlar los costos. La utilidad y el reparto entre socios **NO** se calculan en esta etapa (fuera de alcance).

El negocio es una hamburguesería/dark kitchen con **un solo local** en Chile, opera en pesos chilenos (CLP) y **no factura** (no emite boleta/IVA propio), aunque compra con boleta y paga IVA. Por eso todos los costos se manejan con **monto total (IVA incluido)**, sin neto ni crédito fiscal.

La semana de cálculo va de **lunes a sábado** (lunes = producción; martes a sábado = venta; domingo cerrado). Hoy los módulos (gastos, compras, mano de obra, contabilidad, KPIs, sobres) no se comunican y todo es mensual; esta funcionalidad los conecta y los lleva a una cadencia **semanal**.

El problema central a resolver es que hoy una compra actualiza stock y costo pero **no aparece como egreso** en el resultado, por lo que el dueño no sabe cuánto le queda de verdad. Esta funcionalidad cierra ese hueco con una vista de caja semanal, gestión de costos fijos recurrentes prorrateados, provisión en sobres, cálculo de mano de obra disponible, break-even y semáforos de objetivos.

## Glossary

- **Sistema_Costos**: El módulo de Control de Costos objeto de este documento.
- **Cierre_Semanal**: Proceso y pantalla que consolida y "fotografía" (snapshot) los montos de una semana lunes-sábado y muestra el resultado de caja.
- **Resultado_Semanal**: Cálculo de caja de una semana: ventas cobradas − variables − mano de obra pagada − fijos prorrateados = lo que queda.
- **Semana_Operativa**: Período de cálculo de lunes a sábado (domingo cerrado).
- **Costo_Variable**: Costo que cambia con las ventas: food/insumos, packaging y bencina de reparto.
- **Costo_Fijo**: Costo que no cambia con las ventas: arriendo, luz, gas, internet, sueldos base.
- **Costo_Fijo_Recurrente**: Registro de un costo fijo con nombre, categoría, monto, periodicidad (mensual/semanal/anual) y estado activo.
- **Prorrateo_Semanal**: Conversión del monto de un Costo_Fijo_Recurrente a su porción semanal (mensual ÷ 4,33; anual ÷ 52; semanal × 1).
- **Snapshot_Semanal**: Copia congelada de los montos y costos usados en un Cierre_Semanal, que no se recalcula al cambiar precios posteriormente.
- **Gestor_Compras**: Componente que registra compras, actualiza stock y actualiza el costo del artículo con el último precio pagado.
- **Costo_Articulo**: Costo unitario en unidad base de un artículo (campo `costo_base`), usado para valorizar recetas y mermas.
- **Categoria_Articulo**: Clasificación de un artículo: Ingredientes, Packaging o Insumos.
- **Food_Cost_Teorico**: Costo teórico de insumos según recetas, obtenido por el RPC `consumo_teorico_periodo`. Se usa como control de porcentaje, no para la caja.
- **Mano_Obra_Disponible**: Monto máximo destinable a sueldos en una semana = porcentaje objetivo × ventas de la semana.
- **Break_Even_Semanal**: Nivel de ventas necesario para no perder = costos fijos de la semana ÷ margen de contribución.
- **Margen_Contribucion**: 1 − (porcentaje de costos variables sobre ventas).
- **Sobre_Cuenta**: Sobre de tipo CUENTA (dinero real): Efectivo y Tarjeta.
- **Sobre_Fondo**: Sobre de tipo FONDO (provisión/aparte): p. ej. "Servicios", "Arriendo".
- **Provision_Sobres**: Sugerencia semanal de cuánto transferir desde un Sobre_Cuenta a un Sobre_Fondo para apartar la porción semanal de los fijos.
- **Rutina_Semanal**: Checklist guiado de cierre: conteo de inventario, registro de mermas, provisión de fijos en sobres y visualización del Resultado_Semanal.
- **Objetivo_Semaforo**: Umbral configurable (food ≤ 32%, mano de obra ≤ 30%, arriendo ≤ 10%) que dispara un indicador de color.
- **Dueño**: Usuario con rol de administración que revisa el Resultado_Semanal y configura objetivos.

## Requirements

### Requirement 1: Gestión de costos fijos recurrentes

**User Story:** Como dueño, quiero registrar mis costos fijos recurrentes con su periodicidad, para que la app los considere automáticamente cada semana sin volver a cargarlos.

#### Acceptance Criteria

1. THE Sistema_Costos SHALL almacenar cada Costo_Fijo_Recurrente con nombre, categoría, monto total en CLP con IVA incluido, periodicidad y estado activo.
2. THE Sistema_Costos SHALL aceptar como periodicidad exactamente uno de los valores: mensual, semanal o anual.
3. WHEN el Dueño crea un Costo_Fijo_Recurrente sin especificar estado, THE Sistema_Costos SHALL registrarlo como activo.
4. WHEN el Dueño registra un Costo_Fijo_Recurrente con monto igual a 0, THE Sistema_Costos SHALL guardarlo y tratarlo como fijo de monto 0 en los cálculos.
5. WHILE un Costo_Fijo_Recurrente está inactivo, THE Sistema_Costos SHALL excluirlo de los cálculos de fijos semanales.
6. IF el Dueño intenta guardar un Costo_Fijo_Recurrente con monto negativo, THEN THE Sistema_Costos SHALL rechazar la operación y mostrar un mensaje que indique que el monto debe ser mayor o igual a 0.

### Requirement 2: Prorrateo de costos fijos a la semana

**User Story:** Como dueño, quiero que la app convierta mis costos fijos a su porción semanal, para saber cuánto pesan los fijos en una Semana_Operativa.

#### Acceptance Criteria

1. WHEN un Costo_Fijo_Recurrente activo tiene periodicidad mensual, THE Sistema_Costos SHALL calcular su Prorrateo_Semanal dividiendo el monto por 4,33.
2. WHEN un Costo_Fijo_Recurrente activo tiene periodicidad anual, THE Sistema_Costos SHALL calcular su Prorrateo_Semanal dividiendo el monto por 52.
3. WHEN un Costo_Fijo_Recurrente activo tiene periodicidad semanal, THE Sistema_Costos SHALL usar el monto como Prorrateo_Semanal sin conversión.
4. THE Sistema_Costos SHALL calcular el total de fijos semanales como la suma de los Prorrateo_Semanal de todos los Costo_Fijo_Recurrente activos.

### Requirement 3: Categorías de artículos

**User Story:** Como dueño, quiero clasificar cada artículo por categoría, para agrupar los costos variables por tipo (ingredientes, packaging, insumos).

#### Acceptance Criteria

1. THE Sistema_Costos SHALL asociar a cada artículo exactamente una Categoria_Articulo con valor Ingredientes, Packaging o Insumos.
2. WHEN el Dueño registra un artículo sin especificar Categoria_Articulo, THE Sistema_Costos SHALL asignar la categoría Ingredientes por defecto.
3. THE Sistema_Costos SHALL permitir modificar la Categoria_Articulo de un artículo existente.

### Requirement 4: Inventario solo gestiona stock

**User Story:** Como dueño, quiero que la pantalla de Inventario muestre solo stock, para no confundir la gestión de existencias con el costeo.

#### Acceptance Criteria

1. THE Sistema_Costos SHALL excluir la función de "calcular costos" de la pantalla de Inventario.
2. THE pantalla de Inventario SHALL mostrar únicamente información de stock de los artículos.
3. WHEN no hay datos de stock disponibles, THE pantalla de Inventario SHALL mostrarse vacía con un mensaje que indique que no hay stock disponible.

### Requirement 5: Actualización del costo del artículo desde Compras

**User Story:** Como dueño, quiero que el costo de un artículo se actualice al registrar una compra con el último precio pagado, para que los costos reflejen lo que pago realmente sin tener que editarlos a mano.

#### Acceptance Criteria

1. WHEN el Gestor_Compras registra una compra de un artículo, THE Sistema_Costos SHALL actualizar el Costo_Articulo con el último precio pagado en esa compra.
2. WHEN el último precio pagado de un artículo difiere del Costo_Articulo actual, THE Sistema_Costos SHALL actualizar el Costo_Articulo y recalcular el costo de todas las recetas que usan ese artículo.
3. WHEN el último precio pagado de un artículo es igual al Costo_Articulo actual, THE Sistema_Costos SHALL mantener el Costo_Articulo sin cambios.
4. THE Sistema_Costos SHALL registrar el Costo_Articulo y los costos de compra como monto total con IVA incluido, sin separar neto ni IVA.

### Requirement 6: Congelamiento del histórico semanal

**User Story:** Como dueño, quiero que los resultados de semanas pasadas no cambien cuando actualizo un precio hoy, para que mis cierres anteriores no descuadren.

#### Acceptance Criteria

1. WHEN el Dueño confirma un Cierre_Semanal, THE Sistema_Costos SHALL guardar un Snapshot_Semanal con los montos de ventas, variables, mano de obra y fijos prorrateados de esa Semana_Operativa.
2. WHEN cambia un Costo_Articulo o un precio después de un Cierre_Semanal confirmado, THE Sistema_Costos SHALL mantener sin cambios los valores del Snapshot_Semanal de las semanas ya cerradas.
3. WHEN el Dueño consulta una Semana_Operativa ya cerrada, THE Sistema_Costos SHALL mostrar los valores del Snapshot_Semanal en lugar de recalcularlos con precios actuales.

### Requirement 7: Clasificación de costos en variables y fijos

**User Story:** Como dueño, quiero ver mis costos separados en variables y fijos, para entender qué cambia con las ventas y qué no.

#### Acceptance Criteria

1. THE Sistema_Costos SHALL clasificar cada costo en exactamente uno de dos grupos: Costo_Variable o Costo_Fijo.
2. THE Sistema_Costos SHALL clasificar food/insumos, packaging y bencina de reparto como Costo_Variable.
3. THE Sistema_Costos SHALL clasificar arriendo, luz, gas, internet y sueldos base como Costo_Fijo.
4. THE Sistema_Costos SHALL restringir cada costo a su grupo previsto, de modo que arriendo, luz, gas, internet y sueldos base solo puedan clasificarse como Costo_Fijo.
5. IF el Sistema_Costos no puede determinar automáticamente el grupo de un costo, THEN THE Sistema_Costos SHALL requerir que el Dueño lo clasifique manualmente como Costo_Variable o Costo_Fijo.

### Requirement 8: Compras como egreso en la caja semanal

**User Story:** Como dueño, quiero que las compras se cuenten como egreso en el resultado semanal, para saber cuánto me queda de verdad.

#### Acceptance Criteria

1. THE Sistema_Costos SHALL incluir las compras registradas dentro de una Semana_Operativa como egreso del Resultado_Semanal.
2. THE Sistema_Costos SHALL calcular el resultado de caja de una Semana_Operativa como: ventas cobradas − compras − gastos − sueldos pagados.
3. THE Sistema_Costos SHALL usar el Food_Cost_Teorico obtenido del RPC `consumo_teorico_periodo` únicamente para el porcentaje de control y no para el cálculo de caja.

### Requirement 9: Resultado / Cierre semanal (mirada de caja)

**User Story:** Como dueño, quiero una pantalla de resultado semanal con selector de semana, para ver cuánto vendí, cuánto se fue en costos y cuánto me quedó.

#### Acceptance Criteria

1. THE Cierre_Semanal SHALL permitir seleccionar una Semana_Operativa de lunes a sábado.
2. WHEN el Dueño selecciona una Semana_Operativa, THE Cierre_Semanal SHALL mostrar las ventas cobradas, los Costo_Variable (food/packaging/bencina), la mano de obra pagada y los fijos prorrateados de esa semana.
3. WHEN el Dueño selecciona una Semana_Operativa, THE Cierre_Semanal SHALL mostrar el valor "lo que queda" igual a ventas cobradas − Costo_Variable − mano de obra pagada − fijos prorrateados.
4. WHEN el Dueño selecciona una Semana_Operativa, THE Cierre_Semanal SHALL mostrar el porcentaje de food y el porcentaje de mano de obra sobre las ventas de esa semana, calculados de forma independiente aunque su suma supere el 100%.
5. WHEN el Dueño selecciona una Semana_Operativa, THE Cierre_Semanal SHALL mostrar el Break_Even_Semanal y cuánto falta vender para no perder.
6. THE Cierre_Semanal SHALL excluir cualquier cálculo de utilidad o reparto entre socios.

### Requirement 10: Mano de obra disponible

**User Story:** Como dueño, quiero saber cuánto puedo destinar a sueldos según las ventas de la semana, para repartirlo entre los que trabajan y saber cuándo alcanza para contratar.

#### Acceptance Criteria

1. THE Sistema_Costos SHALL almacenar un porcentaje objetivo de mano de obra configurable, con valor inicial de 30%.
2. WHEN el Dueño consulta una Semana_Operativa, THE Sistema_Costos SHALL calcular la Mano_Obra_Disponible como el porcentaje objetivo de mano de obra multiplicado por las ventas de esa semana.
3. THE Sistema_Costos SHALL dividir la Mano_Obra_Disponible entre la cantidad de empleados activos que trabajan en la Semana_Operativa para mostrar el monto por persona.
4. IF no hay empleados activos trabajando en una Semana_Operativa con ventas positivas, THEN THE Sistema_Costos SHALL mostrar la Mano_Obra_Disponible completa como presupuesto potencial para contratar.
5. WHEN la Mano_Obra_Disponible por persona alcanza o supera un umbral configurable para incorporar un nuevo empleado Y la Mano_Obra_Disponible total es mayor que 0, THE Sistema_Costos SHALL mostrar un indicador de que ya alcanza para contratar.
6. IF la Mano_Obra_Disponible total es igual a 0, THEN THE Sistema_Costos SHALL ocultar el indicador de que alcanza para contratar.
7. THE Sistema_Costos SHALL calcular la Mano_Obra_Disponible unificando la información del módulo Empleados y Mano de Obra existente.

### Requirement 11: Objetivo/techo de arriendo

**User Story:** Como dueño, quiero fijar un techo de arriendo como porcentaje de las ventas, para que me avise si el arriendo se pasa de lo que el negocio puede pagar.

#### Acceptance Criteria

1. THE Sistema_Costos SHALL almacenar un porcentaje techo de arriendo configurable, con valor inicial de 10%.
2. WHEN el arriendo prorrateado de una Semana_Operativa supera el porcentaje techo de arriendo aplicado sobre las ventas de esa semana, THE Sistema_Costos SHALL mostrar una alerta al Dueño.

### Requirement 12: Break-even semanal

**User Story:** Como dueño, quiero saber cuánto tengo que vender para no perder, para tener una meta clara de ventas cada semana.

#### Acceptance Criteria

1. THE Sistema_Costos SHALL calcular el Margen_Contribucion como 1 menos el porcentaje de Costo_Variable sobre las ventas de la Semana_Operativa.
2. THE Sistema_Costos SHALL calcular el Break_Even_Semanal como el total de fijos prorrateados de la Semana_Operativa dividido por el Margen_Contribucion.
3. WHEN las ventas de la Semana_Operativa son menores que el Break_Even_Semanal, THE Sistema_Costos SHALL mostrar cuánto falta vender para no perder.
4. IF el Margen_Contribucion es igual a 0, THEN THE Sistema_Costos SHALL impedir el cálculo del Break_Even_Semanal e indicar que no es calculable con los costos variables actuales.
5. IF el Margen_Contribucion es menor que 0, THEN THE Sistema_Costos SHALL impedir el cálculo del Break_Even_Semanal e indicar que no es calculable con los costos variables actuales.

### Requirement 13: Provisión de fijos en sobres

**User Story:** Como dueño, quiero que la app me sugiera cuánto apartar cada semana en un sobre de fondo, para no gastarme la plata de los costos fijos.

#### Acceptance Criteria

1. WHEN el Dueño ejecuta la Provision_Sobres de una Semana_Operativa, THE Sistema_Costos SHALL sugerir un monto a apartar igual al Prorrateo_Semanal de cada Costo_Fijo_Recurrente activo.
2. THE Sistema_Costos SHALL proponer la transferencia desde un Sobre_Cuenta hacia un Sobre_Fondo por el monto sugerido.
3. THE Sistema_Costos SHALL validar que cada monto sugerido a apartar sea mayor que 0 antes de proponer o ejecutar cualquier transferencia.
4. WHEN el Dueño confirma la Provision_Sobres, THE Sistema_Costos SHALL registrar la transferencia entre sobres usando el sistema de sobres existente.
5. IF existen Costo_Fijo_Recurrente activos por provisionar Y el saldo del Sobre_Cuenta seleccionado es menor que el monto a apartar, THEN THE Sistema_Costos SHALL advertir que el saldo es insuficiente antes de confirmar la transferencia.
6. IF no existen Costo_Fijo_Recurrente activos por provisionar, THEN THE Sistema_Costos SHALL omitir la advertencia de saldo insuficiente.

### Requirement 14: Rutina semanal guiada

**User Story:** Como dueño, quiero una rutina de cierre semanal paso a paso, para no olvidar contar inventario, registrar mermas, provisionar fijos y revisar el resultado.

#### Acceptance Criteria

1. THE Rutina_Semanal SHALL presentar los pasos: conteo de inventario, registro de mermas, Provision_Sobres de fijos y visualización del Resultado_Semanal.
2. WHEN el Dueño completa un paso de la Rutina_Semanal, THE Sistema_Costos SHALL marcar ese paso como completado para la Semana_Operativa.
3. THE Rutina_Semanal SHALL orquestar los módulos existentes de conteo de inventario y de mermas sin duplicar su funcionalidad.
4. WHEN todos los pasos de la Rutina_Semanal de una Semana_Operativa están completados Y se cumplen las condiciones de validación de datos definidas para el cierre, THE Sistema_Costos SHALL permitir confirmar el Cierre_Semanal de esa semana.
5. IF todos los pasos están completados pero no se cumplen las condiciones de validación de datos del cierre, THEN THE Sistema_Costos SHALL impedir la confirmación del Cierre_Semanal e indicar qué validación falta.

### Requirement 15: Objetivos y semáforos configurables

**User Story:** Como dueño, quiero ver semáforos de colores según mis objetivos, para detectar rápido dónde se está fugando la plata.

#### Acceptance Criteria

1. THE Sistema_Costos SHALL almacenar como objetivos configurables el porcentaje máximo de food (inicial 32%), el porcentaje máximo de mano de obra (inicial 30%) y el porcentaje máximo de arriendo (inicial 10%).
2. WHEN el porcentaje de food de una Semana_Operativa supera el objetivo de food, THE Sistema_Costos SHALL mostrar el Objetivo_Semaforo de food en estado de alerta.
3. WHEN el porcentaje de mano de obra de una Semana_Operativa supera el objetivo de mano de obra, THE Sistema_Costos SHALL mostrar el Objetivo_Semaforo de mano de obra en estado de alerta.
4. WHEN el porcentaje de arriendo de una Semana_Operativa supera el objetivo de arriendo, THE Sistema_Costos SHALL mostrar el Objetivo_Semaforo de arriendo en estado de alerta.
5. WHEN las ventas de una Semana_Operativa están bajo el Break_Even_Semanal, THE Sistema_Costos SHALL mostrar una alerta de que se está bajo el break-even.
6. WHILE un indicador se encuentra dentro de su objetivo, THE Sistema_Costos SHALL mostrar ese Objetivo_Semaforo en estado favorable.

### Requirement 16: Sección de Costos y terminología unificada

**User Story:** Como dueño, quiero que toda esta funcionalidad esté agrupada en una sección propia de "Costos" en el menú y que la app hable siempre de "costos", para encontrar todo en un solo lugar y no confundirme con el término "gastos".

#### Acceptance Criteria

1. THE Sistema_Costos SHALL agrupar las pantallas de esta funcionalidad (costos fijos recurrentes, resultado/cierre semanal, mano de obra disponible, break-even, provisión en sobres, rutina semanal y objetivos/semáforos) en una sección propia de "Costos" del menú de la app.
2. THE Sistema_Costos SHALL usar el término "costos" en la interfaz de esta sección en lugar del término "gastos".
3. WHERE una pantalla existente muestra el término "gastos" y forma parte de esta funcionalidad, THE Sistema_Costos SHALL presentar dicho término como "costos".

### Requirement 17: Alcance excluido — utilidad y reparto

**User Story:** Como dueño, quiero que quede claro que la utilidad y el reparto entre socios no se calculan todavía, para enfocar esta etapa solo en cuánto queda.

#### Acceptance Criteria

1. THE Sistema_Costos SHALL limitar el resultado semanal a "lo que queda" después de costos.
2. THE Sistema_Costos SHALL excluir todo cálculo de utilidad neta, reparto entre socios y distribución de excedentes.
