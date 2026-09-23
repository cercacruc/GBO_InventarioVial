# SIC-23: Derecho de vía

Se incorpora al menú principal el registro SIC-23, con los campos comunes de ruta, calzada, PR/distancia de inicio y fin, lado y fecha; fotografías múltiples y GPS inicial/final usan el flujo existente.

Fuente: Manual de Inventarios Viales IV-2014/2015, páginas impresas 155-157 (tabla III.10 y formato SIC-23) y 245 (tipos de datos).

- Clases: 21 Derecho de vía, 22 Zona urbana, 23 Punto específico y 24 Cantera.
- Clase 21: tipos 1 Ancho total, 2 Berma central, 3 Obstrucción, 4 Instalación de servicio público, 5 Vereda.
- Clase 23: tipos 1 Cruce importante y 2 Otro. Las clases 22 y 24 no tienen tipo.
- Ancho requerido para clase 21/tipos 1 y 2; admite coma o punto y hasta dos decimales. Los demás casos almacenan NULL (sin objeto), siguiendo la tabla III.10. La tabla genérica de tipos de datos marca ancho como requerido, pero la tabla específica identifica estos casos como sin objeto; no se inventa una medición cero.
- Descripción independiente de las observaciones: se normaliza a mayúsculas, sin tildes ni caracteres especiales.
- Se validan ambos extremos, distancias finitas no negativas, PR de hasta cuatro dígitos (guardados con ceros iniciales), orden de progresivas y lado D/I/S. Un punto puede tener inicio y fin iguales.
- Room pasa de versión 2 a 3 creando sic23_details con clave foránea y borrado en cascada. Se conserva la migración 1 a 2. No hay recreación destructiva.
- El historial muestra clase, tipo, ancho y descripción SIC-23; la edición existente sigue siendo de datos generales, con validación también al editar.
- Las fotografías mantienen la nomenclatura y reintentos existentes, con nombres DERECHO_VIA, ZONA_URBANA, PUNTO_ESPECIFICO o CANTERA. Se usa SIB-02 como convención de carpeta de la aplicación, no como equivalencia oficial entre formatos SIC y SIB. El servidor Apps Script no está en este repositorio y su recepción debe comprobarse en el entorno de despliegue.
- SIC-23 está integrado en la exportación Excel local; ver EXPORTACION_SIC.md. La exportación GIS sigue pendiente.

## Verificación

Pruebas unitarias: medidas inválidas, decimales, clases/tipos, campos no aplicables, texto, extremos y destino de fotografías.
Prueba Android InventoryMigrationTest: migración desde versión 1 conservando los datos, inserción y lectura de SIC-23 y cascada de borrado.

Verificado: assembleDebug y seis pruebas unitarias correctas; InventoryMigrationTest ejecutada correctamente en SM-T733 con Android 14. La prueba usa una base temporal, conserva registros/fotos antiguos, guarda y consulta SIC-23 y verifica el borrado en cascada. No se ha probado una subida real a Drive.
