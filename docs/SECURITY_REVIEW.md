# Revisión de seguridad: usuarios centrales y login offline

## Arquitectura implementada

`DeviceAccessManager → servicio central Apps Script ↔ Room 8 → login local → AuthSession → aplicación existente`.

La autorización de tablets existente se conserva. No hay Firebase, OAuth de Drive ni servidor
tradicional. El servicio de usuarios utiliza una Sheet privada de GBO y un despliegue independiente.
No se modifican contratos, carpetas, ZIP, formatos SIC, cálculos SCAP ni cargas del servicio de archivos.

Room contiene `user_cache`, `user_sync_state`, `local_login_state` y `auth_audit`. La migración
7→8 solo añade estas tablas e índices; conserva las migraciones anteriores y no utiliza migración
destructiva. El esquema exportado 8 forma parte del cambio.

## PIN y caché de credenciales

- PIN de 6–12 dígitos, PBKDF2-HMAC-SHA256, 210.000 iteraciones, salt aleatorio de 32 bytes y
  resultado de 32 bytes. Sin pepper distinto por instalación.
- Android usa `SecretKeyFactory`/`SecureRandom`; el bootstrap usa `node:crypto`.
- Apps Script verifica con módulos de CryptoJS 4.2.0 incluidos con licencia MIT y pruebas cruzadas.
- El verificador recibido se cifra con AES-256-GCM mediante una clave no exportable de Android
  Keystore. IV de 12 bytes generado por el proveedor, incluido en el blob; AAD vinculado al id del usuario.
- La configuración y el token de tablet se cifran con otro contexto AAD. No están en BuildConfig.
- Los PIN no se escriben en Room, preferencias, registros, archivos ni estado guardable de Compose.
  Los arrays temporales se limpian cuando termina la operación. Las cadenas de UI/JSON son inmutables
  y pueden permanecer temporalmente en memoria hasta que el runtime las recoja; no se promete borrado
  completo de memoria ni protección ante instrumentación de un dispositivo comprometido.
- El servidor conserva el verificador central en la Sheet privada y hashes de los tokens en
  Script Properties. Los administradores de esa cuenta tienen acceso a datos sensibles.

## Login, sincronización y caducidad

El login consulta exclusivamente Room, con comparación resistente a timing. La caché debe tener
una primera sincronización exitosa y una antigüedad máxima de 72 horas, configurable en
`AuthRepository.USER_CACHE_MAX_AGE_HOURS`. La comprobación sin cambios renueva esa antigüedad
sin descargar usuarios ni volver a cifrarlos. Una fecha local anterior al último éxito se rechaza.

Las respuestas se validan antes de modificar Room; datos, versión y fecha se actualizan juntos
en una transacción. Errores HTTP, de JSON, de credenciales cifradas o de estructura conservan la
caché. Una revocación explícita sí marca la tablet como revocada y bloquea nuevos accesos.

Se encola trabajo único al abrir/volver a la app y al recuperar conexión mientras el proceso
está activo. El worker tiene restricción `CONNECTED`, reintento exponencial y respaldo periódico
de seis horas. La UI y el worker comparten un repositorio con mutex para evitar sincronizaciones
simultáneas. El login no espera una llamada de red.

Cinco fallos consecutivos de login producen cinco minutos de bloqueo local por tablet, persistido
separadamente de la caché. Cambiar el nombre de usuario o sincronizar no elimina el bloqueo.
Un login correcto lo restablece. Se usan mensajes genéricos para usuario inexistente, inactivo o PIN incorrecto.

## Sesiones y permisos

La sesión de autenticación existe únicamente en memoria y contiene identidad y rol. Al superar
15 minutos en segundo plano se solicita el PIN del mismo usuario mediante una pantalla modal;
la composición de la ficha abierta no se desmonta por ese bloqueo. La recuperación ante muerte
del proceso sigue dependiendo de los mecanismos de guardado/borradores ya existentes de cada ficha.

Una desactivación sincronizada marca la sesión para cierre: impide nuevas sesiones de campo y
administración, permite terminar la operación abierta y cierra el acceso al regresar al inicio
sin recorrido activo. Los datos se conservan. La caducidad de 72 horas por sí sola no corta una
sesión activa; se aplica al siguiente proceso de autenticación.

El operador de las nuevas `FieldSession` procede de la sesión autenticada, también en la capa
lógica; el formulario muestra un nombre de solo lectura. El cierre manual seguro finaliza la
sesión de campo abierta y audita. Al cambiar de identidad después de reiniciar, se evita continuar
atribuyendo nuevas operaciones a la sesión de campo del usuario anterior.

ADMIN añade administración y auditoría a todas las funciones operativas. OPERATOR conserva las
funciones de campo existentes. La autorización se verifica en el repositorio y en el servicio:
no basta con ocultar controles. Las mutaciones requieren conexión y reautenticación central con
un token de cinco minutos vinculado a tablet, usuario y revisión. Las listas no se editan primero
en Room. La condición de último ADMIN se verifica en ambos lados bajo bloqueo en el servidor.

