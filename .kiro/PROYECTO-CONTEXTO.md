# ToppisERP - Contexto del Proyecto

**Última Actualización**: 2026-06-09  
**Versión Actual**: 2.0 (Supabase Cloud)  
**Ubicación**: Chile (CLP, integración SII futura)

---

## 1. Descripción General

ToppisERP es una aplicación móvil Android para gestionar las operaciones de una dark kitchen, actualmente enfocada en:
- Punto de venta (POS)
- Gestión de inventario
- Control de gastos
- Sistema de sobres (múltiples cajas de dinero)
- Dashboard de métricas
- Reportes y exportación de datos

**Estado actual**: App funcional con base de datos local (Room SQLite), autenticación local, y arquitectura manual DI.

**Visión futura**: Evolucionar hacia un ERP completo en la nube con:
- Base de datos centralizada en la nube
- Emisión de boletas electrónicas (integración SII Chile)
- Módulo de contabilidad e impuestos
- Arquitectura multi-app o modular
- Preparación para integración con IA

---

## 2. Stack Tecnológico Actual

### Framework y Lenguaje
- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35
- **Compile SDK**: 36

### Arquitectura
- **Patrón**: MVVM (Model-View-ViewModel)
- **Navegación**: Jetpack Navigation Compose
- **DI**: Manual (sin Hilt/Dagger)
- **Base de Datos**: Room (SQLite local)
- **Exportación**: Apache POI para Excel/XLSX

### Dependencias Principales
```kotlin
// Compose & Material 3
androidx.compose.material3
androidx.compose.material:material-icons-extended
androidx.navigation.compose
androidx.lifecycle.viewmodel.compose

// Room Database
androidx.room.runtime (version según libs.versions.toml)
androidx.room.ktx
androidx.room.compiler (KSP)

// Exportación
org.apache.poi:poi:5.2.3
org.apache.poi:poi-ooxml:5.2.3
```

---

## 3. Modelo de Datos (Room v12)

### 3.1 Entidades (13 tablas)

#### **Usuario**
```kotlin
@Entity(tableName = "usuarios")
- id: Int (PK, autoincrement)
- nombre: String
- email: String
- passwordHash: String
- rol: Rol (ADMIN | CAJERO)
- activo: Boolean
- fechaCreacion: Long
```

#### **Sobre** (cajas de dinero)
```kotlin
@Entity(tableName = "sobres")
- id: Int (PK, autoincrement)
- nombre: String
- descripcion: String
- saldo: Double
- fechaCreacion: Long
```

#### **MovimientoSobre**
```kotlin
@Entity(tableName = "movimientos_sobre")
- id: Int (PK, autoincrement)
- sobreId: Int (FK → Sobre)
- tipo: TipoMovimiento (INGRESO | EGRESO | TRANSFERENCIA)
- monto: Double
- fecha: Long
- concepto: String
- sobreDestinoId: Int? (solo para transferencias)
```

#### **Insumo** (productos para cocina)
```kotlin
@Entity(tableName = "insumos")
- id: Int (PK, autoincrement)
- nombre: String
- descripcion: String
- precio: Double
- stock: Int
- unidadMedida: String
- activo: Boolean
```

#### **Ingrediente**
```kotlin
@Entity(tableName = "ingredientes")
- id: Int (PK, autoincrement)
- nombre: String
- stockGramos: Int
- precioGramo: Double
- activo: Boolean
```

#### **Venta**
```kotlin
@Entity(tableName = "ventas")
- id: Int (PK, autoincrement)
- fecha: Long
- total: Double
- metodoPago: MetodoPago (EFECTIVO | DEBITO)
- sobreId: Int (FK → Sobre, RESTRICT)
- usuarioId: Int? (FK → Usuario, SET_NULL)
- estado: EstadoVenta (COMPLETADA | ANULADA)
- incluirEnvio: Boolean
- montoEnvio: Double
- stickersEnviados: Int
```

#### **Gasto**
```kotlin
@Entity(tableName = "gastos")
- id: Long (PK, autoincrement)
- descripcion: String
- monto: Double
- categoria: CategoriaGasto (INSUMOS, SUELDOS, SERVICIOS, ARRIENDO, etc.)
- sobreId: Int? (FK → Sobre, SET_NULL)
- usuarioId: Int? (FK → Usuario, SET_NULL)
- fecha: Long
- comprobante: String?
```

