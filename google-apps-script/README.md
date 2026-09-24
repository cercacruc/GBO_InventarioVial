# Corrección de carpetas duplicadas

El script adjunto buscaba y creaba carpetas sin exclusión mutua. La app programa un trabajo independiente por fotografía, por lo que dos solicitudes pueden buscar la misma ruta antes de que alguna la cree. La misma carrera afecta al archivo si se reintenta una subida todavía en ejecución.

`Code.gs` protege la búsqueda/creación de ruta, SIB y archivo con `LockService.getScriptLock()`. Espera hasta 30 segundos; si está ocupado, responde `ok:false` y Android vuelve a intentar. Libera el bloqueo en `finally`. No requiere cambiar ni reinstalar Android.

La estructura se conserva: **carpeta raíz → ruta → SIB → foto**. El SIC recibido es metadato de la respuesta, no una subcarpeta. No se añade otro nivel ni se mueven archivos.

## Aplicación

1. Abrir el proyecto original en Google Apps Script y conservar una copia del código anterior.
2. Sustituir su código por `Code.gs`. Mantener el ID de carpeta raíz correcto y reemplazar `PON_AQUI_TU_TOKEN_ACTUAL` por el mismo token de la app. El archivo entregado no contiene la credencial real.
3. Guardar. Elegir **Implementar → Gestionar implementaciones → editar la implementación actual → Nueva versión → Implementar**. Actualizar la existente permite conservar la URL que usa Android; guardar el editor por sí solo no actualiza la versión publicada.
4. Registrar dos o tres fotos de prueba bajo una ruta nueva, comprobar que usan la misma ruta/SIB y repetir la sincronización para verificar que no aparecen copias de esas fotos.

## Duplicados ya creados

El arreglo previene la carrera entre ejecuciones de este proyecto; no elimina duplicados históricos. Cuando encuentra varias carpetas con el mismo nombre, usa una elección estable por ID para las nuevas subidas. Las otras permanecen intactas, con sus fotografías. Una foto antigua situada en otra carpeta duplicada podría volver a subirse al destino elegido: consolidar esas carpetas requiere revisar antes todos sus archivos. No ejecutar simultáneamente otro proyecto de Apps Script que cree esta misma estructura: el bloqueo no se comparte entre proyectos diferentes. Dejar terminar las ejecuciones de la versión anterior antes de probar la nueva.

También se corrigió la aceptación de códigos malformados como `SIC-23texto`, que antes `parseInt` convertía silenciosamente en `SIC-23`.

## Verificación

Pruebas locales con servicios Google simulados (`node --test google-apps-script/Code.test.cjs`): varias fotos en la misma carpeta, reintento idempotente, solicitud competidora con bloqueo ocupado, liberación ante error, validaciones y selección estable entre duplicados existentes. No se ha desplegado ni ejecutado contra Drive real.

Referencias: [LockService](https://developers.google.com/apps-script/reference/lock), [actualización de implementaciones](https://developers.google.com/apps-script/concepts/deployments).
