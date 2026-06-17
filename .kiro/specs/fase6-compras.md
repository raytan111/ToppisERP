# Fase 6 — Compras y Proveedores — Spec Técnica

## Estado: en implementación
## Base: Fase 4 (artículos) + Fase 5 (inventario pro)

> Objetivo: que el stock entre por **compras** (no a mano), actualizando el
> **costo promedio ponderado** y registrando **caducidad por lote**. Cierra el
> círculo del variance (consumo real = stock inicial + compras − stock final).

---

## 1. Módulos

### 6.1 Proveedores (CRUD)
- Tabla `proveedores`: nombre, contacto, telefono, email, nota, activo.

### 6.2 Compras / Recepción de mercadería
- Tabla `compras` (cabecera): proveedor, fecha, total, tiene_iva, nota.
- Tabla `compra_detalle`: artículo, cantidad (unidad base), costo_unitario (por unidad base, bruto), subtotal, vencimiento (lote, opcional).
- Al registrar (RPC atómico `registrar_compra`):
  - **Suma stock** del artículo (stock_base += cantidad).
  - **Costo promedio ponderado**: nuevo costo bruto = (stock·costo_bruto_actual + cantidad·costo_compra) / (stock + cantidad); costo_base = nuevo_bruto / rendimiento.
  - Guarda el detalle con su **vencimiento por lote**.
  - Opcional: registra un **gasto** (categoría INSUMOS, con IVA si corresponde) y descuenta un **sobre** (dinero que sale).

### 6.3 Caducidad por lote (alertas)
- Cada línea de compra con vencimiento queda como "lote". Pantalla/alertas de próximos a vencer.

---

## 2. Conversión de unidades
La app convierte en Kotlin: el usuario ingresa cantidad y costo en la **unidad de compra** del artículo (kg/L/un); el repo manda a la RPC ya en **unidad base** (g/ml/un) y costo por unidad base.

## 3. Orden de implementación
1. Proveedores (SQL + repo + pantalla)  ← empezamos
2. Compras/Recepción (SQL + RPC + repo + pantalla full screen)
3. Caducidad por lote (lista de próximos a vencer)

Cada paso con build verde + commit.

---
**Fecha**: 2026-06-16