#### **Presupuesto**
```kotlin
@Entity(tableName = "presupuestos")
- id: Int (PK, autoincrement)
- mes: Int (1-12)
- anio: Int
- categoriaGasto: CategoriaGasto
- montoPresupuestado: Double
```

#### **ItemMenu** (productos del menú)
```kotlin
@Entity(tableName = "items_menu")
- id: Int (PK, autoincrement)
- nombre: String
- descripcion: String
- precio: Double
- activo: Boolean
```

#### **RecetaMenu** (relación many-to-many ItemMenu ↔ Ingredientes/Insumos)
```kotlin
@Entity(tableName = "recetas_menu")
- id: Int (PK, autoincrement)
- itemMenuId: Int (FK → ItemMenu, CASCADE)
- componenteId: Int (ID del ingrediente o insumo)
- tipoComponente: TipoComponente (INGREDIENTE | INSUMO)
- cantidadGramos: Int
```

#### **Salsa** (complementos del menú)
```kotlin
@Entity(tableName = "salsas")
- id: Int (PK, autoincrement)
- nombre: String
- activo: Boolean
```

#### **Comanda** (orden de cocina)
```kotlin
@Entity(tableName = "comandas")
- id: Int (PK, autoincrement)
- ventaId: Int (FK → Venta, CASCADE)
- fecha: Long
- detalleTexto: String
- estado: EstadoComanda (PENDIENTE | ENTREGADA)
```

#### **ItemVentaMenu** (detalle de venta)
```kotlin
@Entity(tableName = "items_venta_menu")
- id: Int (PK, autoincrement)
- ventaId: Int (FK → Venta, CASCADE)
- itemMenuId: Int (FK → ItemMenu, RESTRICT)
- cantidad: Int
- precioUnitario: Double
- subtotal: Double
```

### 3.2 Enumeraciones

```kotlin
enum class Rol { ADMIN, CAJERO }

enum class TipoMovimiento { INGRESO, EGRESO, TRANSFERENCIA }

enum class MetodoPago { EFECTIVO, DEBITO }

enum class EstadoVenta { COMPLETADA, ANULADA }

enum class TipoComponente { INGREDIENTE, INSUMO }

enum class EstadoComanda { PENDIENTE, ENTREGADA }

enum class ZonaEnvio(val label: String, val precio: Double) {
    SIN_ENVIO("Sin envío", 0.0),
    ZONA_1("Zona 1", 500.0),
    ZONA_2("Zona 2", 1000.0),
    ZONA_3("Zona 3", 1500.0),
    ZONA_4("Zona 4", 2000.0)
}

enum class CategoriaGasto(val label: String) {
    INSUMOS("Insumos"),
    SUELDOS("Sueldos"),
    SERVICIOS("Servicios"),
    ARRIENDO("Arriendo"),
    TRANSPORTE("Transporte"),
    ENVIOS("Envíos"),
    PACKAGING("Packaging / Stickers"),
    OTROS("Otros")
}
```

---

## 4. Arquitectura de la Aplicación

### 4.1 Estructura de Carpetas

```
app/src/main/java/com/toppis/app/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt (Room Database v12)
│   │   ├── dao/ (13 DAOs)
│   │   └── entities/ (13 entidades + Enums.kt)
│   └── repository/ (10 repositorios)
│       ├── AuthRepository.kt
│       ├── ComandaRepository.kt
│       ├── DashboardRepository.kt
│       ├── FlujoCajaRepository.kt
│       ├── GastoRepository.kt
│       ├── InventarioRepository.kt
│       ├── MenuRepository.kt
│       ├── ReporteRepository.kt
│       ├── SobreRepository.kt
│       └── VentaRepository.kt
├── ui/
│   ├── auth/ (Login screen + AuthViewModel)
│   ├── components/ (MainScaffold, ToppisTopBar)
│   ├── dashboard/ (Pantalla principal con métricas)
│   ├── exportacion/ (Exportar a Excel/CSV)
│   ├── flujo/ (Flujo de caja y proyecciones)
│   ├── gastos/ (Registro de gastos)
│   ├── inventario/ (Gestión de insumos e ingredientes)
│   ├── menu/ (Configuración de menú y recetas)
│   ├── navigation/ (NavGraph centralizado)
│   ├── pos/ (Punto de venta)
│   ├── reportes/ (Gráficos ventas vs gastos)
│   └── sobres/ (Gestión de cajas de dinero)
└── util/ (Utilidades varias)
```

