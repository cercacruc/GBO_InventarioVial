# Revisión de ingeniería de puentes 2

## Inspección previa

Base revisada: `8cf36fc2a2fad78e172f07757a7174a8a68fe4b2`, árbol limpio al comenzar. Room: **7**.

- SIC-18A ya tiene entidad, persistencia, formulario, herencia de SIC-18, exportador y acceso después del guardado. Faltan aviso con acción durante la captura y estado explícito en historial.
- SCAP → SIC-17 conserva los campos seguros, pero faltan los códigos complementarios. Se reutilizarán `scap_values` y los catálogos SIC-17 existentes.
- C5 ya muestra apoyos y juntas en lista. C2/C3/C4/D1/D4 todavía ocultan elementos mediante selectores.
- F2 es una lectura de elementos presentes; las condiciones y defectos están separados en F1/F3. Se integrarán sin eliminar registros.
- El panel fotográfico admite varias fotos, descripción, categoría, orden y eliminación; falta organizarlo por tomas visibles y ofrecer copia sellada.
- `PhotoStampService` conserva el original, normaliza EXIF y genera el sello técnico inferior derecho. Se extenderá con el PNG oficial, sin cambiar su contenido.
- La identidad azul/rojo GBO ya está aplicada en el tema; se reutilizará.

## Análisis de la plantilla antes de modificar el exportador

Fuente canónica: `docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx`, SHA-256 `1df9f651722d06d6cd886c9be3edf565df7aae8b6fca10097011f48b12430b13`.

En la primera hoja, C4 ocupa los bloques de estribos (118–126), pilares (168–183) y anclajes (187–199). Los pilares tienen **tres columnas de valores**, G, J y N, en las filas 175, 177, 181 y 183. N173 dice “Pilar 3, 4 y 5”: es un bloque colectivo, no tres bloques individuales. D1 dispone de suelos en J392, L392 y N392. No hay fórmulas de condición global que dependan de esos valores de pilares.

C2 distingue dos tipologías (filas 72–86), C3 tiene un bloque de tablero (91–110), C5 dos columnas de apoyos (237–245) y una junta (247–251). La copia exportada puede extender estos bloques hacia abajo manteniendo las coordenadas originales y sus referencias. Esto evita desplazar fórmulas y dibujos existentes. Las extensiones deben duplicar estilos, alturas, merges y validaciones del bloque y extender la impresión. El archivo canónico no se modifica.

## Cambios entregados y verificados

