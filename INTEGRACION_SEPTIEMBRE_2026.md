# Integración del ZIP y correcciones de captura

Se integró `C:/Users/CORE I7/Downloads/GBO_InventarioVial.zip` sobre el proyecto existente. Se conservaron la exportación Excel SIC-17 a SIC-23, Drive, fotografías e historial. No se copiaron cachés de compilación, configuraciones personales ni credenciales del ZIP.

## Funciones incorporadas del ZIP

- Sesiones de campo, recorrido GNSS con borradores recuperables, filtros de calidad y estimaciones de longitud.
- Exportación KML; conservación de interrupciones del recorrido sin unirlas con líneas ficticias.
- Sugerencias de ubicación y lado cuando se dispone de cartografía vial validada.
- Sello fotográfico en una copia independiente, conservando el original.
- Catálogos SIC centralizados y formularios complementarios SIC-17A/17B/18A.
- Capas preparadas para sincronización empresarial, autorización de dispositivos y receptor GNSS externo.

El servidor, control de dispositivos, IA y formularios complementarios conservan las banderas de activación del ZIP (`false` por defecto). La autorización local de demostración no equivale a un sistema empresarial de usuarios o licencias temporales. El transporte Bluetooth y el análisis IA aún no están implementados. Ver `server.local.properties.example` y los contratos en `docs/`.

## Correcciones solicitadas

| Solicitud | Resultado |
| --- | --- |
| Tramo, ruta, calzada al inicio | Encabezado común, con Tramo 1/2/3; nombres provisionales hasta recibir denominaciones reales. |
| Tres tipos de señal vertical | Reglamento, Preventiva e Informativa. Se conservan los códigos históricos en registros existentes. |
| Rutas con opciones | Catálogo editable por tramo desde el formulario. Una ruta por línea, en orden de visita; botones para pocas opciones, desplegable para muchas. Sin rutas ficticias. |
| Opciones establecidas | Selector común con botones resaltados o desplegable. Códigos no catalogados, nombres y observaciones siguen admitiendo texto. |
| Sin dimensiones en señal vertical | Retiradas de nuevas capturas; las columnas y medidas históricas no se borran. |
| Sin material en señal horizontal | Retirado para marcas y tachas; se mantiene en elementos de seguridad vial. |
| Continuidad de ruta y progresiva | Se recuerdan tramo/ruta/calzada/kilómetro/sentido después del guardado. Los metros del siguiente registro deben medirse. Se permite repetir ubicación para varios elementos. |
| Ambos sentidos | Control creciente/decreciente según lo confirmado por el usuario. El final del elemento debe respetar el sentido. “Iniciar otro recorrido” reinicia explícitamente la continuidad sin borrar registros. |
| Siguiente ruta | Botón según catálogo de visita; se rechaza retroceder en el catálogo dentro del tramo sin reiniciar el recorrido. No se deduce una ruta sumando su código. |
| Alcantarilla circular | Solo diámetro; Dimensión 2 no aplica. Para ovalada se solicitan ancho y altura, conservando el código SIC 2 compartido. |
| Condiciones de alcantarilla | Porcentajes observados de daño longitudinal y obstrucción, de 0 a 100; calificación manual con criterios de referencia. No se inventa una clasificación automática. |
| Verificación previa | Resumen obligatorio de ubicación, campos SIC, condiciones, observaciones, GPS y cantidad de fotos antes de confirmar. |
| Observaciones aparte | Exportación HTML independiente desde Inicio, para abrir/imprimir como PDF. Conserva UUID y ubicación para trazabilidad. Solo registros activos con observaciones. Plantilla contractual pendiente. |
| Badenes, túneles y muros | Entradas individuales desde Inicio, con clase SIC-20 fija en cada formulario. |
| Cámara al final | Después de los datos y observaciones; el resumen precede al guardado definitivo. |
| Optimización | Miniaturas con decodificación reducida y carga fuera del hilo de interfaz. Los archivos originales no se reducen. |

No hay entrada manual de “ítem” en señal vertical. El correlativo calculado del historial se conserva como referencia, sin cambiar identificadores ni nombres de fotos.

## Datos y progresivas

Room pasa de v3 a v5 por migraciones aditivas v3→4→5. También se mantiene la cadena desde v1/v2 y el paso desde v4 del ZIP. Los campos nuevos de registros históricos quedan desconocidos (`null`), sin inferir tramo, forma circular/ovalada ni porcentajes.

