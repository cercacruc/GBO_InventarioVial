# Entrega: usuarios centrales GBO

## Resultado

Implementación incremental sobre el proyecto existente: Apps Script independiente como fuente
central, sincronización HTTPS por versión, Room 8 como caché, login offline y roles ADMIN/OPERATOR.
Se conserva `DeviceAccessManager`, el modelo operativo de SIC/SCAP, las fotografías, GPS,
exportaciones, historial y el servicio actual de Drive. No se cambiaron los cálculos ni formatos.

El primer ADMIN se crea centralmente. No se agrega ninguna cuenta local, PIN maestro ni token
de usuarios a BuildConfig. El paquete Kotlin nuevo `auth` reúne las entidades, DAOs, repositorios,
pantallas y worker para mantener la integración limitada. `UserEntities.kt` agrupa las cuatro
entidades y `UserDao.kt` agrupa ambos DAOs, evitando archivos vacíos o divisiones artificiales.

## Sincronización y login

1. Se valida la tablet con el control existente.
2. En una instalación nueva se provisionan URL y token individual, y se exige una primera
   sincronización. La pantalla muestra la huella completa para que GBO pueda provisionarla.
3. En aperturas posteriores se muestra el login local y se encola la comprobación de versión.
   Si no cambió, no se descarga la lista. Si cambió, se valida y se sustituye la caché en una
   transacción junto con la versión y fecha de éxito.
4. El login verifica usuario/PIN contra Room; no llama al servicio. Cinco fallos causan cinco
   minutos de bloqueo local. Un fallo de sincronización conserva datos y fecha anteriores.
5. La caché permite nuevos logins hasta 72 horas desde la última comprobación exitosa. Después
   exige sincronizar. Esto no corta una sesión de campo activa.
6. `UserSyncWorker` usa red conectada, trabajo único, reintentos y periodicidad de seis horas.
   Se encola también al recuperar conectividad en el proceso activo. Android controla cuándo
   puede ejecutar el trabajo en segundo plano.
7. Una desactivación impide nuevos accesos y marca el acceso actual para finalizar de forma
   segura al salir de la ficha/recorrido. Los cambios de rol se reflejan en la sesión.
8. Tras 15 minutos en segundo plano se solicita el PIN del mismo usuario en una pantalla modal
   conservando la composición de la ficha. No se persiste una autorización permanente.

El operador de `FieldSession` se obtiene de `AuthSession.displayName` en la lógica y se muestra
como solo lectura. Las sesiones anteriores conservan su texto; no se reescriben fotografías ni historial.

## Migración 7→8

Se agregan `user_cache`, `user_sync_state`, `local_login_state` y `auth_audit`, con índices de
username único y fecha de auditoría. Se mantienen las migraciones 1→7 y se registra 7→8 en el
builder. No hay borrado de tablas operativas ni `fallbackToDestructiveMigration`.

La prueba nueva construye una base a partir del esquema 7, inserta un registro en cada tabla
anterior, aplica la migración con el validador completo de Room y comprueba la conservación de
todas las filas, del operador y de las rutas de fotos. Existe también su variante instrumental.

## Administración y despliegue

La guía completa está en [backend/user-service/README.md](../backend/user-service/README.md).

| Operación | Procedimiento |
| --- | --- |
| Desplegar Apps Script | Crear Sheet privada GBO, copiar los seis `.gs` y manifiesto, habilitar Sheets API, configurar propiedades y publicar como Web App ejecutada por GBO. |
| Primer ADMIN | Ejecutar la herramienta local en modo `admin`; copiar el verificador en la propiedad temporal y ejecutar `bootstrapFirstAdmin`. Se deshabilita automáticamente. |
| Tablet nueva | Autorizar con el mecanismo existente; generar token ligado a la huella, ejecutar `provisionTablet`, ingresar URL/token en la tablet y sincronizar. |
| Crear usuario | ADMIN → Administración → Autorizar cambios online → Nuevo usuario. |
| Desactivar usuario | ADMIN → Administración → Desactivar. El cambio llega a otras tablets al sincronizar. |
| Cambiar rol o PIN | Acciones específicas de Administración con autorización central de cinco minutos. |
| Revocar tablet | Propiedad temporal `TABLET_REVOKE_ID` y función `revokeTablet` en el editor GBO. |
| Actualizar despliegue | Publicar una nueva versión de la misma implementación, conservando la Sheet, propiedades, contador y URL. |

OPERATOR conserva todas las operaciones de campo existentes. ADMIN tiene esas mismas funciones
y puede administrar usuarios y consultar auditoría. Los cambios requieren Internet y autorización
comprobada en el servidor. El último ADMIN activo está protegido en ambos lados. Los usuarios no
se eliminan físicamente como operación normal.

## Validación

- Suite Android local: **138 pruebas, 0 fallos, 0 errores, 0 omitidas**, incluidas **24 nuevas** de autenticación.
- Servicio Apps Script: **17 pruebas aprobadas**, ejecutando el código real con dobles de los servicios de Google.
- `:app:assembleDebug`: compilación correcta.
- `:app:assembleRelease`: compilación correcta, con R8/optimización y ofuscación comprobadas en el mapping.
- `:app:assembleDebugAndroidTest`: APK instrumental compilado. Su ejecución física requiere una tablet.
- Manifiesto release comprobado: backup y HTTP deshabilitados; sin debugging habilitado.
- `git diff --check`: sin errores de whitespace.

