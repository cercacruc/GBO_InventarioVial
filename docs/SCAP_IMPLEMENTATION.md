# Implementación SCAP

## Alcance y uso

Desde **Puentes** se abre el listado SCAP. «Nueva inspección» crea una ficha vacía con UUID; «Historial → Inspecciones SCAP de puentes» abre el mismo listado. Elegir una ficha permite continuar su captura o consultar una completa. El historial anterior conserva SIC-17 y sus complementarios, edición y anulación.

El encabezado permanece visible con nombre, ruta, progresiva, sección, estado y guardado. La navegación principal es A–G; C, D y F tienen subsecciones. Solo se presenta el formulario de la sección y del tramo/elemento seleccionado. No se precargan valores del puente Agua Blanca.

| Sección | Captura |
|---|---|
| A | Identificación, ubicación, UTM manual, inspector y GNSS aceptado explícitamente; ruta de sesión como sugerencia. |
| B | Datos generales, dimensiones, años, servicio, tráfico y ambiente. |
| C1 | Resumen, número de tramos calculado de la colección, longitud total y luz principal manuales. |
| C2 | N tramos, longitud, categoría y tipo dependiente, característica secundaria, borde y material. |
| C3 | Losa y vigas de cada tramo; dimensiones decimales y conteos enteros. Peralte admite rango. |
| C4 | Estribos y anclajes izquierdo/derecho aplicables; 0..N pilares. Elevación y cimentación. |
| C5 | Barandas, veredas, sardineles, N apoyos, juntas y drenaje. |
| C6–C7 | Accesos izquierdo/derecho independientes, geometría, visibilidad y señalización. |
| C8–C10 | Cargas manuales, rutas alternas condicionales, condición de carretera. |
| D1–D3 | Suelos por subestructura, niveles de agua, gálibos y datos hidráulicos manuales. |
| D4 | Perfil longitudinal y N puntos, presente en el workbook aunque omitido en el listado del prompt. |
| E | Múltiples imágenes de elevación, planta y sección transversal por cámara/importación. |
| F1 | Búsqueda de los 112 elementos por nombre/código; selección de presentes, metrado y porcentajes 0–5. |
| F2 | Condición Global del Puente (hoja física `sec F`): grupos I–V, código, elemento y descripción. |
| Panel fotográfico | Hoja física `F.2.-PANEL FOTOGRAFICO`; fotos, fecha, descripción, categoría, elemento, reordenación y eliminación. |
| F3 | N defectos por elemento, descripción, ubicación, foto opcional y validación de sugerencias. |
| G | Identificación, datos generales, tramos, elementos, defectos, conteos de fotos/croquis, errores, pendientes y cálculo global cuando está definido. |

No se calculan capacidad resistente, cargas, metrados, dimensiones, hidráulica ni coordenadas UTM a partir de fotos o GNSS.

## Arquitectura

Paquete independiente `com.tuempresa.inventariovial.scap`:

- `catalog`: carga JSON de assets; valida versión por SHA-256, códigos únicos y factores.
- `data`: entidades, relaciones, DAO, repositorio transaccional y migración.
- `domain`: validación de campos/cierre, números, propuestas SIC e interfaz IA.
- `calculator`: función pura documentada en `SCAP_CALCULATION.md`.
- `ui`: controlador retenido por InventoryViewModel, cola de guardado y pantallas Compose.

La integración inicial pasó de 5 a 6. La revisión de ingeniería actual usa **Room 7**, con `MIGRATION_6_7` explícita y cadena 1→2→3→4→5→6→7, sin migración destructiva. Esquema actual: `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/7.json`. Ver `REVISION_INGENIERIA_UX.md`.

## Persistencia

| Entidad | Relación / propósito |
|---|---|
| ScapInspectionEntity | UUID; FK única a InventoryRecord; nombre/código, auditoría, estados, posición GNSS nullable. |
| ScapFieldValueEntity | PK inspectionId/ownerId/key; conserva valor textual y source, incluidos datos intermedios no válidos. |
| ScapSpanEntity | UUID, FK inspección, índice único dentro de ella; configuración y longitud tipadas. |
| ScapSubstructureEntity | UUID; clase (estribo/pilar/anclaje), índice, elevación, cimentación y suelo. |
| ScapSupportEntity | UUID; N apoyos, tipo/material/ubicación/conteo. |
| ScapJointEntity | UUID; N juntas, tipo/material; migración de la junta histórica. |
| ScapElementEntity | UUID; código único por inspección, nombre, unidad/factor oficiales, metrado y presencia. |
| ScapElementConditionEntity | UUID; relación única al elemento; seis porcentajes nullable mientras se captura. |
| ScapDefectEntity | UUID; N defectos, código opcional, foto opcional de la misma inspección, revisión IA y fecha. |
| ScapSketchEntity | UUID; inspección, tipo y ruta privada de imagen. |
| ScapProfilePointEntity | UUID; N puntos del perfil, distancia y cotas. |
| PhotoEntity existente | v6 añade scapInspectionId, scapElementCode y photoCategory; v7 añade description nullable. Metadatos Drive y rutas históricas se conservan. |

Las colecciones tienen FK e índices; la eliminación del registro vial padre propaga a SCAP. Las relaciones de foto/defecto se verifican transaccionalmente en el repositorio. El valor raw evita perder un campo parcialmente escrito; las columnas numéricas reflejan valores analizables. Se validan los valores completos al cerrar. Las selecciones retiradas conservan la evaluación y pueden reactivarse.