El usuario confirmó que PR representa **progresiva kilométrica**, en ambos sentidos. En nuevas capturas se usa kilómetro entero y metros entre 0 y menos de 1000. Las sugerencias GNSS se convierten desde la progresiva del eje a esa representación. Los registros anteriores conservan su interpretación de PR; no se reinterpretan ni modifican sus valores. El orden del catálogo de rutas es el orden de visita, independiente del sentido kilométrico dentro de cada ruta.

Se corrigieron dos fallos detectados durante la integración: la captura GPS ahora actualiza también el objeto con hora/altitud usado por sugerencias y sellos; tomar una foto al final ya no sobrescribe el punto inicial del elemento.

## Pendientes de la empresa / ingeniera

1. Códigos, nombres y orden definitivo de rutas en los tres tramos; catálogo de calzadas y señales si quieren restringir también sus códigos.
2. Criterio contractual de alcantarillas: tratamiento exacto del 30 %, definición de deformación y porcentajes de obstrucción. El porcentaje por sí solo no sustituye la evaluación estructural.
3. Plantilla y campos del informe separado; la exportación HTML actual es un documento de apoyo.
4. Archivo/plantilla y versión de **SCAP** que usarán. Los complementarios del ZIP no se presentan como SCAP ni se asume una conversión automática SCAP→SIC. El mapeo requiere identificar campos equivalentes y conservar datos de inspección sin equivalencia en SIC.
5. Cartografía y progresivas de los ejes para habilitar sugerencias GPS reales. El catálogo de códigos de ruta no contiene geometría y no permite por sí solo detectar cambios de carretera.

## Evaluación de pantalla fija

No se fuerza toda la captura a una pantalla inmóvil: puentes, controles GNSS, fotos y teclado exceden el espacio disponible en pantallas pequeñas. Se conserva desplazamiento accesible y se reducen controles mediante catálogos y campos condicionales. Un diseño por pasos o paneles para una tablet concreta requiere conocer su tamaño/orientación y aprobar el flujo de campo.

## Referencias técnicas

- [MTC, Manual de Inventarios Viales 2014/2015](https://portal.mtc.gob.pe/transportes/caminos/normas_carreteras/MTC%20NORMAS/ARCH_PDF/MAN_8%20IV-2014_2015.pdf), ficha SIC-18, página impresa 216: distingue condición estructural y funcional; la segunda describe grado de obstrucción. El umbral de daño longitudinal distingue los intervalos por debajo/encima de 30 %, por lo que el caso límite requiere aclaración.
- [MTC, actualización de señalización de 2024](https://www.gob.pe/institucion/mtc/noticias/1055740-mtc-actualiza-manual-de-senales-de-transito): clasificación reglamentaria, preventiva e informativa.
- [Provías Descentralizado, referencia a SCAP](https://apps.proviasdes.gob.pe/pvdsgc/PublicacionWeb/DescargarArchivo/103892): evaluación de daños y parámetros de condición de puentes; no es una plantilla de intercambio de datos para esta aplicación.

## Verificación

- `testDebugUnitTest`, `assembleDebug` y `assembleDebugAndroidTest`: correctos.
- **49 pruebas unitarias** correctas: migraciones, exportación Excel/KML, geometría y orden vial, sellado de fotos, validaciones, persistencia de rutas y progresivas, porcentajes, resumen e informe de observaciones.
- **7 pruebas Android** correctas en emulador Pixel Tablet / Android 15 (API 35): migración real SQLite/Room, navegación a exportación y cuatro pruebas de formularios (tres tipos verticales sin dimensiones, horizontal sin material, geometría circular/ovalada y desplegables).
- Se comprobó la migración desde v1/v2/v3 y v4; la migración de v4 conserva las dos dimensiones históricas y deja sin inferir los nuevos campos.
- APK de prueba: `app/build/outputs/apk/debug/app-debug.apk`.

Las pruebas de migración usan bases temporales con datos sintéticos; no modifican inventarios reales. El emulador se ejecutó sin guardar cambios en su imagen. No se ha validado hardware GNSS externo, servicio empresarial ni captura física en carretera.

En este Windows/JDK 25 se ejecutó Gradle con `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp` para evitar un error de conexión local de Java. No se cambió la configuración global de Java.

Los documentos de arquitectura y validación bajo `docs/` proceden del ZIP como referencia. Este documento describe las modificaciones y pruebas de la integración actual.