### 4.2 Capa de Datos


**Patrón**: Repository + DAO  
**Base de datos**: Room (SQLite local)  
**Inicialización**: Singleton en MainActivity con manual DI

```kotlin
// Ejemplo: Manual DI en MainActivity.onCreate()
val database = AppDatabase.getDatabase(this)
val sobreRepo = SobreRepository(database, sobreDao, movimientoSobreDao)
val sobreFactory = SobreViewModelFactory(sobreRepo)
```

**Características clave**:
- Todos los DAOs usan `suspend fun` (coroutines)
- Flows para observar cambios (`Flow<List<Entity>>`)
- Foreign keys con diferentes estrategias: CASCADE, SET_NULL, RESTRICT
- Migración destructiva habilitada (`.fallbackToDestructiveMigration()`)

### 4.3 Capa de Presentación

**ViewModels**: Un ViewModel por pantalla principal  
**State Management**: `StateFlow` y `MutableStateFlow`  
**UI**: Composables funcionales con Material 3

**Navegación actual** (post-refactorización):
- `MainScaffold` centralizado con BottomNavigationBar de 5 items
- ModalBottomSheet "Más" con opciones filtradas por rol
- 6 pantallas principales envueltas en MainScaffold
- 4 pantallas admin sin scaffold (solo contenido)

---

## 5. Funcionalidades Actuales

### 5.1 Autenticación
- Login local con email/password
- Hash de contraseñas (PasswordHash almacenado en Room)
- Roles: ADMIN y CAJERO
- Usuario admin por defecto creado al iniciar la app
- AuthViewModel gestiona estado de sesión
- Logout disponible desde menú "Más"

### 5.2 Dashboard
**Acceso**: Todos los usuarios (pantalla inicial post-login)  
**Funcionalidades**:
- Vista de métricas principales (ventas, gastos, sobres)
- Acceso rápido a funciones principales
- Diferenciación visual para Admin vs Cajero

### 5.3 Punto de Venta (POS)
**Acceso**: Todos los usuarios  
**Funcionalidades**:
- Selección de items del menú (layout 50/50 móvil optimizado)
- Carrito de compras con +/- cantidad
- Selección de método de pago (Efectivo/Débito)
- Selección de sobre destino
- Opciones de envío con zonas (500-2000 CLP)
- Generación automática de comanda para cocina
- Descuento automático de stock al vender
- Registro de venta con items detallados

### 5.4 Gestión de Sobres
**Acceso**: Todos los usuarios  
**Funcionalidades**:
- Crear/editar/eliminar sobres (cajas de dinero)
- Ver saldo actual de cada sobre
- Registrar ingresos/egresos/transferencias
- Historial de movimientos por sobre
- Validaciones: no permitir saldo negativo

### 5.5 Inventario
**Acceso**: Todos los usuarios  
**Funcionalidades**:
- Gestión de insumos (productos generales con stock unitario)
- Gestión de ingredientes (stock en gramos con precio/gramo)
- CRUD completo para ambos tipos
- Activar/desactivar items
- Validación de stock antes de ventas

### 5.6 Gastos
**Acceso**: Todos los usuarios (Admin ve todos, Cajero solo propios)  
**Funcionalidades**:
- Registrar gastos con categoría (8 categorías disponibles)
- Vincular gastos a sobres (descuenta saldo automáticamente)
- Comprobante opcional (texto libre)
- Filtros por categoría y fecha
- Historial completo de gastos

### 5.7 Reportes
**Acceso**: Todos los usuarios  
**Funcionalidades**:
- Gráficos de ventas vs gastos
- Filtros por rango de fechas
- Comparativa por período
- Visualización de métricas clave

