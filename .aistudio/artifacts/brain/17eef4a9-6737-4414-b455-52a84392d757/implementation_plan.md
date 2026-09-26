# Plan de Implementación: FranyuLauncher v1.4

## Resumen Ejecutivo
La versión 1.4 es una actualización mayor orientada a:
1. **Rediseño Visual Completo (1000% Mejor)**: Moderno oscuro con suntuosos acentos esmeralda (`#10B981`, `#059669`), tarjetas limpias con esquinas redondeadas, elevación visual y tipografía moderna en toda la aplicación.
2. **Optimización Extrema para Gama Baja (Ej. Samsung A30 / Mali-G71 / Exynos)**: Eliminación de tirones, micro-freezes del Garbage Collector, escalado inteligente de resolución nativa para GPUs de pocos núcleos, aceleración por lotes de GL4ES (`LIBGL_BATCH`, caché de shaders Mesa) y balance óptimo de memoria RAM.
3. **Identidad de Marca en Notificaciones**: Reemplazo de todos los recursos `notif_icon` (heredados de Mojo/Pojav) por el icono oficial de FranyuLauncher en todas las densidades (MDPI, HDPI, XHDPI, XXHDPI, XXXHDPI). *(Se mantendrá fuera del changelog público a petición del usuario)*.
4. **Estabilidad y Corrección de Bugs**: Prevención de fugas de memoria, optimización del hilo de renderizado, y mejoras en la carga de instancias y mods.

---

## 1. Rediseño Visual Completo (Moderno Oscuro + Esmeralda)
- **Paleta de Colores y Temas (`colors.xml` y `styles.xml`)**:
  - Fondo primario: Negro carbón profundo (`#0D1117` / `#161B22`) que ahorra batería en pantallas AMOLED (como la del Samsung A30).
  - Acentos y Primario: Verde Esmeralda vibrante (`#10B981` y gradiente `#059669`).
  - Tarjetas y Contenedores: Gris pizarra oscuro (`#1F2937`) con bordes sutiles esmeralda translúcidos (`#10B98122`).
  - Superficies interactivas: Efectos ripple esmeralda y feedback táctil claro.
- **Pantalla Principal (`fragment_launcher.xml` y `fragment_launcher.xml` landscape)**:
  - Header superior moderno con estado de perfil actual, avatar con halo esmeralda y badges de estado.
  - Tarjetas de acciones rápidas bien organizadas:
    - *Instancias y Versiones Optimizadas* con badge distintivo.
    - *Explorador de Mods & Addons* con iconos temáticos.
    - *Selector de versión/perfil* compacto y refinado.
    - Botón de **JUGAR** heroico, prominente con gradiente esmeralda de alto impacto.
- **Diálogos y Selectores (`ProfileTypeSelectFragment`, Modales de Ajustes y Actualizaciones)**:
  - Tarjetas de selección con bordes destacados al tocar, iconos HD centrados y textos legibles.

---

## 2. Optimización Profunda de Rendimiento (Samsung A30 y Gama Baja)
### A. Motor Gráfico y GL4ES / Mesa para GPUs Mali
Los dispositivos de gama baja como el Samsung A30 equipan GPUs ARM Mali (ej. Mali-G71 MP2 con solo 2 núcleos de sombreado) y pantallas FHD+ (1080x2340). Correr a resolución nativa colapsa el fillrate de la GPU y genera caídas de 60 FPS a 12 FPS:
- **Detección Automática de Hardware de Bajo Rendimiento**:
  - Detección de GPUs Mali (`GLInfoUtils` / CPU core count / RAM <= 4GB).
  - Aplicación de un **factor de resolución óptimo predeterminado (60% - 70%)** para GPUs Mali/gama baja, manteniendo nitidez con filtrado bilineal y duplicando la tasa de fotogramas.
- **Variables de Entorno Optimizadas en `JREUtils.java`**:
  - `LIBGL_BATCH=1`: Activa el agrupamiento de draw calls en GL4ES, reduciendo la sobrecarga de la CPU y llamadas JNI hasta en un 40%.
  - `MESA_GLSL_CACHE_DISABLE=false` y tamaño de caché ampliado a 256MB: Evita los tirones constantes al compilar shaders mientras se exploran nuevos chunks.
  - `LIBGL_STREAM=1` y `LIBGL_SHRINK=1`: Reduce la memoria requerida por texturas y optimiza el stream de vértices en chips de poco ancho de banda.
  - Forzado de `SurfaceView` en lugar de `TextureView` en dispositivos de gama baja para evitar el cuello de botella de composición en `SurfaceFlinger`.

### B. Ajustes JVM Ultra-Afinados para Evitar Tirones (Zero GC Stutters)
- Anteriormente se usaba `MaxGCPauseMillis=150` y `AlwaysPreTouch`, lo que congelaba el juego cada vez que el recolector de basura se activaba en dispositivos con 3GB/4GB de RAM.
- Nuevo preset optimizado de baja latencia:
  ```
  -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=20 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:G1NewSizePercent=20 -XX:G1MaxNewSizePercent=35 -XX:G1ReservePercent=15 -XX:InitiatingHeapOccupancyPercent=45 -XX:G1MixedGCCountTarget=4 -XX:G1PeriodicGCInterval=0
  ```
- **Asignación inteligente de memoria RAM**: Para teléfonos con 3GB-4GB (como el A30), asegurar que no asigne más del 45% de la RAM física para evitar que el Android Low Memory Killer (LMK) mate procesos o use swap zRAM agresivo que provoca tirones.

---

## 3. Identidad de Notificaciones
- Reemplazo de los archivos `notif_icon.png` en todas las carpetas drawable (`drawable-mdpi`, `drawable-hdpi`, `drawable-xhdpi`, `drawable-xxhdpi`, `drawable-xxxhdpi`) usando el icono oficial de FranyuLauncher con silueta limpia y transparente para barra de estado de Android.
- *(Privado: No se incluirá en las notas de la versión 1.4).*

---

## 4. Corrección de Errores y Pulido General
- Corrección de cierres inesperados al rotar la pantalla durante la descarga de versiones.
- Corrección de sincronización al abrir el explorador de mods.
- Verificación de la compilación completa mediante Gradle (`compile_applet`).

---

## Plan de Ejecución Paso a Paso
1. **Paso 1**: Actualizar los iconos de notificación `notif_icon.png` con la imagen oficial de FranyuLauncher.
2. **Paso 2**: Implementar el motor de optimizaciones para gama baja y Mali en `LauncherPreferences.java` y `JREUtils.java`.
3. **Paso 3**: Rediseñar la paleta de colores (`colors.xml`, `styles.xml`) e implementar el diseño moderno oscuro con acentos esmeralda en `fragment_launcher.xml` (modo vertical y horizontal).
4. **Paso 4**: Probar la compilación completa (`compile_applet`) y verificar que no existan errores ni regresiones.
