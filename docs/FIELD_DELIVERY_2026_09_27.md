# Cierre de campo y Drive SCAP — 27/09/2026

Este documento reemplaza las afirmaciones de entregas anteriores sobre Room 6/7 y ausencia de subida SCAP. Room permanece en **8**, sin migraciones nuevas. El usuario confirmó que el sistema de seguridad funciona y autorizó explícitamente implementar SCAP en el endpoint Drive existente. No se crean servicios de usuarios ni endpoints alternativos.

## Arquitectura y alcance

La captura Compose utiliza entidades/DAO/repositorios Room. SCAP tiene relaciones propias y un registro técnico separado de los SIC. La copia de la plantilla se genera con `ScapExcelExporter`/`TemplateWorkbook`; SIC utiliza `SicExportFormat`/`SicExcelWriter`. Fotos y marca obligatoria reutilizan `RequiredWatermark`. La subida usa la configuración Drive y WorkManager existentes. Se conservaron login, verificador local, bloqueo, usuarios, Keystore, permisos, backup, release, cámara y plantilla/mapa canónicos.

Antes de editar se inventariaron arquitectura, archivos protegidos, continuaciones, impacto y pruebas en `tmp/field-final/architecture.md`. Base Git: `211bd62`; árbol inicialmente limpio. Baseline: 138 pruebas Android, cero fallos, APK debug compilado. No se ejecutó commit, push, desinstalación, limpieza de almacenamiento ni cambio de versión Room.

## Cambios funcionales

- SCAP: botón **Subir SCAP a Drive** en G, estado persistente y reintento. Entrega Excel, fotos propias con marca, croquis y manifiesto; el servidor conserva además un ZIP autocontenido. Solo una confirmación completa que coincida en UUID, revisión, SHA-256 y archivos permite `SYNCED`. Una edición durante la subida continúa pendiente.
- Apps Script: actualizado desde la **v3 que aportó el usuario**, conservando fotos por ruta/SIB y exportaciones SIC Excel/ZIP. Rama SCAP aislada, mismo token y bloqueo; no renombra, mueve ni borra archivos anteriores.
- Historial: edición técnica SIC-21/22 reutilizando formularios existentes. Se mantienen campos históricos, tipos 4/5/6 y dimensiones antiguas; una nueva selección solo ofrece los tres tipos vigentes.
- GIS KML/KMZ: regla de ubicación final tomada de EndLocationPolicy; atributos de la misma proyección normalizada de XLSX, observaciones, condición funcional e identificación/categoría/referencia Drive de fotos. No expone rutas privadas locales ni inventa una GDB contractual.

## Auditoría del documento solicitado

| Requisito | Implementación/evidencia |
|---|---|
| Seguridad intacta | Comparación SHA-256 de 44 archivos protegidos; suites Android y backend de usuarios sin alterar |
| SCAP A–G y ejemplo limpio | ScapFullExportTest (421 comprobaciones), ScapExportTest y prueba nativa Android; plantilla original inmutable |
| 4/5 tramos, 7 pilares, más de 2 apoyos | Continuaciones existentes conservadas; caso adicional de cinco tramos; prueba de paquete en Android con cinco/siete/tres |
| Cálculo aprobado | ScapCalculationTest conserva el caso 2.0700860438436695; no se modifican fórmulas |
| SCAP→SIC17/A/B | Mapper y complementos existentes; faltantes deshabilitan solo el formato afectado, no guardado SCAP |
| SIC18 circular/oval y condiciones manuales | EngineeringDecisionsTest, SurveyCorrectionsTest, pantallas instrumentadas; segunda dimensión circular vacía en proyección, históricos legibles |
| SIC18A | Persistencia ligada al padre, estados y exportador existente; Engineering2DataTest y EngineeringUxTest |
| SIC21/22 | Proyección normalizada preservada; edición técnica añadida y ciclo persistir/reabrir/editar/exportar probado |
| Rutas y PR | Seis rutas comunes existentes, continuidad sin trasladar coordenadas/mediciones; pruebas existentes |
| Catálogos, C4, C5 y F2 | Catálogos completos existentes; fijos simultáneos, pilares N, orden y condiciones de No aplica conservados |
| Fotos y croquis | Categorías actuales, defectos asociados, originales intactos, marca reutilizada; paquete no mezcla inspecciones |
| GNSS | WGS84/precisión y política existente; GIS ya consume la misma política. Exactitud real de campo pendiente |
| SIC17–23 XLSX | Orden/columnas en SicExportFormat, limpieza circular/material/tipos históricos, SicExcelWriterTest. No se añaden coordenadas/observaciones a columnas que el formato no define: disponibles en GIS/informes existentes |
| TXT | BLOCKED: no hay escritor ni especificación suficiente de orden, delimitador/anchos, codificación, nulos, fechas/decimales, encabezado y nomenclatura. No se inventa |
| GDB oficial | BLOCKED: faltan esquema, dominios, relaciones, sistema de referencia y reglas de entrega aprobados. KML/KMZ WGS84 sigue siendo intermedio |
| Muros/túneles/cunetas | Edición/persistencia/condición funcional existentes; ciclo de muro y túnel probado; longitud interna de muro fuera del SIC; descripciones pavimentado/tierra mantenidas |

