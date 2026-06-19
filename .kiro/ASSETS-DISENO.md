# Guía de imágenes / assets de diseño — ToppisERP

Qué imágenes necesito, dónde van y con qué nombre. Formatos y tamaños
recomendados para que se vean nítidas en todos los teléfonos.

---

## 1. Logo dentro de la app (Login y Home) — REQUERIDO

Hoy hay un **placeholder** vectorial en:
`app/src/main/res/drawable/toppis_logo.xml`

El código lo usa como `R.drawable.toppis_logo`. Para poner el logo real,
elegí UNA opción:

- **Opción A (recomendada): vector**
  - Exportá tu logo a SVG y convertilo a Android Vector (en Android Studio:
    botón derecho sobre `res/drawable` → New → Vector Asset → Local file (SVG)).
  - Guardalo reemplazando el archivo, con el nombre **`toppis_logo.xml`**.
  - Ventaja: nítido en cualquier tamaño, pesa poco.

- **Opción B: PNG**
  - Imagen **PNG con fondo transparente**, cuadrada, **512×512 px** (ideal 1024×1024).
  - Borrá `toppis_logo.xml` y poné el archivo como **`toppis_logo.png`** en
    `app/src/main/res/drawable/`.
  - (No pueden coexistir `toppis_logo.xml` y `toppis_logo.png`: mismo nombre = error.)

> Nota: el nombre del recurso debe ser solo minúsculas, números y guion bajo.

---

## 2. Ícono de la app (el del cajón de apps del teléfono) — RECOMENDADO

Es distinto al logo interno. Android usa "adaptive icons" en varias densidades.
La forma más fácil:

- En Android Studio: botón derecho en `app` → New → **Image Asset** →
  *Launcher Icons (Adaptive and Legacy)*.
  - **Foreground**: tu logo en PNG transparente **1024×1024** (con margen, que el
    dibujo ocupe ~70% del centro).
  - **Background**: un color sólido (el de la marca, ej. `#E63946`) o una imagen.
  - Android genera automáticamente todos los `mipmap-*` y el adaptive icon.

Si preferís hacerlo a mano, reemplazá los archivos en:
`app/src/main/res/mipmap-mdpi/ic_launcher.png` (48×48),
`mipmap-hdpi` (72×72), `mipmap-xhdpi` (96×96),
`mipmap-xxhdpi` (144×144), `mipmap-xxxhdpi` (192×192),
más sus variantes `ic_launcher_round.png`.
(Es bastante más tedioso; usá Image Asset.)

---

## 3. Opcionales (si querés más identidad visual)

- **Splash / pantalla de carga**: PNG transparente 1024×1024 →
  `res/drawable/splash_logo.png` (si lo querés, lo integro con la API de splash).
- **Fondo del login**: imagen 1080×1920 (vertical) →
  `res/drawable/login_fondo.webp` (WebP pesa menos que PNG/JPG).
- **Ilustración "sin datos"** para listas vacías: PNG transparente 600×600 →
  `res/drawable/empty_state.png`.

Avisame si vas a usar alguno de estos y los cableo en las pantallas.

---

## 4. Colores de marca — YA CONFIGURABLE EN LA APP

No necesitás imagen para esto. En **Administración → Configurar Colores**
elegís el color de la empresa por código hex (`#`) y toda la app se repinta
(claro/oscuro incluidos). El color por defecto es `#E63946` (rojo Toppis).

---

## Resumen rápido
| Qué | Dónde | Nombre | Tamaño |
|---|---|---|---|
| Logo en app | `res/drawable/` | `toppis_logo.xml` (vector) o `toppis_logo.png` | 512–1024 px, transparente |
| Ícono de app | `res/mipmap-*` (usar Image Asset) | `ic_launcher` | 1024 px fuente |
| Splash (opc.) | `res/drawable/` | `splash_logo.png` | 1024 px |
| Fondo login (opc.) | `res/drawable/` | `login_fondo.webp` | 1080×1920 |
