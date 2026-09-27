# Servicio central de usuarios GBO

Este servicio es independiente del Apps Script de fotografías y ZIP. La fuente de verdad
es una Google Sheet **privada**, propiedad de GBO. Room solo contiene una copia protegida.
El login normal nunca consulta este servicio. Las operaciones administrativas sí requieren Internet.

## 1. Crear la hoja y el proyecto

1. Entrar con una cuenta de Google controlada por GBO y crear una hoja privada.
2. Copiar su identificador: la parte entre `/d/` y `/edit` en su URL.
3. Mantener el acceso general **Restringido**. Compartir solo con administradores de confianza.
4. Crear un proyecto Apps Script nuevo, separado del script de Drive existente.
5. Copiar al editor los archivos `Code.gs`, `Users.gs`, `Security.gs`, `Auth.gs`, `Audit.gs` y
   `Crypto.gs` de esta carpeta. Se necesitan **todos**, incluido `Crypto.gs`.
6. En Configuración del proyecto, activar la visualización de `appsscript.json` y copiar el
   manifiesto incluido. En Servicios, comprobar que está habilitado **Google Sheets API v4**
   con identificador `Sheets`. Si el proyecto utiliza un proyecto Cloud propio, habilitar
   también Sheets API allí. No hace falta un servidor ni un servicio de pago.

## 2. Configurar Script Properties

En una terminal local de confianza, desde la raíz del repositorio, ejecutar:

```text
node backend/user-service/tools/provision.cjs server-key
```

El comando muestra una clave aleatoria de 256 bits. No guardar su salida en archivos,
historial compartido, capturas, tickets ni Git. En Configuración del proyecto → Propiedades
del script, añadir:

| Propiedad | Valor |
| --- | --- |
| `USERS_SPREADSHEET_ID` | Identificador de la hoja privada |
| `SERVER_RANDOM_KEY` | Clave generada por la herramienta |
| `BOOTSTRAP_ENABLED` | `true`, solo hasta crear el primer ADMIN |

Ejecutar **setupUserService** desde el editor y autorizar a la cuenta GBO para acceder a la
hoja. Se crean las pestañas `Users`, `Meta` y `Audit`. `Users` contiene un objeto JSON por
fila y `Meta!A1` la versión global. No editar sus filas a mano ni ordenar rangos parciales:
las modificaciones se hacen por la API, con actualización atómica de usuarios, versión y auditoría.

## 3. Crear el primer ADMIN central

```text
node backend/user-service/tools/provision.cjs admin
```

La herramienta solicita usuario, nombre y PIN de 6–12 dígitos con entrada oculta. No pasar
el PIN como argumento. Genera un salt con `crypto.randomBytes` y un verificador PBKDF2;
**nunca imprime el PIN**. El verificador también es sensible.

1. Copiar el JSON resultante en la propiedad temporal `BOOTSTRAP_ADMIN`.
2. Ejecutar **bootstrapFirstAdmin** desde el editor de Apps Script.
3. Comprobar que `Users` tiene el ADMIN y `Meta!A1` vale `1`.
4. La función elimina `BOOTSTRAP_ADMIN` y cambia `BOOTSTRAP_ENABLED` a `false`.
5. Eliminar cualquier copia temporal de la salida y limpiar el portapapeles.

El bootstrap no tiene un endpoint público y rechaza una segunda ejecución sobre una lista
existente. No hay usuario local de emergencia, PIN maestro ni credenciales en el APK.

## 4. Desplegar y obtener la URL

En Apps Script: **Implementar → Nueva implementación → Aplicación web**:

- Ejecutar como: la cuenta GBO propietaria del servicio.
- Acceso: **Cualquier persona**, para que Android pueda llamar sin login Google.
  Esto hace accesible el endpoint, **no** la Sheet: cada petición exige el token de una
  tablet provisionada, y los cambios además exigen una sesión ADMIN válida.
- Copiar la URL HTTPS terminada en `/exec`, no `/dev`.

