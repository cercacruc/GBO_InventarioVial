# Corrección funcional y UX — 25 de septiembre de 2026

> Documento de una revisión anterior. Para el estado actual de Room 7, las continuaciones XLSX, F2 integrado, SIC-17/18A y watermark, consultar [Revisión de ingeniería de puentes 2](REVISION_INGENIERIA_PUENTES_2.md). Los límites antiguos de pilares, apoyos y juntas quedan superados por esa revisión.

Implementación sobre el repositorio existente, para revisión de la ingeniera. Sin commit ni push. Las decisiones explícitas de esta revisión prevalecen sobre interpretaciones anteriores del Manual.

## Alcance por punto solicitado

| Punto | Resultado y ubicación |
|---|---|
| 1. Diseño | Tema Material 3 claro, azul corporativo, acento rojo, tarjetas blancas, fondo neutro, sin gradientes ni sombras decorativas. Logo existente `mipmap-xxxhdpi/ic_launcher_foreground.webp`. Pantalla de inicio con dos columnas; encabezado SCAP azul; controles de condición grandes con texto y marca de selección. |
| 2. No aplica | `ScapFieldPolicy.NOT_APPLICABLE` es un texto interno distinto de cualquier código oficial. Se agrega a desplegables de componentes técnicamente opcionales, sin sustituir sus catálogos. No hay selector global Aplica/No aplica. Vacío significa sin completar. |
| 3. Ruta SCAP | `ScapFields` consulta `SurveyPreferences.routes(segment)`, la misma fuente usada por SIC, sin lista duplicada. Mantiene la ruta histórica aunque el catálogo cambie. Room y exportador conservan la selección. |
| 4–5. C.5 | Orden: A Barandas; B Veredas y Sardineles; C Apoyos; D Juntas de Expansión; E Drenaje de Calzada. Tarjeta independiente por subsección. Veredas usa el catálogo real de material; no se inventó un catálogo de tipos. |
| 6. Colecciones | Apoyos conserva su colección. Juntas añade `scap_joints` con UUID, índice, tipo y material, los únicos campos mapeados en la plantilla. Añadir, editar, confirmar eliminación y reabrir. El límite del Excel solo bloquea exportación, nunca la captura. |
| 7. Croquis | Once medidas decimales en metros bajo E, con aviso de uso interno. Estados para componentes opcionales dentro de sus desplegables. Una selección No aplica deshabilita las medidas del grupo y conserva sus valores. |
| 8. Sección F | `F.2 · CONDICIÓN GLOBAL DEL PUENTE` interpreta la hoja física `sec F`. Cinco grupos I–V muestran código, nombre del elemento y descripción procedente de los defectos del mismo elemento. G conserva cálculo y fórmulas. |
| 9. Fotografías SCAP | Panel con número automático, fecha, descripción editable, categoría, elemento, miniatura y estado local. Cámara/importación, eliminar con confirmación y mover arriba/abajo. La hoja física sigue siendo `F.2.-PANEL FOTOGRAFICO`. |
| 10. SIC-18 textos | Los tres criterios estructurales aparecen en las opciones inmediatamente bajo CONDICIÓN ESTRUCTURAL. Funcional tiene su propia explicación de obstrucción. No hay inputs nuevos de porcentajes. |
| 11. Fotos SIC-18 | Tres apartados: panorámica, entrada y salida, cada uno con contador, miniaturas, cámara, selector y eliminación confirmada; para reemplazar, eliminar y capturar/seleccionar otra en el mismo apartado. Persistencia explícita de categoría. Fotos antiguas sin categoría permanecen como tales hasta que el usuario las clasifique. |
| 12. SIC-18A | Se habilita la base existente. Condición 3 estructural o funcional ofrece completar la ficha después de guardar SIC-18, y desde historial. No crea filas automáticamente. FK `recordId` reutiliza el UUID de SIC-18. Cabecera/clase/tipo/vanos heredados, validación de catálogos, edición, resumen en historial y exportación XLSX desde la ficha. |
| 13. SIC-20 funcional | Obligatoria en Badén, Túnel y Muro. Visible en captura, revisión, historial y edición, persistente y exportada en la columna existente. Los valores nulos históricos se conservan como pendientes; no se rellenan silenciosamente. |
| 14. Longitud muro | `wallLengthMeters`, nullable, interno. No reemplaza altura promedio (dimensión 1); dimensión 2 continúa vacía en la exportación de muro. No agrega columnas SIC. |
| 15. SIC-19 | Tierra muestra erosión; Concreto/Mampostería muestran criterio del 30%. Otro exige seleccionar Tierra o Pavimentado; se conserva en `structuralCriterion`. La columna oficial sigue conteniendo 1/2/3. |
| 16. Condiciones | Tarjetas táctiles de mínimo 64 dp, borde visible, símbolo y texto Seleccionado, descripción de cada nivel. Verde correcto; ámbar regular/revisión; rojo malo/error. |
| 17. Navegación | Mantiene A–G, subpestañas C.1–C.10 y subsecciones de C.5. Cabecera con nombre, ruta, progresiva y estado de guardado. Estados de detalle: Sin completar, Completado, No aplica, Revisión. |
| 18. Plantilla | El XLSX canónico y `tools/scap_field_map.json` no se modifican. Se reutiliza el exportador existente para preservar hojas físicas, estilos, geometría, fórmulas estadísticas, nombres definidos y logos. Fotografías del ejemplo se sustituyen por las capturadas como en el exportador anterior. |
| 19. Internos | Claves `internal.sketch.*`, `wallLengthMeters`, `structuralCriterion`, categorías de fotos y `NOT_APPLICABLE` no generan columnas contractuales SIC/SCAP. |
| 20. Room | Versión 7; migración explícita 6→7 y cadena anterior preservada. Schema JSON generado por KSP. Sin borrado de base ni migración destructiva. |
| 21. Pruebas | Suite existente conservada; nuevas pruebas de política/validación, almacenamiento, exportación, migración desde 6 y pantallas Compose. Resultados finales en sección de verificación. |
| 22. Tablet | Checklist manual separado en `CHECKLIST_TABLET_UX.md`, para completar en campo. |
| 23. Entrega | Cambios locales, APK debug, listado de archivos y resultados reproducibles. |

