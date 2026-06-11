# Fase 2 — Boletas Electrónicas (SII Chile)

## Estado: En Implementación - Fase 2A (Comprobantes internos)
## Prioridad: Alta
## Fase: 2 de 5 (Roadmap ERP Completo)

---

## DECISIONES CONFIRMADAS (2026-06-11)

| Pregunta | Decisión |
|----------|----------|
| P1: SII | **No inscrito aún** (informal hasta crecer) → Fase 2A sin SII |
| P2: Proveedor | A futuro directo SII; proveedor elegido para 2B: **LibreDTE** (open source) |
| P3: Documentos | Todos a futuro; modelo `comprobantes` preparado para DTE 39/33 |
| P4: Emisión | **Manual** al cobrar (comanda al aceptar, comprobante al pagar) |
| P5: Entrega | Texto/PDF compartible por **WhatsApp**; preparado para QR/timbre e impresora térmica |
| P6: Seguridad | Edge Function para 2B (emisión real) |
| **IVA** | **B) Solo provisionar** — registrar IVA sin mover dinero entre sobres |

### Enfoque en 2 etapas
- **Fase 2A (AHORA)**: Comprobante interno (no tributario) + cálculo y registro de IVA (provisión) + arquitectura lista para SII. Deja todo auditable.
- **Fase 2B (al inscribirse)**: Emisión real vía LibreDTE + Edge Function. Cambio mínimo gracias a la abstracción.

⚠️ **Nota legal**: sin inscripción en el SII NO se emiten boletas válidas. Los comprobantes de 2A son documentos de control interno, claramente marcados como "no tributario". Formalización con apoyo de un contador.

---


**Documento base**: `.kiro/PROYECTO-CONTEXTO.md`
**Depende de**: Fase 1 (migración a Supabase) ✅ Completada

---

## 0. Contexto

ToppisERP ya registra ventas en Supabase. La Fase 2 busca **emitir boletas
electrónicas válidas ante el SII** por cada venta, y entregar el comprobante
al cliente (impreso o virtual), cumpliendo la normativa chilena vigente.

**Normativa relevante (2025-2026)**: el comercio debe entregar al cliente una
boleta electrónica (impresa o virtual) en ventas presenciales. La obligación
comenzó en mayo 2025 para dispositivos con impresión y marzo 2026 para otros.

---

## 1. El Desafío Técnico (por qué no es trivial)

Emitir un DTE (Documento Tributario Electrónico) ante el SII requiere:

1. **Certificado digital** del contribuyente (firma electrónica).
2. **Folios (CAF)** — códigos de autorización de folios que entrega el SII.
3. **Generar el XML** del DTE con formato exacto del SII.
4. **Firmar digitalmente** el XML (XMLDSig).
5. **Enviar al SII** (ambiente de certificación primero, luego producción).
6. **RCOF** — reporte de consumo de folios diario para boletas.
7. Manejar **respuestas, track ID y estados**.

Hacer todo esto **directamente desde la app Android es inviable y riesgoso**
(el certificado digital NO debe vivir en el dispositivo).

---

## 2. Enfoque Recomendado

### 2.1 Usar un Proveedor de Facturación (PSF / API REST)

En vez de integrar directo con el SII, usar un proveedor que ya maneja
certificado, folios, firma y envío. Tú solo llamas a su API REST.

**Opciones en Chile**:
| Proveedor | Notas |
|-----------|-------|
| **LibreDTE** | Open source + SaaS, API REST, popular, económico |
| **OpenFactura (Haulmer)** | API REST robusta, plan gratuito inicial |
| **Nubox** | Más orientado a contabilidad, API disponible |
| **FacturaSimple** | Económico, enfocado en PYMEs |

**Recomendación**: empezar con **OpenFactura** o **LibreDTE** (ambos tienen
API REST clara y ambiente de pruebas).

### 2.2 Emisión vía Supabase Edge Function (no desde la app)

```
App (POS) → Supabase Edge Function → API del Proveedor → SII
                     ↓
            Guarda DTE en tabla "documentos_tributarios"
```

