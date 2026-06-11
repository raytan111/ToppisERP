# ToppisERP - Scripts de Base de Datos Supabase

## ✅ Proyecto Configurado

- **Project URL**: `https://dkgqrbxizegipxdsypzf.supabase.co`
- **Region**: Americas
- **Fecha**: 2026-06-08

---

## 📄 Archivos

1. **`supabase-schema.sql`** - Schema completo (enums, triggers, 13 tablas con auditoría)
2. **`supabase-rls.sql`** - Políticas Row Level Security (seguridad por rol)
3. **`supabase-realtime.sql`** - Configuración de actualizaciones en tiempo real
4. **`README.md`** - Este archivo (instrucciones)

---

## 🚀 Guía de Instalación Rápida

### Paso 1: Abrir SQL Editor

1. Ve a [https://dkgqrbxizegipxdsypzf.supabase.co](https://dkgqrbxizegipxdsypzf.supabase.co)
2. En el menú lateral, busca el ícono `</>` o "SQL Editor"
3. Click en "New query"

### Paso 2: Ejecutar Scripts en Orden

#### 🔹 Script 1: Schema (tablas, enums, triggers)

1. Abre el archivo `.kiro/database/supabase-schema.sql`
2. **Copia TODO el contenido** del archivo
3. Pega en el SQL Editor de Supabase
4. Click en **"Run"** (botón verde)
5. ⏳ Espera 10-20 segundos (es un script largo)
6. ✅ Deberías ver: "Success. No rows returned"

**¿Qué hace este script?**
- Crea 6 enumeraciones (rol, tipo_movimiento, metodo_pago, etc.)
- Crea trigger para updated_at automático
- Crea 13 tablas con columnas de auditoría
- Crea todos los índices y foreign keys

#### 🔹 Script 2: RLS (seguridad)

1. Abre el archivo `.kiro/database/supabase-rls.sql`
2. **Copia TODO el contenido**
3. Pega en un nuevo SQL Query
4. Click en **"Run"**
5. ✅ Deberías ver: "Success. No rows returned"

**¿Qué hace este script?**
- Crea función helper `get_user_rol()`
- Habilita RLS en todas las tablas
- Crea políticas de seguridad:
  - Admins ven todo
  - Cajeros solo ven sus propios gastos
  - Solo admins pueden modificar inventario/menú/presupuestos

#### 🔹 Script 3: Realtime

1. Abre el archivo `.kiro/database/supabase-realtime.sql`
2. **Copia TODO el contenido**
3. Pega en un nuevo SQL Query
4. Click en **"Run"**
5. ✅ Al final debería mostrar una tabla con 6 filas:
   - comandas
   - insumos
   - ingredientes
   - items_menu
   - sobres
   - ventas

**¿Qué hace este script?**
- Habilita sincronización en tiempo real en 6 tablas críticas
- Permite que cambios en una pantalla se vean instantáneamente en otras

### Paso 3: Verificar Instalación

#### Verificar Tablas Creadas

En SQL Editor, ejecuta:

```sql
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public'
ORDER BY table_name;
```

✅ Deberías ver **13 tablas**:
- comandas, gastos, ingredientes, insumos, items_menu
- items_venta_menu, movimientos_sobre, presupuestos
- recetas_menu, salsas, sobres, usuarios, ventas

#### Verificar RLS Habilitado

```sql
SELECT tablename, rowsecurity
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
```

✅ Todas las tablas deben tener `rowsecurity = true`

#### Verificar Realtime

```sql
SELECT tablename 
FROM pg_publication_tables 
WHERE pubname = 'supabase_realtime'
ORDER BY tablename;
```

✅ Deberías ver **6 tablas** con realtime habilitado

---

## ✅ Siguiente Paso: Configurar Android

Una vez completados los 3 scripts, tu base de datos está lista. 

**Archivos ya configurados**:
- ✅ `/Users/andreslh/Documents/ToppisERP/local.properties` (con tus API keys)

**Próximo paso**: Seguir **Tarea 5** del spec de migración:
- Agregar dependencias Supabase al `build.gradle.kts`
- Ver sección 10.4.1 del spec: `.kiro/specs/migracion-supabase.md`

---

## 🆘 Troubleshooting

### Error: "type X already exists"
**Solución**: Ya ejecutaste el script antes. Puedes:
- Ignorar el error y continuar
- O ejecutar `DROP TYPE nombre_tipo CASCADE;` antes de recrear

### Error: "relation auth.users does not exist"
**Solución**: Espera 1-2 minutos. Supabase crea `auth.users` automáticamente al crear el proyecto.

### Error: "permission denied"
**Solución**: Asegúrate de estar conectado como postgres user (no como anon). En SQL Editor, verifica en la esquina superior derecha.

### RLS bloquea todas las queries
**Causa**: Aún no has creado ningún usuario en la tabla `usuarios`.  
**Solución**: Primero debes registrar un usuario desde la app Android (después de la migración completa) o temporalmente deshabilitar RLS:

```sql
-- SOLO PARA DEBUG - NO DEJAR EN PRODUCCIÓN
ALTER TABLE usuarios DISABLE ROW LEVEL SECURITY;
```

---

**Última actualización**: 2026-06-08  
**Autor**: Kiro + andreslh
**Última actualización**: 2026-06-08  
**Autor**: Kiro + andreslh
