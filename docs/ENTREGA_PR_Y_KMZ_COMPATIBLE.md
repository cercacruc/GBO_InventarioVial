# Preparación de sugerencias PR y KML/KMZ compatible

Fecha: 28/09/2026. Cambios locales, sin commit ni push.

## PR: cálculo y confirmación

Se conservan `RoadPr`, `RoadReferenceData.prs`, `RoadMatchResult` y `LinearReferenceEngine.previousPr()`. `PrSuggestionEngine` reutiliza el matcher existente y devuelve `suggestedPrCode` y `suggestedDistanceFromPrM`; calcular o mostrar una sugerencia no escribe campos contractuales.

El catálogo se filtra por ruta y calzada contractual. Se prefiere la geometría de esa calzada; si no existe, se utiliza el eje `EJE` de la ruta, sin cambiar la calzada del registro. Un PR etiquetado `EJE` no se asigna implícitamente a otra calzada: el catálogo futuro debe identificar la calzada contractual aplicable.

Cada PR se proyecta sobre el mismo conjunto de ejes con `LinearReferenceEngine`. Debe tener código numérico de hasta cuatro dígitos, coordenadas válidas, distancia al eje dentro del umbral configurado y `chainageM` coherente con su proyección (tolerancia de 5 m). Se rechazan duplicados e inconsistencias. El `chainageM` de los PR debe estar expresado sobre la misma referencia geométrica; no se convierte una progresiva contractual o TDR en medida del eje por suposición.

Para un GNSS válido se elige el PR anterior en sentido creciente, dentro del mismo segmento, con `previousPr()`. La distancia es exactamente `match.chainageM - pr.chainageM`. El sentido de captura no invierte esta selección. Sin PR anterior o matching válido no se propone un código. No se generan PR desde kilómetros redondeados, TDR, dkm ni fecha.

Los formularios de captura y la edición del historial muestran «Ubicación sugerida por GNSS», el PR con su distancia y «Usar sugerencia». Solo pulsar ese botón copia los valores al formulario. Inicio y fin tienen confirmaciones independientes. Cambiar ruta, calzada o coordenadas descarta inmediatamente la sugerencia anterior. La edición manual de un extremo marca ese extremo como `MANUAL`.

La procedencia inicial usa el campo existente `locationSource`; la final usa el nuevo `endLocationSource`. Valores: `MANUAL` y `GNSS_PR_SUGGESTION_CONFIRMED`. Room pasa a versión 10 mediante `MIGRATION_9_10`, que añade únicamente `endLocationSource TEXT NOT NULL DEFAULT 'MANUAL'`. Se conservan IDs, fotos, fechas, PR existentes y la migración geométrica 8→9.

Con catálogo, la validación permite distancias mayores que 999 m (por ejemplo, PR 0010 + 2650 m) y compara ubicaciones mediante las medidas del catálogo. Los códigos no se interpretan como kilómetros. Sin catálogo para la ruta/calzada se mantiene el ingreso manual previo. El JSON de producción sigue teniendo `prs: []`; no se cargan PR sintéticos.

Archivos principales: `road/PrSuggestionEngine.kt`, `road/ContractualPrInput.kt`, `field/PrSuggestionPanel.kt`, `MainActivity.kt`, `RecordHistoryScreen.kt`, `field/SurveyPreferences.kt`, `field/FieldController.kt`, `data/entity/InventoryRecordEntity.kt`, `data/dao/InventoryDao.kt`, `data/database/InventoryDatabase.kt`, `viewmodel/InventoryViewModel.kt`. Las rutas Java parten de `app/src/main/java/com/tuempresa/inventariovial/`.

## KML/KMZ

- Eje inferior negro `ff000000`, ancho 8; eje superior amarillo `ff00ffff`, ancho 5. Se reutiliza la misma `LineString` serializada, sin simplificar ni modificar vértices.
- Marcadores `INICIO PE-XX` verdes y `FIN PE-XX` rojos, escala 1.4 y texto visible.
- Se elimina `ScreenOverlay` de todas las exportaciones. `legend.png` queda como recurso opcional del KMZ.
- El documento contiene la carpeta `LEYENDA`, con siete entradas consultables desde el panel lateral y referencias a estilos: ALC azul, SV rojo, P verde, CUN naranja, BAD violeta, EJE amarillo y ELM gris. Las entradas no introducen puntos ficticios en el mapa.
- `Document.description` incluye la misma leyenda textual como respaldo. Los colores de inventario conservan sus valores anteriores; el amarillo se reserva al eje.
- Los datos extendidos incluyen `startPrSource` y `endPrSource`.

Archivos principales: `gis/KmlExporter.kt`, `gis/KmlPresentation.kt`, `gis/KmlMapImages.kt`, `gis/LocalKmlExport.kt`.

La geometría de las seis rutas sigue intacta. SHA-256 de `road_reference.json`: `2EEF92F7112BEBB32604263EA262C42267B26EAC435DE5109FE2CF0C3945A3E4`.

## Verificación y entrega

`.\gradlew.bat testDebugUnitTest assembleDebug --console=plain`: **BUILD SUCCESSFUL**. Resultado: **176 pruebas, 0 fallos, 0 errores y 0 omitidas**. APK debug generado correctamente con el JDK y la caché Gradle locales existentes.

Las pruebas `PrSuggestionTest`, `PrSuggestionScreenTest` y `PrSuggestionStorageTest` cubren el caso 10000/15000/12650 → PR 0010 + 2650 m, confirmación explícita, independencia de extremos, persistencia, migración, calzadas distintas, catálogos vacíos o inconsistentes, ausencia de PR anterior y descarte de sugerencias obsoletas. `RoadReferenceDeliveryTest` comprueba colores, anchos, igualdad de ambas geometrías, extremos, leyenda, ausencia de ScreenOverlay, XML, apertura del ZIP KMZ y recursos PNG.

Archivos de entrega:

- `app/build/outputs/apk/debug/app-debug.apk`.
- `app/build/outputs/road-reference/DEMO-PE-22A.kmz`, con inventario sintético identificado como DEMO.
- `output/GBO_ejes_KML_KMZ.zip`, con seis pares KML/KMZ de ejes reales, el DEMO y recursos auxiliares.
- Informe de pruebas: `app/build/reports/tests/testDebugUnitTest/index.html`.

La verificación de KMZ es automatizada (XML y contenedor ZIP); no se abrió Google Earth ni se probó en tablet física. No se ejecutaron pruebas instrumentadas en dispositivo. La confirmación visual en el cliente concreto queda pendiente. No se alteraron backend, DriveUploadWorker, endpoints, seguridad, SCAP, Excel SIC, proveedor GNSS, cámara ni credenciales. Las modificaciones en pruebas existentes de Room registran la migración nueva.
