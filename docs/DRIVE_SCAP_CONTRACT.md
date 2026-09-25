# Contrato adicional de Drive para puentes SCAP

Estado: preparado en cliente, **sin subida SCAP activada**. `ScapDrivePayload.uploadEnabled=false`. No hay worker ni llamada HTTP SCAP. Las exclusiones de consulta, encolado y worker SIC siguen vigentes, incluso si un padre SCAP aparece ACTIVE por error.

La UI muestra: «SCAP guardado localmente. Sincronización de puente pendiente de configuración del servidor.»

## Contrato SIC conservado

El endpoint actual recibe `token`, `fileName`, `routeCode`, `sibCode`, `sicCode`, `mimeType`, `base64`. No se modifica este contrato, `DriveFolderRouter` ni `InventoryViewModel.buildDriveFileName`. La configuración real permanece en `drive.local.properties`, ignorado por Git y excluido de la entrega. No hay tokens en este documento.

## Identidad y aislamiento de puentes

Cada puente necesita una carpeta propia. Resolverla mediante un registro persistente de identidad → folderId, usando primero el **bridgeCode completo**; si falta, usar el **UUID scapInspectionId**. `ScapDrivePayload.bridgeIdentity` distingue los espacios `code:` e `inspection:`. El nombre del puente es una etiqueta y nunca la identidad. No agrupar por nombre ni por categoría de foto. Dos UUID sin código deben producir carpetas distintas, aunque ambos se llamen igual.

El desarrollador del servidor debe confirmar unicidad del catálogo de bridgeCode y el traslado/asociación del fallback cuando posteriormente se complete el código. No normalizar o truncar códigos de manera que produzca colisiones. Una nueva inspección con el mismo código pertenece al mismo puente; conservar el identificador de inspección en los metadatos. La estructura y los nombres finales de carpetas quedan a cargo de su desarrollador; el cliente no crea una estructura alternativa.

## Payload separado propuesto

`ScapDrivePayload.prepare` exige que tanto scapInspectionId como recordId de la foto pertenezcan a la inspección y conserva el fileName recibido; no genera nombres finales.

```json
{
  "recordKind": "SCAP",
  "scapInspectionId": "UUID_DE_LA_INSPECCION",
  "bridgeCode": "CODIGO_REGISTRADO_O_VACIO",
  "bridgeName": "NOMBRE_REGISTRADO",
  "routeCode": "RUTA_REGISTRADA",
  "photoCategory": "GENERAL",
  "scapElementCode": null,
  "fileName": "NOMBRE_FINAL_ASIGNADO_POR_EL_RESPONSABLE.jpg",
  "mimeType": "image/jpeg",
  "base64": "CONTENIDO_BINARIO_CODIFICADO"
}
```

Este JSON es un esquema ilustrativo, no una solicitud enviada. Categorías actuales: GENERAL, UPSTREAM, DOWNSTREAM, TRANSVERSE, ELEMENT, DEFECT, ACCESS, CHANNEL, OTHER. El código del elemento puede ser nulo en una foto general. La autenticación del nuevo endpoint deberá definirse explícitamente; no se usa el endpoint SIC para probar este payload.

## Antes de conectar el cliente

El servidor debe aceptar explícitamente recordKind=SCAP, validar la pertenencia de la foto, resolver la carpeta por identidad estable, devolver éxito y fileId comprobables y definir idempotencia y manejo de cambios de código. Rechazar formatos desconocidos sin guardarlos en la carpeta SIC. Probar dos puentes con el mismo nombre y códigos diferentes, dos puentes sin código y reintentos de la misma foto. Solo después se implementará y habilitará el worker específico. El exportador local XLSX funciona independientemente del servidor.
