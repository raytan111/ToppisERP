# Migración a Base de Datos en la Nube (Supabase)

## Estado: ✅ FASE 1 COMPLETADA - App 100% en Supabase
## Prioridad: Alta
## Estimación: 2-3 semanas
## Fase: 1 de 5 (Roadmap ERP Completo)

---

## 0. Contexto del Proyecto

**Documento base**: `.kiro/PROYECTO-CONTEXTO.md`

**Estado actual**: ToppisERP funciona 100% local con Room (SQLite), 13 entidades, autenticación local, sin backup ni sincronización.

**Objetivo de esta fase**: Migrar la base de datos y autenticación a la nube (Supabase) para habilitar acceso multi-dispositivo, backup automático, y sentar las bases para las fases 2-5 del roadmap (boletas electrónicas, contabilidad, multi-app, IA).

**Referencia**: #[[file:../.kiro/PROYECTO-CONTEXTO.md]]

---

## 1. Requisitos Funcionales

### RF-1: Migración de Entidades a Supabase
**Descripción**: Migrar las 13 tablas actuales de Room a PostgreSQL en Supabase manteniendo estructura y relaciones.

**Entidades a migrar**:

1. ✓ usuarios (con rol, activo, fechaCreacion)
2. ✓ sobres (cajas de dinero)
3. ✓ movimientos_sobre (INGRESO/EGRESO/TRANSFERENCIA)
4. ✓ insumos (stock unitario)
5. ✓ ingredientes (stock en gramos)
6. ✓ ventas (con método pago, estado, envío)
7. ✓ gastos (con categoría y comprobante)
8. ✓ presupuestos (por mes/año/categoría)
9. ✓ items_menu (productos del menú)
10. ✓ recetas_menu (relación ItemMenu ↔ Ingredientes/Insumos)
11. ✓ salsas (complementos)
12. ✓ comandas (órdenes de cocina)
13. ✓ items_venta_menu (detalle de ventas)

**Conservar**:
- Foreign keys (CASCADE, SET_NULL, RESTRICT según lógica actual)
- Índices para performance
- Auto-increment en PKs
- Enumeraciones (mapear a PostgreSQL ENUMs o strings con CHECK constraint)

**Decisión pendiente**: ¿Agregar columnas de auditoría cloud?
- `created_at TIMESTAMPTZ` (timestamp con zona horaria)
- `updated_at TIMESTAMPTZ`
- `created_by UUID` (referencia a usuario Supabase)

---

### RF-2: Migración de Autenticación a Supabase Auth

**Descripción**: Reemplazar autenticación local (email/passwordHash en Room) por Supabase Auth.

**Funcionalidades requeridas**:
- Login con email/password
- Mantener roles (ADMIN/CAJERO) como metadata de usuario
- Sesión persistente (token refresh automático)
- Logout
- (Futuro opcional) Recuperación de contraseña por email

**Cambios en tabla usuarios**:
- **Opción A**: Eliminar `passwordHash`, usar solo `id` (UUID de Supabase) como PK
- **Opción B**: Mantener tabla usuarios separada con FK a auth.users de Supabase
- **Decisión pendiente**: ¿Qué opción preferimos?

**Row Level Security (RLS)**:
- ¿Habilitar RLS desde el inicio?
- Si sí: Políticas por rol (Admin ve todo, Cajero solo sus registros en ciertas tablas)
- Si no: Implementar lógica de permisos en app (más simple inicialmente)

---

### RF-3: Estrategia de Conexión

**Pregunta clave**: ¿Cómo manejamos la conectividad?

**Opción A: Online-Only (100% dependiente de internet)**
- ✅ Pros: Más simple, siempre datos actualizados, sin conflictos de sync
- ❌ Contras: App no funciona sin internet

**Opción B: Offline-First con Sincronización**

- ✅ Pros: Funciona sin internet, mejor UX, resiliencia
- ❌ Contras: Complejidad alta (sync bidireccional, resolución de conflictos, caché local)

**Opción C: Híbrido (lectura offline, escritura online)**
- ✅ Pros: Balance entre simplicidad y funcionalidad
- ❌ Contras: Experiencia inconsistente (puedes ver menú pero no vender sin internet)

**Decisión pendiente**: ¿Qué opción queremos? (Recomendación inicial: A - Online-Only para MVP, considerar B en futuro)

---

### RF-4: Migración de Datos Existentes

**Problema**: Usuarios actuales tienen datos en Room local.

**Solución requerida**:
1. **Script de exportación**: Exportar todos los datos de Room a JSON/CSV
2. **Script de importación**: Importar datos a Supabase vía API REST
3. **Validación**: Verificar integridad post-migración (conteos, foreign keys)

**Consideraciones**:
- ¿Migrar automáticamente en primera apertura de app actualizada?
- ¿O proporcionar herramienta manual de exportación/importación?
- ¿Mantener Room temporal hasta confirmar migración exitosa?

**Decisión pendiente**: Estrategia de migración para usuarios existentes.

---

### RF-5: Realtime Updates (Opcional)

**Descripción**: ¿Queremos actualizaciones en tiempo real entre dispositivos?

**Caso de uso**: 
- Cajero A registra venta → Cajero B ve inventario actualizado instantáneamente
- Admin modifica menú → POS se actualiza sin refrescar

**Implementación**: Supabase Realtime (WebSockets sobre tablas específicas)

**Decisión pendiente**: ¿Es prioritario para MVP o lo dejamos para v2?

---

## 2. Requisitos No Funcionales

### RNF-1: Performance
- Latencia de lectura < 500ms (queries simples)
- Latencia de escritura < 1s (inserts/updates)
- Carga inicial de menú (items_menu) < 2s
- Soporte para hasta 1000 ventas/día sin degradación

### RNF-2: Seguridad
- **Autenticación**: JWT tokens con refresh automático
- **Transporte**: HTTPS/TLS obligatorio
- **Secrets**: API keys en BuildConfig (no hardcoded)
- **RLS**: Decidir si se implementa en Fase 1 o después

### RNF-3: Costos
**Restricción**: Presupuesto bajo (gratis o < $10 USD/mes)

**Supabase Free Tier**:
- ✅ 500 MB database storage
- ✅ 2 GB file storage
- ✅ 50,000 monthly active users
- ✅ 500 MB egress (datos salientes)

**Estimación de uso**:
- 13 tablas con ~1000 registros/mes = ~50 MB (bien dentro del límite)
- 5-10 usuarios concurrentes = OK
- Operaciones CRUD normales = OK

**Conclusión**: Free tier es suficiente para dark kitchen individual. Escalar a Pro ($25/mes) si:
- Múltiples dark kitchens (multi-tenant)
- > 500 MB de datos históricos
- > 50 usuarios

### RNF-4: Compatibilidad
- Mantener soporte Android API 26+ (actual)
- Kotlin + Jetpack Compose (sin cambios)
- Supabase Kotlin SDK compatible con Android

### RNF-5: Mantenibilidad
- Mantener arquitectura MVVM actual
- Repositorios con interfaz común (fácil swap Room ↔ Supabase)
- Manual DI (sin Hilt, consistente con proyecto actual)

---

## 3. Alcance de Fase 1

### ✅ Dentro del Alcance

1. **Base de datos**:
   - Migrar 13 tablas a Supabase PostgreSQL
   - Configurar foreign keys e índices
   - Crear enums en PostgreSQL

2. **Autenticación**:
   - Implementar Supabase Auth (email/password)
   - Migrar roles a metadata de usuario
   - Sesión persistente con tokens

3. **Repositories**:
   - Refactorizar 10 repositories para usar Supabase SDK
   - Mantener interfaces similares (mínimo cambio en ViewModels)

4. **Configuración**:
   - Setup Supabase project
   - Configurar API keys en BuildConfig
   - Script SQL de creación de schema

5. **Testing básico**:
   - Verificar CRUD en cada tabla
   - Verificar login/logout
   - Verificar foreign keys funcionan

### ❌ Fuera del Alcance (Fases Futuras)

1. **Modo offline**: Se implementa online-only, offline-first queda para v2
2. **Row Level Security avanzado**: Políticas básicas o diferido a Fase 1.5
3. **Realtime updates**: Opcional, se evalúa post-MVP
4. **Migración automática**: Herramienta manual suficiente para MVP
5. **Optimizaciones avanzadas**: Caching, query batching, etc. (optimizar después)
6. **Boletas electrónicas**: Fase 2
7. **Contabilidad**: Fase 3
8. **Multi-app**: Fase 4

