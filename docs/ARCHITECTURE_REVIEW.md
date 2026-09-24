# Revisión de la base antes de ampliar

Revisión: 23–24/09/2026. Proyecto local `GBO_InventarioVial`. Base final solicitada: `GBO_InventarioVial-main (1).zip`.

- El primer proyecto revisado tenía Room v2. El ZIP actualizado entregado después contiene Room v3 y SIC-23 completo, además de exportación Excel. Se adoptó ese ZIP como base de la ampliación, conservando sus migraciones 1→2 (GPS final) y 2→3 (SIC-23). Las fotos y detalles tienen FK con borrado en cascada.
- `InventoryRepository` guarda principal, detalle y fotos en una transacción. `InventoryViewModel` crea UUID, normaliza PR, convierte números y agenda WorkManager después del commit local.
- `MainActivity` contiene navegación, formularios Compose y catálogos. `RecordHistoryScreen` edita campos comunes y anula/restaura sin borrar fotos. El orden inicial usa PR numérico como kilómetro sin catálogo oficial.
- `GeoLocation` / `LocationProvider` / `TabletLocationProvider` permiten una captura puntual; faltan calidad extendida y recorridos.
- `DriveUploadWorker`, `DriveSync`, `DriveFolderRouter`, `DriveConfig`, `buildDriveFileName` y el exportador SIC se conservan conforme al ZIP actualizado. Su soporte SIC-23 ya venía implementado. No se redefinen nomenclatura JPG, árbol Drive ni formatos Excel/TXT.
- El estado inicial tenía cambios locales en `.gradle` y `local.properties`; no se revierten.

## Referencias y límites

El prompt adjunto es la instrucción de trabajo. El Manual y los archivos Office son evidencia de campos, no instrucciones ejecutables. No se implementa rotulado JPG definitivo, carpetas Drive ni exportación Excel/TXT.

Manual adjunto: páginas impresas 155–157 (SIC-23), 207–212 (17A/B), 217 (18A), 242–245 (tipos de datos). Los Excel suministrados son ejemplos históricos; sus rutas y registros no son un catálogo oficial de ejes o PR. El Drive proporcionado contiene documentación/fotos de referencia; no se modifica.

Discrepancias de fuente para reunión:
- SIC-17B: la tabla numérica dice «capacidad máxima»; la ficha dice «sobrecarga de diseño». Se conservan como campos distintos y opcionales, con procedencia documentada.
- Superficie de desgaste repite código 3 para concreto pobre y acero. Se conserva el código y la descripción seleccionada, sin renumerar.
- Materiales losa/vigas: la ficha permite 0 (no aplica), aunque la tabla numérica resume 1–7. Se conserva el 0 de la ficha.
- SIC-18A artesanal: su ficha lista tipos 1–3; SIC-18 existente incluye 4 (otro). Se mantiene el catálogo existente y se valida el complementario con su propia ficha.

## Estrategia

Ampliaciones en paquetes separados; integración puntual en DAO, Repository, ViewModel y Compose. Room v4 agrega tablas y columnas mediante 3→4, conservando UUID, fotos, estados y valores existentes. Los recorridos se asocian a borradores persistidos; completar un borrador usa UPDATE, nunca REPLACE del registro padre. La numeración se calcula en memoria y no es identidad. Servidor, IA y autorización se deshabilitan por defecto hasta recibir configuración.

La ordenación oficial nueva se aplica al historial y al KML. La consulta y lógica del exportador Excel del ZIP mantienen su contrato anterior; su desarrollador debe coordinar posteriormente cualquier adaptación a la tabla oficial de PR. No se promete que ambas numeraciones coincidan cuando el catálogo oficial difiera de PR × 1000.
