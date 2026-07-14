# ToppisERP

ERP móvil en la nube para una **hamburguesería / dark kitchen**: punto de venta, food cost, inventario profesional, compras, control de caja, mano de obra, multi-local y KPIs ejecutivos — todo sincronizado en tiempo real.

Construido con **Jetpack Compose** y **Supabase** (PostgreSQL en la nube). Español chileno, CLP.

---

## ✨ Características

- 🛒 **Punto de Venta (POS)** — **múltiples pedidos en paralelo** (carritos por cliente, persistidos y en tiempo real), catálogo con imágenes y pestañas Menú/Promos, popup protegido de modificadores por categoría + comentarios, **promos configurables** (el cliente elige por espacio), estados abierto/cerrado/pagado/entregado con aviso de deuda, **pantalla de cocina (KDS)**, **clientes + cuponera** y envío por zonas.
- 🍔 **Cocina / Food Cost** — artículos unificados, preparaciones (sub-recetas), recetas de menú, food cost % por plato y menu engineering.
- 📦 **Inventario Pro** — stock en unidad base, mermas (waste log), conteos, compra sugerida y análisis de variación.
- 🚚 **Compras** — proveedores y recepción con costo promedio ponderado y caducidad por lote.
- 💰 **Fondos** — sobres (cuenta = dinero real / fondo = provisión), costos puntuales, **arqueo de caja**, flujo de caja y contabilidad (IVA).
- 📊 **Control de Costos** — semana operativa lunes–sábado, costo por **último precio** (histórico congelado), costos fijos prorrateados + variables, **resultado semanal** ("lo que queda") con semáforos, break-even y mano de obra disponible, objetivos configurables y **rutina de cierre** guiada (conteo → mermas → provisión de fijos → resultado).
- 👥 **Personal** — empleados (sueldo fijo/turno/hora), jornadas, propinas y **Prime Cost**.
- 🏪 **Multi-local** — locales, asignación de usuarios y reportes por local.
- 📈 **KPIs Ejecutivos** — ventas, ticket, food/labor/prime cost %, merma, alertas y **delivery por mes/día**.
- 👤 **Usuarios y roles** — ADMIN, ADMIN_LOCAL, SUPERVISOR, CAJERO con permisos y alcance por local. Login por **nombre de usuario**.
- 🎨 **Tema de marca configurable** — color de la empresa por código hex; modo claro/oscuro.
- ✨ **Diseño de alto impacto** — splash con logo, ícono de app propio, Login con **moneda 3D dorada giratoria**, Home con hero POS en gradiente y tarjetas con acento por categoría, transiciones de navegación, **buscadores**, **skeletons de carga** y **estados vacíos** en las listas, y **diálogos unificados**.
- 📤 **Exportación** — Excel / CSV / ZIP.
- ⚡ **Tiempo real** + 🔒 **Row Level Security (RLS)**.

---

## 🧱 Stack Tecnológico

| Capa | Tecnología |
|------|------------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Repository (DI manual) |
| Navegación | Navigation Compose (un NavHost, menú por categorías, sin bottom bar) |
| Backend | Supabase (PostgreSQL, Auth, Realtime, Edge Functions) |
| Cliente | supabase-kt + Ktor (OkHttp) |
| Tema | MaterialKolor (esquema dinámico desde un color semilla) |
| Imágenes | Coil (fotos de productos/promos) |
| Splash | androidx.core:core-splashscreen |
| Exportación | Apache POI |

**Operaciones críticas atómicas** (ventas, gastos, compras, arqueo, mermas, transferencias) se ejecutan vía **funciones RPC en PostgreSQL**. Operaciones admin que requieren `service_role` (borrar cuenta de auth, reset de contraseña) van por una **Edge Function** (`admin-usuarios`).

---

## 📋 Requisitos