Cobertura de los casos solicitados:

| Caso | Comprobación |
| --- | --- |
| Hash/PIN correcto e incorrecto | Vector conocido PBKDF2-SHA256 y verificación positiva/negativa; comparación cruzada con Node en backend. |
| Username normalizado | Espacios y mayúsculas producen el mismo usuario; duplicados rechazados centralmente. |
| Usuario inactivo | Rechazo genérico sin iniciar sesión. |
| Bloqueo por intentos | Cinco fallos, recreación del repositorio, caducidad y reinicio del contador. |
| Login offline | Servicio simulado desconectado; el contador de llamadas no aumenta al entrar. |
| Sin caché | Se exige primera sincronización y no se crea ADMIN. |
| Sync fallida | Se conservan usuarios, versión y fecha. |
| Versión igual/nueva | Sin descarga ni recifrado; cambios aplicados cuando aumenta la versión. |
| Baja tras sync/cambio de rol | Se invalidan nuevos accesos y se actualizan los permisos en memoria. |
| OPERATOR/acciones offline | Rechazo en repositorio antes de llamar al servidor o modificar Room. |
| Último ADMIN | Rechazo tanto en cliente como en backend. |
| 72 horas | Límite exacto permitido; exceso y retroceso de reloj rechazados; sesión existente conservada. |
| Operador de campo | El controlador toma el nombre del usuario autenticado. |
| Migración 7→8 | Conservación de todas las tablas previas con validación Room. |
| Integridad adicional | Respuestas inválidas/duplicadas, rollback ante fallo real de escritura SQLite, aislamiento AES-GCM y cambios de identidad. |

Artefactos generados:

- `app/build/outputs/apk/debug/app-debug.apk` — instalable para pruebas con la firma debug correspondiente.
- `app/build/outputs/apk/release/app-release-unsigned.apk` — optimizado; **falta firmarlo con la clave GBO antes de distribuir**.
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` — pruebas de dispositivo.
- `app/build/outputs/mapping/release/mapping.txt` — correspondencias de ofuscación; custodiar con la versión.
- `app/build/reports/tests/testDebugUnitTest/index.html` — informe de pruebas locales.

En este Windows fue necesario indicar un directorio corto para los sockets locales del JDK de
Android Studio. No se alteró la configuración global del equipo ni el proyecto para ello:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\PROYECTOS\GBO_InventarioVial\tmp'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest
node backend/user-service/tests/service.test.cjs
```

## Pendientes de puesta en marcha

1. Desplegar el servicio bajo una cuenta GBO y crear el ADMIN/token reales. No se generaron
   secretos reales ni se accedió a una cuenta Google para publicar el servicio en esta tarea.
2. Conectar la tablet y comprobar su firma/versión antes de actualizar. **No desinstalar ni
   borrar datos**, porque contienen trabajo de campo.
3. Ejecutar las pruebas instrumentales de Keystore y migración en dispositivo.
4. Comprobar el flujo real con Internet y modo avión, dos tablets, roles, revocación, retorno
   del segundo plano, guardado de fichas, cámara/GPS y exportación/subida con el despliegue GBO.
5. Firmar release con la clave de distribución de GBO y probar la versión optimizada antes
   de distribuirla a todas las tablets.

Riesgos y limitaciones detallados en [SECURITY_REVIEW.md](SECURITY_REVIEW.md): el token actual
de Drive sigue dentro del APK; los PIN/verificadores offline y un equipo comprometido tienen
límites de protección; la revocación requiere conectividad; quedan pendientes rotación de tokens,
retención de auditoría y una revisión independiente. No se cifraron masivamente las fotografías.

## Lista exacta de archivos

### Creados

- `app/proguard-rules.pro`
- `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/8.json`
- `app/src/androidTest/java/com/tuempresa/inventariovial/UserSecurityDeviceTest.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/AdminUsersRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/AdminUsersScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/AuthGraph.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/AuthRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/AuthSessionManager.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/CredentialProtector.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/LoginScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/PasswordHasher.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/UserDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/UserEntities.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/UserServiceClient.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/UserSyncRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/UserSyncWorker.kt`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/test/java/com/tuempresa/inventariovial/UserAuthenticationTest.kt`
- `backend/user-service/Audit.gs`
- `backend/user-service/Auth.gs`
- `backend/user-service/Code.gs`
- `backend/user-service/Crypto.gs`
- `backend/user-service/README.md`
- `backend/user-service/Security.gs`
- `backend/user-service/Users.gs`
- `backend/user-service/appsscript.json`
- `backend/user-service/tests/service.test.cjs`
- `backend/user-service/tools/provision.cjs`
- `backend/user-service/vendor/LICENSE`
- `backend/user-service/vendor/README.md`
- `docs/SECURITY_REVIEW.md`
- `docs/USER_AUTH_IMPLEMENTATION.md`

### Modificados

- `app/build.gradle.kts`
- `app/src/androidTest/java/com/tuempresa/inventariovial/InventoryMigrationTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryMigrations.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/FieldController.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/test/java/com/tuempresa/inventariovial/Engineering2DataTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/EngineeringUxTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/InventoryRoomMigrationTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapStorageTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SurveyStorageTest.kt`