Si las políticas del dominio no permiten este tipo de aplicación web, GBO deberá habilitar
un despliegue compatible; no publicar la Sheet ni eliminar la validación de tokens.

## 5. Provisionar cada tablet

1. Instalar/actualizar el APK **sin desinstalar ni borrar datos**.
2. Conservar el proceso existente de autorización de `DeviceAccessManager`. Si está activo,
   la huella debe estar en la lista autorizada de la app. Este servicio no reemplaza esa lista.
3. Abrir la app. En Configuración inicial copiar la **huella completa de 64 caracteres**.
4. En la computadora GBO ejecutar:

   ```text
   node backend/user-service/tools/provision.cjs tablet
   ```

5. Introducir la huella. La herramienta genera un token exclusivo de 256 bits y su hash.
6. Copiar el objeto `TABLET_PROVISION` en la propiedad temporal del mismo nombre.
7. Ejecutar **provisionTablet** en el editor. Se guarda solo el hash del token, asociado a la
   huella; la propiedad temporal se elimina. No modificar una tablet ya provisionada sin planificar
   la rotación de su token.
8. Entregar el token por un canal seguro al responsable de esa tablet. En la app introducir la
   URL `/exec` y el token, y pulsar **Guardar configuración y sincronizar** con Internet.
9. El token queda cifrado con una clave AES de Android Keystore, fuera de BuildConfig.
10. Una primera sincronización válida habilita el login local. Probar después en modo avión.

Antes de la primera sincronización exitosa se puede usar **Corregir configuración inicial**
si hubo un error de URL o token. Después, la app no ofrece reconfiguración anónima del servicio.
No borrar los datos de la app para resolver errores: contiene fotografías y trabajo de campo.
Una rotación posterior de credenciales de tablet requiere un procedimiento supervisado y queda
como mejora de administración; la revocación está disponible inmediatamente en el servidor.

## 6. Crear, editar o desactivar usuarios

1. Iniciar sesión con un ADMIN y abrir **Administración** en la pantalla principal.
2. Con Internet, introducir de nuevo su PIN y pulsar **Autorizar cambios online**.
3. La autorización central dura cinco minutos; se pierde al salir de Administración,
   cerrar sesión o mandar la app a segundo plano.
4. Pulsar **Nuevo usuario**, **Editar nombre**, **Cambiar PIN**, **Cambiar rol** o
   **Activar/Desactivar**. Los PIN nuevos se derivan en Android con salt aleatorio; solo se
   envía el verificador dentro de la operación administrativa autenticada.
5. Cada modificación central incrementa `usersVersion` y deja auditoría. La tablet solicita
   después la nueva versión. Si el cambio ya se guardó pero falla esa sincronización, se
   informa expresamente y se puede pulsar **Sincronizar usuarios** sin repetir la modificación.
6. Las demás tablets reciben el cambio al abrir la app, recuperar conectividad en un proceso
   activo, ejecutar el trabajo pendiente o durante la actualización periódica de seis horas.

Un OPERATOR conserva SIC, SCAP, GPS, fotos, sesiones de campo, historial, exportación y
sincronización de archivos. Un ADMIN tiene esas mismas funciones y la administración/auditoría.
El último ADMIN activo no se puede desactivar ni convertir en OPERATOR, ni en cliente ni en servidor.

La pantalla muestra los últimos 200 eventos **locales**. La auditoría central está en la pestaña
privada `Audit`; también existe la acción autenticada `getAudit` para consultar sus últimos 200 eventos.

## 7. Revocar una tablet

1. Poner su huella completa en `TABLET_REVOKE_ID` en Script Properties.
2. Ejecutar **revokeTablet** desde el editor.
3. La tablet queda inactiva; su sesión administrativa se elimina y se registra `TABLET_REVOKED`.
4. Su siguiente petición es rechazada. Android marca el acceso como revocado, conserva datos
   y permite finalizar la operación abierta antes de cerrar el acceso.