- Android Studio (versión reciente) · JDK 11+ · Android API 26+ (Android 8.0)
- Un proyecto en [Supabase](https://supabase.com)

---

## 🚀 Configuración

### 1. Clonar
```bash
git clone https://github.com/raytan111/ToppisERP.git
cd ToppisERP
```

### 2. Base de datos (Supabase SQL Editor)
Ejecutá los scripts de `.kiro/database/`. El orden general: primero el esquema base y RLS/realtime, luego los de cada fase (food cost, inventario, compras, dinero, mano de obra, multi-local) y por último los de ajustes recientes. Scripts clave:
- `supabase-schema.sql`, `supabase-rls.sql`, `supabase-realtime.sql`
- `supabase-fase4-*` (food cost), `supabase-fase5-*` (mermas/conteos), `supabase-fase6-*` (compras), `supabase-fase7-dinero.sql`, `supabase-fase8-manoobra.sql`, `supabase-fase9-*` (locales/roles)
- `supabase-roles-v2.sql` — roles ADMIN_LOCAL / SUPERVISOR
- `supabase-metodo-pago-v2.sql` — método de pago EFECTIVO / TARJETA / TRANSFERENCIA
- `supabase-clean-slate.sql` — dejar la app lista para operar (borra transaccionales, conserva configs)

> Notas de auth en `.kiro/database/NOTAS-SUPABASE-AUTH.md` y de seguridad en `NOTAS-SEGURIDAD.md`.

### 3. Credenciales
Editá `local.properties` (no se sube al repo):
```properties
SUPABASE_URL=https://TU-PROYECTO.supabase.co
SUPABASE_ANON_KEY=tu-anon-key
```

### 4. Auth (importante)
- **Authentication → Providers → Email**: dejá el proveedor **ENCENDIDO** y **"Confirm email" APAGADO** (la app usa login por usuario con dominio interno `@toppis.local`, no envía correos).

### 5. Primer administrador
Creá el usuario en Authentication (Auto Confirm) y su perfil:
```sql
INSERT INTO usuarios (id, nombre, email, rol, activo)
VALUES ('UID-COPIADO', 'Administrador', 'admin@toppis.local', 'ADMIN', true);
```

### 6. Edge Function (opcional, para borrar/resetear usuarios)
Desplegar `supabase/functions/admin-usuarios` (ver su README: Dashboard o `npx supabase functions deploy admin-usuarios`).

### 7. Compilar
```bash
./gradlew :app:assembleDebug
```

---

## 📁 Estructura

```
app/src/main/java/com/toppis/app/
├── data/
│   ├── models/        # Modelos @Serializable (Supabase)
│   ├── repository/    # Repositorios (datos + RPC)
│   ├── supabase/      # Cliente Supabase
│   └── db/entities/   # Enums (Enums.kt)
└── ui/
    ├── home/          # Menú principal (Home + categorías)
    ├── auth/ · pos/ · sobres/ · inventario/ · menu/ · gastos/
    ├── preparaciones/ · modificadores/ · promociones/ · foodcost/
    ├── mermas/ · conteos/ · compras/ · proveedores/ · variance/
    ├── arqueo/ · empleados/ · manoobra/ · locales/ · kpis/
    ├── reportes/ · flujo/ · contabilidad/ · exportacion/ · ventas/
    ├── ajustes/       # Configurar Colores
    ├── components/    # BackScaffold, DatePickerField, TopBar, SearchField,
    │                  # StateComponents (EmptyState/Skeleton), ToppisDialogs, ImagePickerField
    └── navigation/    # NavGraph

com/toppis/erp/ui/theme/   # Theme + ThemeManager (color de marca)
supabase/functions/        # Edge Functions (admin-usuarios)
.kiro/                     # database/ (SQL + notas), specs/, PROYECTO-CONTEXTO.md
```

---

## 🗺️ Roadmap

- [x] Migración a la nube (Supabase)
- [x] ERP de operación: food cost, inventario, compras, dinero, mano de obra, multi-local, KPIs
- [x] Roles + permisos + login por usuario + tema de marca
- [x] Imágenes de productos/promos (Coil + Supabase Storage)
- [x] Sistema de diseño (splash, ícono, Login moneda 3D, Home, tipografía, modo oscuro, transiciones, buscadores, skeletons/estados vacíos, diálogos unificados)
- [x] Control de costos y resultado semanal (fijos/variables, semáforos, break-even, rutina de cierre)
- [x] Rediseño del POS (pedidos múltiples, promos configurables, cocina/KDS, clientes + cuponera)
- [x] Promociones v2 (grupos de elección con imágenes, editor en pantalla, repetición configurable)
- [ ] Boletas electrónicas (SII Chile)
- [ ] Contabilidad/tributario completo
- [ ] IA (predicción de demanda, optimización de inventario)

---

## 📄 Licencia

Proyecto privado. Todos los derechos reservados.

Hecho con ❤️ para una hamburguesería en Chile 🇨🇱
