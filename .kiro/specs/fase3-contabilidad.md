# Fase 3 — Contabilidad e Impuestos

## Estado: ✅ Implementado (núcleo de Fase 3)
## Prioridad: Media-Alta
## Fase: 3 de 5

**Decisiones**: D1=A (IVA en gastos), D2=B (todas las ventas), D3=snapshot

### Implementado (2026-06-11)
- ✅ IVA en gastos: checkbox "con factura" + cálculo neto/IVA (RPC actualizada)
- ✅ Tabla `cierres_mensuales` (snapshot por mes/año)
- ✅ `ContabilidadRepository`: resumen IVA, libro ventas, libro compras, cierre
- ✅ Pantalla **Contabilidad** (Admin): selector mes/año, tabs Resumen IVA / Ventas / Compras, botón "Cerrar mes"
- ✅ Resumen IVA (débito − crédito = a pagar/favor) + estado de resultados simple
- 📜 SQL: `supabase-contabilidad.sql`

### Pendiente (mejoras futuras)
- Exportar libros a Excel/CSV
- Historial de cierres mensuales
- Bloqueo de meses cerrados (si se decide)

**Depende de**: Fase 1 (Supabase) ✅ y Fase 2A (comprobantes + IVA) ✅

⚠️ **Nota**: No soy asesor tributario. Esto es apoyo técnico para llevar registros
ordenados; la presentación oficial de impuestos debe validarse con un contador.

---

## 0. Contexto

Ya tenemos: ventas, gastos, comprobantes con neto/IVA, sobres y movimientos.
La Fase 3 busca **ordenar esa información en términos contables** para:
- Tener libros de ventas y compras
- Ver el IVA (débito vs crédito) por mes
- Cierres mensuales y un resumen tipo "estado de resultados" simple
- Quedar listo para declarar cuando te formalices

---

## 1. Alcance Propuesto

### 1.1 Libro de Ventas (mensual)
- Listado de comprobantes/ventas del mes con neto, IVA débito, total
- Totales del período
- Exportable (Excel/CSV)

### 1.2 Libro de Compras (mensual)
- Listado de gastos del mes con neto, IVA crédito, total
- **Requiere registrar el IVA de los gastos** (ver Decisión D1)

### 1.3 Resumen de IVA (mensual)
- IVA débito (ventas) − IVA crédito (compras) = **IVA a pagar / a favor**
- Es la base del formulario F29 (mensual en Chile)

### 1.4 Estado de Resultados Simple (mensual)
- Ingresos netos − Costos/Gastos netos = Resultado operacional
- Margen %

### 1.5 Cierre Mensual
- "Cerrar mes": snapshot de los totales del período para histórico

---

## 2. Decisiones Clave (necesito tus respuestas)

### D1: IVA de los gastos (IVA crédito)
Hoy los gastos guardan solo el monto total, sin separar IVA. Para un libro de
compras y el IVA crédito real necesitamos saber el IVA de cada gasto.
- **A) Agregar IVA a los gastos**: al registrar un gasto, indicar si tiene IVA
  (con factura) y calcularlo. Más preciso para el F29.
- **B) Estimar**: asumir que cierto % de gastos tienen IVA (aproximado).
- **C) Por ahora solo Libro de Ventas + IVA débito**, y dejamos compras para después.

**Mi recomendación**: **A** (agregar un check "tiene IVA / con factura" + cálculo
al registrar gasto). Es lo correcto y no es mucho trabajo.

### D2: ¿Sobre qué se calcula el Libro de Ventas?
- **A) Sobre comprobantes emitidos** (lo formal)
- **B) Sobre todas las ventas** (haya o no comprobante)

**Mi recomendación**: **B** para control interno completo (todas las ventas
tienen IVA implícito), y marcar cuáles tienen comprobante.

### D3: ¿Cierre mensual con bloqueo?
- ¿El "cierre de mes" solo guarda un snapshot (informativo), o también
  **bloquea** edición de datos de ese mes?
- **Mi recomendación**: snapshot informativo por ahora (sin bloqueo), para no
  complicar. El bloqueo se puede agregar después.

---

## 3. Diseño Preliminar

### Nuevos campos en `gastos` (si D1 = A)
```sql
ALTER TABLE gastos ADD COLUMN tiene_iva BOOLEAN DEFAULT false;
ALTER TABLE gastos ADD COLUMN monto_neto NUMERIC(12,2);
ALTER TABLE gastos ADD COLUMN monto_iva NUMERIC(12,2);
```

### Tabla `cierres_mensuales` (snapshot)
```sql
CREATE TABLE cierres_mensuales (
    id SERIAL PRIMARY KEY,
    mes INTEGER NOT NULL,
    anio INTEGER NOT NULL,
    ventas_netas NUMERIC(12,2),
    iva_debito NUMERIC(12,2),
    compras_netas NUMERIC(12,2),
    iva_credito NUMERIC(12,2),
    iva_a_pagar NUMERIC(12,2),
    resultado NUMERIC(12,2),
    fecha_cierre TIMESTAMPTZ DEFAULT now(),
    created_by UUID REFERENCES usuarios(id),
    UNIQUE(mes, anio)
);
```

### Nueva pantalla "Contabilidad" (Admin)
- Selector de mes/año
- Tabs: Resumen IVA · Libro Ventas · Libro Compras · Resultado
- Botón "Cerrar mes" (snapshot)
- Exportar

---

## 4. Próximos Pasos

1. ⏳ Usuario responde **D1, D2, D3**
2. Implementar registro de IVA en gastos (si D1=A)
3. Crear repositorio + pantalla de Contabilidad (resumen IVA + libros)
4. Cierre mensual (snapshot)
5. Exportación de libros

---

**Fecha de Creación**: 2026-06-11
**Autor**: Kiro + andreslh
**Estado**: Draft - Esperando D1, D2, D3