- **SIC-18A existente:** aviso rojo junto a las condiciones cuando estructural o funcional es 3. «COMPLETAR SIC-18A» usa el mismo guardado validado de SIC-18 y abre la complementaria después del éxito. Tras guardar se muestra «registrado», con edición y exportación. Historial: No requerido / Pendiente / Completo, con herencia vigente de clase, tipo y vanos. Se mantienen ruta, calzada y progresiva del registro padre.
- **Complemento SIC-17:** bloque separado en A y G con catálogos SIC existentes. Persiste como claves `sic17.*` en `scap_values`; valida las combinaciones clase/tipo. Se mantienen los requisitos del exportador, sin valores adivinados. Completarlo habilita SIC-17/17A/17B cuando sus demás requisitos están satisfechos. Estas claves no se exportan a SCAP.
- **Elementos físicos visibles:** C2/C3 muestran cada tramo y tablero; C4 muestra ambos estribos y anclajes aunque estén vacíos, más todos los pilares; C5 conserva apoyos y juntas en tarjetas; C6/C7 muestran ambos accesos; D1/D4 muestran todos los elementos/puntos. Los selectores se usan para características. La eliminación de filas requiere confirmación.
- **N pilares y continuaciones:** se exportan los tres primeros individualmente y se duplican bloques para el resto. Se añaden suelos, apoyos, juntas y tableros por tramo. Se conservan posiciones originales, alturas, estilos, merges, validaciones y fórmulas; se amplían dimensiones y rangos de impresión en la copia. No se toca la plantilla canónica. Verificado con siete pilares de atributos distintos, cuatro apoyos, tres juntas y cuatro tramos.
- **F2 integrado:** 14 recordatorios (104, 110, 111, 202, 205, 302, 311, 353, 372, 401, 402, 501, 511, 530), cinco grupos, búsqueda entre los 112 elementos del catálogo, presencia explícita, metrado, seis porcentajes, descripción y ubicación editables, múltiples observaciones y fotografías asociadas. Ver los recordatorios no crea evaluaciones. Marcar No aplica conserva los datos. F1/F3 salen de la navegación; una sección restaurada F1/F3 se redirige a F2. Se conservan los defectos generales sin código.
- **Panel fotográfico:** nueve tarjetas principales con captura/importación directa, varias fotos, miniaturas, descripción, categoría, elemento, orden y acceso al sello. Se conservan las seis categorías adicionales históricas. La hoja fotográfica mantiene su nombre y añade categoría legible a la descripción. Las imágenes del libro se encajan proporcionalmente en sus marcos, sin recorte.
- **Logo oficial:** copia byte por byte de `D:/Downloads/GNO_transparente.png` a `app/src/main/res/drawable-nodpi/gbo_logo_watermark.png`, RGBA 1448 × 1086. No se usa el JPG. `PhotoStampService(context)` lo decodifica con `inScaled=false`, lo dibuja mediante Canvas después de normalizar EXIF y genera una copia sellada independiente.
- **Configuración reutilizable:** `showCorporateLogo=true`, `logoWidthFraction=0.18f`, `logoAlpha=200` (78,4 %; rango 0–255), `logoPosition=TOP_RIGHT`, margen 2,5 %. En panoramas extremos se reduce proporcionalmente para reservar espacio al texto inferior derecho. La transparencia original se combina con el alpha configurado. La fotografía original no se escribe; `stampedPath` sigue teniendo prioridad en Drive y exportación cuando existe.
- **Dos correcciones detectadas durante la verificación:** conservar los otros porcentajes al editar una condición histórica sin filas auxiliares; dejar vacía la búsqueda de unidad en las filas auxiliares de F sin código, evitando tres #N/D. Las búsquedas con un código real siguen mostrando un error si el código es incorrecto. Las fórmulas de condición G se conservan.

## Compatibilidad y archivos conservados

Room **7 → 7**. No cambia ninguna entidad ni el esquema; no se necesita migración nueva. Se conservan las migraciones existentes, sin fallback destructivo. Los estribos y anclajes virtuales se materializan únicamente al editar. Los recordatorios y las observaciones vacías no se guardan como datos observados. No se borra ni renumera información histórica.

Comparación por SHA-256 con el inventario tomado antes de editar: **12 archivos protegidos sin cambios**, incluyendo `google-apps-script/`, `app/schemas/`, plantilla canónica y ejemplos de configuración. No se modificaron protocolos/endpoint de Drive, `DriveUploadPolicy`, `EndLocationPolicy`, credenciales ni archivos de configuración local. No se crearon commits ni se ejecutaron push/reset. HEAD permanece `8cf36fc2a2fad78e172f07757a7174a8a68fe4b2`.

SHA-256 del PNG oficial incorporado: `4f126a163787bf58f00ec5a7e226d120ab924f37bd6ae539015fff0645dda0dc`.

## Verificación automatizada

Comando ejecutado: `gradlew.bat testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin --console=plain`.

