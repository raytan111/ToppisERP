---
inclusion: auto
---

# Convenciones de Código - ToppisERP Android

## Arquitectura

### Patrón MVVM
- **ViewModels**: Manejan lógica de negocio y estado con `StateFlow`
- **Repositories**: Acceso a datos (DAOs Room)
- **Entities**: Clases de Room con anotaciones `@Entity`
- **DAOs**: Interfaces con operaciones de base de datos
- **UI**: Composables stateless que reciben ViewModels

### Inyección de Dependencias
- **Manual DI**: No usar Hilt/Dagger
- **ViewModelFactory**: Crear factory para cada ViewModel con dependencias
- **MainActivity**: Punto de inicialización, instanciar repos y factories

## Compose UI

### Composables
```kotlin
// Naming: PascalCase terminado en "Screen" para pantallas principales
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    isAdmin: Boolean = false,
    // Preferir callbacks sobre navegación directa
    onNavigateToX: () -> Unit = {}
) { }

// Componentes reutilizables sin sufijo "Screen"
@Composable
fun ToppisTopBar(titulo: String) { }
```

### Estado
```kotlin
// StateFlow en ViewModels
val uiState = MutableStateFlow<UiState>(UiState.Loading)

// collectAsState en Composables
val state by viewModel.uiState.collectAsState()

// LaunchedEffect para side effects
LaunchedEffect(uiState) {
    when (uiState) {
        is UiState.Error -> snackbarHostState.showSnackbar(message)
    }
}
```

### Material 3
- Usar `Material3` (no Material 2)
- Iconos: `androidx.compose.material.icons.filled.*` y `Icons.AutoMirrored.*`
- Theme: `ToppisERPTheme` wrapper en MainActivity
- Colors: `MaterialTheme.colorScheme.*`

## Room Database

### Entities
```kotlin
@Entity(tableName = "nombre_tabla")
data class MiEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val campo: String,
    @ColumnInfo(name = "fecha_creacion") val fechaCreacion: Long
)
```

### Enums
- Definir en `entities/Enums.kt`
- Room almacena como String por defecto

### DAOs
```kotlin
@Dao
interface MiDao {
    @Query("SELECT * FROM tabla")
    fun getAll(): Flow<List<MiEntity>>
    
    @Insert
    suspend fun insert(entity: MiEntity)
    
    @Update
    suspend fun update(entity: MiEntity)
    
    @Delete
    suspend fun delete(entity: MiEntity)
}
```

### Migrations
- `fallbackToDestructiveMigration()` en desarrollo
- Incrementar versión en `@Database` al cambiar esquema

## Formateo

### Nombres de Variables
- `camelCase` para variables y funciones
- `PascalCase` para clases, interfaces, objetos
- `SCREAMING_SNAKE_CASE` para constantes

### Español en Dominio
- Nombres de clases de dominio en español: `Sobre`, `Gasto`, `Venta`
- Nombres técnicos en inglés: `ViewModel`, `Repository`, `Dao`
- UI strings en español

### Estructura de Packages
```
com.toppis.app/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entities/
│   └── repository/
└── ui/
    ├── [feature]/
    │   ├── [Feature]Screen.kt
    │   ├── [Feature]ViewModel.kt
    │   └── [Feature]ViewModelFactory.kt
    ├── components/
    └── navigation/
```

## Convenciones de UI

### Scaffold Pattern
```kotlin
Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = { ToppisTopBar(titulo = "Título") },
    bottomBar = { NavigationBar { /* items */ } },
    floatingActionButton = { /* FAB opcional */ }
) { padding ->
    Content(modifier = Modifier.padding(padding))
}
```

### Dialogs
- Usar `AlertDialog` para confirmaciones
- State: `var showDialog by remember { mutableStateOf(false) }`
- Cerrar con `showDialog = false` en dismiss y confirm

### Formateo de Números
```kotlin
// Moneda
private val moneyFmt = DecimalFormat("$#,##0")
moneyFmt.format(1234) // "$1,234"

// Fechas
private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
dateFmt.format(Date())
```

## Git

### Commits
- Mensajes en español
- Formato: `[Módulo] Descripción breve`
- Ejemplos:
  - `[POS] Agregar selector de salsas`
  - `[Sobres] Corregir cálculo de transferencias`
  - `[UI] Refactorizar NavigationBar`

### Branches
- `main` - producción
- `develop` - desarrollo
- `feature/nombre-feature` - nuevas características
- `fix/nombre-bug` - correcciones

## Testing

- Tests en `androidTest/` para tests instrumentados (Room, UI)
- Tests en `test/` para tests unitarios (ViewModels, lógica)
- No hay tests actualmente (proyecto en desarrollo)

## Dependencias

### Versioning
- Usar version catalog (`libs.versions.toml`)
- KSP para Room annotation processing
- Evitar dependencias deprecated

### Core Dependencies
- Compose BOM + Material 3
- Room + KTX
- Navigation Compose
- Lifecycle ViewModel Compose
- Apache POI (exportación Excel)

## Seguridad

- No hardcodear credenciales (admin por defecto OK para demo)
- Room DB sin encriptación (app local, sin datos sensibles críticos)
- FileProvider para compartir archivos exportados

## Performance

- Usar `LazyColumn`/`LazyRow` para listas largas
- `remember` para evitar recomposiciones innecesarias
- `derivedStateOf` para estados computados
- `Flow` en lugar de `LiveData`

## Accesibilidad

- `contentDescription` en todos los `Icon` y `Image`
- Labels descriptivos en `NavigationBarItem`
- Contraste suficiente en colores personalizados