## Decisiones de ingeniería y límites explícitos

- SIC-20 funcional en las tres clases es un override expreso del proyecto. No se infiere otra regla del Manual ni se crea una columna adicional.
- SIC-19 Otro requiere un criterio interno. No se infiere por similitud del nombre o material.
- `No hay` y `No tiene` del catálogo canónico se conservan como opciones reales. También desactivan sus dependientes, como ausencia explícita del componente.
- Al pasar a `No aplica`, si hay datos dependientes se pide confirmación para **marcar y conservar**. No existe una eliminación implícita. Cambiar a una opción real vuelve a habilitarlos.
- La plantilla contiene el literal `No Aplica` en H249, correspondiente al tipo de junta. Solo allí se escribe ese literal para el estado interno. En otros campos se deja vacío si no hay representación explícita. Nunca se inventa 0, 99 u otro código.
- Juntas: el mapa canónico valida H249 (tipo) y H251 (material). J249 contiene texto de ejemplo sin correspondencia de una segunda junta en el mapa; no se asume una segunda columna. Se limita la exportación a **una junta**. Si el cliente valida una capacidad diferente, habrá que documentar y probar el mapeo, manteniendo el archivo canónico.
- Apoyos: la plantilla admite dos grupos H/J. Se puede capturar un número mayor, pero la exportación queda bloqueada con aviso.
- Croquis: no hay catálogos oficiales de existencia/tipo para las medidas auxiliares nuevas. El desplegable ofrece Sin completar y No aplica, sin inventar tipos; las medidas se ingresan manualmente. Para divisorio, aceras y barandas, No aplica deshabilita ambos campos del grupo.
- SIC-18A queda asociado uno a uno al SIC-18 existente. Fecha/ruta/calzada/PR/distancia se leen del padre; clase/tipo/vanos son de solo lectura en SIC-18A. Editar SIC-18 actualiza estos tres valores en su ficha complementaria si ya existe.
- El botón XLSX de SIC-18A exporta el contenido visible validado. Guardar la ficha sigue siendo la acción que persiste su edición; no se oculta ese paso.
- Los datos internos nuevos no se agregan al payload del servidor ni al contrato de Drive. Su respaldo remoto requiere un alcance específico posterior.