- **111 pruebas, 0 fallos, 0 errores, 0 omitidas**. Incluye 14 pruebas nuevas: CorporateWatermarkTest (4), Engineering2DataTest (4), Engineering2ScreenTest (5), ScapFullExportTest (1).
- `assembleDebug`: correcto. APK local: `app/build/outputs/apk/debug/app-debug.apk`.
- `compileDebugAndroidTestKotlin`: correcto. La suite instrumentada compila; no se ejecutó en una tablet física.
- SIC-17: requisitos faltantes, ingreso/persistencia, habilitación de las tres exportaciones, rechazo de tipo inválido y ausencia del complemento en SCAP.
- SIC-18A: tres estados, guardado/edición sin duplicar, herencia y XLSX; prueba Compose del acceso visible junto a condición mala. La transición completa con cámara/GPS debe comprobarse en tablet.
- SCAP: siete pilares recuperados tras cerrar/abrir Room, apoyos/juntas, observaciones múltiples y categorías históricas; exportación completa y reapertura de diez hojas.
- Watermark: PNG exacto y con alpha; original idéntico byte por byte; diferencia de píxeles localizada arriba a la derecha; opacidades 0/100/200 y logo desactivado; proporción/márgenes; ocho orientaciones EXIF; fotos verticales/horizontales y panoramas; texto abajo a la derecha. También pasan las pruebas existentes de preferencia `stampedPath`.
- Microsoft Excel 16: libro final abierto en solo lectura, recálculo completo, sin errores de fórmula y con 12 imágenes incrustadas. Condición sintética **2,3926073822254574 → REGULAR**. El informe de celdas está en [SCAP_FULL_EXPORT_VERIFICATION.md](SCAP_FULL_EXPORT_VERIFICATION.md).
- Apps Script: no afectado; no fue necesario ejecutar sus pruebas.
- `git diff --check`: correcto.

## Límites y pendientes reales

1. Pendiente el checklist de tablet física de abajo: cámara real, permisos, GNSS, navegación completa y apertura/compartición desde Android. Las pruebas Compose se ejecutaron con Robolectric, sin afirmar validación en hardware.
2. El formato SCAP original conserva cupos para **14 elementos evaluados distribuidos 3/2/4/2/3 por grupo**, **32 fotografías**, **15 puntos de perfil** y **un croquis por tipo**. Si se exceden, se bloquea la exportación con explicación; no hay truncamiento silencioso. Capturar otros elementos está permitido. Pilares, apoyos, juntas y tramos tienen continuaciones; quedan sujetos al límite técnico de filas de XLSX y memoria del dispositivo.
3. Conclusiones y recomendaciones de texto libre siguen sin un mapeo aprobado; se exportan vacías. Se conserva el método de cálculo de la plantilla.
4. **Impresión en Excel de este equipo:** los nombres originales `_xlnm.Print_Area` y `_xlnm.Print_Titles` se leen como nombres ordinarios en Excel 16, tanto en la fuente canónica como en el exportado. La automatización de impresión del panel produjo páginas adicionales. El XML mantiene sus rangos y las nuevas continuaciones, pero no se afirma que la impresión predeterminada esté validada. Para revisar las fotos se aplicó explícitamente el rango A1:F21 en una sesión aislada, sin guardar el libro. Revisar/establecer las áreas antes de imprimir; los rangos se documentan en el informe de exportación. No se alteró la fuente para eludir este comportamiento.
5. La subida de una inspección SCAP completa sigue limitada al contrato actual del servidor; la generación del Excel es local. No se cambió Apps Script ni se habilitó una subida incompatible.

## Checklist manual en tablet — pendiente

No se marca ningún paso como ejecutado en hardware.

