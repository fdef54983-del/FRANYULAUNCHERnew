# Plan de Implementación: Compatibilidad de Instalación y Gestor de Runtimes Java

Este plan aborda los dos requerimientos planteados:
1. **Solucionar el fallo al instalar actualizaciones sobre la app existente** ("No se instala la app hasta que borras la anterior").
2. **Descarga automática y selector de múltiples versiones de Java Runtime** (Java 8, 17, 21, 25 y alternativas) para evitar errores de incompatibilidad en instancias y mods.

---

## 1. Solución al Error de Instalación de la App

### Causa
- **Discrepancia de `versionCode`**: En `build.gradle`, el `versionCode` local estaba configurado como un valor estático bajo (`172005`), mientras que las compilaciones previas generaban números del orden de `206000000+`. Android rechaza la actualización directa (`INSTALL_FAILED_VERSION_DOWNGRADE`) y arroja el mensaje genérico *"No se instaló la aplicación"*, forzando a desinstalar la app para poder instalar la nueva.
- **Inconsistencia de firma**: Es imprescindible garantizar que todas las variantes (`debug`, `release`) utilicen la misma clave de firma persistente (`customDebug` con `debug.keystore.base64`).

### Solución
- Configurar un `versionCode` dinámico basado en tiempo Unix que supere permanentemente cualquier versión anterior y aumente de forma monótona en cada compilación.
- Asegurar que la firma `customDebug` sea idéntica y consistente en todas las compilaciones.
- Verificar flags de actualización en `AndroidManifest.xml`.

---

## 2. Sistema de Runtimes de Java: Descarga Automática y Selector Múltiple

### Causa de los Errores de Instancias/Mods
- Minecraft clásico y modloaders antiguos (1.12.1 OptiFine, Forge 1.7.10 - 1.16.5) requieren **Java 8**.
- Minecraft moderno (1.17 - 1.20.4) requiere **Java 17**.
- Minecraft 1.20.5+ requiere **Java 21**.
- Si un runtime no está descargado o preinstalado, el juego crashea de inmediato o falla la instalación de la instancia.

### Solución
1. **Descarga Automática Inteligente en Tiempo de Ejecución (`NewJREUtil.java`)**:
   - Si una instancia o modloader requiere una versión de Java (ej. Java 8 para OptiFine 1.12.1) y no se encuentra instalada localmente, el launcher ya no mostrará un error bloqueante.
   - En su lugar, descargará e instalará automáticamente el paquete JRE correspondiente desde el repositorio oficial con una barra de progreso amigable.
2. **Selector y Gestor de Runtimes Java en la Interfaz (`MultiRTConfigDialog.java` y nuevo diálogo de catálogo)**:
   - Añadir una opción *"Descargar Java"* en la configuración de runtimes.
   - Ofrecer un catálogo seleccionable de versiones:
     - **Java 8 (JRE 8)**: Ideal para Minecraft 1.12.2, 1.12.1 OptiFine, 1.8.9, 1.7.10.
     - **Java 17 (JRE 17)**: Para Minecraft 1.17 hasta 1.20.4.
     - **Java 21 (JRE 21)**: Para Minecraft 1.20.5, 1.21 y versiones recientes.
     - **Java 25 (Experimental)**: Para pruebas de rendimiento y futuras versiones.
   - Permitir al usuario descargar con 1 toque cualquier versión, probarla con sus mods y alternar entre ellas sin complicaciones.
3. **Instalador y Extractor Robusto de Runtimes (`MultiRTUtils.java`)**:
   - Soporte para descompresión de paquetes ZIP/Tar.XZ multiarquitectura (ARM64, ARM32, x86_64) con verificación de archivos y configuración de enlaces simbólicos.

---

## 3. Plan de Verificación
1. **Compilación de la App (`compile_applet`)**:
   - Verificar que el proyecto compile limpiamente con Gradle.
2. **Validación del `versionCode` y Keystore**:
   - Verificar que los metadatos del APK generado permitan la instalación sobre versiones existentes.
3. **Validación de la Descarga de Runtimes**:
   - Comprobar la resolución de URLs y el proceso de extracción de JRE 8, 17 y 21.
