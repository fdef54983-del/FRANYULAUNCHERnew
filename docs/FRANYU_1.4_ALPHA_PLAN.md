# FranyuLauncher 1.4 — Plan maestro de Alpha

## Objetivo
1.4 no será una colección de funciones pequeñas. Será una evolución por sistemas completos, construida sobre la infraestructura que ya existe y sin duplicar funcionalidades.

## Reglas de ingeniería
- Mantener nombres de archivos existentes salvo necesidad real.
- No eliminar funcionalidades existentes para sustituirlas por versiones incompletas.
- No tocar el flujo de Ely.by, sus credenciales, URLs ni OAuth.
- Cada Alpha debe compilar antes de pasar a la siguiente.
- Preferir ampliar clases existentes antes que crear duplicados.
- Separar UI, estado persistente, lógica de negocio y operaciones de archivos.
- Todas las operaciones destructivas tendrán confirmación y manejo de errores.
- Las funciones experimentales deben poder desactivarse.
- La compatibilidad con Android y el flujo móvil son requisitos de primera clase.

## Auditoría inicial: infraestructura ya existente
### Instancias
Existe `Instance`, `InstanceManager`, `InstanceEditorFragment`, adaptadores, instalador y perfiles de compatibilidad. También existe `lastPlayedAt`.
**Decisión:** ampliar este sistema; no crear otro gestor de instancias.

### Mods
Ya existen búsqueda/listado/versiones e integración con Modrinth/CurseForge. `FranyuModInspector` ya detecta duplicados por ID y hace una correlación básica de RAM.
**Decisión:** convertirlo en un Mod Center, evitando otro inspector paralelo.

### Descargas
Existe `Downloader`, tareas de descarga, comprobación y progreso.
**Decisión:** construir un Download Center encima de esta infraestructura.

### Rendimiento
Existe `FranyuPerformanceGuard` con batería, carga lenta, temperatura, comprobación previa al lanzamiento y perfil seguro.
**Decisión:** convertirlo en Performance Center, no crear otro sistema de batería/temperatura.

### Recuperación
Existe papelera temporal mediante `FranyuTrashManager`, compartir mediante `FranyuShareManager` y funciones de almacenamiento.
**Decisión:** unificar recuperación, copias, importación/exportación y reparación.

### Cuentas
Ya existen Microsoft/Ely.by y sus flujos de autenticación.
**Decisión:** crear Account Center alrededor de los sistemas actuales. Ely.by permanece intacto.

### Apariencia
Existe `FranyuFeatureStore` para preferencias de funciones y colores.
**Decisión:** evolucionarlo hacia Theme Center.

### Actualizaciones
Existe `UpdateChecker` + `UpdateStartupGate`, ya usado como puerta de entrada.
**Decisión:** conservarlo y endurecerlo, sin crear otro updater.

### Diagnóstico
Ya existe comprobación previa al lanzamiento y `CrashWhyActivity`.
**Decisión:** convertirlos en un Diagnostics Center común.

## Arquitectura funcional 1.4
1. **Library/Home** — biblioteca de instancias, búsqueda, filtros, recientes y lanzamiento.
2. **Instance Center** — configuración completa, versiones, modloader, Java, renderer, controles, packs, mundos, herramientas.
3. **Mod Center** — mods instalados, búsqueda, actualizaciones, dependencias, duplicados/conflictos y compatibilidad.
4. **Download Center** — cola global, pausa/reanudación, prioridad, reintentos, ETA, errores y descargas programadas.
5. **Performance Center** — perfiles, RAM, Java, batería, temperatura, Charge & Play y diagnósticos.
6. **Recovery Center** — backup/restore, reparación, migración, papelera, import/export y compartir.
7. **Account Center** — cuentas, cuenta activa y asociación por instancia.
8. **Diagnostics Center** — preflight, logs, crash explanation y correlaciones con advertencias explícitas.
9. **Theme Center** — paleta, fondos, animación opcional y preferencias visuales.
10. **Update/Release Center** — updater de arranque y posteriormente historial/estado de releases.

## Integración de las funciones pequeñas ya existentes
- Low Battery + Charge & Play + temperatura -> Performance Center.
- Storage precheck + descargas nocturnas -> Download Center.
- Duplicados + conflictos + RAM correlation -> Mod/Diagnostics Center.
- CrashWhy -> Diagnostics Center.
- VersionComparator -> Instance Center.
- QR/share -> Recovery Center.
- Trash 7 días -> Recovery Center.
- Palette + animated background -> Theme Center.
- Widget -> Library/Home.
- PackPreview -> Mod/Pack Center.

## Roadmap
### Alpha 1 — Foundation
- navegación central de 1.4;
- Library/Home nueva sobre InstanceManager;
- modelo de estado y preferencias centralizado;
- base de Instance Center;
- Diagnostics común;
- integración de funcionalidades existentes;
- regresión y compilación.

### Alpha 2 — Mod Center
- inventario real de mods;
- estado activo/inactivo;
- actualización;
- dependencias;
- conflictos;
- compatibilidad con versión/modloader;
- previews.

### Alpha 3 — Download Center
- cola global;
- prioridad;
- pausa/reanudar/cancelar;
- reintentos;
- ETA;
- almacenamiento previo;
- programación nocturna;
- recepción de resultados.

### Alpha 4 — Performance Center
- perfiles Ahorro/Equilibrado/Rendimiento/Personalizado;
- RAM/Java;
- temperatura;
- batería;
- Charge & Play;
- preflight configurable.

### Alpha 5 — Recovery Center
- backup/restore;
- reparación;
- import/export;
- papelera segura;
- compartir sin mundo;
- comprobaciones de integridad.

### Alpha 6 — Diagnostics Center
- diagnóstico central;
- logs;
- explicación de crashes;
- historial de fallos;
- correlaciones de mods con lenguaje no concluyente.

### Alpha 7 — Account + Theme
- gestión de cuentas;
- cuenta activa;
- asociación por instancia;
- editor de paleta;
- fondos;
- animación opcional.

### Alpha 8 — Mobile UX / Polish
- rendimiento de UI;
- accesibilidad;
- estados vacíos;
- errores recuperables;
- orientación horizontal sin fullscreen;
- navegación táctil;
- pruebas de rotación y ciclo de vida.

### Beta
Solo estabilización: bugs, rendimiento, compatibilidad, migraciones y regresiones. No añadir grandes sistemas nuevos.

## Criterio de finalización de 1.4
La versión 1.4 debe sentirse como un launcher reorganizado alrededor de sistemas completos, no como una lista de toggles. Cada Center debe reutilizar la infraestructura original y compartir estados, errores, progreso y preferencias.

## Primera implementación
La Alpha 1 comienza con Foundation. Antes de tocar funciones delicadas:
1. conservar 1.3 como referencia;
2. trabajar en `feat/franyu-1.4-alpha`;
3. crear la arquitectura base sin borrar sistemas actuales;
4. integrar primero Library/Instance/Diagnostics;
5. compilar;
6. corregir regresiones;
7. solo después avanzar a Alpha 2.