### 5.8 Configuración de Menú (Admin)
**Acceso**: Solo ADMIN  
**Funcionalidades**:
- CRUD de items del menú
- Configuración de recetas (ingredientes + cantidades)
- Gestión de salsas complementarias
- Activar/desactivar items
- Cálculo automático de costos por receta

### 5.9 Flujo de Caja (Admin)
**Acceso**: Solo ADMIN  
**Funcionalidades**:
- Análisis de flujo de caja
- Proyecciones financieras
- Comparación con presupuestos
- Alertas de desviaciones

### 5.10 Exportación de Datos (Admin)
**Acceso**: Solo ADMIN  
**Funcionalidades**:
- Exportar datos a Excel (XLSX)
- Exportar a CSV
- Generar archivo ZIP con múltiples reportes
- Compartir archivos vía sistema Android
- Utiliza Apache POI 5.2.3

---

## 6. Convenciones del Proyecto

### 6.1 Nomenclatura
- **Entidades**: Singular en español (Usuario, Venta, Sobre)
- **Tablas**: Plural en español (usuarios, ventas, sobres)
- **DAOs**: `[Entidad]Dao.kt` (UsuarioDao, VentaDao)
- **Repositories**: `[Entidad]Repository.kt`
- **ViewModels**: `[Pantalla]ViewModel.kt`
- **Screens**: `[Pantalla]Screen.kt`

### 6.2 Idioma
- **UI**: 100% español
- **Código**: Español (variables, funciones, clases)
- **Comentarios**: Español
- **Commits**: Español preferido

### 6.3 Material Design
- Material 3 estricto
- NavigationBar máximo 5 items
- ModalBottomSheet para overflow
- Accesibilidad: content descriptions obligatorios
- Theming: ToppisERPTheme personalizado

### 6.4 Coroutines y Flows
```kotlin
// Patrón estándar en ViewModels
viewModelScope.launch {
    repository.getFlow().collect { data ->
        _uiState.value = data
    }
}
```

### 6.5 Estado de UI
```kotlin
// Pattern típico
data class [Pantalla]UiState(
    val isLoading: Boolean = false,
    val data: List<Entity> = emptyList(),
    val error: String? = null
)
```

---

## 7. Limitaciones y Deuda Técnica Conocida


### 7.1 Base de Datos Local
- **Limitación**: Datos solo en el dispositivo, no compartidos entre usuarios
- **Riesgo**: Pérdida de datos si se pierde el dispositivo
- **Solución planeada**: Migración a Supabase (Fase 1)

### 7.2 Sin Sincronización Cloud
- **Limitación**: No hay backup automático
- **Limitación**: No se puede acceder desde múltiples dispositivos
- **Solución planeada**: Fase 1 de migración

### 7.3 Autenticación Local
- **Limitación**: Usuarios solo existen en el dispositivo
- **Limitación**: No hay recuperación de contraseña
- **Solución planeada**: Migrar a Supabase Auth

### 7.4 Sin Emisión de Comprobantes
- **Limitación**: No se emiten boletas ni facturas electrónicas
- **Impacto**: No cumple normativa tributaria chilena (SII)
- **Solución planeada**: Fase 2 - Integración SII

### 7.5 Sin Módulo Contable
- **Limitación**: No hay cierre de mes, estados financieros, ni declaraciones
- **Solución planeada**: Fase 3 - Módulo contabilidad

### 7.6 Manual DI
- **Limitación**: MainActivity tiene mucho boilerplate de inicialización
- **Consideración**: Se mantiene intencionalmente para simplicidad (sin Hilt)

---

## 8. Historial de Cambios Importantes

### v1.0 - Estado Actual (2026-06-08)

**Refactorización de Navegación** (Spec: `.kiro/specs/refactorizacion-navegacion.md`)
- ✅ Creado MainScaffold centralizado con BottomNavigationBar de 5 items
- ✅ Implementado ModalBottomSheet "Más" con filtrado por rol
- ✅ Simplificados 6 screens eliminando código duplicado
- ✅ Mejorado diseño POS (50/50 layout, cards más grandes)
- **Impacto**: Reducción de ~100 líneas duplicadas, UX más consistente

