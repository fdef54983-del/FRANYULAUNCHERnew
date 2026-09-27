# Plan de Innovación y Mejoras para FranyuLauncher v1.5 (50 Ideas)

A continuación se detalla la recopilación de **50 ideas y mejoras estratégicas y técnicas** diseñadas específicamente para maximizar el rendimiento, la compatibilidad y la experiencia de usuario en FranyuLauncher v1.5. No se aplicará ningún cambio en el código hasta que elijas cuáles implementar.

---

### I. Rendimiento, Drivers y Motores Gráficos (1 - 10)
1. **Gestor Integrado de Turnip / Zink Drivers**: Selector en la app para descargar y alternar versiones de controladores Adreno/Mesa Turnip sin requerir root ni sustituir archivos manualmente.
2. **Preset "FPS Boost" con un solo clic**: Generador inteligente de perfiles de rendimiento (`options.txt`, JVM args y flags GC adaptadas al modelo de procesador detectado: Snapdragon, MediaTek, Exynos o Tensor).
3. **Optimizador Automático de Java Garbage Collector (GC)**: Detección dinámica de memoria libre para alternar automáticamente entre Shenandoah, ZGC Generacional o G1GC según la RAM del dispositivo.
4. **Modo Ahorro de Batería Inteligente**: Límite adaptativo de FPS en segundo plano o menús, reduciendo la tasa de refresco a 30 FPS cuando no hay interacción activa.
5. **Soporte Nativo de FSR (FidelityFX Super Resolution)**: Reescalado espacial integrado para permitir renderizar a 720p/540p escalando a 1080p con nitidez y ganancia masiva de FPS.
6. **Compilación Anticipada de Shaders (Shader Pre-caching)**: Precompilación de shaders GLSL/SPIR-V durante la pantalla de carga para erradicar tirones (*stuttering*) al explorar nuevos chunks.
7. **Limpiador Automático de Caché de Texturas y Chunks**: Tarea en segundo plano para purgar cachés huérfanas de Minecraft que saturan el almacenamiento interno.
8. **Asignación Dinámica de RAM en Caliente**: Permitir que el launcher aumente el tope de RAM asignado si detecta que el modpack está al 95% de uso para evitar crashes por `OutOfMemoryError`.
9. **Compatibilidad con Angle / Vulkan Backend Actualizado**: Actualización de librerías EGL/Vulkan para soporte de las últimas versiones de Sodium e Iris Shaders en MC 1.21+.
10. **Acelerador de Lanzamiento con DexCache Compartido**: Precarga en memoria de clases DEX comunes para reducir el tiempo de apertura del launcher en un 40%.

---

### II. Gestión de Instancias, Modpacks y Mods (11 - 20)
11. **Instalador Integrado de Shaders (Iris / Oculus / Canvas)**: Pestaña dedicada para buscar, descargar y preconfigurar shaders compatibles con GPU móviles (ej. Complementary, BSL, MakeUp UltraFast).
12. **Actualizador de Mods con 1 Clic**: Detección de versiones obsoletas en la carpeta de mods y botón directo para actualizar a la última release de Modrinth / CurseForge compatible.
13. **Clonador y Exportador de Instancias (.zip / share)**: Exportar mundos, configuraciones y mods en un solo archivo comprimido listo para compartir por Bluetooth, Telegram o WhatsApp.
14. **Detección Automática de Incompatibilidad entre Mods**: Analizador pre-lanzamiento que detecta mods duplicados, arquitecturas incorrectas o faltas de librerías (ej. Cloth Config faltante).
15. **Importador Directo de Modpacks desde Enlaces URL**: Pegar un link de Modrinth / CurseForge y descargarlo directamente sin necesidad de descargarlo en el navegador.
16. **Interruptor Rápido de Mods (Habilitar / Deshabilitar)**: Lista de casillas tipo check para desactivar mods temporalmente sin borrarlos de la carpeta.
17. **Copia de Seguridad Automática de Mundos**: Backup programado de partidas guardadas antes de actualizar mods o versiones.
18. **Explorador de Archivos Interno Integrado**: Editor y explorador rápido de carpetas (`saves`, `resourcepacks`, `screenshots`, `config`) sin depender de ZArchiver.
19. **Gestor de Texture Packs y Resource Packs**: Vista previa visual con iconos de resource packs e instalación con un toque.
20. **Sincronización Local entre Dispositivos por Wi-Fi**: Transferir instancias o mundos de un teléfono a otro en la misma red local a alta velocidad.

---

