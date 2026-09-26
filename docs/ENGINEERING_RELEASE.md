# Cierre de decisiones confirmadas de ingeniería

> Documento de una revisión anterior. Para el estado actual de Room 7, las continuaciones XLSX, F2 integrado, SIC-17/18A y watermark, consultar [Revisión de ingeniería de puentes 2](REVISION_INGENIERIA_PUENTES_2.md). Los límites antiguos de pilares, apoyos y juntas quedan superados por esa revisión.

Se continúa el repositorio existente, manteniendo Room v6 y registros históricos. No se cambian la nomenclatura de fotos, el enrutamiento Drive, el contrato SIC ni EndLocationPolicy. El índice Git no se limpia ni se reescribe.

## Captura y exportación SIC

- Nuevas SIC-22: solo 1 Reglamento, 2 Preventiva y 3 Informativa. El hito kilométrico es Informativa. Ya no se captura su número independiente. La entidad conserva typeCode 4/5/6 y kilometerPostNumber; la lectura histórica y su exportación siguen admitidas.
- SIC-18: condición estructural manual 1 Bueno, 2 Regular, 3 Malo; funcional 1 Buena (limpia), 2 Regular (parcialmente obstruida), 3 Mala (totalmente obstruida). Se conserva literalmente «Quebrado o en menos del 30% de la longitud o con ligera 20% deformación.» Los porcentajes auxiliares no aparecen en captura, resumen ni exportación y no intervienen en validación. Sus columnas históricas continúan nullable.
- Circular: diámetro en dimension1, dimension2 vacío en exportación aun si quedó un valor anterior. Ovalada y otras secciones: ancho y altura. Los registros legacy sin sectionShape conservan su segunda dimensión.
- SIC-21: material vacío para 18 y 20; clase 19 conserva el valor. Room no cambia. No existe un exportador TXT en este repositorio; la proyección XLSX aplica la regla y debe reutilizarse si se incorpora TXT.
- Rutas iniciales comunes: PE-3N, PE-22A, PE-28C, PE-12A, PE-3NG, PE-28H. No se asignan a Tramo 1/2/3. El catálogo inicial no impone pertenencia ni orden. Solo un catálogo configurado explícitamente para el tramo activa esas restricciones. Se conserva ruta y progresiva para registros siguientes.

## Excel SCAP

`ScapExcelExporter` carga una copia de `docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx`, empaquetada por Gradle como asset generado. El mapa empaquetado procede directamente de `tools/scap_field_map.json`. No se usa Apache POI ni se incorpora una librería de Excel adicional al APK. El archivo fuente canónico no se modifica.

Se conservan las diez hojas, estilos, rangos combinados, anchos, alturas, catálogos, nombres definidos y fórmulas de cálculo. Se actualizan los campos A–D con sus tipos numéricos/fecha/progresiva, elementos, metrados, porcentajes, defectos por elemento, fotos y croquis. La tabla porcentual F usa fracciones de 0–1 en orden 5→0; G usa porcentajes de 0–100 en orden 0→5. Las fotos se insertan en los espacios existentes; las originales y sus nombres no se modifican.

Se eliminan fotos/croquis del ejemplo, medios que quedan sin referencia, cadenas compartidas sin uso y cachés de fórmulas. Se conserva la geometría de las imágenes de la plantilla y sus logos. Excel recalcula al abrir mediante calcMode=auto, fullCalcOnLoad y forceFullCalc; se retira la cadena de cálculo obsoleta.

**Distinción necesaria entre fórmulas y datos del ejemplo:** E66 conserva el enlace a la longitud total. Se sustituyen J62 (presuponía un único tramo de la longitud total), las fórmulas de metrado de G/C14:C27 y las autodescripciones de accesos por los datos efectivamente capturados. Se vacían las fórmulas narrativas de CONCLUSIONES B19/B29/B39/E39 porque contienen afirmaciones y recomendaciones del ejemplo. Se limpian las tablas auxiliares del perfil dibujado del ejemplo. No se alteran las fórmulas técnicas de evaluación de G. La plantilla canónica conserva también esas fórmulas originales para revisión.

### Límites y mapeos pendientes

No se recorta silenciosamente una ficha. La revisión bloquea únicamente la generación SCAP si excede los casilleros disponibles: 3 elementos de superestructura, 2 subestructura, 4 detalles, 2 cauce, 3 accesos; 32 fotos; 15 puntos de perfil; 3 longitudes de tramo; 2 grupos de apoyos; 3 pilares; un croquis por tipo. La captura y el guardado local de N filas siguen funcionando. Para exportar más se necesita ampliar y validar la plantilla y sus fórmulas.

Los bloques principal/secundario, el tablero único C.3, la casilla colectiva «Pilar 3, 4 y 5», los defectos generales sin elemento y las conclusiones requieren confirmación de correspondencia. Los datos sin mapeo inequívoco se dejan vacíos y se anuncian antes de exportar; permanecen en Room. No se infiere tramo principal, materiales equivalentes ni recomendaciones. Ver `SCAP_PENDING_CLIENT_DECISIONS.md`.

La plantilla original contiene nombres #REF! y casos de cálculo indefinido; se conservan, no se corrigen mediante fórmulas nuevas. Una ficha incompleta o un caso indefinido puede mostrar errores al recalcular en Excel. La app explica el motivo cuando no puede producir índice global. Confirmar con ingeniería la fórmula definitiva y esos casos.

## SCAP → SIC

La revisión G ofrece SCAP, SIC-17, SIC-17A y SIC-17B. `ScapSicExporter` solo toma DIRECT y TRANSFORM de `ScapToSicMapper`. Los destinos con datos necesarios ausentes tienen el botón deshabilitado y muestran los faltantes; esto nunca impide guardar SCAP. Los campos opcionales ausentes se enumeran y salen vacíos.

