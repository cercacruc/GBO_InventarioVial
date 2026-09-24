# Contrato propuesto para la API empresarial

Este contrato es una propuesta para implementar en el servidor. No existe URL ni autenticación empresarial configurada en el proyecto. Android se comunica exclusivamente mediante HTTPS; no contiene conexiones a bases SQL.

## Activación

Copiar `server.local.properties.example` a `server.local.properties` y establecer `SERVER_BASE_URL` con la URL HTTPS acordada. Vacía o inválida: no se agenda ningún envío. BuildConfig contiene configuración pública, nunca contraseñas ni tokens. `ServerCredentialProvider` es el punto de integración de una sesión autenticada; la implementación de producción debe inyectarlo en `ServerSyncWorker` cuando se defina el método de autenticación. La implementación actual permite probar una API HTTPS sin autenticación, configurada expresamente.

## Operación

`POST {SERVER_BASE_URL}/api/v1/inventory/records/upsert`

Cabeceras: `Content-Type: application/json; charset=utf-8`, `Idempotency-Key: <UUID del registro>`. `Authorization: Bearer <token>` únicamente cuando exista un proveedor de credenciales de ejecución. No seguir redirecciones a otros destinos.

```json
{
  "schemaVersion": 1,
  "id": "6eedc9f3-9ee5-40db-bd92-a031c833a708",
  "updatedAt": 1790208000000,
  "status": "ACTIVE",
  "record": {
    "id": "6eedc9f3-9ee5-40db-bd92-a031c833a708",
    "sicCode": "SIC-23", "assetType": "DERECHO_VIA",
    "routeCode": "RUTA_CLIENTE", "roadbedCode": "CD",
    "startPrCode": "0045", "startDistanceM": 273.0,
    "endPrCode": "0045", "endDistanceM": 350.0,
    "sideCode": "D", "surveyDate": "23/09/2026",
    "observations": null, "createdAt": 1790207990000, "sessionId": null
  },
  "location": {
    "latitude": -12.0, "longitude": -77.0, "altitudeM": 100.0,
    "horizontalAccuracyM": 0.5, "verticalAccuracyM": null,
    "timestamp": 1790207989000, "provider": "TABLET", "fixType": "FUSED",
    "satellites": null, "hdop": null, "correctionAge": null,
    "isRtkFixed": false, "source": "MANUAL", "sideSource": "MANUAL",
    "endLatitude": -12.001, "endLongitude": -77.001,
    "endAccuracyM": 0.8, "endTimestamp": 1790207990000
  },
  "sic": {
    "SIC-23": {
      "recordId": "6eedc9f3-9ee5-40db-bd92-a031c833a708",
      "classCode": "21", "typeCode": "1", "widthM": 12.5,
      "description": "ANCHO TOTAL"
    },
    "SIC-17A": null, "SIC-17B": null, "SIC-18A": null
  },
  "photos": [], "track": [], "session": null
}
```

Los ejemplos son sintéticos. `ServerPayload` es la fuente exacta de claves para los siete detalles principales. Los detalles no aplicables se envían como `null`. Los formatos complementarios conservan sus valores textuales validados (vacío = pendiente); no se convierten códigos con ceros iniciales en números. SIC-18A comparte ruta/calzada/PR/distancia/fecha con `record`. SIC-17A/B incluyen `bridgeCode` y se asocian al mismo UUID del puente. Superficie de desgaste conserva código y descripción debido al código 3 duplicado en el Manual.

Cada foto incluye `id`, `recordId`, `photoIndex`, `isPrimary`, `fileName`, `createdAt`, `hasOriginal` y `hasStampedCopy`. No se envían binarios, base64, rutas privadas locales ni enlaces definitivos de Drive.

Cada punto incluye `id`, `recordId`, `sequence`, `latitude`, `longitude`, `altitude`, `accuracy`, `timestamp`, ordenado por secuencia. La sesión incluye proyecto, operador, hash de dispositivo, ruta/tramo/calzada opcionales, sentido, inicio y fin. Tiempos de auditoría/GNSS en milisegundos Unix UTC; fecha de levantamiento local `dd/MM/yyyy`. Coordenadas WGS84 en grados decimales; distancias en metros.

Los valores numéricos GNSS desconocidos o no finitos se serializan como `null`, nunca como Infinity/NaN. Un salto en `sequence` entre puntos de recorrido señala una pausa/reanudación o interrupción prolongada: no unir ese intervalo como geometría observada. Al cerrar la sesión se vuelven a marcar pendientes los registros asociados para transmitir `endTime`; la versión avanza de manera atómica incluso si retrocede el reloj del dispositivo.

## Idempotencia, modificaciones y anulación

- El servidor hace upsert por UUID, sin crear otro registro en reintentos. Nunca usa la secuencia operativa como llave.
- Debe comparar `updatedAt`: repetir la misma versión devuelve el mismo acuse; una versión anterior no sobrescribe la nueva. El encabezado UUID identifica el recurso y **no** debe congelar para siempre la primera respuesta si llega una versión posterior.
- Guardar principal, detalles, metadatos de fotos y recorrido en una transacción del servidor. Las listas son el snapshot completo de esa versión.
- `ANNULLED` es una marca persistente; no borrar el registro. Restaurar envía `ACTIVE` con nueva versión.
- Los borradores de recorrido no se envían.
- Acuse 200/201 requerido:

```json
{"accepted":true,"id":"6eedc9f3-9ee5-40db-bd92-a031c833a708","updatedAt":1790208000000}
```

Room pasa de `PENDING` a `SYNCING` y a `SYNCED` solo con acuse que coincida en UUID y versión. Si se edita durante la petición, el acuse anterior no marca sincronizada la nueva versión. Estados `SYNCING` interrumpidos se recuperan en la siguiente ejecución. HTTP 408, 429, 5xx y fallos de red reintentan con WorkManager y espera exponencial. Otros 4xx o acuses incompatibles dejan `ERROR` visible, para corregir configuración/datos y reintentar desde Inicio. No se registran cuerpos sensibles ni tokens en logs del módulo empresarial.

## Decisiones pendientes

Autenticación y renovación, autorización por proyecto, certificados, resolución de conflictos entre tablets, reloj del servidor/versiones monotónicas, límites de tamaño de recorridos, política de retención y API separada para binarios. `updatedAt` del dispositivo no sustituye una estrategia de concurrencia empresarial entre dispositivos con relojes distintos. El servidor debe definirla antes de producción.