### III. Controles Táctiles y Experiencia de Entrada (21 - 30)
21. **Creador Visual de Controles en Pantalla (Drag & Drop Moderno)**: Diseñador visual con guías magnéticas, ajuste de opacidad, colores dinámicos y bordes redondeados.
22. **Presets de Controles por Categoría (PvP, Construcción, Aventura)**: Diseños de control prediseñados y optimizados para combates rápidos o juego técnico.
23. **Control por Giroscopio para Apuntado Fino**: Usar los sensores de inclinación del teléfono para micro-ajustes al apuntar con arcos, ballestas o cañas.
24. **Soporte Completo de Mandos (Xbox, PlayStation, JoyCons, Switch Pro)**: Mapeo automático de gatillos analógicos, botones hápticos y navegación por cruceta en los menús de Minecraft.
25. **Vibración Háptica Personalizable**: Respuesta táctil sutil al romper bloques, recibir daño, interactuar con cofres o disparar flechas.
26. **Rueda Radial de Acceso Rápido (Weapon / Tool Wheel)**: Menú circular desplegable al mantener pulsado un botón táctil para cambiar entre espada, pico, comida y antorchas.
27. **Gestos Táctiles Personalizables (Swipe Actions)**: Doble toque para esprintar, deslizamiento con dos dedos para cambiar de perspectiva F5 o agacharse.
28. **Modo Mouse Virtual con Touchpad Flotante**: Emulación de trackpad de portátil con dos dedos para clic derecho y scroll natural en los inventarios.
29. **Botones de Macros y Combinaciones de Teclas**: Crear botones para combinaciones comunes como F3+G (ver chunks), F3+B (hitboxes) o escribir comandos frecuentes (`/gamemode`, `/tp`).
30. **Soporte de Teclado y Ratón Físico sin Retardo (Input Lock)**: Bloqueo de puntero Android auténtico (`capturePointer`) para jugar con mouse y teclado OTG/Bluetooth exactamente como en PC.

---

### IV. Interfaz de Usuario y Personalización (31 - 40)
31. **Diseño de Interfaz Material You (M3)**: Tema dinámico que adapta los colores de FranyuLauncher a la paleta del fondo de pantalla del teléfono en Android 12+.
32. **Modo Oscuro Puro (AMOLED Black)**: Tema optimizado para pantallas OLED que ahorra batería y descansa la vista.
33. **Fondos de Pantalla Animados / Temáticos**: Fondos personalizables con arte de Minecraft, capturas de tus mundos o shaders en loop.
34. **Página de Inicio con Noticias y Servidores Destacados**: Sección informativa con las últimas actualizaciones oficiales de Minecraft, parches de mods y servidores comunitarios.
35. **Contador de Tiempo de Juego y Estadísticas**: Medidor de horas jugadas por instancia, fecha de última partida y tasa de FPS media alcanzada.
36. **Galería Integrada de Capturas de Pantalla (Screenshots)**: Visualizador de fotos tomadas con F2 dentro del launcher con opción de compartirlas o exportarlas directamente.
37. **Modo Inmersivo Completo**: Opción para ocultar completamente la barra de navegación gestual y la barra de estado superior durante el juego.
38. **Selector de Idioma y Voces TTS**: Interfaz completamente multilingüe con detección automática y soporte en español neutro impecable.
39. **Sonidos de Interfaz Personalizables**: Efectos sonoros de clics estilo Minecraft o diseño moderno Material al presionar botones.
40. **Iconos Adaptativos y Accesos Directos Dinámicos**: Crear accesos directos en la pantalla de inicio del teléfono para iniciar una versión o mundo específico directamente.

---

### V. Conectividad, Multijugador y Diagnóstico (41 - 50)
41. **Buscador y Verificador de Servidores (Ping Monitor)**: Monitor en vivo que muestra el ping, estado del servidor (online/offline) y jugadores conectados antes de entrar al juego.
42. **Integración con e4mc / Essential para LAN por Internet**: Compartir mundos locales con amigos a través de Internet sin necesidad de configurar puertos en el router.
43. **Visor de Registros en Tiempo Real con Búsqueda y Filtros**: Consola de `latestlog.txt` con resaltado de sintaxis en color (Info, Warn, Error) y filtro para ubicar crashes al instante.
44. **Diagnóstico Inteligente de Crashes con Sugerencias de Solución**: Detección automática de la causa de cierres (ej. "Falta Java 17", "Incompatibilidad de driver GPU", "RAM insuficiente").
45. **Generador de Enlaces para Compartir Crash Logs**: Subida segura de reportes de error a sitios como Pastebin / mclogs con un solo clic para pedir ayuda en Discord.
46. **Gestor de Múltiples Cuentas (Offline y Microsoft)**: Cambio rápido entre cuentas desde la barra superior sin tener que cerrar sesión.
47. **Soporte de Skins en Cuentas Offline**: Sistema de carga local de skins personalizadas y capas visibles para cuentas sin conexión.
48. **Notificaciones Push de Descargas y Servidores**: Alertas al completarse la instalación de un modpack pesado o cuando un servidor favorito esté en línea.
49. **Modo Sin Conexión Inteligente (Offline First)**: Detección de falta de Wi-Fi para omitir comprobaciones innecesarias y arrancar el juego de inmediato.
50. **Verificador de Integridad de Archivos del Juego**: Función de comprobación hash (SHA-1) para reparar archivos dañados o corruptos sin borrar los mundos.