Cada cambio se encola y guarda en Room. DRAFT es una ficha recién creada; una edición cambia a IN_PROGRESS; COMPLETE se asigna tras revisión y confirmación. Las fichas completas son de lectura hasta reabrirlas. Un cierre inesperado permite abrir lo ya guardado desde el listado. El encabezado informa «Guardando», «Guardado local» o el error con reintento; salir del proceso antes de que termine la escritura puede perder la última operación aún pendiente.

El padre InventoryRecord tiene `sicCode=SCAP`, `status=DRAFT` y UUID independiente. Es un vínculo técnico, no una fila SIC terminada. Su ubicación obligatoria inicial se almacena como 0/0, pero la ubicación real en ScapInspection es **null** hasta aceptar GNSS; los ceros técnicos no son exportados ni propuestos como GNSS. Incluso tras cerrar SCAP el padre conserva DRAFT para no entrar en las colas de exportación/Drive/servidor existentes. La pantalla de recorridos y el historial SIC omiten esos padres y ofrecen el listado SCAP propio. `syncStatus=PENDING` conserva el estado futuro, sin prometer envío.

## Catálogos reproducibles

Fuente inalterada: `docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx`, SHA-256 `1df9f651722d06d6cd886c9be3edf565df7aae8b6fca10097011f48b12430b13`.

```text
python -m pip install openpyxl
python tools/extract_scap_catalog.py
```

El script de desarrollo produce `scap_elements.json`, `scap_options.json`, `scap_fields.json`, evidencia de extracción y fixture Agua Blanca. Son **112 elementos**, **49 catálogos de opciones**, cuatro listas dependientes por categoría y **141 definiciones de campos**; estas definiciones incluyen campos reutilizados por sección y filas repetibles, no 141 columnas fijas. `tools/scap_field_map.json` relaciona las claves de captura con sus celdas. Todos los assets contienen origen y hash. La aplicación no abre el Excel en runtime ni incluye una dependencia nueva para ello.

Ver las diferencias, referencias rotas y decisiones de obligatoriedad en `SCAP_PENDING_CLIENT_DECISIONS.md`.

## Validación y cálculo

Cada elemento presente exige seis porcentajes finitos entre 0 y 100 y suma 100 ±0.01. Se bloquea COMPLETE si falta evaluación o existe un valor inválido. Los borradores sí pueden guardarse incompletos. Categoría/tipo son dependientes; cambiar categoría borra el tipo anterior. No se usan opciones inventadas ni valores de la ficha ejemplo como predeterminados.

G incluye el índice solo cuando la fórmula está definida y todas las evaluaciones seleccionadas son válidas. Se reproduce el resultado del ejemplo con el catálogo extraído: **2.0700860438436695 → 2.070, REGULAR**. Los casos con división por cero o distribución final fuera de escala muestran el motivo, no un índice alternativo. Ver derivación, discrepancias G/Hoja1/M12 y límites en `SCAP_CALCULATION.md`.

## Integraciones preservadas y pendientes

Se conservan las entidades/formularios SIC-17, 17A y 17B. `ScapToSicMapper` entrega correspondencias inequívocas y pendientes de confirmación; `ScapSicExporter` genera XLSX únicamente con DIRECT/TRANSFORM, mostrando y comprobando faltantes antes de exportar. No persiste entidades SIC nuevas. Ver tabla `SCAP_TO_SIC_MAPPING.md` y `ENGINEERING_RELEASE.md`.

Se mantienen intactos los generadores de nombres definitivos y router Drive. La revisión de ingeniería modifica solamente las proyecciones autorizadas de SIC-18/21 y añade salida XLSX complementaria. `ScapExcelExporter` copia la plantilla fuente, empaquetada como asset generado, y usa el mapa fuente. La revisión de estabilidad añade diagnóstico, estados y selección de foto sellada a la sincronización SIC y al worker; ver FIELD_READINESS.md. Las fotos SCAP reutilizan PhotoEntity, createPhotoFile y FileProvider; la importación copia la imagen al almacenamiento privado para lectura sin conexión. El payload SCAP está preparado por separado, sin activación de subida, según `DRIVE_SCAP_CONTRACT.md`.

`ScapAiAnalyzer` y `DisabledScapAiAnalyzer` definen el contrato de sugerencias visuales. `AI_SCAP_ENABLED` tiene valor predeterminado false y está en el archivo de configuración de ejemplo. No hay proveedor IA, peticiones de análisis ni claves; todo el módulo funciona manualmente. La activación de la bandera por sí sola no conecta un proveedor.

## Verificación

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug --console=plain
```

`ScapCalculationTest` comprueba el ejemplo completo de 14 elementos, condición global, umbrales, errores, catálogos, duplicados, fuentes mezcladas, decimales/conteos/fechas. `ScapStorageTest` comprueba reapertura de la base, N tramos/pilares/apoyos/defectos, accesos independientes, selección, cierre, foto/defecto, integridad referencial, GNSS, mapeo parcial y preservación desde v5. Se amplían las cadenas de las pruebas anteriores desde v1–v4. Las pruebas existentes cubren SIC, exportación, edición/anulación, GNSS y sincronización; no se sustituyen.

La entrega incluye `SCAP_VERIFICATION.json` con resultados y comprobación de archivos protegidos. La prueba de cámara, selector de archivos y GNSS en una tablet real se mantiene explícitamente pendiente si no existe dispositivo conectado; no se confunde compilación con validación física.