Una tablet desconectada no puede conocer una revocación hasta conectarse. Sin nueva validación,
la caché impide **nuevos logins** al superar 72 horas. No se corta una ficha activa al cumplirse
el plazo. Para un equipo perdido, retirar también su autorización del control de dispositivos
existente y aplicar las medidas de gestión del equipo de GBO.

## 8. Actualizar una implementación existente

1. Conservar la Sheet, Script Properties, tokens y `RANDOM_COUNTER`; no ejecutar bootstrap de nuevo.
2. Actualizar los `.gs` y, si corresponde, `appsscript.json`.
3. Ejecutar las pruebas locales descritas abajo y probar en una implementación de ensayo.
4. **Implementar → Gestionar implementaciones → Editar → Nueva versión → Implementar**.
5. Conservar el mismo identificador de implementación para mantener la URL `/exec`.
6. Comprobar una consulta sin cambios, un cambio de usuario y login offline en una tablet de prueba.

No reiniciar `RANDOM_COUNTER` manteniendo `SERVER_RANDOM_KEY`. Si fuera necesario restaurar
propiedades antiguas, generar una clave nueva y borrar las propiedades `SESSION_…`; las
tablets seguirán usando sus propios tokens pero los ADMIN deberán reautorizarse.

## Contrato y límites

Todas las operaciones usan POST JSON. Campos comunes: `action`, `deviceId`, `tabletToken`.
El cliente acepta únicamente HTTPS en `script.google.com/macros/s/…/exec`. Sigue la redirección
de ContentService mediante GET a `script.googleusercontent.com`, sin reenviar el POST secreto.

| Acción | Campos adicionales obligatorios |
| --- | --- |
| `syncUsers` | `clientVersion` (`-1` sin caché) |
| `adminAuthenticate` | `username`, `pin` (transitorio, no persistido) |
| `createUser` | `adminToken`, `username`, `displayName`, `role`, `credential` |
| `updateUser` | `adminToken`, `id`, `displayName` |
| `setUserActive` | `adminToken`, `id`, `active` |
| `changeUserRole` | `adminToken`, `id`, `role` |
| `resetUserPin` | `adminToken`, `id`, `credential` |
| `getAudit` | `adminToken` |

`credential` contiene exclusivamente `salt`, `pinHash`, `hashIterations`. Se rechazan campos
desconocidos. La respuesta usa `success` y errores genéricos; Apps Script puede devolver errores
funcionales con HTTP 200, por lo que hay que comprobar `success`.

Sin cambios: `{ "success": true, "changed": false, "version": 15 }`.
Con cambios se añade `users`, la fotografía completa de usuarios, incluidos los inactivos.
Límite de esta versión: 1.000 usuarios, petición de hasta 8 KB y respuesta Android hasta 2 MB.
PBKDF2-HMAC-SHA256: 210.000 iteraciones, salt de 32 bytes, resultado de 32 bytes en Base64.

Los tokens administrativos se derivan con HMAC-SHA256 usando la clave aleatoria del servidor
y un contador persistente bajo bloqueo. En servidor solo se guarda su hash, vinculado al usuario,
su revisión, la tablet y la caducidad. Un cambio del usuario invalida su autorización anterior.
Cinco intentos fallidos bloquean temporalmente la reautenticación administrativa por tablet.

## Pruebas y puesta en marcha

```text
node backend/user-service/tests/service.test.cjs
```

Las pruebas usan dobles en memoria de Sheets/Properties/Lock y ejecutan el código `.gs` real,
incluido CryptoJS. No acceden a cuentas ni datos reales. Cubren sincronización por versión,
bootstrap, usuarios, roles, último ADMIN, PIN, autorización, caducidad, revocación y auditoría.

Antes del uso en campo verificar en la cuenta GBO: permisos reales de la Web App, Sheets API,
tiempo de PBKDF2, cuotas, creación del ADMIN, primera sincronización y cambios en dos tablets.
WorkManager respeta las restricciones del sistema: reconexión no implica ejecución inmediata
si Android ha detenido el proceso, forzado su cierre o impuesto ahorro de batería.
