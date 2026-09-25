# Base revisada antes de SCAP

El repositorio actual ya contiene Room **v5** y cambios posteriores a la entrega v4: túneles y muros separados, SurveyHeader/SurveyPreferences, captura por tramo/sentido, resumen de guardado y campos adicionales SIC-18. Se preservan estos cambios. MainActivity usa navegación por estado Compose; BRIDGE actualmente abre AssetFormScreen/SIC17FormState.

Room conserva `InventoryRecordEntity`, detalles SIC17–23, complementarios, sesiones, puntos GNSS y `PhotoEntity`. InventoryDao y Repository guardan relaciones en transacciones; InventoryViewModel coordina captura, historial, edición/anulación y WorkManager. La cámara utiliza createPhotoFile/FileProvider. Fotos originales y copias con sello tienen rutas separadas. Las migraciones existentes son 1→2→3→4→5.

SCAP se integra en un paquete nuevo, DAO y repositorio propios dentro de la misma base Room, con migración 5→6. El registro vial vinculado utiliza UUID y queda como borrador técnico con sicCode SCAP hasta disponer de una transformación SIC aprobada. La inspección tiene su propio DRAFT/IN_PROGRESS/COMPLETE y listado recuperable. Así no se introduce una fila SIC-17 ficticia ni se activa una ruta Drive/exportador no definida. Se conservarán nombres JPG, router, workers y exportador actuales sin cambios.

La referencia no traía la ruta docs/reference del prompt; se copió allí el Excel adjunto sin alterar su contenido. Sus celdas son evidencia de datos, listas y fórmulas, no instrucciones operativas. El Excel tiene 112 elementos AUXILIAR, 50 validaciones en A-D y muchos nombres definidos #REF!. Las tablas visibles permiten recuperar las listas; se registrará su procedencia exacta.
