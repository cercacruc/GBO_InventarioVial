# Entrega: referencia geométrica y orden de elementos

Este documento describe la primera entrega. La preparación de PR, Room 10 y la variante KML/KMZ sin ScreenOverlay se documentan en [ENTREGA_PR_Y_KMZ_COMPATIBLE.md](ENTREGA_PR_Y_KMZ_COMPATIBLE.md), que sustituye esos detalles de esta entrega.

Fecha: 28/09/2026. Repositorio actual; sin commit ni push.

## A. Archivos modificados y añadidos

Rutas relativas a `D:/Downloads/GBO_InventarioVial`.

Producción:

- `app/src/main/assets/road_reference.json`: referencia exacta extraída del ZIP suministrado. El archivo anterior del repositorio contenía segmentos vacíos.
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordEntity.kt`: seis campos geométricos nullable.
- `app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt`: lectura y actualización exclusiva de la posición calculada; invalida posiciones al cambiar de ruta y rechaza resultados obsoletos por edición concurrente.
- `app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt`: versión 9 y migración 8→9.
- `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/9.json`: esquema generado por Room.
- `app/src/main/java/com/tuempresa/inventariovial/road/RoadReference.kt`: orden por familia e historial geométrico; conserva el método anterior para otros consumidores. La implementación de RoadMatcher y LinearReferenceEngine no cambia.
- `app/src/main/java/com/tuempresa/inventariovial/road/RoadReferenceRepository.kt`: corrige mensajes que llamaban oficial a la medida geométrica; conserva el importador y sus validaciones.
- `app/src/main/java/com/tuempresa/inventariovial/road/RoadAxisPositioner.kt` (nuevo): filtra por ruta antes de utilizar el motor existente.
- `app/src/main/java/com/tuempresa/inventariovial/road/RoadAxisStorage.kt` (nuevo): recalcula y persiste exclusivamente los campos geométricos.
- `app/src/main/java/com/tuempresa/inventariovial/road/OrderedAsset.kt` (nuevo): secuencia, código, carpeta, fuente de orden y advertencias derivados.
- `app/src/main/java/com/tuempresa/inventariovial/road/AssetPresentationCatalog.kt` (nuevo): catálogo único de familias, abreviaturas, colores y carpetas KML.
- `app/src/main/java/com/tuempresa/inventariovial/DrivePathPlanner.kt` (nuevo): plan puro de carpetas y fotografías, sin llamadas remotas.
- `app/src/main/java/com/tuempresa/inventariovial/road/AssetOrderScreen.kt` (nuevo): revisión por ruta, recálculo, plan previsto y exportación KMZ.
- `app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt`: acceso a «Orden de elementos», códigos cortos y fuente del orden.
- `app/src/main/java/com/tuempresa/inventariovial/viewmodel/InventoryViewModel.kt`: matching al guardar/editar, recálculo de históricos al cargar los ejes y acción manual de recálculo.
- `app/src/main/java/com/tuempresa/inventariovial/field/SurveyPreferences.kt`: continuidad entre capturas como advertencia para permitir registros retroactivos; conserva las validaciones de la ficha y del catálogo configurado.
- `app/src/main/java/com/tuempresa/inventariovial/validation/CaptureValidation.kt`: control de distancia sobre la ruta seleccionada.
- `app/src/main/java/com/tuempresa/inventariovial/gis/KmlExporter.kt`: extensión del exportador existente con ejes, extremos, carpetas, estilos y datos extendidos.
- `app/src/main/java/com/tuempresa/inventariovial/gis/LocalKmlExport.kt`: carga referencia local, recalcula, filtra por ruta y prepara KML/KMZ con MIME correspondiente.
- `app/src/main/java/com/tuempresa/inventariovial/gis/KmlMapImages.kt` (nuevo): leyenda e icono PNG locales, con los colores del catálogo.

Pruebas nuevas:

- `app/src/test/java/com/tuempresa/inventariovial/AssetOrderingTest.kt`.
- `app/src/test/java/com/tuempresa/inventariovial/RoadAxisStorageTest.kt`.
- `app/src/test/java/com/tuempresa/inventariovial/RoadReferenceDeliveryTest.kt`.

Pruebas existentes actualizadas para la versión 9 o la continuidad no bloqueante, sin eliminar pruebas:

- `app/src/test/java/com/tuempresa/inventariovial/InventoryRoomMigrationTest.kt`.
- `app/src/test/java/com/tuempresa/inventariovial/SurveyStorageTest.kt`.
- `app/src/test/java/com/tuempresa/inventariovial/EngineeringDecisionsTest.kt`.
- `app/src/test/java/com/tuempresa/inventariovial/Engineering2DataTest.kt`.
- `app/src/test/java/com/tuempresa/inventariovial/EngineeringUxTest.kt`.
- `app/src/test/java/com/tuempresa/inventariovial/ScapStorageTest.kt`.
- `app/src/test/java/com/tuempresa/inventariovial/UserAuthenticationTest.kt`.
- `app/src/androidTest/java/com/tuempresa/inventariovial/InventoryMigrationTest.kt`.
- `app/src/androidTest/java/com/tuempresa/inventariovial/UserSecurityDeviceTest.kt`.

Documentación: este archivo. `.idea/vcs.xml` y `local.properties` ya tenían modificaciones antes de esta ronda y no se editaron.

## B. Migración Room

`MIGRATION_8_9` usa únicamente `ALTER TABLE ... ADD COLUMN`. Añade cinco columnas REAL (`axisMeasureM`, `distanceToRoadAxisM`, `roadMatchConfidence`, `projectedLatitude`, `projectedLongitude`) y una TEXT (`matchedSegmentId`), todas nullable. Los históricos migrados comienzan con NULL; posteriormente se intenta su matching. No hay migración destructiva, borrado ni renumeración de claves.

## C. Cálculo geométrico

`RoadAxisPositioner` filtra `RoadReferenceData` por `routeCode` normalizado antes de llamar a `LinearReferenceEngine`/`RoadMatcher`. Una ruta seleccionada sin eje no provoca búsqueda en otra carretera. La búsqueda global se permite solo con ruta vacía y conserva la detección de ambigüedad. La ruta del operador nunca se reemplaza.

El motor existente proyecta el GNSS inicial sobre cada arista del eje, escoge la candidata válida e interpola `chainageM` entre los dos vértices usando el parámetro de proyección. Ese resultado se guarda como `axisMeasureM`. Para elementos lineales se usa el GNSS inicial; el cálculo de longitudes y los tracks existentes no cambian. No se modifican los cuatro campos contractuales de inicio/fin.

Umbrales existentes: precisión máxima de matching 15 m y distancia al eje configurable (30 m por defecto). Cuando el matcher devuelve NULL, se conservan el registro y su GPS y se muestra «Posición sobre eje no determinada».

Referencia validada: seis segmentos, 23 429 vértices, coordenadas WGS84 válidas, identificadores/secuencias únicos y medidas estrictamente crecientes. No se añade geometría externa.

| Ruta | Vértices | Longitud geométrica cargada (m) |
|---|---:|---:|
| PE-3N | 4471 | 151669.903 |
| PE-22A | 4305 | 193079.105 |
| PE-28C | 3195 | 105950.028 |
| PE-12A | 5331 | 239503.579 |
| PE-3NG | 1847 | 114064.332 |
| PE-28H | 4280 | 150231.890 |

Son distancias acumuladas desde el inicio de cada referencia TDR; no son progresivas contractuales. SHA-256 del JSON: `2EEF92F7112BEBB32604263EA262C42267B26EAC435DE5109FE2CF0C3945A3E4`.

## D. Numeración

Cada grupo `(routeCode normalizado, SIB de DriveFolderRouter, assetFamily)` empieza en 1. La calzada no crea un contador adicional. Solo se numeran activos SIC; se excluyen borradores, anulados y SCAP. La secuencia es derivada y se regenera; UUID, fotos y `createdAt` no cambian.

Primero se ordenan los registros con eje válido por `axisMeasureM`; después, los registros sin matching con PR calculable; finalmente, los que solo tienen fecha y UUID. Los empates se resuelven por `createdAt` y UUID. Los orígenes geométrico y contractual no se mezclan como si fueran equivalentes. La vista y KML identifican explícitamente el respaldo usado.

## E–F. Códigos y colores

| Familia | Prefijo | Color | KML AABBGGRR |
|---|---|---|---|
| Puente | P | Verde | ff228b22 |
| Alcantarilla | ALC | Azul | ffff6600 |
| Cuneta | CUN | Naranja | ff0088ff |
| Badén | BAD | Violeta | ffcc3399 |
| Señalización vertical | SV | Rojo | ff0000ff |
| Túnel | TUN | Gris | ff808080 |
| Muro | MUR | Gris | ff808080 |
| Canal | CAN | Gris | ff808080 |
| Bajada de agua | BAJ | Gris | ff808080 |
| Zanja de drenaje | ZD | Gris | ff808080 |
| Señalización horizontal | SH | Gris | ff808080 |
| Derecho de vía | DV | Gris | ff808080 |
| Otros/no clasificados | ELM | Gris | ff808080 |

Las clases técnicas SIC-19, SIC-20 y SIC-23 tienen prioridad sobre el selector genérico. La zanja de coronación (SIC-19 clase 12), seguridad vial sin abreviatura acordada y las otras clases de SIC-23 reciben ELM y advertencia; no se inventa una abreviatura. Un SIC sin equivalencia configurada conserva SIB nulo, se informa y no recibe una ruta Drive inventada.

## G. Inserción retroactiva

Captura A, B, C, D; posiciones 10000, 15000, 20000 y 17000 m: resultado A=ALC-1, B=ALC-2, D=ALC-3, C=ALC-4. Probado también mediante coordenadas interpoladas sobre el eje real PE-22A. Una alcantarilla intercalada no cambia los códigos de los puentes.

## H. Plan Drive

Ejemplo: `PE-3N/SIB-02/ALCANTARILLA 1/Foto 01.jpg`.

`DrivePathPlanner.plan(records)` devuelve carpetas, fotos y advertencias. Cada foto incluye su UUID, `recordId`, índice original, ruta local intacta, nombre visible y `plannedPath`. Las fotos se ordenan por `photoIndex` y se nombran `Foto 01.jpg`, `Foto 02.jpg`, etc. El plan puede recalcularse íntegramente; no modifica nombres persistidos ni mueve archivos, no programa subidas y no llama a Drive.

## I. KML/KMZ e interfaz

Acceso: **Historial → Orden de elementos**. Selección de una de las seis rutas, código/tipo/SIB, posición y distancia al eje, PR manual, GPS, estado y carpeta/fotos previstas. UUID y posición calculada son de lectura. Acciones: recalcular y exportar KMZ de la ruta; abrir, compartir y guardar.

Cada ruta contiene las carpetas `EJE`, `INICIO / FIN`, `PUENTES`, `ALCANTARILLAS`, `CUNETAS`, `BADENES`, `SEÑALIZACIÓN` y `OTROS`. El eje conserva todos los vértices del JSON. Los extremos provienen del primer/último vértice de cada segmento, sin usar los PR de registros.

Cada elemento usa su código corto como nombre y conserva UUID, SIC/SIB, familia, ruta/calzada, PR/distancias manuales, posición geométrica, distancia/confianza/segmento, GPS original/proyectado, precisión, fecha, condición, inspector de sesión cuando existe y los atributos previos. Los elementos lineales conservan sus LineString y las interrupciones del track.

Cada KMZ incluye `doc.kml`, `legend.png` y `marker.png`, sin dependencias de iconos remotos. La leyenda ScreenOverlay mide 260×170 px. El KML independiente conserva una leyenda textual estándar en la descripción del documento.

Generados mediante el exportador de la app en `app/build/outputs/road-reference/`:

- Seis KML y seis KMZ de los ejes reales, con extremos y leyenda, sin inventario ficticio.
- `DEMO-PE-22A.kmz`: demostración separada con seis elementos sintéticos expresamente marcados como DEMO; no son levantamientos reales.
- `legend.png` y `longitudes.txt`.

Paquete local: `output/GBO_ejes_KML_KMZ.zip` (incluye los ejes y la demostración identificada por su nombre DEMO).

## J. Pruebas

`.\gradlew.bat testDebugUnitTest --console=plain`: **BUILD SUCCESSFUL**. Resultado final: **159 pruebas, 0 fallos, 0 errores y 0 omitidas**. Incluye 14 pruebas nuevas. Informe HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.

Las pruebas nuevas cubren los diez casos solicitados, matching por ruta y ambigüedad global, GPS inválido, respaldo explícito, clasificación técnica, color AABBGGRR, XML válido, todos los vértices del eje, extremos reales, ZIP/PNG de KMZ, exportación por ruta y conservación de campos/fotos tras migrar 8→9 y reabrir la base. Las pruebas de versiones anteriores también incorporan 8→9.

## K. Build

`assembleDebug`: BUILD SUCCESSFUL. APK: `app/build/outputs/apk/debug/app-debug.apk`.

JDK local: `D:/Android Studio/jbr`; caché Gradle existente: `C:/Users/gerbc/.gradle`. No se alteraron los archivos de configuración locales ni se instalaron dependencias nuevas en el proyecto.

## L. Limitaciones reales y componentes preservados

- No se probó en una tablet física ni se abrió Google Earth. La nueva vista de orden se compiló, sin comprobación manual de navegación. Se validaron XML, estructura KMZ, recursos PNG y se inspeccionó visualmente la leyenda.
- La prueba de intents usa un adaptador de FileProvider solo en Robolectric, porque el FileProvider AndroidX espera separadores `/` y el host Windows usa `\\`. La configuración y seguridad del proveedor Android real no cambian.
- Los registros sin matching quedan al final con respaldo visible. La referencia no contiene un catálogo oficial de PR; no convierte distancias geométricas en progresivas oficiales.
- Los KMZ de ejes no contienen inventario real porque no se suministró una base de datos de campo. La app exportará los registros locales activos cuando se use en el dispositivo.
- La sincronización futura por lotes y carpetas definitivas sigue pendiente de otra ronda. Se conservaron DriveFolderRouter, DriveSync, DriveUploadWorker, Code.gs, endpoints y el comportamiento actual de subida.
- No se modificó código de seguridad/login, usuarios, activación, SCAP, Excel SIC, cámara, watermark, GNSS provider, credenciales ni backend. Los cambios en pruebas de seguridad/SCAP solo registran la migración nueva.
- No se ejecutaron pruebas instrumentadas en dispositivo. Sus constructores de Room se actualizaron para incluir la migración.
- No se hizo commit ni push.
