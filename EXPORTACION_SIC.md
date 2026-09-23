# Exportación local del inventario vial calificado

## Uso

1. En Inicio, pulsar **Exportar SIC a Excel**.
2. Elegir un SIC o **Todos los SIC (ZIP)** y una ruta o todas.
3. Completar, si corresponde, proyecto/servicio, carretera y tramo. Estos encabezados se recuerdan y se aplican a las rutas seleccionadas.
4. Pulsar **Generar Excel** o **Generar ZIP de Excel**.
5. Pulsar **Guardar en el dispositivo** y elegir Descargas u otra carpeta local en el selector de Android.

La generación funciona sin internet. Se exportan los registros activos presentes en una consulta transaccional, con sus detalles SIC; no se exportan registros anulados. Un Excel contiene una hoja por ruta, ordenada por calzada y progresiva. El ZIP contiene únicamente los SIC con registros en la selección. Las selecciones vacías muestran un mensaje sin crear archivos vacíos.

El archivo preparado representa el momento de generación. Si cambian datos, filtros o encabezados, volver a generarlo. Cancelar el selector permite guardar posteriormente el archivo preparado. Si Android cierra el proceso o elimina la caché, generar de nuevo. El guardado no altera registros, fotografías ni estados de sincronización con Drive.

## Formato y fuentes

Se toma como referencia el diseño tabular de los seis Excel SIC-17 a SIC-22 facilitados por el usuario: encabezados de proyecto y tramo, agrupación de ubicación inicial/final, hoja por ruta y tabla con bordes. No se copian datos de las obras antiguas, sus rutas, fechas ni marcas. Los valores salen de la base local.

Las columnas oficiales se contrastaron con el Manual IV-2014/2015, inventario calificado:

| Formato | Página impresa | Columnas |
| --- | --- | --- |
| SIC-17 | 210 | 20 |
| SIC-18 | 216 | 13 |
| SIC-19 | 221 | 13 |
| SIC-20 | 225 | 14 |
| SIC-21 | 151 | 12 |
| SIC-22 | 154 | 14 |
| SIC-23 | 157 | 12 |

SIC-17 incluye tipo de servicio, singularidad salvada, nombre de singularidad y dimensión 3, ausentes en el ejemplo antiguo. SIC-22 incluye material y código de señal, ausentes en el ejemplo antiguo. SIC-23 se construye a partir del manual.

Los PR y códigos se almacenan como texto (PR con cuatro dígitos, clase con dos), las medidas como números con dos decimales y las fechas como fechas de Excel dd/mm/yyyy. Los campos sin información o no aplicables se dejan vacíos, sin sustituirlos por cero. Los textos se escriben como texto literal aunque empiecen con un signo igual. Se validan fechas, medidas no finitas, detalles ausentes y límites de Excel antes de ofrecer el guardado.

Las hojas tienen filtros, encabezados inmovilizados y títulos de impresión repetidos. Impresión horizontal A4; SIC-17 usa A3 por sus veinte columnas. La exportación incluye las columnas oficiales SIC: las fotos, coordenadas, observaciones y medidas complementarias ajenas a esas columnas permanecen en la aplicación; no es una copia de seguridad de toda la base.

## Implementación

- InventoryRecordForExport y InventoryDao.recordsForExport: snapshot Room de registros activos y sus siete posibles detalles, con filtros de ruta/SIC. No requiere migración de esquema.
- SicExportFormat: orden y correspondencia de las columnas.
- SicExcelWriter: escritura secuencial Open XML XLSX y ZIP, usando bibliotecas estándar de Java compatibles con Android; sin bibliotecas de escritorio ni servicios remotos.
- LocalSicExport: preparación en caché y copia al URI elegido, ambas en Dispatchers.IO; evita exportaciones simultáneas y permite reintentar el guardado.
- SicExportScreen: selección, encabezados y Storage Access Framework. No necesita permisos amplios de almacenamiento.

## Verificación

- Compilación debug y 11 pruebas unitarias: paquetes XLSX, columnas oficiales, nulos, ceros, fechas, orden, filtros de estado, nombres de hojas, texto literal y ZIP anidado.
- Los siete XLSX con datos sintéticos fueron reabiertos con openpyxl y renderizados para revisar encabezados y columnas. Se verificaron los ceros iniciales en el XML y con el lector independiente; el renderizador interpreta los textos numéricos como números en su vista previa. Microsoft Excel de escritorio no está disponible para una comprobación nativa.
- InventoryMigrationTest en SM-T733/Android 14: preservación de datos, consultas de exportación por ruta/SIC, exclusión de anulados y generación ZIP en Android, usando base temporal.
- SicExportScreenTest pasó en SM-T733/Android 14: navegación a exportación, selección SIC-23 y regreso al inicio. Requiere la tablet desbloqueada y encendida.

Los XLSX de prueba se generan en app/build/sic-export-verification y contienen datos sintéticos; no son inventarios del proyecto.
