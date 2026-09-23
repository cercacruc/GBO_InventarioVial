# Integración de funciones del ZIP

Se adaptaron las funciones de GBO_InventarioVial-main.zip sobre la arquitectura existente.

- Historial ordenado por ruta, calzada y progresiva; edición de datos generales, anulación y restauración.
- Varias fotografías por elemento en los formularios SIC existentes, con numeración individual y opción de quitar fotos antes de guardar.
- GPS inicial y final independientes. El GPS final es opcional, como en el ZIP.
- LocationProvider y TabletLocationProvider del ZIP, integrados con la captura actual.
- Migración Room 1 → 2: añade coordenadas y precisión finales, conservando tablas, UUID, detalles SIC y fotografías.
- Guardado transaccional del registro, detalle SIC y todas sus fotos antes de programar las subidas.
- Sincronización manual de pendientes, trabajos identificados por foto y actualización transaccional del estado de sincronización del registro. Los registros anulados se excluyen de las nuevas subidas.
- Se conservan los formularios y la nomenclatura de fotos actuales. Editar un registro no renombra archivos ya creados en Drive.

## Configuración local de Drive

El token existente se trasladó a `drive.local.properties`, excluido mediante `.gitignore`:

```properties
DRIVE_API_TOKEN=tu_token
```

Se usa este archivo separado porque `local.properties` ya estaba versionado. BuildConfig incorpora el valor al compilar; no se debe versionar el archivo de credenciales.

## Verificación

- Compilación debug y pruebas unitarias con Gradle.
- Comprobación SQLite de conservación de datos, coincidencia con el esquema generado por Room, anulación/restauración y sincronización de varias fotos.
- Prueba Android `InventoryMigrationTest`, con esquema de versión 1 y datos de ejemplo, para ejecutar en dispositivo o emulador.

La exportación Excel local SIC-17 a SIC-23 se añadió posteriormente; ver EXPORTACION_SIC.md. La conexión a un receptor GNSS Bluetooth, la exportación GIS y la IA siguen pendientes.
