# Validación de entrega — 24/09/2026

Base: `GBO_InventarioVial-main (1).zip`. Base de datos resultante: Room v4.

## Comandos ejecutados

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\gerbc\.gradle'
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest --console=plain
```

Resultado final: **BUILD SUCCESSFUL**, 83 tareas (43 ejecutadas, 40 actualizadas). **40 pruebas, 0 fallos, 0 errores, 0 omitidas.** Robolectric ejecutó pruebas de Room y gráficos/EXIF con SDK 28 en el equipo de desarrollo.

| Suite | Pruebas | Comprobación principal |
|---|---:|---|
| ExampleUnitTest | 1 | Prueba existente |
| InventoryRoomMigrationTest | 3 | Migración de versiones 1/2/3, conservación de datos y fotos, borradores, acuses atrasados, cierre de sesión |
| PhotoStampServiceTest | 2 | Original sin cambios, copia orientada, sello multilínea, EXIF y campos opcionales |
| RoadMatcherTest | 5 | Proyección, PR anterior, distancia, lado en ambos sentidos, precisión, ambigüedad, JSON/GeoJSON |
| RoadPositionTest | 6 | Progresiva oficial/estimada/desconocida, inserción retroactiva, anulación, grupos y desempate |
| Sic23Test | 5 | Formato y validaciones del ZIP actualizado |
| SicExcelWriterTest | 5 | Regresión del exportador entregado en el ZIP |
| TrackAndKmlTest | 8 | Curvas, filtrado, sentido decreciente, KML/XML, KMZ, pausas, geometrías SIC-20 |
| ValidationAndServerTest | 5 | Avisos, PR, JSON sin binarios/rutas, HTTPS, catálogos y NMEA |

Además, se generó correctamente el APK de instrumentación (`assembleDebugAndroidTest`). **Las pruebas instrumentadas no se ejecutaron en un dispositivo:** `adb devices -l` no mostró ninguna tablet/emulador conectado. Compilar ese APK no equivale a probar cámara, servicios, permisos o interacción real.

El APK de aplicación pasó `apksigner verify --verbose` con firma APK v2. Es una compilación **debug**, no una entrega firmada para producción.

## Preservación del ZIP suministrado

Se compararon 12 componentes/bloques contra la extracción del ZIP base: archivos Drive, exportador SIC, pantalla de exportación, entidad de exportación, PhotoUtils, funciones de nombres JPG y consulta de exportación. **Todos se conservaron**. Archivos comparados byte a byte; bloques de funciones comparados normalizando saltos de línea. Evidencia con hashes en `PRESERVACION_BASE.json`.

Las migraciones no eliminan tablas existentes y no usan `fallbackToDestructiveMigration`. La prueba de conservación abre las tres versiones anteriores con SQLite y deja que Room valide el esquema resultante. El fallo inicial de preparación de la prueba en Windows se resolvió acortando el nombre temporal de la base, sin cambiar la migración para ocultar errores.

## Advertencias y límites

- Gradle informa una opción experimental heredada de la base (`android.disallowKotlinSourceSets=false`) y compatibilidad de target Kotlin con JDK 25; la compilación terminó correctamente. Compose señala usos antiguos de Divider conservados en el proyecto.
- Pendientes de aceptación física: actualización sobre la base real, permisos Android, captura de cámara, segundo plano/ahorro de batería, sellos en fotos reales, recorridos, abrir/compartir KML y cartografía de la empresa.
- Sin API empresarial ni receptor externo: se probaron contratos y cálculos locales; no se afirma conexión real a servidor, Bluetooth, RTK o Google Earth.
- No se alteró la base de datos de una tablet ni se reinstaló la aplicación sobre un dispositivo.

## Reproducir y abrir

Descomprimir el proyecto, abrir su carpeta raíz en Android Studio e indicar el SDK Android local si hace falta. Los archivos `local.properties`, `drive.local.properties` y `server.local.properties` son específicos del equipo y no se incluyen en el ZIP. Gradle puede necesitar descargar dependencias en otra computadora.

Las configuraciones opcionales se describen en `IMPLEMENTACION_PENDIENTE_REUNION.md`. Para instalar sobre una versión existente debe utilizarse la misma clave de firma; no borrar datos ni desinstalar para forzar una actualización. El paquete conserva applicationId y versión de aplicación de la base, añadiendo la migración Room.
