# Drive SCAP — contrato implementado v1

Implementado por solicitud explícita del usuario del 27/09/2026. Sustituye la propuesta anterior. Utiliza el mismo Apps Script, URL, token, raíz y WorkManager del proyecto. SCAP sigue excluido de las consultas, colas y trabajadores SIC.

## Publicación

`google-apps-script/Code.gs` conserva las fotos SIC y `uploadSicExport` del script v3 aportado. El repositorio contiene un marcador de token. La entrega local `output/entrega-campo/Code_actualizado.gs` conserva la configuración del archivo proporcionado. Actualizar la misma implementación, conservando la URL. `doGet` informa `scapProtocol:1`.

## Paquete

En G se guarda/exporta localmente y, por separado, se pulsa Subir SCAP a Drive. Requiere revisión guardada y exportable. Incluye `SCAP.xlsx`, `manifest.json`, `fotos/<UUID>.png` y `croquis/<UUID>.png`. Fotos con marca corporativa reutilizada, proporciones intactas y originales sin sobrescribir. El manifiesto contiene puente, UUID, revisión, ruta, SHA-256 por archivo y metadatos de categoría/elemento/descripción/orden o tipo de croquis. No incluye contraseñas ni rutas privadas.

El servidor extrae los archivos en una carpeta de revisión y guarda además el ZIP completo. El XLSX no se convierte a Sheets. No se eliminan ni mueven versiones anteriores.

## Solicitud

POST JSON: `token`, `action:"uploadScap"`, `recordKind:"SCAP"`, `protocol:1`, `scapInspectionId` (UUID), `revision` (updatedAt entero), `bridgeCode`, `bridgeName`, `sha256` (hex del ZIP), `base64` (ZIP). La ruta está en el manifiesto; no requiere SIB ni SIC. Android transmite base64 desde disco sin imprimir solicitudes ni respuestas sensibles.

Antes de acceder a Drive, se validan versión, UUID, hash real, nombres seguros/únicos, manifiesto coincidente y hash de cada miembro. Solo se admiten Excel, manifiesto, fotos y croquis del contrato. Buscar/crear queda protegido por el bloqueo de script compartido.

## Identidad e idempotencia

`RAÍZ/SCAP/PUENTE_<SHA256 identidad>/<UUID inspección>/<revisión>_<SHA256 paquete>/`.

Identidad = `code:` + código completo (solo se quitan espacios extremos); sin código = `inspection:` + UUID. Nunca agrupar por nombre ni código truncado/normalizado. ScriptProperties conserva UUID→carpeta y hash del código completo→carpeta. Inspecciones con igual código comparten puente; códigos distintos con igual nombre permanecen separados.

Si primero falta código y después se completa, se conserva la carpeta UUID y se registra el alias de código. Si este ya pertenece a otra carpeta, se exige revisión; no se fusiona automáticamente. Correcciones de identidad entre puentes requieren resolver la asociación con el responsable.

Los reintentos usan bytes idénticos conservados por revisión en almacenamiento privado. En Drive se compara el contenido antes de reutilizar un archivo. Un fallo parcial se recupera sin duplicar archivos ya escritos; el ZIP se confirma después de todos sus miembros.

## ACK y estados

ACK: `ok:true`, `protocol:1`, `recordKind:"SCAP"`, UUID/revisión/hash coincidentes, `fileId` del ZIP, `folderId` de revisión, `bridgeFolderId`, `fileUrl`, `alreadyExists`, `fileCount` y `files:[{path,fileId,folderId}]`.

Android exige la lista exacta de archivos y sus identificadores, hash e identidad. La transacción Room marca SYNCED solamente si updatedAt sigue siendo la revisión enviada. Guarda referencias del ZIP en scap_values con owner drive y referencias reales de cada foto en sus columnas existentes. La proyección contractual ignora ese owner.

Una edición concurrente sigue PENDING y se reintenta la revisión nueva. Estados: sin configuración, pendiente, programada, subiendo, sincronizada, error. Red interrumpida/servidor ocupado: reintento exponencial WorkManager. Rechazo de contrato/autorización: error y botón de reintento. Programar no equivale a sincronizar.

Room sigue en 8; sin cambios de seguridad, cámara, permisos, backup ni release. Temporales de generación eliminados al finalizar; paquete pendiente conservado para recuperar respuesta perdida y eliminado tras ACK. Un cierre forzoso puede dejar temporales privados de generación; no son archivos públicos ni se añaden al paquete.

## Verificación

`node --test google-apps-script/Code.test.cjs`: SIC anterior, exportaciones v3, autorización, hashes, nombres, aislamiento, UUID sin código, asignación posterior, conflictos, bloqueo y fallo parcial.

ScapDriveTest: propiedad de fotos, píxeles marcados idénticos en Excel/PNG, manifiesto, reintento idéntico, cuerpo transmitido y ACK condicionado a revisión. ScapDriveDeviceTest: Room en memoria y datos sintéticos en Android real. Red opt-in mediante `scapDriveE2E=true`; nunca credenciales en argumentos.

Resultados efectivos y pruebas no ejecutadas: SCAP_VERIFICATION.json. La salud v4 del endpoint por sí sola no prueba una subida.