---

## 4. Preguntas Pendientes para el Usuario

**IMPORTANTE**: Necesito tus respuestas a estas preguntas antes de continuar al diseño:

### P1: Estrategia de Conexión
¿Qué opción prefieres?

- **A**: Online-only (más simple, app requiere internet)
- **B**: Offline-first (más complejo, app funciona sin internet)
- **C**: Híbrido (lectura offline, escritura online)

**Mi recomendación**: A para Fase 1, considerar B después si es crítico.

### P2: Row Level Security (RLS)
¿Implementamos RLS desde el inicio?
- **Sí**: Políticas en Supabase (Admin ve todo, Cajero filtrado). Más seguro pero más complejo.
- **No**: Lógica de filtrado en app. Más simple pero menos seguro si alguien accede directo a la DB.

**Mi recomendación**: Sí, RLS básico desde inicio (no es tan complejo y es buena práctica).

### P3: Realtime Updates
¿Quieres que los cambios se vean instantáneamente en todos los dispositivos?
- **Sí**: Implementar Supabase Realtime (más "wow factor")
- **No**: Refresco manual o por intervalo (más simple)

**Mi recomendación**: No para MVP, es un "nice to have" que agrega complejidad.

### P4: Migración de Datos Existentes
Si ya tienes usuarios usando la app actual, ¿cómo migrarán sus datos?
- **A**: Herramienta de exportación manual (usuario exporta → importa en nueva versión)
- **B**: Migración automática en primer launch de app actualizada
- **C**: No hay usuarios reales aún, ignorar este problema

**Mi recomendación**: Si no hay usuarios reales, opción C. Si los hay, opción A (manual es más seguro).

### P5: Tabla de Usuarios
¿Cómo manejamos la tabla usuarios con Supabase Auth?

- **A**: Usar solo auth.users de Supabase + metadata para rol
- **B**: Mantener tabla usuarios propia con FK a auth.users (más flexible para agregar campos custom)

**Mi recomendación**: B, porque necesitas campos como `nombre`, `activo`, `fechaCreacion` que no están en auth.users.

### P6: Columnas de Auditoría
¿Agregamos columnas de auditoría cloud en todas las tablas?
- `created_at TIMESTAMPTZ`
- `updated_at TIMESTAMPTZ`
- `created_by UUID` (usuario que creó el registro)

**Ventajas**: Trazabilidad completa, útil para auditorías  
**Desventajas**: Más complejidad en migraciones, más storage

**Mi recomendación**: Sí, es una buena práctica cloud y te servirá para Fase 3 (contabilidad).

---

## 5. Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Costos de Supabase exceden free tier | Baja | Alto | Monitorear uso, optimizar queries, implementar caching |
| Latencia afecta UX (internet lento Chile) | Media | Medio | Considerar CDN, optimizar payloads, agregar loading states |
| Pérdida de datos en migración | Baja | Crítico | Backup completo antes de migrar, validación exhaustiva |
| Complejidad de sync (si se elige offline-first) | Alta | Alto | Empezar con online-only, diferir sync a Fase 1.5 |
| Breaking changes en Supabase SDK | Baja | Medio | Pin versión específica, testear antes de actualizar |
| Curva de aprendizaje Supabase Kotlin SDK | Media | Bajo | Documentación oficial, ejemplos de comunidad |

---

## 6. Dependencias Externas

