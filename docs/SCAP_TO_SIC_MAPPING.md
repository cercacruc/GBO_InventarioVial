# Mapeo parcial SCAP → SIC

`ScapToSicMapper` devuelve propuestas tipadas (`ScapSicMapping`), no entidades persistidas. `ScapSicExporter` consume solo DIRECT y TRANSFORM para generar XLSX tras mostrar los faltantes en G. No modifica registros SIC anteriores. Los campos vacíos o ambiguos no se propagan. Los mínimos y límites de esta salida parcial están en `ENGINEERING_RELEASE.md`.

| SCAP FIELD | SIC FIELD | STATUS | NOTES |
|---|---|---|---|
| bridgeCode | 17/17A/17B.bridgeCode | DIRECT | Identificador documental; el UUID de Room sigue siendo la clave. |
| route | InventoryRecord.routeCode para 17 | DIRECT | Misma ruta; no decide calzada o sentido. |
| progressive | 17.startPrCode/startDistanceM | TRANSFORM | Kilómetro entero y resto en metros; no inventa ubicación final. |
| lastInspection | 17.surveyDate | DIRECT | Fecha registrada de inspección. |
| totalLengthM | 17.dimension1LengthM | DIRECT | Longitud total en metros. |
| lowerClearanceM | 17.dimension2LowerHeightM | DIRECT | Altura libre inferior; no capacidad hidráulica. |
| upperClearanceM | 17.dimension3UpperHeightM | DIRECT | Altura libre superior. |
| singularityName | 17.singularityName | DIRECT | Nombre, sin asignar código de singularidad. |
| cantidad de spans | 17.spans | TRANSFORM | Cuenta las filas registradas; comprobar configuración completa. |
| bridgeName | 17A.bridgeName | DIRECT | Texto. |
| constructionYear | 17A.constructionYear | DIRECT | Año. |
| politicalDepartment | 17A.department | DIRECT | Departamento político, no vial. |
| province, district, nearbyTown | 17A.province, district, nearbyTown | DIRECT | Textos homólogos. |
| altitudeM | 17A.altitude | DIRECT | Altitud manual. |
| roadwayWidthM, sidewalkWidthM | 17A.roadwayWidthM, sidewalkWidthM | DIRECT | Metros; no suma inferida. |
| GNSS.latitude, GNSS.longitude | 17A.latitude, longitude | DIRECT | Solo coordenadas GNSS capturadas; nunca ceros de inicialización del registro técnico. |
| designLoad | 17B.designLoad | DIRECT | Denominación de sobrecarga, no capacidad máxima. |
| mainSpanM | 17B.mainSpanM | DIRECT | Luz principal indicada por el inspector. |
| categoría/tipo SCAP | 17.classCode/typeCode | PENDING_CLIENT_CONFIRMATION | Catálogos diferentes. |
| índice y clasificación SCAP | 17.structuralConditionCode | PENDING_CLIENT_CONFIRMATION | Sin equivalencia aprobada; no convertir REGULAR en un código SIC. |
| vías de tránsito | 17A.lanes | PENDING_CLIENT_CONFIRMATION | Confirmar semántica de vías frente a carriles. |
| alineamiento SCAP | 17A.alignmentCode | PENDING_CLIENT_CONFIRMATION | Requiere tabla de equivalencias. |
| anchos parciales | 17A.deckWidthM/superstructureWidthM | PENDING_CLIENT_CONFIRMATION | No inferir anchos sumando dimensiones. |
| UTM Este/Norte 19K | 17A.latitude/longitude | PENDING_CLIENT_CONFIRMATION | Falta módulo de conversión validado y confirmación del sistema de referencia. |
| carga actual/futura/sobreesfuerzo | 17B.maximumCapacity | PENDING_CLIENT_CONFIRMATION | No representan automáticamente capacidad resistente. |
| materiales/bordes/forma de vigas y losa | 17B códigos técnicos | PENDING_CLIENT_CONFIRMATION | No usar el índice de un dropdown como código SIC. |
| N tramos/pilares con materiales diferentes | Campos agregados 17B | PENDING_CLIENT_CONFIRMATION | Definir cómo representar estructuras heterogéneas. |
| porcentajes 0–5, defectos, croquis, perfil longitudinal | — | NO_MAPPING | Se conservan como información detallada SCAP. |
| campos SIC sin fuente inequívoca en SCAP | Servicio/calzada/códigos restantes | PENDING_CLIENT_CONFIRMATION | No inventar ni sobrescribir datos existentes. |

Los estados DIRECT y TRANSFORM se adjuntan a los valores propuestos. Las correspondencias ambiguas se devuelven como pendientes y no como valores. La tabla documenta además NO_MAPPING y PENDING_CLIENT_CONFIRMATION. Se implementan XLSX SIC-17/17A/17B; cada destino se bloquea por separado si le falta un campo necesario. No hay creación automática de registros SIC ni subida a Drive.