**Por qué Edge Function**:
- El **token/credenciales del proveedor** quedan en el servidor (no en la app)
- Lógica de emisión centralizada y segura
- La app solo dispara la emisión y recibe el resultado (folio, PDF, estado)

---

## 3. Alcance Propuesto (a confirmar)

### Dentro del alcance
- Emitir **boleta electrónica afecta (DTE tipo 39)** por cada venta del POS
- Guardar el resultado (folio, track ID, estado, URL del PDF/XML) ligado a la venta
- Entregar al cliente: mostrar/compartir PDF o enviar por WhatsApp
- Ambiente de **certificación/pruebas** primero, luego producción

### Fuera del alcance (por ahora)
- Facturas electrónicas (DTE 33) — se puede agregar después
- Notas de crédito/débito
- Libro de ventas electrónico (eso es Fase 3 - Contabilidad)

---

## 4. Preguntas Clave (necesito tus respuestas)

### P1: ¿Estás habilitado como emisor electrónico en el SII?
- ¿Tienes **RUT de empresa/persona** habilitado para boletas electrónicas?
- ¿Tienes **certificado digital** vigente?
- ¿O partimos desde cero con el enrolamiento?

### P2: ¿Proveedor o integración directa?
- **A) Proveedor (recomendado)**: más rápido y seguro. ¿Tienes preferencia (OpenFactura, LibreDTE, otro)?
- **B) Directa con SII**: control total pero mucho más trabajo y responsabilidad.

### P3: ¿Qué documentos emitir ahora?
- Solo **boleta electrónica (39)**, o ¿también **factura (33)** para clientes con RUT?

### P4: ¿Cuándo se emite la boleta?
- **A) Automática** al confirmar la venta en el POS
- **B) Manual/opcional** (botón "Emitir boleta" después de la venta)

### P5: ¿Cómo se entrega al cliente?
- ¿Mostrar PDF en pantalla? ¿Compartir por WhatsApp? ¿Imprimir (impresora térmica)?
- ¿Mostrar código QR / timbre electrónico?

### P6: ¿Edge Functions en Supabase?
- ¿Estás de acuerdo con usar **Supabase Edge Functions** (Deno/TypeScript) para
  la emisión segura? Es la opción recomendada.

---

## 5. Diseño Preliminar de Datos (borrador)

Nueva tabla para registrar los documentos emitidos:

```sql
CREATE TABLE documentos_tributarios (
    id SERIAL PRIMARY KEY,
    venta_id INTEGER REFERENCES ventas(id) ON DELETE SET NULL,
    tipo_dte INTEGER NOT NULL,        -- 39 = boleta, 33 = factura
    folio INTEGER,
    rut_receptor TEXT,                -- null para boleta sin RUT
    monto_neto NUMERIC(12,2),
    monto_iva NUMERIC(12,2),
    monto_total NUMERIC(12,2),
    estado TEXT NOT NULL,             -- PENDIENTE, EMITIDO, RECHAZADO, ANULADO
    track_id TEXT,                    -- ID de seguimiento SII / proveedor
    pdf_url TEXT,
    xml_url TEXT,
    fecha_emision TIMESTAMPTZ,
    respuesta_proveedor JSONB,        -- payload completo para trazabilidad
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    created_by UUID REFERENCES usuarios(id)
);
```

Esto deja todo trazable y listo para la contabilidad (Fase 3).

---

## 6. Próximos Pasos

1. ⏳ **Usuario responde P1-P6**
2. Elegir proveedor y crear cuenta + obtener credenciales de prueba
3. Diseñar el esquema final (tabla documentos_tributarios + relación con ventas)
4. Crear la Edge Function de emisión
5. Integrar en el POS (botón emitir / emisión automática)
6. UI para mostrar/compartir el comprobante
7. Probar en ambiente de certificación
8. Pasar a producción

---

**Fecha de Creación**: 2026-06-11
**Autor**: Kiro + andreslh
**Estado**: Draft - Esperando respuestas P1-P6