## Servicio y transporte

HTTPS obligatorio, sin certificados de usuario ni HTTP en la configuración de red. El cliente
limita URL y redirección a los hosts del Apps Script y no reenvía tokens en URL o en un POST
redirigido. Las respuestas se limitan a 2 MB. El servidor valida campos explícitos y procesa
usuarios, versión y auditoría en un único `Sheets.Spreadsheets.batchUpdate` bajo `ScriptLock`.

Las altas/restablecimientos envían verificadores derivados en Android, después de autorización
ADMIN. La reautenticación online envía el PIN temporalmente mediante HTTPS, sin persistirlo. El
bootstrap y la provisión se ejecutan solo desde el editor propiedad de GBO, no mediante endpoints.

Los tokens de tablet son independientes, generados con 256 bits aleatorios fuera del APK y
revocables. Los administrativos usan una función HMAC con clave aleatoria y contador persistente;
hay que conservar ese contador o rotar la clave al restaurar propiedades. No se usa `Math.random`.

## Auditoría, backups y release

Auditoría local: login correcto/fallido, logout, sincronización correcta/fallida, cambios de
usuarios y apertura/cierre de campo. Auditoría central: reautenticación ADMIN, cambios de
usuarios y revocación de tablets. No se incluyen PIN, hashes, salts ni tokens.

`allowBackup=false`; las reglas de backup y transferencia excluyen bases, archivos y preferencias.
El FileProvider y sus rutas permanecen intactos. Fotografías en `context.filesDir`, sin migración
masiva a cifrado: fotos, marcas de agua, exportación y subidas conservan sus rutas actuales.

Release habilita `optimization { enable = true }`, compatible con AGP 9.4.1, y desactiva debugging.
Una regla R8 elimina llamadas de Android Log en release para evitar los diagnósticos existentes
con rutas de archivos. No hay una regla para conservar indiscriminadamente el paquete de la app.
El archivo de correspondencias de R8 debe custodiarse para diagnosticar incidencias de cada APK.

Referencias: [DSL Optimization de AGP 9.4](https://developer.android.com/reference/tools/gradle-api/9.4/com/android/build/api/dsl/Optimization),
[ContentService](https://developers.google.com/apps-script/guides/content),
[actualizaciones atómicas de Sheets](https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets/batchUpdate).

## Riesgos pendientes y límites

1. **DRIVE_API_TOKEN está en BuildConfig y termina dentro del APK.** No es un secreto resistente
   a ingeniería inversa. Base64, dividir cadenas, NDK, nombres extraños o R8 no solucionan esto.
   Se mantiene por compatibilidad y queda pendiente revisar la autenticación del servicio de archivos.
2. Una tablet legítima recibe verificadores de todos los usuarios para trabajar offline. Keystore
   protege la copia en reposo, pero no frente a root, un proceso instrumentado o un APK modificado.
   PIN cortos tienen entropía limitada incluso con PBKDF2; favorecer PIN largos y gestión física/MDM.
3. Una revocación no llega a una tablet sin conexión. El límite de 72 horas afecta nuevos logins,
   no una sesión de campo ya abierta. El reloj del sistema puede manipularse en equipos comprometidos;
   este esquema no constituye una garantía de tiempo confiable offline.
4. El flag existente `ACCESS_CONTROL_ENABLED` se conserva. Revisar que GBO lo configure como
   corresponde antes de distribución; si está desactivado, el control local de dispositivos no se
   ejecuta, aunque el servicio central continúa exigiendo la provisión individual.
5. No existe rotación remota completa de tokens de tablet ni recuperación de una clave Keystore
   perdida. No borrar datos para recuperar acceso; diseñar un procedimiento supervisado que preserve
   inventarios y fotos. La instalación/actualización debe conservar la firma existente.
6. Apps Script/Sheets tienen cuotas y latencia. Las pruebas locales no sustituyen comprobar el
   despliegue GBO, la ejecución real de PBKDF2, dos tablets y las restricciones de batería del fabricante.
   La protección de intentos en servidor es por tablet; una lista grande o equipos comprometidos
   justificarían límites globales/adicionales por cuenta.
7. La cuenta GBO y los editores del script/Sheet deben estar protegidos. No compartir la hoja ni
   modificar registros manualmente; el propietario es parte de la frontera de confianza.
8. Fotografías privadas siguen sin cifrado de archivo. `SecurePhotoStore` es trabajo futuro, junto
   con políticas de retención/rotación de auditoría, recuperación de borradores tras muerte del proceso
   y revisión independiente de seguridad antes de una distribución amplia.

El procedimiento de despliegue, bootstrap, provisión, revocación y actualización está en
[`backend/user-service/README.md`](../backend/user-service/README.md).
