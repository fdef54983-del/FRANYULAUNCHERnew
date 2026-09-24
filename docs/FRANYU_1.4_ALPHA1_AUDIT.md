# FranyuLauncher 1.4 Alpha 1 — Auditoría de duplicados

## Ya existente y reutilizado
| Sistema | Existía | Acción 1.4 |
|---|---|---|
| Instancias | Instance + InstanceManager + InstanceEditorFragment | Reutilizar como fuente de verdad |
| Biblioteca/home | MainMenuFragment + layouts | Evolucionar, no reemplazar por otro launcher |
| Mods | SearchModFragment + ModVersionListFragment + Modrinth/CurseForge | Convertir en Mod Center |
| Descargas | Downloader + tareas | Convertir en Download Center |
| Rendimiento | FranyuPerformanceGuard | Convertir en Performance Center |
| Diagnóstico | preLaunchCheck + CrashWhyActivity | Centralizar en Diagnostics Center |
| Papelera | FranyuTrashManager | Integrar en Recovery Center |
| Compartir | FranyuShareManager | Integrar en Recovery Center |
| Preferencias Franyu | FranyuFeatureStore | Base de Theme/Feature settings |
| Actualizador | UpdateChecker + UpdateStartupGate | Mantener como sistema único |
| Cuentas | Microsoft + Ely.by | Mantener flujos actuales |
| Packs/previews | PackPreviewActivity e infraestructura de packs | Integrar en Mod/Pack Center |
| Comparador | VersionComparatorActivity | Integrar en Instance Center |
| Widget | FranyuWidgetProvider | Integrar con Library/Home |

## Duplicados evitados
- No se creó un segundo InstanceManager.
- No se creó un segundo sistema de descargas.
- No se creó un segundo updater.
- No se creó un segundo sistema de batería/temperatura.
- No se creó un segundo sistema de papelera.
- No se creó un segundo sistema de cuentas.
- No se creó un segundo sistema de diagnóstico.

## Alpha 1 implementada en esta rama
- Documento maestro de planificación 1.4.
- FranyuInstanceCenter como fachada de Library/Instance, usando InstanceManager.
- FranyuDiagnosticsCenter como fachada de diagnóstico, usando las fuentes existentes.
- FranyuPerformanceGuard ahora consume la fachada central para el preflight.
- MainMenuFragment usa FranyuInstanceCenter para recientes y conteo de mods.
- Se eliminaron restos de updater de menú que estaban vacíos; el updater válido sigue siendo UpdateStartupGate.
- No se alteró Ely.by.
- No se cambiaron nombres de archivos existentes.

## Pendiente de Alpha 1
- Navegación real entre Library, Instance Center y Diagnostics.
- Pantallas de estado/errores comunes.
- Pruebas de ciclo de vida y rotación.
- Build Android completo.
- Revisión de regresiones antes de Alpha 2.
