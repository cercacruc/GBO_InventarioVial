# Versión rústica para prueba de campo

Se aplica sobre el estado actual del proyecto y del módulo SCAP. No se reescribe la aplicación. El objetivo de esta entrega es captura local comprobable y diagnóstico claro de la sincronización SIC.

## Configuración Drive

Copiar `drive.local.properties.example` a `drive.local.properties`, en la raíz, y completar localmente:

```properties
DRIVE_API_TOKEN=
DRIVE_WEB_APP_URL=
```

`DRIVE_WEB_APP_URL` debe ser la URL del despliegue actual de Apps Script, con forma `https://script.google.com/macros/s/ID_DE_IMPLEMENTACION/exec`. Una URL de edición `/d/.../edit`, `/dev`, HTTP, otro dominio, parámetros o fragmentos no es válida para esta configuración. El archivo local se ignora en Git y no se incluye en el ZIP. Reconstruir e instalar el APK después de configurar estos valores.

Sin token, la app muestra exactamente: **Drive no configurado: falta DRIVE_API_TOKEN en drive.local.properties.** Sin URL o con URL inválida muestra el error correspondiente. Las comprobaciones se hacen antes de pedir a WorkManager una subida, tanto al guardar automáticamente como al tocar Sincronizar; el worker vuelve a comprobarlas para trabajos antiguos. La aplicación funciona localmente sin esas credenciales. No se hizo una subida real durante la verificación: este equipo no tiene `drive.local.properties`.

Se conserva el contrato del endpoint: POST JSON con token, fileName, routeCode, sibCode, sicCode, mimeType y base64; respuesta `ok`, `fileId`, `alreadyExists` o `error`. No cambian DriveFolderRouter ni los nombres finales. No se registra token, cuerpo de petición ni respuesta íntegra del servidor. No se agregan claves ni proveedores IA.

## Estados y archivo enviado

Inicio muestra diagnóstico y cantidades; cada ficha del historial muestra el estado de sus fotos. El diagnóstico de configuración se separa de los estados locales almacenados:

| Almacenamiento / situación | UI |
|---|---|
| Token o URL ausentes/incorrectos | SIN CONFIGURAR + motivo |
| PENDING | PENDIENTE |
| QUEUED | PROGRAMADA |
| UPLOADING | SUBIENDO |
| SYNCED | SINCRONIZADA |
| ERROR | ERROR |

PROGRAMADA significa aceptada para ejecución, no subida terminada. SUBIENDO lo establece el worker. SYNCED se establece únicamente al confirmar éxito el endpoint. Los fallos dejan ERROR; los casos transitorios conservan el reintento de WorkManager. El usuario también puede reprogramar desde Sincronizar. No se creó un sistema paralelo de trabajos.

Se prefiere `stampedPath` cuando apunta a una imagen legible y no vacía. Si está ausente, es un directorio, está vacía, no existe o no se puede decodificar como imagen, se usa `localPath`. `originalPath`, `generatedFileName` y las carpetas no se modifican. El worker usa `localPath` como identidad de Room y una ruta separada para leer los bytes: la confirmación de una copia sellada actualiza la foto correcta.

## SCAP local y GPS

SCAP se excluye en consulta, política de encolado y worker. Incluso un padre SCAP marcado ACTIVE por error no entra en la cola SIC. Una ficha SCAP completa muestra: **SCAP guardado localmente. Sincronización de puente pendiente de configuración del servidor.** Su PENDING interno es información para una integración futura, no una subida activa. La exportación local ya está disponible en G; ver `ENGINEERING_RELEASE.md`.

`validation/EndLocationPolicy.kt` contiene `requiresEndLocation`, compartida por formularios, historial y CaptureValidation. Conserva literalmente la regla provisional anterior: SIC-19, SIC-21, MURO/TUNEL, detalles SIC-20 clases 13/14 y SIC-23 excepto clases 23/24. GPS inicial se muestra para todos; captura de GPS final solamente para elementos lineales. No se cambió la clasificación de ingeniería ni se borran coordenadas históricas.

## Limpieza y verificación

Se ignoran `.gradle`, `.kotlin`, `.idea`, `build`, configuración local y `tmp`. No se ejecutó limpieza del índice, reset ni eliminación de fuentes. Git puede seguir mostrando archivos generados que ya estaban versionados antes: `.gitignore` no los retira del índice.

La plantilla canónica es `docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx`. No se recrea ni se altera la eliminación previa del archivo duplicado en la raíz. El ZIP usa la copia canónica; Gradle la incorpora como asset generado junto con el mapa de campos para `ScapExcelExporter`. Los temporales no entran en assets ni runtime.

Pruebas: `testDebugUnitTest` y `assembleDebug`; resultados exactos en `SCAP_VERIFICATION.json`. `FieldReadinessTest` cubre token/URL, preferencia de foto sellada, preservación de nombre/identidad, estados, exclusión SCAP y política GPS. Se ejecuta además toda la suite anterior (SIC-18–23, exportadores, historial, edición/anulación, persistencia, GNSS y KML) y las pruebas SCAP.

`adb devices -l` no encontró tablet ni emulador conectado. Por ello, cámara física, permisos, selector de imágenes, ergonomía de pantallas, ubicación GNSS real y conexión al despliegue Drive deben validarse en campo. No se afirma que esos ensayos se hayan realizado.

Decisiones pendientes: aportar configuración de Drive y endpoint SCAP; confirmar lista de lineales, casos indefinidos de cálculo, equivalencias SIC, matriz contractual de obligatorios y correspondencias de bloques ambiguos de la plantilla. La exportación SCAP desde la plantilla y los SIC complementarios ya están implementados. Se enumeran en `SCAP_PENDING_CLIENT_DECISIONS.md`.