**Funcionalidades Base Completadas**:
- ✅ Módulo de autenticación (roles Admin/Cajero)
- ✅ Sistema de sobres (cajas múltiples)
- ✅ POS con comandas y descuento de stock
- ✅ Inventario (insumos e ingredientes separados)
- ✅ Gastos con categorías
- ✅ Reportes con gráficos
- ✅ Configuración de menú y recetas
- ✅ Flujo de caja con presupuestos
- ✅ Exportación Excel/CSV/ZIP
- ✅ Dashboard con métricas

---

## 9. Roadmap Futuro (Visión a Largo Plazo)

### Fase 1: Migración a la Nube ⬅️ PRÓXIMA FASE
**Objetivo**: Centralizar datos en Supabase (PostgreSQL)  
**Beneficios**:
- Backup automático
- Acceso multi-dispositivo
- Sincronización en tiempo real
- Preparación para multi-tenant
- Base para integración IA futura

**Alcance estimado**:
- Migrar 13 entidades de Room a Supabase
- Migrar autenticación a Supabase Auth
- Implementar Supabase Kotlin SDK
- Decidir estrategia offline-first vs online-only
- Plan de migración de datos existentes

### Fase 2: Emisión de Comprobantes Electrónicos
**Objetivo**: Integración con SII (Servicio de Impuestos Internos, Chile)  
**Funcionalidades**:
- Emisión de boletas electrónicas
- Emisión de facturas electrónicas
- Envío automático al SII
- Almacenamiento de XMLs firmados
- Registro contable automático

### Fase 3: Módulo de Contabilidad e Impuestos
**Objetivo**: Cumplimiento tributario completo  
**Funcionalidades**:
- Libro de ventas y compras
- Cierre mensual
- Cálculo de IVA
- Estados financieros (Balance, PyG)
- Declaraciones juradas
- Integración con contador

### Fase 4: Arquitectura Escalable Multi-App
**Objetivo**: Modularizar en apps especializadas o app maestra  
**Opciones a evaluar**:
- **Opción A**: Apps separadas compartiendo DB central
  - App Ventas (POS actual)
  - App Contabilidad
  - App Producción (cocina)
  - App Administración
- **Opción B**: App maestra con módulos intercambiables
  - Dashboard principal
  - Navegación a módulos especializados
  - Permisos granulares por módulo

### Fase 5: Preparación para IA
**Objetivo**: Base de datos optimizada para ML/IA  
**Use cases potenciales**:
- Predicción de demanda
- Optimización de inventario
- Detección de fraudes
- Recomendaciones de precios
- Análisis de rentabilidad por producto

---

## 10. Referencias Técnicas

### Documentación Room
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Room Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions)

### Material Design 3
- [Navigation Bar](https://m3.material.io/components/navigation-bar/overview)
- [Bottom Sheets](https://m3.material.io/components/bottom-sheets/overview)

### Jetpack Compose
- [Navigation](https://developer.android.com/jetpack/compose/navigation)
- [State Management](https://developer.android.com/jetpack/compose/state)

### SII Chile (Futuro)
- [Portal SII](https://www.sii.cl)
- [Facturación Electrónica](https://www.sii.cl/servicios_online/1039-1185.html)

### Supabase (Futuro)
- [Supabase Docs](https://supabase.com/docs)
- [Supabase Kotlin SDK](https://github.com/supabase-community/supabase-kt)

---

## 11. Contacto y Mantenimiento

**Propietario**: andreslh  
**Ubicación**: Chile  
**Moneda**: CLP (Peso Chileno)  
**Contexto**: Dark kitchen (cocina de producción sin local físico)

**Archivos clave de contexto**:
- Este archivo: `.kiro/PROYECTO-CONTEXTO.md`
- Spec navegación: `.kiro/specs/refactorizacion-navegacion.md`
- Convenciones: `.kiro/steering/android-conventions.md` (si existe)

---

**Nota para futuras sesiones de trabajo**:  
Este documento debe actualizarse cada vez que se complete una fase mayor o se realicen cambios estructurales significativos. Es el **punto de partida obligatorio** para cualquier nuevo spec o feature.

**Última revisión**: 2026-06-08  
**Próximo spec planeado**: Migración a Supabase (Fase 1)
