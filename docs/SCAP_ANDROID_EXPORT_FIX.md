# Exportación SCAP compatible con Android

Verificación actual: 26/09/2026. El código encontrado todavía llamaba a `external-general-entities` y `external-parameter-entities`, aunque la versión anterior de este documento afirmaba que estaban eliminadas. Se confirmó la discrepancia; no se determinó qué merge o cambio la originó. La fábrica DOM de Android no admite esos indicadores SAX y fallaba antes de procesar la plantilla.

`TemplateWorkbook.document` ahora recorre primero los tokens con `Xml.newPullParser().nextToken()`, detectando la codificación desde los bytes originales y rechazando `DOCDECL` antes de crear el DOM. Una declaración `ENTITY` aislada también se rechaza como XML inválido. Los errores no se ignoran. Después utiliza DOM con espacios de nombres y un `EntityResolver` que lanza una excepción ante cualquier resolución externa. No se invocan las features SAX incompatibles ni se decodifica/reconstruye el contenido antes del DOM.

La prueba en la tablet descubrió además un segundo fallo: `ScapTemplateMedia.replace` suponía que la lista de nodos `srcRect` se actualizaba al eliminar un nodo. En Android la lista conservaba el nodo retirado, provocando un NullPointerException en la siguiente iteración. Se corrige tomando una lista de los nodos y eliminando cada uno una sola vez.

No se modificaron `ScapExcelExporter`, la plantilla, los mapeos, las dependencias ni la ampliación dinámica mediante `copyRows`. No se añadió Apache POI.

Validación realizada:

- `assembleDebug` y `assembleDebugAndroidTest`: correctos.
- JVM/Robolectric: 22 pruebas aprobadas, cero fallos: `ScapCalculationTest` (7), `ScapExportTest` (7), `ScapFullExportTest` (1), `ScapStorageTest` (7).
- `ScapFullExportTest`: siete pilares con valores distintos, cuatro apoyos, tres juntas, cuatro tramos, elementos F2, defectos, panel fotográfico, croquis, fórmulas de condición estadística, doce imágenes, estilos y geometría original. Se conserva la ampliación de filas y fórmulas; no existe un nuevo límite de tres pilares.
- Android real: Samsung SM-T733, Android 14, `ScapAndroidExportTest`, **OK (3 tests)**. `exportsAndReopensRealTemplateOnAndroid` exporta y reabre la plantilla real, comprueba diez hojas y lee todas las partes XML/relaciones. `rejectsInternalAndExternalDtdInUtf8AndUtf16` cubre UTF-8, UTF-16, UTF-16LE y UTF-16BE, DTD internas, SYSTEM/PUBLIC, entidades externas generales y de parámetro y ENTITY aislada. `preservesNamespacesUnicodeAndEscapedText` conserva namespaces, caracteres acentuados, ideogramas, emoji y entidades XML normales en las cuatro codificaciones.
- APK instalado con `adb install -r`, sin desinstalar ni borrar datos. Las pruebas instrumentadas no acceden a la base de datos del usuario.
- Prueba de interfaz en tablet: fichas existentes «1» y «Hola», sección G, «Guardar Excel SCAP», selector Android y guardado en Descargas completados sin error SAX. No se editaron ni reabrieron las fichas cerradas.
- Los dos XLSX de tablet y el XLSX de `ScapFullExportTest` se abrieron en Microsoft Excel de escritorio sin aviso de reparación. El archivo de «Hola» contiene las diez hojas, nombre/código/ruta, elemento 104, cuatro fotos y tres croquis. Se inspeccionaron las cuatro imágenes exportadas: todas muestran la marca de agua GBO.
- Las fichas existentes probadas no contienen pilares; los siete pilares adicionales se verificaron con el caso sintético completo, no con esas fichas.

Evidencia local de esta ejecución (directorio `tmp`, no versionado): `scap-android-test.txt`, `engineering2/SCAP_COMPLETO_TEST.xlsx`, `engineering2/full-export-cells.json`, `SCAP_TABLET_EXISTENTE.xlsx`, `SCAP_TABLET_FOTOS.xlsx`. Informes JVM: `app/build/test-results/testDebugUnitTest`.

El primer arranque de Gradle falló por el socket local de Java en Windows. Se ejecutó con `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/PROYECTOS/GBO_InventarioVial/tmp`, sin modificar la configuración del proyecto.

Cambios pendientes de revisión; no se hizo commit ni push.

Para exportar una ficha existente: abrir SCAP, seleccionar la ficha, ir a G y pulsar «Guardar Excel SCAP»; seleccionar el destino en el selector de Android.