## Migración 6 → 7

`EngineeringMigration.MIGRATION_6_7`:

1. Agrega `sic19_details.structuralCriterion TEXT` nullable.
2. Agrega `sic20_details.wallLengthMeters REAL` nullable.
3. Agrega `photos.description TEXT` nullable; no cambia IDs, índices, nombres, rutas, archivos ni estado de Drive.
4. Crea `scap_joints` con FK a inspección e índice único de inspección/posición.
5. Promueve los valores históricos `inspection/jointType` y `inspection/jointMaterial` a la primera junta. Conserva también sus valores originales para trazabilidad.

Las medidas auxiliares se almacenan en `scap_values`; no duplican campos contractuales. Se preservan las tablas SIC-18A y categorías fotográficas que ya existían.

## Google Drive / Apps Script

La revisión se hizo sobre `google-apps-script/Code.gs` y `DriveUploadWorker`; no sobre una implementación publicada.

**No hace falta cambiar el script para las fotos SIC de esta entrega.** Las categorías se guardan localmente y la petición de subida conserva token, ruta, SIB, SIC, nombre y contenido. Fotos históricas conservan `generatedFileName`, `originalPath`, `stampedPath`, IDs remotos y estados. Las fotos nuevas añadidas desde historial usan el fallback de nombre de archivo ya soportado por la cola de Drive.

**Exportar XLSX no significa sincronizarlo automáticamente.** Actualmente la app guarda/entrega los archivos mediante el selector de documentos. `DriveUploadWorker` procesa imágenes, no libros Excel. El script construye blobs con el MIME recibido, pero su lista de códigos válidos solo incluye SIC-17…SIC-23, sin SIC-18A. Para una futura subida automática de Excel hay que implementar una cola separada en Android y acordar destino, nombre/versionado y comportamiento de reintentos; para SIC-18A, ampliar además la validación del código en el script. No basta cambiar solamente Apps Script.

No se cambiaron `Code.gs`, las carpetas remotas, la implementación web ni las credenciales. No se realizaron envíos a Drive durante las pruebas.

## Verificación

Verificación final del 25 de septiembre de 2026:

- `testDebugUnitTest`: **97 pruebas aprobadas**, cero fallos, errores u omitidas. Incluye migración, persistencia, validación y exportación.
- `assembleDebug` y `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**. APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Instrumentación en emulador Pixel_Tablet: **15 pruebas aprobadas**, incluidas 8 de esta revisión. Log: `tmp/ux-ui-final.log`.
- Apps Script: **8 pruebas aprobadas**; código del script sin cambios.
- Revisión visual de capturas de inicio, C.5 y condiciones realizada en el emulador.
- Plantilla canónica sin cambios. SHA-256: `1df9f651722d06d6cd886c9be3edf565df7aae8b6fca10097011f48b12430b13`. Mapa canónico sin cambios.

Log de compilación y pruebas unitarias: `tmp/ux-verify2.log`; informe HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.

Pendiente de aceptación manual en la tablet: cámara real, permisos/GNSS, legibilidad bajo sol, conexión real a Drive y apertura/recalculado del XLSX en Excel. Las pruebas automatizadas de estructura y contenido del libro no sustituyen esa apertura. Usar `CHECKLIST_TABLET_UX.md`. No se hizo commit, push ni despliegue de Apps Script.

En este Windows, el problema previo `Unable to establish loopback connection` se evitó usando un directorio temporal corto y la preferencia IPv4 en el proceso de Gradle. No se cambió la configuración global de Java:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:TEMP='C:\PROYECTOS\GBO_InventarioVial\tmp'
$env:TMP=$env:TEMP
$env:JAVA_OPTS='-Djava.net.preferIPv4Stack=true -Djava.io.tmpdir=C:\PROYECTOS\GBO_InventarioVial\tmp'
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest --console=plain
```

La prueba visual/instrumentada se dirige exclusivamente al emulador Pixel_Tablet. No se instala esta versión ni se ejecutan pruebas sobre la tablet física conectada.
