# ToppisERP

ERP móvil en la nube para **dark kitchens**: punto de venta, inventario, control de caja, gastos y reportes, todo sincronizado en tiempo real.

Construido con **Jetpack Compose** y **Supabase** (PostgreSQL en la nube).

---

## ✨ Características

- 🛒 **Punto de Venta (POS)** — venta con carrito, salsas, envíos y generación de comanda
- 📦 **Inventario** — insumos e ingredientes con cálculo de merma y costo por gramo
- 💰 **Sobres** — múltiples cajas de dinero con transferencias atómicas
- 💵 **Gastos** — registro por categoría con descuento automático de saldo
- 🍽️ **Menú y recetas** — items con receta (ingredientes, insumos y salsas en gramos)
- 📊 **Reportes** — ventas vs gastos por período, desglose por categoría
- 📈 **Dashboard** — KPIs, serie temporal y distribución de egresos
- 💹 **Flujo de caja** — proyecciones y presupuesto vs real
- 📤 **Exportación** — Excel / CSV / ZIP
- 👥 **Usuarios y roles** — Admin y Cajero con permisos diferenciados
- ⚡ **Tiempo real** — los cambios se reflejan al instante entre dispositivos
- 🔒 **Seguridad** — Row Level Security (RLS) a nivel de base de datos

---

## 🧱 Stack Tecnológico

| Capa | Tecnología |
|------|------------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Repository (DI manual) |
| Navegación | Navigation Compose |
| Backend | Supabase (PostgreSQL, Auth, Realtime) |
| Cliente | supabase-kt + Ktor (OkHttp) |
| Exportación | Apache POI |

**Operaciones críticas atómicas** (ventas, gastos, transferencias) se ejecutan vía **funciones RPC en PostgreSQL**, garantizando consistencia: si algo falla, se revierte todo (nunca se pierde dinero ni stock).

---

## 📋 Requisitos

- Android Studio (versión reciente)
- JDK 11+
- Un proyecto en [Supabase](https://supabase.com) (free tier es suficiente)
- Android API 26+ (Android 8.0)

---

## 🚀 Configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/raytan111/ToppisERP.git
cd ToppisERP
```

### 2. Crear el proyecto en Supabase

1. Crea un proyecto en [supabase.com](https://supabase.com)
2. En el **SQL Editor**, ejecuta los scripts de `.kiro/database/` en este orden:
   1. `supabase-schema.sql` — tablas, enums y triggers
   2. `supabase-rls.sql` — políticas de seguridad (RLS)
   3. `supabase-realtime.sql` — sincronización en tiempo real
   4. `supabase-fix-movimientos.sql` — tabla de movimientos + función de transferencia
   5. `supabase-fix-ingredientes.sql` — corrección de ingredientes
   6. `supabase-add-cantidad-comprada.sql`
   7. `supabase-fix-menu.sql` — salsas y recetas
   8. `supabase-add-salsa-componente.sql`
   9. `supabase-venta-rpc.sql` — función de venta atómica
   10. `supabase-gasto-rpc.sql` — función de gasto atómica
   11. `supabase-fix-usuarios-insert.sql` — política de creación de usuarios

### 3. Configurar credenciales

Crea (o edita) el archivo `local.properties` en la raíz del proyecto y agrega:

```properties
SUPABASE_URL=https://TU-PROYECTO.supabase.co
SUPABASE_ANON_KEY=tu-anon-key
```

> 🔑 Obtén la URL y la anon key en Supabase → **Settings → API**.
> Este archivo está en `.gitignore` y **no se sube** al repositorio.

### 4. Crear el primer administrador

1. En Supabase → **Authentication → Users → Add user** (marca *Auto Confirm User*)
2. Copia el **User UID** y ejecuta en el SQL Editor:

```sql
INSERT INTO usuarios (id, nombre, email, rol, activo)
VALUES ('UID-COPIADO', 'Administrador', 'admin@tucorreo.com', 'ADMIN', true);
```

### 5. Compilar y correr

Abre el proyecto en Android Studio y presiona **Run**, o desde terminal:

```bash
./gradlew assembleDebug
```

---

## 📁 Estructura del Proyecto

```
app/src/main/java/com/toppis/app/
├── data/
│   ├── models/        # Modelos serializables (Supabase)
│   ├── repository/    # Repositorios (acceso a datos + RPC)
│   ├── supabase/      # Cliente Supabase centralizado
│   ├── util/          # Utilidades (fechas, etc.)
│   └── db/entities/   # Enumeraciones compartidas
└── ui/
    ├── auth/ · dashboard/ · pos/ · sobres/ · inventario/
    ├── menu/ · gastos/ · reportes/ · flujo/ · exportacion/
    ├── components/    # MainScaffold, TopBar
    └── navigation/    # NavGraph centralizado

.kiro/
├── database/          # Scripts SQL de Supabase
├── specs/             # Documentos de diseño
└── PROYECTO-CONTEXTO.md
```

---

## 🗺️ Roadmap

- [x] **Fase 1** — Migración a la nube (Supabase) ✅
- [ ] **Fase 2** — Boletas electrónicas (integración SII Chile)
- [ ] **Fase 3** — Módulo de contabilidad e impuestos
- [ ] **Fase 4** — Arquitectura multi-app
- [ ] **Fase 5** — Preparación para IA

---

## 📄 Licencia

Proyecto privado. Todos los derechos reservados.

---

Hecho con ❤️ para dark kitchens en Chile 🇨🇱