SIC-17 requiere identificación, ubicación inicial y los códigos de clasificación/condición/servicio/singularidad de la ficha. Las equivalencias no aprobadas normalmente bloquean este formato. 17A/17B usan las definiciones de los formularios complementarios existentes, cuyo contenido era opcional; el mínimo técnico de 17A es código y nombre, y de 17B es código. **Esto no constituye una matriz contractual de completitud aprobada:** debe confirmarse si ingeniería necesita elevar otros campos a obligatorios. Los XLSX parciales muestran antes de exportar todos sus vacíos y no se presentan como fichas completas. Los nuevos exportadores no escriben SIC en Room ni activan subida.

## Prueba manual exacta en tablet

1. Respaldar los datos existentes. Instalar el APK de esta entrega encima de la app actual, sin desinstalarla ni borrar almacenamiento. Abrir el historial y comprobar una SIC-22 anterior tipo 4; verificar su número en la exportación histórica.
2. Iniciar un registro SIC-22. Confirmar solo Reglamento/Preventiva/Informativa. Registrar un hito como Informativa; no debe aparecer número independiente. Guardar y abrir el siguiente registro: ruta y PR/progresiva deben conservarse.
3. Sin catálogo personalizado, recorrer los tres botones de tramo: deben ofrecer las mismas seis rutas. Elegir PE-28H y luego PE-3N; el catálogo inicial no debe bloquear ese cambio. Probar un código manual real. Configurar un catálogo de prueba en un tramo y comprobar que solo ese tramo usa su orden.
4. Capturar SIC-18 circular: solo diámetro. Cambiar a ovalada: exigir ancho y altura. Elegir manualmente las condiciones y revisar sus descripciones. No deben aparecer porcentajes auxiliares en formulario/resumen/historial. Exportar ambos: circular con segunda dimensión vacía, ovalada con altura.
5. Capturar SIC-21 clases 18, 20 y 19. Solo Seguridad (19) muestra Material. Exportar y comprobar vacío/vacío/valor respectivamente.
6. Abrir SCAP, crear una ficha con nombre y código de prueba distintos de Agua Blanca, ruta, progresiva y fecha. Añadir un tramo y dos elementos; ingresar sus metrados y seis porcentajes válidos que sumen 100. Guardar y reabrir para comprobar persistencia.
7. Añadir una foto desde cámara, otra del selector y un croquis. Revisar las categorías y la vinculación a elementos/defectos. Capturar y aceptar GNSS. Ir a G y esperar «Guardado local».
8. Leer los faltantes de exportación. Tocar «Guardar Excel SCAP», elegir carpeta mediante el selector Android y abrir el XLSX en Excel. Confirmar las diez hojas, nombre/ruta/PR propios, imágenes propias, ausencia de datos de Agua Blanca, fechas y metrados correctos. Permitir recálculo y revisar G con el ingeniero.
9. En G, comprobar el bloqueo de SIC-17 y su lista de códigos faltantes. Exportar 17A/17B cuando sus mínimos estén presentes y comprobar que los campos sin equivalencia permanecen vacíos. Una ficha sin código debe bloquear ambos. Volver a guardar SCAP: debe seguir permitido.
10. Superar una capacidad de plantilla (por ejemplo, agregar una cuarta superestructura) y comprobar que el motivo aparece antes de generar, sin borrar elementos de la ficha. Volver a la capacidad admitida para continuar la prueba.
11. Completar/reabrir SCAP y comprobar el mensaje local exacto. Guardar dos puentes distintos y verificar que sus fotos siguen separadas en sus fichas. Intentar sincronización SIC: ninguna foto SCAP debe encolarse. La separación en carpetas remotas se probará cuando exista endpoint SCAP; esta entrega no hace subidas SCAP.
12. Comprobar que SIC-18 y otros elementos puntuales no muestran GPS final. SIC-19 y los demás tipos provisionales lineales conservan su flujo de GPS final.

La ejecución en tablet, GNSS real y Drive real requiere el dispositivo/configuración del usuario. Los resultados automatizados y el APK compilado se registran en `SCAP_VERIFICATION.json`. Los archivos modificados están en `ENGINEERING_CHANGED_FILES.md` y `FIELD_CHANGED_FILES.md`.

## Verificación de esta entrega

`testDebugUnitTest` y `assembleDebug` finalizaron correctamente: **82 pruebas, 0 fallos**. Se verificaron también hojas, geometría, estilos, conservación de fórmulas de cálculo, limpieza de ejemplos, sustitución de medios, límites de plantilla, tipos históricos en Room, proyecciones SIC y exclusión del uploader.

Microsoft Excel 16.0 abrió el SCAP generado en modo solo lectura, sin actualizar enlaces ni guardar el archivo. `CalculateFullRebuild` contó los dos elementos sintéticos y calculó **2.2804320641498554 → 2.280, REGULAR**, coincidiendo con la fórmula independiente de la aplicación. El ejemplo canónico de 14 elementos sigue dando **2.0700860438436695** en la suite. El visor de comprobación gráfica mostró errores en su propio motor de cálculo; por ello el resultado se contrastó con Microsoft Excel. Las previsualizaciones se usaron para revisar el formato y no se reexportaron como archivos finales.

`tools/verify_excel_export.ps1` reproduce esa comprobación local si hay Microsoft Excel instalado; nunca guarda ni reescribe el workbook. Su resultado está en `docs/SCAP_EXCEL_VALIDATION.json`. El ensayo usa datos sintéticos claramente identificados y no constituye una inspección real ni sustituye la prueba de tablet.