### 6.1 Supabase Project
- **Acción requerida**: Crear proyecto en [supabase.com](https://supabase.com)
- **Información necesaria**:
  - Nombre del proyecto (ej: "toppis-erp-prod")
  - Región (recomendado: South America - São Paulo para menor latencia desde Chile)
  - Password de base de datos (guardar en lugar seguro)

### 6.2 Supabase Kotlin SDK
- **Librería**: `io.github.jan-tennert.supabase:postgrest-kt`
- **Versión recomendada**: `2.0.0+` (verificar última estable)
- **Módulos necesarios**:
  - `postgrest-kt` (queries a DB)
  - `gotrue-kt` (autenticación)
  - `realtime-kt` (solo si se decide implementar realtime)

### 6.3 API Keys
- **Anon Public Key**: Para acceso desde app (se puede exponer)
- **Service Role Key**: Para operaciones admin (NUNCA en app, solo en scripts backend)

---

## 7. Criterios de Aceptación (Fase 1 Completa)

- [ ] Proyecto Supabase creado y configurado
- [ ] Schema SQL creado con 13 tablas + enums + foreign keys
- [ ] Supabase Kotlin SDK integrado en proyecto Android
- [ ] AuthRepository refactorizado para usar Supabase Auth
- [ ] Login/logout funciona con Supabase
- [ ] Roles (ADMIN/CAJERO) se leen correctamente de metadata/tabla
- [ ] Los 10 repositories migrados a Supabase SDK
- [ ] CRUD funciona en todas las entidades
- [ ] Foreign keys y cascadas funcionan correctamente
- [ ] UI no tiene cambios visibles (misma experiencia de usuario)
- [ ] App compila sin errores
- [ ] Testing manual de flujo completo: Login → Venta → Ver reportes → Logout
- [ ] Documentación de setup de Supabase para futuros desarrolladores
- [ ] (Opcional) Herramienta de migración de datos Room → Supabase

---

## 8. Decisiones del Usuario (2026-06-08)

### ✅ Decisiones Confirmadas

| Pregunta | Decisión | Justificación |
|----------|----------|---------------|
| **P1: Conexión** | **Opción A - Online-Only** | Dark kitchen con WiFi estable, más simple para MVP |
| **P2: RLS** | **Sí - RLS completo** | Seguridad profesional desde inicio |
| **P3: Realtime** | **Sí - Implementar** | UX premium, sincronización instantánea |
| **P4: Migración** | **No aplica** | Proyecto nuevo, sin datos existentes |
| **P5: Tabla usuarios** | **Opción B - Tabla propia** | Más flexible, campos custom necesarios |
| **P6: Auditoría** | **Sí - Columnas completas** | created_at, updated_at, created_by en todas las tablas |

### Implicaciones Técnicas

**Online-Only**:
- ✅ Sin necesidad de mantener Room
- ✅ Reducción significativa de complejidad
- ⚠️ Agregar indicadores de "Sin conexión" en UI

**RLS Completo**:
- ✅ Políticas para todas las tablas sensibles
- ✅ Admin ve todo, Cajero filtrado automáticamente
- ⏱️ +1-2 días de desarrollo

**Realtime**:
- ✅ Inventario se actualiza instantáneamente entre dispositivos
- ✅ Cambios en menú visibles sin refresh
- ⏱️ +2-3 días de desarrollo
- 📦 Dependencia: `io.github.jan-tennert.supabase:realtime-kt`

**Auditoría**:
- ✅ Preparado para Fase 3 (Contabilidad)
- ✅ Trazabilidad completa
- 💾 +15-20% storage (aceptable)

---

## 9. Próximos Pasos

### Ahora (Definición de Requisitos)
1. ✅ Crear documento de contexto del proyecto → `PROYECTO-CONTEXTO.md`
2. ✅ Crear spec de migración con requisitos → Este archivo
3. ✅ **Usuario respondió preguntas P1-P6** → Decisiones confirmadas
4. ⏳ Crear sección de Diseño Técnico (siguiente)

### Después (Diseño e Implementación)
5. Crear sección de Diseño en este spec:
   - Schema PostgreSQL completo con auditoría
   - Políticas RLS para todas las tablas
   - Arquitectura de repositories con Realtime
   - Scripts SQL de creación
   - Mapeo Room → Supabase
6. Crear tareas de implementación con estimaciones
7. Ejecutar implementación (setup Supabase, migrar repositories, testing)
8. Actualizar `PROYECTO-CONTEXTO.md` al completar Fase 1

---

## 10. Diseño Técnico

### 10.1 Schema PostgreSQL con Auditoría

#### Columnas de Auditoría Estándar
Todas las tablas tendrán:
```sql
created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
```

#### Trigger para updated_at automático
```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

#### 10.1.1 Enumeraciones PostgreSQL

```sql
-- Roles de usuario
CREATE TYPE rol AS ENUM ('ADMIN', 'CAJERO');

-- Tipo de movimiento en sobres
CREATE TYPE tipo_movimiento AS ENUM ('INGRESO', 'EGRESO', 'TRANSFERENCIA');

-- Método de pago
CREATE TYPE metodo_pago AS ENUM ('EFECTIVO', 'DEBITO');

-- Estado de venta
CREATE TYPE estado_venta AS ENUM ('COMPLETADA', 'ANULADA');

-- Tipo de componente en recetas
CREATE TYPE tipo_componente AS ENUM ('INGREDIENTE', 'INSUMO');

-- Estado de comanda
CREATE TYPE estado_comanda AS ENUM ('PENDIENTE', 'ENTREGADA');

-- Zonas de envío (se maneja en app, no necesita enum DB)
-- Categorías de gasto (se maneja en app, no necesita enum DB)
```

---

#### 10.1.2 Tabla usuarios

```sql
CREATE TABLE usuarios (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    nombre TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    rol rol NOT NULL DEFAULT 'CAJERO',
    activo BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_rol ON usuarios(rol);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);

-- Trigger para updated_at
CREATE TRIGGER update_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.3 Tabla sobres

```sql
CREATE TABLE sobres (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT NOT NULL DEFAULT '',
    saldo NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (saldo >= 0),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_sobres_nombre ON sobres(nombre);

CREATE TRIGGER update_sobres_updated_at
    BEFORE UPDATE ON sobres
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.4 Tabla movimientos_sobre

```sql
CREATE TABLE movimientos_sobre (
    id SERIAL PRIMARY KEY,
    sobre_id INTEGER NOT NULL REFERENCES sobres(id) ON DELETE RESTRICT,
    tipo tipo_movimiento NOT NULL,
    monto NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    concepto TEXT NOT NULL,
    sobre_destino_id INTEGER REFERENCES sobres(id) ON DELETE SET NULL,
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    
    -- Constraint: transferencias deben tener sobre_destino_id
    CONSTRAINT chk_transferencia_destino CHECK (
        tipo != 'TRANSFERENCIA' OR sobre_destino_id IS NOT NULL
    )
);

CREATE INDEX idx_movimientos_sobre_sobre_id ON movimientos_sobre(sobre_id);
CREATE INDEX idx_movimientos_sobre_fecha ON movimientos_sobre(fecha);
CREATE INDEX idx_movimientos_sobre_tipo ON movimientos_sobre(tipo);

CREATE TRIGGER update_movimientos_sobre_updated_at
    BEFORE UPDATE ON movimientos_sobre
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.5 Tabla insumos

```sql
CREATE TABLE insumos (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT NOT NULL DEFAULT '',
    precio NUMERIC(12, 2) NOT NULL CHECK (precio >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    unidad_medida TEXT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_insumos_nombre ON insumos(nombre);
CREATE INDEX idx_insumos_activo ON insumos(activo);

CREATE TRIGGER update_insumos_updated_at
    BEFORE UPDATE ON insumos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.6 Tabla ingredientes

```sql
CREATE TABLE ingredientes (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    stock_gramos INTEGER NOT NULL DEFAULT 0 CHECK (stock_gramos >= 0),
    precio_gramo NUMERIC(12, 4) NOT NULL CHECK (precio_gramo >= 0),
    activo BOOLEAN NOT NULL DEFAULT true,
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_ingredientes_nombre ON ingredientes(nombre);
CREATE INDEX idx_ingredientes_activo ON ingredientes(activo);

CREATE TRIGGER update_ingredientes_updated_at
    BEFORE UPDATE ON ingredientes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.7 Tabla items_menu

```sql
CREATE TABLE items_menu (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    descripcion TEXT NOT NULL DEFAULT '',
    precio NUMERIC(12, 2) NOT NULL CHECK (precio >= 0),
    activo BOOLEAN NOT NULL DEFAULT true,
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_items_menu_nombre ON items_menu(nombre);
CREATE INDEX idx_items_menu_activo ON items_menu(activo);

CREATE TRIGGER update_items_menu_updated_at
    BEFORE UPDATE ON items_menu
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.8 Tabla recetas_menu

```sql
CREATE TABLE recetas_menu (
    id SERIAL PRIMARY KEY,
    item_menu_id INTEGER NOT NULL REFERENCES items_menu(id) ON DELETE CASCADE,
    componente_id INTEGER NOT NULL,
    tipo_componente tipo_componente NOT NULL,
    cantidad_gramos INTEGER NOT NULL CHECK (cantidad_gramos > 0),
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_recetas_menu_item_menu_id ON recetas_menu(item_menu_id);
CREATE INDEX idx_recetas_menu_componente ON recetas_menu(componente_id, tipo_componente);

CREATE TRIGGER update_recetas_menu_updated_at
    BEFORE UPDATE ON recetas_menu
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.9 Tabla salsas

```sql
CREATE TABLE salsas (
    id SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_salsas_nombre ON salsas(nombre);
CREATE INDEX idx_salsas_activo ON salsas(activo);

CREATE TRIGGER update_salsas_updated_at
    BEFORE UPDATE ON salsas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.10 Tabla ventas

```sql
CREATE TABLE ventas (
    id SERIAL PRIMARY KEY,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    metodo_pago metodo_pago NOT NULL,
    sobre_id INTEGER NOT NULL REFERENCES sobres(id) ON DELETE RESTRICT,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    estado estado_venta NOT NULL DEFAULT 'COMPLETADA',
    incluir_envio BOOLEAN NOT NULL DEFAULT false,
    monto_envio NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (monto_envio >= 0),
    stickers_enviados INTEGER NOT NULL DEFAULT 0 CHECK (stickers_enviados >= 0),
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_ventas_fecha ON ventas(fecha);
CREATE INDEX idx_ventas_sobre_id ON ventas(sobre_id);
CREATE INDEX idx_ventas_usuario_id ON ventas(usuario_id);
CREATE INDEX idx_ventas_estado ON ventas(estado);

CREATE TRIGGER update_ventas_updated_at
    BEFORE UPDATE ON ventas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.11 Tabla items_venta_menu

```sql
CREATE TABLE items_venta_menu (
    id SERIAL PRIMARY KEY,
    venta_id INTEGER NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    item_menu_id INTEGER NOT NULL REFERENCES items_menu(id) ON DELETE RESTRICT,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12, 2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_items_venta_menu_venta_id ON items_venta_menu(venta_id);
CREATE INDEX idx_items_venta_menu_item_menu_id ON items_venta_menu(item_menu_id);

CREATE TRIGGER update_items_venta_menu_updated_at
    BEFORE UPDATE ON items_venta_menu
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.12 Tabla comandas

```sql
CREATE TABLE comandas (
    id SERIAL PRIMARY KEY,
    venta_id INTEGER NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    detalle_texto TEXT NOT NULL,
    estado estado_comanda NOT NULL DEFAULT 'PENDIENTE',
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_comandas_venta_id ON comandas(venta_id);
CREATE INDEX idx_comandas_fecha ON comandas(fecha);
CREATE INDEX idx_comandas_estado ON comandas(estado);

CREATE TRIGGER update_comandas_updated_at
    BEFORE UPDATE ON comandas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.13 Tabla gastos

```sql
CREATE TABLE gastos (
    id BIGSERIAL PRIMARY KEY,
    descripcion TEXT NOT NULL,
    monto NUMERIC(12, 2) NOT NULL CHECK (monto > 0),
    categoria TEXT NOT NULL CHECK (categoria IN (
        'INSUMOS', 'SUELDOS', 'SERVICIOS', 'ARRIENDO', 
        'TRANSPORTE', 'ENVIOS', 'PACKAGING', 'OTROS'
    )),
    sobre_id INTEGER REFERENCES sobres(id) ON DELETE SET NULL,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),
    comprobante TEXT,
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_gastos_sobre_id ON gastos(sobre_id);
CREATE INDEX idx_gastos_usuario_id ON gastos(usuario_id);
CREATE INDEX idx_gastos_fecha ON gastos(fecha);
CREATE INDEX idx_gastos_categoria ON gastos(categoria);

CREATE TRIGGER update_gastos_updated_at
    BEFORE UPDATE ON gastos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

#### 10.1.14 Tabla presupuestos

```sql
CREATE TABLE presupuestos (
    id SERIAL PRIMARY KEY,
    mes INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    anio INTEGER NOT NULL CHECK (anio >= 2020),
    categoria_gasto TEXT NOT NULL CHECK (categoria_gasto IN (
        'INSUMOS', 'SUELDOS', 'SERVICIOS', 'ARRIENDO', 
        'TRANSPORTE', 'ENVIOS', 'PACKAGING', 'OTROS'
    )),
    monto_presupuestado NUMERIC(12, 2) NOT NULL CHECK (monto_presupuestado >= 0),
    
    -- Auditoría
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    
    -- Constraint: único presupuesto por mes/año/categoría
    UNIQUE(mes, anio, categoria_gasto)
);

CREATE INDEX idx_presupuestos_periodo ON presupuestos(anio, mes);
CREATE INDEX idx_presupuestos_categoria ON presupuestos(categoria_gasto);

CREATE TRIGGER update_presupuestos_updated_at
    BEFORE UPDATE ON presupuestos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

### 10.2 Políticas Row Level Security (RLS)

#### Helper Function: Obtener rol del usuario actual

```sql
CREATE OR REPLACE FUNCTION get_user_rol()
RETURNS rol AS $$
BEGIN
    RETURN (SELECT rol FROM usuarios WHERE id = auth.uid());
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

#### Habilitar RLS en todas las tablas

```sql
ALTER TABLE usuarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE sobres ENABLE ROW LEVEL SECURITY;
ALTER TABLE movimientos_sobre ENABLE ROW LEVEL SECURITY;
ALTER TABLE insumos ENABLE ROW LEVEL SECURITY;
ALTER TABLE ingredientes ENABLE ROW LEVEL SECURITY;
ALTER TABLE items_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE recetas_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE salsas ENABLE ROW LEVEL SECURITY;
ALTER TABLE ventas ENABLE ROW LEVEL SECURITY;
ALTER TABLE items_venta_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE comandas ENABLE ROW LEVEL SECURITY;
ALTER TABLE gastos ENABLE ROW LEVEL SECURITY;
ALTER TABLE presupuestos ENABLE ROW LEVEL SECURITY;
```

---

#### Políticas para tabla usuarios

```sql
-- Usuarios pueden ver su propio perfil
CREATE POLICY "Usuarios ven su propio perfil"
ON usuarios FOR SELECT
USING (id = auth.uid());

-- Admins pueden ver todos los usuarios
CREATE POLICY "Admins ven todos los usuarios"
ON usuarios FOR SELECT
USING (get_user_rol() = 'ADMIN');

-- Solo admins pueden crear usuarios
CREATE POLICY "Solo admins crean usuarios"
ON usuarios FOR INSERT
WITH CHECK (get_user_rol() = 'ADMIN');

-- Solo admins pueden actualizar usuarios
CREATE POLICY "Solo admins actualizan usuarios"
ON usuarios FOR UPDATE
USING (get_user_rol() = 'ADMIN');

-- Solo admins pueden eliminar usuarios
CREATE POLICY "Solo admins eliminan usuarios"
ON usuarios FOR DELETE
USING (get_user_rol() = 'ADMIN');
```

---

#### Políticas para sobres, movimientos, inventario (todos pueden ver/editar)

```sql
-- Sobres: todos pueden ver y editar
CREATE POLICY "Todos ven sobres"
ON sobres FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados crean sobres"
ON sobres FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Usuarios autenticados actualizan sobres"
ON sobres FOR UPDATE
USING (auth.uid() IS NOT NULL);

CREATE POLICY "Solo admins eliminan sobres"
ON sobres FOR DELETE
USING (get_user_rol() = 'ADMIN');

-- Movimientos: todos pueden ver y crear
CREATE POLICY "Todos ven movimientos"
ON movimientos_sobre FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados crean movimientos"
ON movimientos_sobre FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

-- Insumos/Ingredientes: todos pueden ver, solo admins editan
CREATE POLICY "Todos ven insumos"
ON insumos FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican insumos"
ON insumos FOR ALL
USING (get_user_rol() = 'ADMIN');

CREATE POLICY "Todos ven ingredientes"
ON ingredientes FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican ingredientes"
ON ingredientes FOR ALL
USING (get_user_rol() = 'ADMIN');
```

---

#### Políticas para menú (items_menu, recetas, salsas)

```sql
-- Items menú: todos ven, solo admins editan
CREATE POLICY "Todos ven items menú"
ON items_menu FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican items menú"
ON items_menu FOR ALL
USING (get_user_rol() = 'ADMIN');

-- Recetas: todos ven, solo admins editan
CREATE POLICY "Todos ven recetas"
ON recetas_menu FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican recetas"
ON recetas_menu FOR ALL
USING (get_user_rol() = 'ADMIN');

-- Salsas: todos ven, solo admins editan
CREATE POLICY "Todos ven salsas"
ON salsas FOR SELECT
USING (true);

CREATE POLICY "Solo admins modifican salsas"
ON salsas FOR ALL
USING (get_user_rol() = 'ADMIN');
```

---

#### Políticas para ventas y comandas

```sql
-- Ventas: todos ven y crean
CREATE POLICY "Todos ven ventas"
ON ventas FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados crean ventas"
ON ventas FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Solo admins actualizan ventas"
ON ventas FOR UPDATE
USING (get_user_rol() = 'ADMIN');

CREATE POLICY "Solo admins eliminan ventas"
ON ventas FOR DELETE
USING (get_user_rol() = 'ADMIN');

-- Items venta menú: heredan política de ventas
CREATE POLICY "Todos ven items venta"
ON items_venta_menu FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados crean items venta"
ON items_venta_menu FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

-- Comandas: todos ven y crean
CREATE POLICY "Todos ven comandas"
ON comandas FOR SELECT
USING (true);

CREATE POLICY "Usuarios autenticados gestionan comandas"
ON comandas FOR ALL
USING (auth.uid() IS NOT NULL);
```

---

#### Políticas para gastos (cajeros solo ven los suyos)

```sql
-- Admins ven todos los gastos
CREATE POLICY "Admins ven todos los gastos"
ON gastos FOR SELECT
USING (get_user_rol() = 'ADMIN');

-- Cajeros solo ven sus propios gastos
CREATE POLICY "Cajeros ven sus gastos"
ON gastos FOR SELECT
USING (get_user_rol() = 'CAJERO' AND usuario_id = auth.uid());

-- Usuarios autenticados crean gastos
CREATE POLICY "Usuarios autenticados crean gastos"
ON gastos FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

-- Solo admins actualizan/eliminan gastos
CREATE POLICY "Solo admins modifican gastos"
ON gastos FOR UPDATE
USING (get_user_rol() = 'ADMIN');

CREATE POLICY "Solo admins eliminan gastos"
ON gastos FOR DELETE
USING (get_user_rol() = 'ADMIN');
```

---

#### Políticas para presupuestos (solo admins)

```sql
CREATE POLICY "Solo admins gestionan presupuestos"
ON presupuestos FOR ALL
USING (get_user_rol() = 'ADMIN');
```

---

### 10.3 Configuración Realtime

#### Publicación para Realtime

```sql
-- Crear publicación para Realtime en tablas críticas
ALTER PUBLICATION supabase_realtime ADD TABLE insumos;
ALTER PUBLICATION supabase_realtime ADD TABLE ingredientes;
ALTER PUBLICATION supabase_realtime ADD TABLE items_menu;
ALTER PUBLICATION supabase_realtime ADD TABLE ventas;
ALTER PUBLICATION supabase_realtime ADD TABLE sobres;
ALTER PUBLICATION supabase_realtime ADD TABLE comandas;
```

**Tablas con Realtime habilitado**:
- ✅ **insumos/ingredientes**: Inventario actualizado en tiempo real
- ✅ **items_menu**: Cambios en menú visibles instantáneamente
- ✅ **ventas**: Ver ventas de otros cajeros al instante
- ✅ **sobres**: Saldos actualizados en vivo
- ✅ **comandas**: Cocina ve órdenes nuevas sin refresh

**Tablas sin Realtime** (no críticas para sincronización instantánea):
- gastos, presupuestos, recetas_menu, usuarios

---

### 10.4 Arquitectura Android con Supabase

#### 10.4.1 Dependencias (build.gradle.kts)

```kotlin
dependencies {
    // Existentes (mantener)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // ⚠️ REMOVER Room (ya no se usa)
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)
    // ksp(libs.androidx.room.compiler)
    
    // ✅ AGREGAR Supabase Kotlin SDK
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.4")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.4")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.4")
    implementation("io.ktor:ktor-client-android:2.3.7")
    
    // Apache POI (mantener para exportación)
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3") {
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "org.bouncycastle")
    }
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

---

#### 10.4.2 Configuración Supabase (BuildConfig)

**Opción A: En build.gradle.kts (no recomendado, expone keys)**
```kotlin
android {
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"https://tu-proyecto.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"tu-anon-key-aqui\"")
    }
}
```

**Opción B: En local.properties (RECOMENDADO)**
```properties
# local.properties (no se sube a git)
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-anon-key-aqui
```

```kotlin
// build.gradle.kts
val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

android {
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${properties.getProperty("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${properties.getProperty("SUPABASE_ANON_KEY")}\"")
    }
    buildFeatures {
        buildConfig = true
    }
}
```

---

#### 10.4.3 Cliente Supabase Singleton

**Archivo**: `app/src/main/java/com/toppis/app/data/supabase/SupabaseClient.kt`

```kotlin
package com.toppis.app.data.supabase

import com.toppis.erp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
```

---

#### 10.4.4 Modelos de Datos (Data Classes)

**Archivo**: `app/src/main/java/com/toppis/app/data/models/`

```kotlin
package com.toppis.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: String,  // UUID como String
    val nombre: String,
    val email: String,
    val rol: String,  // "ADMIN" o "CAJERO"
    val activo: Boolean,
    @SerialName("fecha_creacion")
    val fechaCreacion: String,  // ISO 8601 timestamp
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("created_by")
    val createdBy: String?
)

@Serializable
data class Sobre(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val saldo: Double,
    @SerialName("fecha_creacion")
    val fechaCreacion: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("created_by")
    val createdBy: String?
)

@Serializable
data class Venta(
    val id: Int,
    val fecha: String,
    val total: Double,
    @SerialName("metodo_pago")
    val metodoPago: String,
    @SerialName("sobre_id")
    val sobreId: Int,
    @SerialName("usuario_id")
    val usuarioId: String?,
    val estado: String,
    @SerialName("incluir_envio")
    val incluirEnvio: Boolean,
    @SerialName("monto_envio")
    val montoEnvio: Double,
    @SerialName("stickers_enviados")
    val stickersEnviados: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("created_by")
    val createdBy: String?
)

// ... (similar para todas las entidades)
```

**Nota**: Las enumeraciones se manejan como Strings en Kotlin y se validan en queries.

---

#### 10.4.5 Ejemplo Repository: SobreRepository

**Archivo**: `app/src/main/java/com/toppis/app/data/repository/SobreRepository.kt`

```kotlin
package com.toppis.app.data.repository

import com.toppis.app.data.models.Sobre
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SobreRepository {
    private val client = SupabaseClient.client
    
    // ✅ Obtener todos los sobres (Flow para Compose)
    suspend fun getAllSobres(): Flow<List<Sobre>> {
        val channel = client.channel("sobres-changes")
        
        // Subscribe a cambios en tiempo real
        val changesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "sobres"
        }
        
        // Mapear cambios a lista actualizada
        return changesFlow.map {
            client.from("sobres")
                .select()
                .decodeList<Sobre>()
        }
    }
    
    // ✅ Obtener sobre por ID
    suspend fun getSobreById(id: Int): Sobre? {
        return try {
            client.from("sobres")
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingleOrNull<Sobre>()
        } catch (e: Exception) {
            null
        }
    }
    
    // ✅ Crear sobre
    suspend fun insertSobre(sobre: Sobre): Sobre {
        return client.from("sobres")
            .insert(sobre)
            .decodeSingle<Sobre>()
    }
    
    // ✅ Actualizar sobre
    suspend fun updateSobre(sobre: Sobre): Sobre {
        return client.from("sobres")
            .update(sobre) {
                filter {
                    eq("id", sobre.id)
                }
            }
            .decodeSingle<Sobre>()
    }
    
    // ✅ Eliminar sobre
    suspend fun deleteSobre(id: Int) {
        client.from("sobres")
            .delete {
                filter {
                    eq("id", id)
                }
            }
    }
    
    // ✅ Actualizar saldo (transacción)
    suspend fun actualizarSaldo(sobreId: Int, nuevoSaldo: Double) {
        client.from("sobres")
            .update(mapOf("saldo" to nuevoSaldo)) {
                filter {
                    eq("id", sobreId)
                }
            }
    }
}
```

---

#### 10.4.6 AuthRepository con Supabase Auth

**Archivo**: `app/src/main/java/com/toppis/app/data/repository/AuthRepository.kt`

```kotlin
package com.toppis.app.data.repository

import com.toppis.app.data.models.Usuario
import com.toppis.app.data.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AuthRepository {
    private val client = SupabaseClient.client
    
    // ✅ Login
    suspend fun login(email: String, password: String): Result<Usuario> {
        return try {
            // Autenticar con Supabase Auth
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            // Obtener usuario de tabla usuarios
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("No se pudo obtener ID de usuario"))
            
            val usuario = client.from("usuarios")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<Usuario>()
                ?: return Result.failure(Exception("Usuario no encontrado en BD"))
            
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ✅ Logout
    suspend fun logout() {
        client.auth.signOut()
    }
    
    // ✅ Obtener usuario actual
    suspend fun getCurrentUser(): Usuario? {
        val userId = client.auth.currentUserOrNull()?.id ?: return null
        
        return client.from("usuarios")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull<Usuario>()
    }
    
    // ✅ Verificar si hay sesión activa
    fun isLoggedIn(): Boolean {
        return client.auth.currentUserOrNull() != null
    }
    
    // ✅ Crear usuario (solo Admin)
    suspend fun crearUsuario(
        email: String,
        password: String,
        nombre: String,
        rol: String
    ): Result<Usuario> {
        return try {
            // 1. Crear usuario en Supabase Auth
            val authResponse = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            
            val userId = authResponse.user?.id
                ?: return Result.failure(Exception("Error al crear usuario en Auth"))
            
            // 2. Crear registro en tabla usuarios
            val usuario = Usuario(
                id = userId,
                nombre = nombre,
                email = email,
                rol = rol,
                activo = true,
                fechaCreacion = System.currentTimeMillis().toString(),
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString(),
                createdBy = client.auth.currentUserOrNull()?.id
            )
            
            val createdUsuario = client.from("usuarios")
                .insert(usuario)
                .decodeSingle<Usuario>()
            
            Result.success(createdUsuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

#### 10.4.7 MainActivity - Manual DI Actualizado

**Cambios en**: `app/src/main/java/com/toppis/erp/MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⚠️ REMOVER: val database = AppDatabase.getDatabase(this)
        
        // ✅ Manual DI con Supabase Repositories
        val inventarioRepo = InventarioRepository()
        val sobreRepo = SobreRepository()
        val ventaRepo = VentaRepository()
        val menuRepo = MenuRepository()
        val comandaRepo = ComandaRepository()
        val gastoRepo = GastoRepository()
        val reporteRepo = ReporteRepository()
        val authRepo = AuthRepository()
        val flujoCajaRepo = FlujoCajaRepository()
        val dashboardRepo = DashboardRepository()
        
        // ViewModelFactories (sin cambios)
        val sobreFactory = SobreViewModelFactory(sobreRepo)
        val posFactory = PosViewModelFactory(ventaRepo, sobreRepo, menuRepo, comandaRepo)
        val inventarioFactory = InventarioViewModelFactory(inventarioRepo)
        val gastoFactory = GastoViewModelFactory(gastoRepo, sobreRepo)
        val reporteFactory = ReporteViewModelFactory(reporteRepo)
        val exportacionFactory = ExportacionViewModelFactory(/* pasar repos necesarios */)
        val flujoCajaFactory = FlujoCajaViewModelFactory(flujoCajaRepo)
        val dashboardFactory = DashboardViewModelFactory(dashboardRepo)
        val menuConfigFactory = MenuConfigViewModelFactory(menuRepo)
        val authFactory = AuthViewModelFactory(authRepo)
        
        val authViewModel = androidx.lifecycle.ViewModelProvider(this, authFactory)
            .get(com.toppis.app.ui.auth.AuthViewModel::class.java)

        enableEdgeToEdge()
        setContent {
            ToppisERPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavGraph(
                        sobreViewModelFactory = sobreFactory,
                        posViewModelFactory = posFactory,
                        inventarioViewModelFactory = inventarioFactory,
                        gastoViewModelFactory = gastoFactory,
                        reporteViewModelFactory = reporteFactory,
                        exportacionViewModelFactory = exportacionFactory,
                        flujoCajaViewModelFactory = flujoCajaFactory,
                        dashboardViewModelFactory = dashboardFactory,
                        menuConfigViewModelFactory = menuConfigFactory,
                        comandaRepository = comandaRepo,
                        authViewModel = authViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
```

---

### 10.5 Manejo de Realtime en ViewModels

**Ejemplo**: InventarioViewModel con actualizaciones en tiempo real

```kotlin
package com.toppis.app.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toppis.app.data.models.Insumo
import com.toppis.app.data.repository.InventarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InventarioViewModel(
    private val repository: InventarioRepository
) : ViewModel() {
    
    private val _insumos = MutableStateFlow<List<Insumo>>(emptyList())
    val insumos: StateFlow<List<Insumo>> = _insumos.asStateFlow()
    
    init {
        // Suscribirse a cambios en tiempo real
        viewModelScope.launch {
            repository.getAllInsumosRealtime().collect { lista ->
                _insumos.value = lista
            }
        }
    }
    
    fun crearInsumo(insumo: Insumo) {
        viewModelScope.launch {
            try {
                repository.insertInsumo(insumo)
                // No hace falta actualizar _insumos manualmente,
                // el Flow de realtime lo hace automáticamente
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }
}
```

---

### 10.6 Mapeo Room → Supabase

| Entidad Room | Tabla Supabase | Cambios Principales |
|--------------|----------------|---------------------|
| Usuario | usuarios | PK: Int → UUID, agregar auditoría |
| Sobre | sobres | Agregar auditoría |
| MovimientoSobre | movimientos_sobre | Agregar auditoría |
| Insumo | insumos | Agregar auditoría |
| Ingrediente | ingredientes | Agregar auditoría |
| Venta | ventas | usuarioId: Int? → UUID?, agregar auditoría |
| Gasto | gastos | usuarioId: Int? → UUID?, agregar auditoría |
| Presupuesto | presupuestos | Agregar auditoría |
| ItemMenu | items_menu | Agregar auditoría |
| RecetaMenu | recetas_menu | Agregar auditoría |
| Salsa | salsas | Agregar auditoría |
| Comanda | comandas | Agregar auditoría |
| ItemVentaMenu | items_venta_menu | Agregar auditoría |

**Campos auditoría agregados a todas**:
- `created_at TIMESTAMPTZ`
- `updated_at TIMESTAMPTZ`
- `created_by UUID REFERENCES usuarios(id)`

**Cambios en tipos**:
- `Long` (timestamps) → `TIMESTAMPTZ` (ISO 8601 strings en Kotlin)
- `Enum` → `TEXT` con CHECK constraints o PostgreSQL ENUMs
- `Int` PK (usuarios) → `UUID` (Supabase Auth)

---

### 10.7 Indicadores UI para Estado de Conexión

**Archivo**: `app/src/main/java/com/toppis/app/ui/components/ConnectionIndicator.kt`

```kotlin
package com.toppis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionIndicator(isOnline: Boolean) {
    if (!isOnline) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Sin conexión",
                    tint = MaterialTheme.colorScheme.onError
                )
                Text(
                    text = "Sin conexión a internet",
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```

**Uso en MainScaffold**:
```kotlin
@Composable
fun MainScaffold(...) {
    var isOnline by remember { mutableStateOf(true) }
    
    // Monitor de conectividad (implementar con ConnectivityManager)
    LaunchedEffect(Unit) {
        // Detectar cambios de conectividad
    }
    
    Scaffold(
        topBar = {
            Column {
                ConnectionIndicator(isOnline)
                ToppisTopBar(...)
            }
        },
        // ...
    )
}
```

---

## 12. Tareas de Implementación

### Fase A: Setup Supabase (2-3 días)

#### Tarea 1: Crear Proyecto Supabase
- **Descripción**: Crear proyecto en supabase.com y configurar región
- **Pasos**:
  1. Crear cuenta en [supabase.com](https://supabase.com)
  2. Crear proyecto "toppis-erp-prod"
  3. Elegir región: South America (São Paulo)
  4. Guardar database password en lugar seguro
  5. Copiar URL del proyecto y Anon Key
- **Dependencias**: Ninguna
- **Estimación**: 30 min
- **Estado**: ⏳ Pending

---

#### Tarea 2: Configurar Schema PostgreSQL
- **Descripción**: Ejecutar scripts SQL para crear tablas, enums, índices, triggers
- **Pasos**:
  1. Abrir SQL Editor en Supabase Dashboard
  2. Copiar/pegar script de enumeraciones (sección 10.1.1)
  3. Copiar/pegar script de trigger updated_at (sección 10.1)
  4. Ejecutar scripts de cada tabla (10.1.2 a 10.1.14)
  5. Verificar en Table Editor que todo se creó correctamente
- **Dependencias**: Tarea 1
- **Estimación**: 2 horas
- **Estado**: ⏳ Pending

**Archivo de referencia**: Crear `.kiro/database/supabase-schema.sql` con todos los scripts

---

#### Tarea 3: Configurar Row Level Security
- **Descripción**: Habilitar RLS y crear políticas para todas las tablas
- **Pasos**:
  1. Ejecutar script de helper function `get_user_rol()` (sección 10.2)
  2. Ejecutar comandos `ENABLE ROW LEVEL SECURITY` (sección 10.2)
  3. Ejecutar políticas para cada tabla (sección 10.2)
  4. Probar políticas con SQL Editor simulando diferentes roles
- **Dependencias**: Tarea 2
- **Estimación**: 3 horas
- **Estado**: ⏳ Pending

---

#### Tarea 4: Configurar Realtime
- **Descripción**: Habilitar publicación Realtime en tablas críticas
- **Pasos**:
  1. Ir a Database → Replication en Supabase Dashboard
  2. Habilitar replicación para: insumos, ingredientes, items_menu, ventas, sobres, comandas
  3. Ejecutar comandos `ALTER PUBLICATION` (sección 10.3)
  4. Verificar que aparecen en Realtime Inspector
- **Dependencias**: Tarea 2
- **Estimación**: 1 hora
- **Estado**: ⏳ Pending

---

### Fase B: Configuración Android (1 día)

#### Tarea 5: Agregar Dependencias Supabase
- **Descripción**: Actualizar build.gradle.kts con Supabase SDK
- **Pasos**:
  1. Comentar dependencias Room en `app/build.gradle.kts`
  2. Agregar dependencias Supabase (sección 10.4.1)
  3. Sync proyecto
  4. Resolver conflictos de versiones si aparecen
- **Dependencias**: Ninguna
- **Estimación**: 30 min
- **Estado**: ⏳ Pending

---

#### Tarea 6: Configurar BuildConfig con API Keys
- **Descripción**: Configurar URL y Anon Key de Supabase de forma segura
- **Pasos**:
  1. Agregar `SUPABASE_URL` y `SUPABASE_ANON_KEY` en `local.properties`
  2. Modificar `build.gradle.kts` para leer desde local.properties (sección 10.4.2)
  3. Habilitar `buildConfig = true`
  4. Sync proyecto
  5. Verificar que BuildConfig.SUPABASE_URL es accesible
- **Dependencias**: Tarea 1, Tarea 5
- **Estimación**: 1 hora
- **Estado**: ⏳ Pending

---

#### Tarea 7: Crear SupabaseClient Singleton
- **Descripción**: Crear cliente Supabase centralizado
- **Pasos**:
  1. Crear archivo `data/supabase/SupabaseClient.kt`
  2. Implementar singleton con Auth, Postgrest, Realtime (sección 10.4.3)
  3. Verificar que compila sin errores
- **Dependencias**: Tarea 6
- **Estimación**: 30 min
- **Estado**: ⏳ Pending

---

### Fase C: Modelos de Datos (2-3 días)

#### Tarea 8: Crear Data Classes Supabase
- **Descripción**: Crear modelos serializables para todas las entidades
- **Pasos**:
  1. Crear carpeta `data/models/`
  2. Crear data class para cada entidad (13 total)
  3. Agregar anotaciones `@Serializable` y `@SerialName`
  4. Mapear tipos: Int → String (UUID), Long → String (ISO 8601)
  5. Verificar compilación
- **Dependencias**: Tarea 7
- **Estimación**: 3 horas
- **Estado**: ⏳ Pending

**Entidades**:
- Usuario, Sobre, MovimientoSobre, Insumo, Ingrediente, Venta, Gasto, Presupuesto, ItemMenu, RecetaMenu, Salsa, Comanda, ItemVentaMenu

---

### Fase D: Repositories (5-7 días)

#### Tarea 9: Refactorizar AuthRepository
- **Descripción**: Migrar AuthRepository a Supabase Auth
- **Estado**: ✅ Completed (2026-06-09)
- **Resultado**: Login, logout, getCurrentUser y registrarUsuario funcionando contra Supabase. Verificado end-to-end: login del admin + creación de usuario desde la app.
- **Notas de implementación**:
  - Se usó la nueva publishable key (`sb_publishable_...`) — compatible con supabase-kt 3.1.4
  - Primer admin creado manualmente (Auth + INSERT en tabla usuarios)
  - `login()` retorna `Result<Usuario>` para surface de errores reales en UI
  - Permiso `INTERNET` agregado al AndroidManifest
  - **Lección aprendida**: hubo un typo en la URL del proyecto (i vs j); verificar siempre con nslookup

---

#### Tarea 10: Refactorizar SobreRepository
- **Descripción**: Migrar SobreRepository a Supabase con Realtime
- **Estado**: ✅ Completed (2026-06-09)
- **Resultado**: CRUD de sobres funcionando. Lectura con Realtime. Transferencia atómica vía función RPC `transferir_entre_sobres`.
- **Notas de implementación**:
  - Tabla `movimientos_sobre` recreada con modelo real (origen_id/destino_id) — ver `supabase-fix-movimientos.sql`
  - Función PostgreSQL `transferir_entre_sobres` para atomicidad (SECURITY DEFINER)
  - El bloque Realtime se envolvió en try/catch para que la lista inicial cargue aunque Realtime falle
  - Modelos creados: `data/models/Sobre.kt`, `data/models/MovimientoSobre.kt`
  - Efecto colateral: PosViewModel/GastoViewModel ahora usan `models.Sobre` (quedan parcialmente funcionales hasta migrar esos módulos)

---

#### Tarea 11: Refactorizar InventarioRepository
- **Descripción**: Migrar gestión de insumos e ingredientes a Supabase
- **Estado**: ✅ Completed (2026-06-09)
- **Resultado**: CRUD de insumos e ingredientes con Realtime en ambas tablas.
- **Notas de implementación**:
  - Tabla `ingredientes` recreada — el schema inicial tenía campos equivocados (stock_gramos/precio_gramo) vs el modelo real (costo_unitario, costo_compra, porcentaje_merma, unidad_compra, cantidad_aprovechable, costo_gramo). Ver `supabase-fix-ingredientes.sql`
  - Modelos: `data/models/Insumo.kt`, `data/models/Ingrediente.kt`
  - Patrón consolidado: getX() suspend + observeX() Realtime + refresco tras operaciones
  - **Lección**: el engine HTTP debe ser `ktor-client-okhttp` (no `android`) para que Realtime (WebSocket) funcione — aplicado globalmente

---

#### Tarea 12: Refactorizar VentaRepository
- **Descripción**: Migrar lógica de ventas a Supabase (la más compleja)
- **Estado**: ✅ Completed (2026-06-09)
- **Resultado**: Venta atómica vía función RPC `registrar_venta_menu` (venta + items + descuento stock + saldo sobre + movimiento + comanda). Rollback total si falta stock.
- **Notas de implementación**:
  - Función PostgreSQL `registrar_venta_menu` (SECURITY DEFINER) — ver `supabase-venta-rpc.sql`
  - Columna `salsas_seleccionadas` agregada a items_venta_menu
  - Modelos: `Venta`, `ItemVentaMenu`, `Comanda`
  - ComandaRepository migrado: consultas + cambio estado + constructores de texto (comanda/WhatsApp) puros
  - PosViewModel construye textos desde el carrito; VentaExitosa carga los textos
  - Venta atribuida al usuario logueado (usuarioActual.id)
  - Se removió la dependencia de ComandaRepository en PosScreen (textos vienen del estado)

---

#### Tarea 13: Refactorizar GastoRepository
- **Descripción**: Migrar gestión de gastos a Supabase con RLS
- **Pasos**:
  1. Implementar CRUD para gastos
  2. Verificar que RLS filtra correctamente (Cajero solo ve los suyos)
  3. Implementar descuento de saldo en sobre al crear gasto
  4. Probar con usuario Admin y Cajero
- **Dependencias**: Tarea 10
- **Estimación**: 4 horas
- **Estado**: ⏳ Pending

---

#### Tarea 14: Refactorizar MenuRepository
- **Descripción**: Migrar configuración de menú a Supabase
- **Estado**: ✅ Completed (2026-06-09)
- **Resultado**: CRUD de items_menu, recetas_menu y salsas en Supabase. POS lee el menú desde Supabase con Realtime.
- **Notas de implementación**:
  - Tablas `salsas` (agregado descripcion + activa) y `recetas_menu` (cantidad decimal en vez de cantidad_gramos) corregidas — ver `supabase-fix-menu.sql`
  - Modelos: `ItemMenu`, `Salsa`, `RecetaMenu`
  - MenuConfigViewModel y PosViewModel migrados al patrón refresco + observe Realtime
  - Pendiente: registro de venta (VentaRepository/ComandaRepository) sigue en Room hasta migrar Ventas

---

#### Tarea 15: Refactorizar ComandaRepository
- **Descripción**: Migrar gestión de comandas a Supabase
- **Pasos**:
  1. Implementar CRUD para comandas
  2. Habilitar Realtime (cocina ve órdenes nuevas instantáneamente)
  3. Implementar cambio de estado PENDIENTE → ENTREGADA
  4. Probar flujo: venta → comanda generada → cambio estado
- **Dependencias**: Tarea 12
- **Estimación**: 3 horas
- **Estado**: ⏳ Pending

---

#### Tarea 16: Refactorizar ReporteRepository
- **Descripción**: Migrar queries de reportes a Supabase
- **Pasos**:
  1. Implementar query de ventas por rango de fechas
  2. Implementar query de gastos por rango de fechas
  3. Implementar agregaciones (SUM, COUNT, AVG)
  4. Probar desde ReportesScreen
- **Dependencias**: Tarea 12, Tarea 13
- **Estimación**: 4 horas
- **Estado**: ⏳ Pending

---

#### Tarea 17: Refactorizar FlujoCajaRepository
- **Descripción**: Migrar lógica de flujo de caja a Supabase
- **Pasos**:
  1. Implementar queries de proyecciones
  2. Implementar CRUD de presupuestos
  3. Implementar comparación ventas vs presupuestos
  4. Probar desde FlujoCajaScreen
- **Dependencias**: Tarea 13
- **Estimación**: 5 horas
- **Estado**: ⏳ Pending

---

#### Tarea 18: Refactorizar DashboardRepository
- **Descripción**: Migrar queries de métricas a Supabase
- **Pasos**:
  1. Implementar query de ventas del día/mes
  2. Implementar query de gastos del día/mes
  3. Implementar query de saldo total de sobres
  4. Habilitar Realtime en dashboard (métricas actualizadas en vivo)
  5. Probar desde DashboardScreen
- **Dependencias**: Tarea 12, Tarea 13
- **Estimación**: 4 horas
- **Estado**: ⏳ Pending

---

### Fase E: ViewModels y UI (2-3 días)

#### Tarea 19: Actualizar ViewModels para Realtime
- **Descripción**: Modificar ViewModels para consumir Flows de Realtime
- **Pasos**:
  1. Actualizar InventarioViewModel (ejemplo sección 10.5)
  2. Actualizar SobreViewModel
  3. Actualizar PosViewModel
  4. Actualizar DashboardViewModel
  5. Eliminar refreshes manuales (ya no son necesarios)
- **Dependencias**: Tareas 9-18
- **Estimación**: 4 horas
- **Estado**: ⏳ Pending

---

#### Tarea 20: Agregar Indicador de Conexión
- **Descripción**: Mostrar banner cuando no hay internet
- **Pasos**:
  1. Crear composable ConnectionIndicator (sección 10.7)
  2. Implementar monitoreo de conectividad con ConnectivityManager
  3. Agregar indicator en MainScaffold
  4. Probar desactivando WiFi
- **Dependencias**: Ninguna (paralela)
- **Estimación**: 2 horas
- **Estado**: ⏳ Pending

---

#### Tarea 21: Actualizar MainActivity (Manual DI)
- **Descripción**: Reemplazar inicialización Room por Supabase Repositories
- **Pasos**:
  1. Eliminar `AppDatabase.getDatabase(this)`
  2. Instanciar repositories Supabase (sección 10.4.7)
  3. Mantener factories sin cambios
  4. Verificar que compila
- **Dependencias**: Tareas 9-18
- **Estimación**: 1 hora
- **Estado**: ⏳ Pending

---

### Fase F: Testing y Validación (3-4 días)

#### Tarea 22: Testing CRUD Básico
- **Descripción**: Verificar operaciones CRUD en todas las entidades
- **Casos de prueba**:
  - [ ] Crear, leer, actualizar, eliminar sobres
  - [ ] Crear, leer, actualizar, eliminar insumos
  - [ ] Crear, leer, actualizar, eliminar ingredientes
  - [ ] Crear, leer, actualizar, eliminar items menú
  - [ ] Crear, leer, actualizar, eliminar gastos
  - [ ] Crear, leer, actualizar, eliminar presupuestos
- **Dependencias**: Tarea 21
- **Estimación**: 4 horas
- **Estado**: ⏳ Pending

---

#### Tarea 23: Testing Flujo de Venta Completo
- **Descripción**: Probar flujo end-to-end de venta
- **Casos de prueba**:
  - [ ] Login como Cajero
  - [ ] Abrir POS
  - [ ] Agregar items al carrito
  - [ ] Seleccionar sobre y método de pago
  - [ ] Completar venta
  - [ ] Verificar:
    - [x] Venta guardada en DB
    - [x] Saldo de sobre actualizado
    - [x] Stock descontado correctamente
    - [x] Comanda generada
    - [x] Dashboard actualizado en tiempo real
- **Dependencias**: Tarea 21
- **Estimación**: 2 horas
- **Estado**: ⏳ Pending

---

#### Tarea 24: Testing RLS (Permisos)
- **Descripción**: Verificar políticas RLS funcionan correctamente
- **Casos de prueba**:
  - [ ] Login como Cajero → solo ve sus propios gastos
  - [ ] Login como Admin → ve todos los gastos
  - [ ] Cajero intenta editar inventario → debe fallar
  - [ ] Admin edita inventario → debe funcionar
  - [ ] Cajero intenta ver presupuestos → debe fallar
- **Dependencias**: Tarea 21
- **Estimación**: 2 horas
- **Estado**: ⏳ Pending

---

#### Tarea 25: Testing Realtime
- **Descripción**: Verificar sincronización en tiempo real
- **Casos de prueba**:
  - [ ] Abrir app en 2 dispositivos/emuladores
  - [ ] Dispositivo A: crear venta
  - [ ] Dispositivo B: ver inventario actualizado instantáneamente
  - [ ] Dispositivo A: cambiar precio de item menú
  - [ ] Dispositivo B: ver cambio en POS sin refresh
  - [ ] Dispositivo A: crear gasto
  - [ ] Dispositivo B: ver dashboard actualizado
- **Dependencias**: Tarea 21
- **Estimación**: 3 horas
- **Estado**: ⏳ Pending

---

#### Tarea 26: Testing Sin Conexión
- **Descripción**: Verificar comportamiento cuando no hay internet
- **Casos de prueba**:
  - [ ] Desactivar WiFi
  - [ ] App muestra banner "Sin conexión"
  - [ ] Intentar crear venta → debe fallar con error claro
  - [ ] Reactivar WiFi
  - [ ] Banner desaparece
  - [ ] App funciona normalmente
- **Dependencias**: Tarea 20, Tarea 21
- **Estimación**: 1 hora
- **Estado**: ⏳ Pending

---

#### Tarea 27: Testing Exportación
- **Descripción**: Verificar que exportación Excel sigue funcionando
- **Casos de prueba**:
  - [ ] Login como Admin
  - [ ] Ir a Exportación
  - [ ] Exportar ventas a Excel
  - [ ] Exportar gastos a CSV
  - [ ] Exportar todo a ZIP
  - [ ] Verificar archivos se descargan correctamente
- **Dependencias**: Tarea 21
- **Estimación**: 2 horas
- **Estado**: ⏳ Pending

---

### Fase G: Documentación y Limpieza (1 día)

#### Tarea 28: Documentar Setup de Supabase
- **Descripción**: Crear guía para configurar proyecto desde cero
- **Archivo**: `.kiro/docs/SUPABASE_SETUP.md`
- **Contenido**:
  - Cómo crear proyecto Supabase
  - Cómo ejecutar scripts SQL
  - Cómo obtener API keys
  - Cómo configurar local.properties
  - Troubleshooting común
- **Dependencias**: Todas las tareas completadas
- **Estimación**: 2 horas
- **Estado**: ⏳ Pending

---

#### Tarea 29: Limpiar Código Room
- **Descripción**: Eliminar archivos obsoletos de Room
- **Estado**: ✅ Completed (2026-06-09)
- **Resultado**: Eliminados AppDatabase, 13 DAOs y 13 entidades Room. Se conservó `entities/Enums.kt` (los modelos Supabase usan los enums). Removidas dependencias Room y plugin KSP del Gradle. App compila y empaqueta sin Room.

---

#### Tarea 30: Actualizar PROYECTO-CONTEXTO.md
- **Descripción**: Actualizar documento de contexto con estado post-migración
- **Cambios**:
  - Stack: Room → Supabase PostgreSQL
  - Autenticación: Local → Supabase Auth
  - Realtime: ✅ Implementado
  - RLS: ✅ Implementado
  - Actualizar roadmap: Fase 1 → Completada
- **Dependencias**: Todas las tareas completadas
- **Estimación**: 1 hora
- **Estado**: ⏳ Pending

---

## 13. Estimación Total

| Fase | Tiempo Estimado |
|------|----------------|
| A: Setup Supabase | 2-3 días |
| B: Configuración Android | 1 día |
| C: Modelos de Datos | 2-3 días |
| D: Repositories | 5-7 días |
| E: ViewModels y UI | 2-3 días |
| F: Testing y Validación | 3-4 días |
| G: Documentación y Limpieza | 1 día |
| **TOTAL** | **16-24 días (2-3 semanas)** |

**Nota**: Estimaciones asumen trabajo a tiempo completo. Ajustar según disponibilidad real.

---

## 14. Referencias

### Supabase
- [Documentación oficial](https://supabase.com/docs)
- [Supabase Kotlin SDK GitHub](https://github.com/supabase-community/supabase-kt)
- [Row Level Security Guide](https://supabase.com/docs/guides/auth/row-level-security)
- [PostgreSQL Enums](https://www.postgresql.org/docs/current/datatype-enum.html)

### Room Migration Guides
- [Exporting Room Schema](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schema)
- [Room to Cloud Migration Strategies](https://developer.android.com/topic/architecture/data-layer/offline-first)

### Arquitectura
- [Offline-First Architecture](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Repository Pattern](https://developer.android.com/codelabs/basic-android-kotlin-compose-add-repository)

---

**Fecha de Creación**: 2026-06-08  
**Autor**: Kiro + andreslh  
**Estado**: ✅ Design Complete - Requisitos y diseño técnico finalizados  
**Decisiones confirmadas**: Online-only, RLS completo, Realtime habilitado, Tabla usuarios propia, Auditoría completa  
**Próximo paso**: Crear proyecto Supabase y comenzar Fase A (Setup)  
**Última Actualización**: 2026-06-08