- [ ] 1. Crear SCAP.
- [ ] 2. En C4 completar ambos estribos.
- [ ] 3. Añadir siete pilares.
- [ ] 4. Dar a cada pilar atributos y suelo distintos; cerrar/reabrir y verificar.
- [ ] 5. Añadir varios apoyos.
- [ ] 6. Añadir varias juntas.
- [ ] 7. En F2 verificar los 14 recordatorios sin presencia inferida.
- [ ] 8. Editar una descripción directamente.
- [ ] 9. Buscar y añadir el elemento 112 sin duplicarlo.
- [ ] 10. Registrar una anomalía y varias observaciones del mismo elemento.
- [ ] 11. Capturar lado derecho, izquierdo, aguas arriba, abajo, carretera, señal, barandas/veredas/tuberías y anomalía; importar una foto adicional.
- [ ] 12. Revisar watermark: esquina superior derecha, transparencia y proporción; texto inferior; original intacto; probar vertical/horizontal.
- [ ] 13. Completar los datos complementarios SIC-17.
- [ ] 14. Exportar SIC-17.
- [ ] 15. Exportar SIC-17A.
- [ ] 16. Exportar SIC-17B.
- [ ] 17. Exportar SCAP.
- [ ] 18. Abrir SCAP en Microsoft Excel y revisar áreas antes de imprimir.
- [ ] 19. Confirmar los siete pilares y sus suelos distintos, también en continuaciones.
- [ ] 20. Confirmar F2, porcentajes y observaciones múltiples.
- [ ] 21. Confirmar fotos, categorías, descripciones y croquis.
- [ ] 22. Crear SIC-18 con condición estructural o funcional mala.
- [ ] 23. Pulsar el acceso inmediato SIC-18A; confirmar guardado previo del padre.
- [ ] 24. Completar SIC-18A y comprobar el mensaje de registro y sus acciones.
- [ ] 25. Reabrir, editar y comprobar estado Completo en historial.
- [ ] 26. Exportar XLSX y comprobar cabecera heredada y cinco códigos.

## Archivos y estado de Git

No hay eliminaciones ni archivos preparados en el índice. Se deja el árbol de trabajo para revisión y un commit del usuario.

```text
 M app/build.gradle.kts
 M app/src/androidTest/java/com/tuempresa/inventariovial/EngineeringUxScreenTest.kt
 M app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt
 M app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt
 M app/src/main/java/com/tuempresa/inventariovial/camera/PhotoStampService.kt
 M app/src/main/java/com/tuempresa/inventariovial/catalog/SicCatalogRepository.kt
 M app/src/main/java/com/tuempresa/inventariovial/field/CaptureToolsUi.kt
 M app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt
 M app/src/main/java/com/tuempresa/inventariovial/field/SurveyHeader.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapToSicMapper.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapExcelExporter.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapTemplateMedia.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/export/TemplateWorkbook.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapElements.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapEngineeringPanels.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapForms.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapMedia.kt
 M app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapWorkspace.kt
 M app/src/test/java/com/tuempresa/inventariovial/EngineeringUxTest.kt
 M app/src/test/java/com/tuempresa/inventariovial/PhotoStampServiceTest.kt
 M app/src/test/java/com/tuempresa/inventariovial/ScapExportTest.kt
 M docs/CHECKLIST_TABLET_UX.md
 M docs/ENGINEERING_RELEASE.md
 M docs/REVISION_INGENIERIA_UX.md
 M docs/SCAP_IMPLEMENTATION.md
 M docs/SCAP_PENDING_CLIENT_DECISIONS.md
 M tools/verify_excel_export.ps1
?? app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapPresentation.kt
?? app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapSicSupplement.kt
?? app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapSicSupplementPanel.kt
?? app/src/main/java/com/tuempresa/inventariovial/supplementary/Sic18AStatus.kt
?? app/src/main/res/drawable-nodpi/gbo_logo_watermark.png
?? app/src/test/java/com/tuempresa/inventariovial/CorporateWatermarkTest.kt
?? app/src/test/java/com/tuempresa/inventariovial/Engineering2DataTest.kt
?? app/src/test/java/com/tuempresa/inventariovial/Engineering2ScreenTest.kt
?? app/src/test/java/com/tuempresa/inventariovial/ScapFullExportTest.kt
?? docs/REVISION_INGENIERIA_PUENTES_2.md
?? docs/SCAP_FULL_EXPORT_VERIFICATION.md
```