Los casos no ejecutados o dependientes de datos externos se indican en `SCAP_VERIFICATION.json`; una implementación existente no equivale por sí sola a certificación de campo.

## Capacidades reales y límites conservados

Tramos/tableros, pilares, grupos de apoyos, juntas y observaciones generales tienen continuaciones. No se introduce máximo de tres pilares, tres tramos o dos apoyos. Pruebas actuales cubren cinco tramos, siete pilares, cuatro apoyos, tres juntas, 14 elementos, 16 observaciones, nueve fotos y tres croquis en el caso completo (algunas en casos separados).

La plantilla/calculadora todavía representa como máximo 3 elementos de superestructura, 2 de subestructura, 4 detalles, 2 cauce y 3 accesos; 32 fotografías; 15 puntos de perfil; un croquis por tipo. No existen continuaciones de estas tablas/imágenes validadas. Se conservan todos los datos en Room y se bloquea solamente la entrega incompatible (local o Drive). No se truncan ni se inventan hojas/fórmulas. Cualquier ampliación exige validar su correspondencia técnica, conforme a la sección 4 del pedido.

Conclusiones y recomendaciones sin correspondencia permanecen vacías; no se arrastran textos de Agua Blanca. Las dimensiones del croquis auxiliar son datos internos separados del formato contractual. Casos indefinidos y nombres heredados de la plantilla no se corrigen inventando fórmulas.

## Prueba manual de aceptación

1. Abrir la versión instalada sin borrar datos. Entrar con el usuario autorizado; revisar historial y una ficha antigua. Verificar cerrar sesión/bloquear sin perder información.
2. Crear SIC18 circular y ovalada; guardar, cerrar, reabrir, editar y exportar. Circular: diámetro y segunda dimensión vacía; ovalada: ancho/altura. Fotografías Panorámica/Entrada/Salida y adicionales.
3. Completar/editar SIC18A desde una alcantarilla que lo requiera. Comprobar mismo padre y campos heredados; guardar y exportar.
4. Crear/editar SIC19 con pavimentado y tierra, muro y túnel. Verificar condición funcional, GNSS final según política y longitud interna del muro sin columna SIC extra.
5. SIC21: clases 18/20 con material vacío y 19 con material. SIC22: tres tipos; hito Informativa; registro histórico tipo 4 conserva lectura/exportación. Editar ambos desde historial.
6. SCAP propio: nombre/código/ruta/PR/fecha, cinco tramos con sus tableros, siete pilares con suelos distintos, tres o cuatro grupos de apoyos, varias juntas. Cerrar/reabrir y comprobar todos.
7. F2: elementos, metrado, seis porcentajes, varias observaciones por elemento y generales. Capturar/importar fotografías, categorías/defectos, tres croquis y GNSS. Comprobar marca única y originales intactos.
8. Exportar SCAP local, abrir en Excel y revisar A–G, continuaciones, fórmulas y ausencia de datos/fotos de ejemplo. Completar complementos y exportar SIC17/A/B; faltantes deben bloquear solo su exportación.
9. Subir a Drive sin conexión: debe quedar programado. Recuperar conexión, observar subiendo y solo tras ACK sincronizado. Editar durante una subida y verificar que la revisión nueva no queda falsamente sincronizada.
10. Reintentar una respuesta perdida: no duplicar archivos. Revisar carpeta SCAP, Excel, fotos/croquis separados y ZIP. Dos puentes de igual nombre con código distinto deben quedar separados. Una inspección nueva del mismo código debe agruparse; sin código usa UUID.
11. Confirmar que ninguna foto SCAP aparece en cola SIC. Probar foto SIC y exportación SIC tras actualizar el script. La exportación SCAP local debe seguir disponible si Drive falla.
12. Superar un límite real no ampliado (por ejemplo 33 fotos): debe explicar el bloqueo de exportación, conservar la captura y permitir reabrir todos los datos.

## Publicación

Usar `output/entrega-campo/Code_actualizado.gs` para reemplazar el script y **actualizar la misma implementación**, conservando la URL. Esta entrega local conserva la configuración del archivo proporcionado; el archivo versionado `google-apps-script/Code.gs` utiliza un marcador de token. No copiar credenciales a Git ni a informes. Detalle del protocolo: `DRIVE_SCAP_CONTRACT.md`.
