# Verificación completa del exportador SCAP

Fecha de ejecución: 25/09/2026 (America/Lima). Caso sintético: **SCAP_COMPLETO_TEST**. No contiene mediciones de campo reales.

## Resultado

- Diez hojas originales, mismo orden y nombres; reapertura con el lector XML del exportador y con **Microsoft Excel 16** en solo lectura.
- **421 comparaciones de campo/celda aprobadas** en el archivo reabierto y nuevamente contra `Range.Value2` de Excel. Cero discrepancias y cero errores de fórmula después del recálculo.
- Cuatro tramos/tableros, dos estribos, dos anclajes, **siete pilares diferentes**, cuatro apoyos, tres juntas, ambos accesos y tres puntos de perfil.
- Catorce elementos evaluados, seis porcentajes por elemento, 16 observaciones (incluidas dos para el elemento 110 y una general), nueve fotografías con categorías distintas y tres croquis.
- **12 imágenes incrustadas** reconocidas por Excel. Se conserva el contenido completo y la proporción dentro de los marcos. El alto real de las celdas limita el croquis aunque su antiguo transformado gráfico tenga un tamaño desactualizado.
- Estilos (`xl/styles.xml`) idénticos byte por byte; merges y alturas originales conservados; validaciones y combinaciones copiadas en continuaciones; fórmulas locales trasladadas y fórmulas globales G conservadas. Todas las partes XML y relaciones se pueden analizar.
- No quedan los valores de ejemplo «PUENTE AGUA BLANCA» ni «Ing. Martín Pichón». No se altera `docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx`.

## Cálculo en Excel

| Celda, hoja G | Resultado |
|---|---:|
| E186, elementos | 14 |
| E187, contribución máxima | 1,9156843053023804 |
| E191, condición global | 2,3926073822254574 |
| M12, clasificación | REGULAR |

Se mantienen las fórmulas de la metodología. Se agregó únicamente una condición de celda vacía a las búsquedas auxiliares D45:D61 de F: una fila sin código queda vacía, mientras que un código no reconocido sigue produciendo un error visible. Antes de esta corrección las tres filas auxiliares sobrantes generaban #N/D.

## Hojas revisadas

| Hoja | Comprobación |
|---|---|
| A-D.- FICHA | Campos A/B/C1-C10/D1-D4, herencias, tramos y continuaciones, siete pilares con suelo propio |
| E.- CROQUIS | Tres imágenes reales de prueba, elevación/planta/sección transversal, vinculadas y visibles en Excel |
| F.- ELEMENTOS | 14 códigos, metrados, unidades/factores por fórmulas y seis porcentajes |
| sec F | Observaciones por elemento, dos fallas del 110 y observación general en continuación |
| F.2.-PANEL FOTOGRAFICO | Nueve fotografías, orden, categoría legible, descripción, código y defectos asociados |
| G.- CONDICION ESTADISTICA | 14 registros, porcentajes, fórmulas originales y recálculo real |
| CONCLUSIONES | Se eliminan los textos del puente de ejemplo; no se inventan conclusiones |
| AUXILIAR | Catálogo y factores conservados |
| Hoja1 | Metodología conservada |
| Hoja3 | Etiquetas auxiliares conservadas |

Los renders del visor de hojas sirven para revisar celdas/estilos; ese visor no dibuja todos los objetos originales ni reproduce todos los cálculos de Excel. Las imágenes y el resultado numérico se contrastaron además en Microsoft Excel y en vistas PDF temporales. No se toma un #VALUE del visor alternativo como resultado de Excel.

## Continuaciones verificadas e impresión

| Elementos | Filas, primera hoja | Suelos |
|---|---|---|
| Pilares 1-3 | G/J/N, filas 173-183 | J/L/N392 |
| Pilares 4-6 | 520-535 | J/L/N543 |
| Pilar 7 | 547-562 | J570 |
| Apoyos 3-4 | 574-582 | No corresponde |
| Junta 2 | 586-590 | No corresponde |
| Junta 3 | 594-598 | No corresponde |
| Tablero del tramo 2 | 602-638 | No corresponde |
| Tablero del tramo 3 | 642-678 | No corresponde |
| Tablero del tramo 4 | 682-718 | No corresponde |

Se conserva el área original por segmentos y se añaden A:R de cada continuación. La observación general amplía `sec F` a A45:L46. **Límite observado en este equipo:** Excel 16 reconoce los nombres de impresión de la plantilla como nombres ordinarios y no activa automáticamente `PageSetup.PrintArea`, tanto antes como después de exportar. Por ello no se certifica la impresión predeterminada. Para la revisión visual del panel se estableció A1:F21 y las filas repetidas 1:12 únicamente en la sesión de prueba, sin guardar el libro; las nueve imágenes se visualizaron en dos páginas. Antes de imprimir, establecer los segmentos indicados o revisar el área desde Excel. Los datos, fórmulas y dibujos del XLSX no dependen de esa configuración de impresora.

## Reproducción y evidencias locales

1. Ejecutar `gradlew.bat testDebugUnitTest assembleDebug` desde el repositorio. `ScapFullExportTest` escribe `tmp/engineering2/SCAP_COMPLETO_TEST.xlsx` y `tmp/engineering2/full-export-cells.json`.
2. Con Excel instalado: `tools/verify_excel_export.ps1 -InputWorkbook tmp/engineering2/SCAP_COMPLETO_TEST.xlsx -OutputReport tmp/engineering2/excel-verification.json -CellManifest tmp/engineering2/full-export-cells.json`.
3. Revisar `app/build/reports/tests/testDebugUnitTest/index.html` y el [checklist físico](REVISION_INGENIERIA_PUENTES_2.md).

El libro y los renders son salidas temporales de prueba, no otra plantilla oficial. El inventario previo y la verificación de hashes están en `tmp/engineering2/baseline.json` y `preservation.json`.

## Matriz de campos verificados

`structure0/1`: estribos; `structure2/3`: anclajes; `structure4..10`: pilares 1..7. `support1..4`: apoyos; `joint1..3`: juntas. Las fechas se verifican con su serial Excel y las progresivas con su valor numérico. «vacío» significa ausencia intencional por aplicabilidad/visibilidad, no sustitución por datos del ejemplo. En la columna de valor se muestra el esperado y observado, que coinciden.

| Campo app / propietario | Hoja | Celda | Valor esperado = observado | Estado |
|---|---|---|---|---|
| `inspection.bridgeName` | A-D.- FICHA | J9 | SCAP_COMPLETO_TEST | OK XML + Excel |
| `inspection.bridgeCode` | A-D.- FICHA | J11 | P-TEST-7 | OK XML + Excel |
| `inspection.altitudeM` | A-D.- FICHA | E13 | 2.02 | OK XML + Excel |
| `inspection.utmEasting` | A-D.- FICHA | E15 | 2.03 | OK XML + Excel |
| `inspection.utmNorthing` | A-D.- FICHA | E17 | 2.04 | OK XML + Excel |
| `inspection.segment` | A-D.- FICHA | E19 | TEST_A_5 | OK XML + Excel |
| `inspection.politicalDepartment` | A-D.- FICHA | J13 | TEST_A_6 | OK XML + Excel |
| `inspection.roadDepartment` | A-D.- FICHA | J15 | TEST_A_7 | OK XML + Excel |
| `inspection.province` | A-D.- FICHA | J17 | TEST_A_8 | OK XML + Excel |
| `inspection.district` | A-D.- FICHA | J19 | TEST_A_9 | OK XML + Excel |
| `inspection.nearbyTown` | A-D.- FICHA | P13 | TEST_A_10 | OK XML + Excel |
| `inspection.route` | A-D.- FICHA | P15 | PE-3N | OK XML + Excel |
| `inspection.progressive` | A-D.- FICHA | P17 | 45321.5 | OK XML + Excel |
| `inspection.routeType` | A-D.- FICHA | P19 | Nacional | OK XML + Excel |
| `inspection.over` | A-D.- FICHA | E27 | Río | OK XML + Excel |
| `inspection.singularityName` | A-D.- FICHA | E29 | TEST_B_15 | OK XML + Excel |
| `inspection.totalLengthM` | A-D.- FICHA | E31 | 46.0 | OK XML + Excel |
| `inspection.lanes` | A-D.- FICHA | E33 | 2 | OK XML + Excel |
| `inspection.roadwayWidthM` | A-D.- FICHA | E35 | 2.18 | OK XML + Excel |
| `inspection.designLoad` | A-D.- FICHA | E37 | TEST_C8_46 | OK XML + Excel |
| `inspection.sidewalkWidthM` | A-D.- FICHA | E39 | 2.39 | OK XML + Excel |
| `inspection.upperClearanceM` | A-D.- FICHA | E41 | 2.21 | OK XML + Excel |
| `inspection.lowerClearanceM` | A-D.- FICHA | E43 | 2.7199999999999998 | OK XML + Excel |
| `inspection.projectNumber` | A-D.- FICHA | N27 | TEST_B_23 | OK XML + Excel |
| `inspection.constructionYear` | A-D.- FICHA | N29 | 2009 | OK XML + Excel |
| `inspection.lastInspection` | A-D.- FICHA | N31 | 46290 | OK XML + Excel |
| `inspection.lastWork` | A-D.- FICHA | N33 | TEST_B_26 | OK XML + Excel |
| `inspection.service` | A-D.- FICHA | N35 | Irrestricto | OK XML + Excel |
| `inspection.traffic` | A-D.- FICHA | N37 | 2 | OK XML + Excel |
| `inspection.trafficYear` | A-D.- FICHA | N39 | 2009 | OK XML + Excel |
| `inspection.truckBusPercent` | A-D.- FICHA | N41 | 2.3 | OK XML + Excel |
| `inspection.alignment` | A-D.- FICHA | N43 | Recto | OK XML + Excel |
| `inspection.environment` | A-D.- FICHA | N45 | Severo | OK XML + Excel |
| `inspection.spanArrangement` | A-D.- FICHA | E64 | Iguales | OK XML + Excel |
| `inspection.mainSpanM` | A-D.- FICHA | E68 | 2.35 | OK XML + Excel |
| `inspection.railingType` | A-D.- FICHA | E229 | Postes y pasamanos | OK XML + Excel |
| `inspection.railingMaterial` | A-D.- FICHA | E231 | Concreto | OK XML + Excel |
| `inspection.railingSecondary` | A-D.- FICHA | E233 | 1 pasamano | OK XML + Excel |
| `inspection.sidewalkWidthM` | A-D.- FICHA | N229 | 2.39 | OK XML + Excel |
| `inspection.curbHeightM` | A-D.- FICHA | N231 | 2.4 | OK XML + Excel |
| `inspection.sidewalkMaterial` | A-D.- FICHA | N233 | Concreto | OK XML + Excel |
| `joint1.jointType` | A-D.- FICHA | H249 | Planchas Deslizantes | OK XML + Excel |
| `joint1.jointMaterial` | A-D.- FICHA | H251 | Jebe | OK XML + Excel |
| `inspection.drainType` | A-D.- FICHA | H255 | Tubo | OK XML + Excel |
| `inspection.drainMaterial` | A-D.- FICHA | H257 | Acero | OK XML + Excel |
| `inspection.designLoad` | A-D.- FICHA | E322 | TEST_C8_46 | OK XML + Excel |
| `inspection.currentMaxLoad` | A-D.- FICHA | L322 | TEST_C8_47 | OK XML + Excel |
| `inspection.futureLoad` | A-D.- FICHA | E324 | TEST_C8_48 | OK XML + Excel |
| `inspection.overstress` | A-D.- FICHA | L324 | TEST_C8_49 | OK XML + Excel |
| `inspection.loadSignage` | A-D.- FICHA | E326 | Si tiene | OK XML + Excel |
| `inspection.alternateRoute` | A-D.- FICHA | E335 | TEST_C9_51 | OK XML + Excel |
| `inspection.fordExists` | A-D.- FICHA | E337 | Si | OK XML + Excel |
| `inspection.fordDistanceM` | A-D.- FICHA | E339 | 2.5300000000000002 | OK XML + Excel |
| `inspection.fordPeriod` | A-D.- FICHA | E341 | TEST_C9_54 | OK XML + Excel |
| `inspection.fordMinDepthM` | A-D.- FICHA | E343 | 2.55 | OK XML + Excel |
| `inspection.fordSoil` | A-D.- FICHA | E345 | Roca | OK XML + Excel |
| `inspection.existingDetour` | A-D.- FICHA | E347 | Si | OK XML + Excel |
| `inspection.detourNeeded` | A-D.- FICHA | E349 | Si | OK XML + Excel |
| `inspection.parallelExists` | A-D.- FICHA | L337 | Si | OK XML + Excel |
| `inspection.parallelPossibility` | A-D.- FICHA | N339 | Si | OK XML + Excel |
| `inspection.parallelLengthM` | A-D.- FICHA | L341 | 2.61 | OK XML + Excel |
| `inspection.parallelSubstructure` | A-D.- FICHA | L343 | Similar al existente | OK XML + Excel |
| `inspection.parallelType` | A-D.- FICHA | L345 | Gravedad | OK XML + Excel |
| `inspection.roadCondition` | A-D.- FICHA | L357 | Buena | OK XML + Excel |
| `inspection.soilComments` | A-D.- FICHA | E394 | TEST_D1_65 | OK XML + Excel |
| `inspection.maxWaterM` | A-D.- FICHA | E403 | 2.66 | OK XML + Excel |
| `inspection.maxWaterPeriod` | A-D.- FICHA | N403 | TEST_D2_67 | OK XML + Excel |
| `inspection.minWaterM` | A-D.- FICHA | E405 | 2.68 | OK XML + Excel |
| `inspection.dryPeriod` | A-D.- FICHA | N405 | TEST_D2_69 | OK XML + Excel |
| `inspection.extraordinaryWaterM` | A-D.- FICHA | E407 | 2.7 | OK XML + Excel |
| `inspection.returnFrequency` | A-D.- FICHA | N407 | TEST_D2_71 | OK XML + Excel |
| `inspection.lowerClearanceM` | A-D.- FICHA | E411 | 2.7199999999999998 | OK XML + Excel |
| `inspection.fieldClearanceDate` | A-D.- FICHA | N411 | 46290 | OK XML + Excel |
| `inspection.planClearanceM` | A-D.- FICHA | E413 | 2.74 | OK XML + Excel |
| `inspection.maxWaterClearanceM` | A-D.- FICHA | E415 | 2.75 | OK XML + Excel |
| `inspection.acceptableLength` | A-D.- FICHA | E424 | Si | OK XML + Excel |
| `inspection.requiredLengthM` | A-D.- FICHA | L424 | (vacío) | OK XML + Excel |
| `inspection.acceptableHeight` | A-D.- FICHA | E426 | Si | OK XML + Excel |
| `inspection.additionalHeightM` | A-D.- FICHA | L426 | (vacío) | OK XML + Excel |
| `inspection.channelingNeeded` | A-D.- FICHA | E428 | Si | OK XML + Excel |
| `inspection.channelingLengthM` | A-D.- FICHA | L428 | 2.81 | OK XML + Excel |
| `inspection.scour` | A-D.- FICHA | E430 | Si | OK XML + Excel |
| `inspection.scourDepthM` | A-D.- FICHA | L430 | 2.83 | OK XML + Excel |
| `inspection.profileReference` | A-D.- FICHA | G449 | TEST_D4_84 | OK XML + Excel |
| `inspection.scourProtection` | A-D.- FICHA | G477 | Si | OK XML + Excel |
| `inspection.scourProtectionType` | A-D.- FICHA | L477 | TEST_D4_86 | OK XML + Excel |
| `structure0.elevationType` | A-D.- FICHA | J120 | Gravedad | OK XML + Excel |
| `structure0.elevationMaterial` | A-D.- FICHA | J122 | Concreto Simple | OK XML + Excel |
| `structure0.foundationType` | A-D.- FICHA | J124 | Zapata | OK XML + Excel |
| `structure0.foundationMaterial` | A-D.- FICHA | J126 | Concreto Simple | OK XML + Excel |
| `structure0.soil` | A-D.- FICHA | E392 | Roca | OK XML + Excel |
| `structure1.elevationType` | A-D.- FICHA | N120 | Cantilever | OK XML + Excel |
| `structure1.elevationMaterial` | A-D.- FICHA | N122 | Concreto Armado | OK XML + Excel |
| `structure1.foundationType` | A-D.- FICHA | N124 | Caisson | OK XML + Excel |
| `structure1.foundationMaterial` | A-D.- FICHA | N126 | Concreto Armado | OK XML + Excel |
| `structure1.soil` | A-D.- FICHA | G392 | Conglomerado | OK XML + Excel |
| `structure2.elevationType` | A-D.- FICHA | G191 | Otros | OK XML + Excel |
| `structure2.elevationMaterial` | A-D.- FICHA | G193 | Concreto Simple | OK XML + Excel |
| `structure2.foundationType` | A-D.- FICHA | G197 | Zapata | OK XML + Excel |
| `structure2.foundationMaterial` | A-D.- FICHA | G199 | Concreto Simple | OK XML + Excel |
| `structure3.elevationType` | A-D.- FICHA | L191 | Macizo | OK XML + Excel |
| `structure3.elevationMaterial` | A-D.- FICHA | L193 | Concreto Armado | OK XML + Excel |
| `structure3.foundationType` | A-D.- FICHA | L197 | Otros | OK XML + Excel |
| `structure3.foundationMaterial` | A-D.- FICHA | L199 | Concreto Armado | OK XML + Excel |
| `structure4.elevationType` | A-D.- FICHA | G175 | Columna Capitel | OK XML + Excel |
| `structure4.elevationMaterial` | A-D.- FICHA | G177 | Otros | OK XML + Excel |
| `structure4.foundationType` | A-D.- FICHA | G181 | Zapata | OK XML + Excel |
| `structure4.foundationMaterial` | A-D.- FICHA | G183 | Concreto Simple | OK XML + Excel |
| `structure4.soil` | A-D.- FICHA | J392 | Arcilla | OK XML + Excel |
| `structure5.elevationType` | A-D.- FICHA | J175 | Columna Tarjeta | OK XML + Excel |
| `structure5.elevationMaterial` | A-D.- FICHA | J177 | Concreto Simple | OK XML + Excel |
| `structure5.foundationType` | A-D.- FICHA | J181 | Caisson | OK XML + Excel |
| `structure5.foundationMaterial` | A-D.- FICHA | J183 | Concreto Armado | OK XML + Excel |
| `structure5.soil` | A-D.- FICHA | L392 | Aluvional | OK XML + Excel |
| `structure6.elevationType` | A-D.- FICHA | N175 | Portico | OK XML + Excel |
| `structure6.elevationMaterial` | A-D.- FICHA | N177 | Concreto Armado | OK XML + Excel |
| `structure6.foundationType` | A-D.- FICHA | N181 | Pilotes | OK XML + Excel |
| `structure6.foundationMaterial` | A-D.- FICHA | N183 | Acero | OK XML + Excel |
| `structure6.soil` | A-D.- FICHA | N392 | Otros | OK XML + Excel |
| `structure7.elevationType` | A-D.- FICHA | G527 | Otros | OK XML + Excel |
| `structure7.elevationMaterial` | A-D.- FICHA | G529 | Acero | OK XML + Excel |
| `structure7.foundationType` | A-D.- FICHA | G533 | Otros | OK XML + Excel |
| `structure7.foundationMaterial` | A-D.- FICHA | G535 | Madera | OK XML + Excel |
| `structure7.soil` | A-D.- FICHA | J543 | Roca | OK XML + Excel |
| `structure8.elevationType` | A-D.- FICHA | J527 | Columna Capitel | OK XML + Excel |
| `structure8.elevationMaterial` | A-D.- FICHA | J529 | Madera | OK XML + Excel |
| `structure8.foundationType` | A-D.- FICHA | J533 | Zapata | OK XML + Excel |
| `structure8.foundationMaterial` | A-D.- FICHA | J535 | Concreto Simple | OK XML + Excel |
| `structure8.soil` | A-D.- FICHA | L543 | Conglomerado | OK XML + Excel |
| `structure9.elevationType` | A-D.- FICHA | N527 | Columna Tarjeta | OK XML + Excel |
| `structure9.elevationMaterial` | A-D.- FICHA | N529 | Otros | OK XML + Excel |
| `structure9.foundationType` | A-D.- FICHA | N533 | Caisson | OK XML + Excel |
| `structure9.foundationMaterial` | A-D.- FICHA | N535 | Concreto Armado | OK XML + Excel |
| `structure9.soil` | A-D.- FICHA | N543 | Piedra | OK XML + Excel |
| `structure10.elevationType` | A-D.- FICHA | G554 | Portico | OK XML + Excel |
| `structure10.elevationMaterial` | A-D.- FICHA | G556 | Concreto Simple | OK XML + Excel |
| `structure10.foundationType` | A-D.- FICHA | G560 | Pilotes | OK XML + Excel |
| `structure10.foundationMaterial` | A-D.- FICHA | G562 | Acero | OK XML + Excel |
| `structure10.soil` | A-D.- FICHA | J570 | Arena | OK XML + Excel |
| `pier1.title` | A-D.- FICHA | G173 | Pilar 1 | OK XML + Excel |
| `pier2.title` | A-D.- FICHA | J173 | Pilar 2 | OK XML + Excel |
| `pier3.title` | A-D.- FICHA | N173 | Pilar 3 | OK XML + Excel |
| `pier4.title` | A-D.- FICHA | G525 | Pilar 4 | OK XML + Excel |
| `pier5.title` | A-D.- FICHA | J525 | Pilar 5 | OK XML + Excel |
| `pier6.title` | A-D.- FICHA | N525 | Pilar 6 | OK XML + Excel |
| `pier7.title` | A-D.- FICHA | G552 | Pilar 7 | OK XML + Excel |
| `support1.type` | A-D.- FICHA | H239 | Deslizante | OK XML + Excel |
| `support1.material` | A-D.- FICHA | H241 | Elastomero | OK XML + Excel |
| `support1.location` | A-D.- FICHA | H243 | TEST_C5_3 | OK XML + Excel |
| `support1.number` | A-D.- FICHA | H245 | 3 | OK XML + Excel |
| `support2.type` | A-D.- FICHA | J239 | Roller | OK XML + Excel |
| `support2.material` | A-D.- FICHA | J241 | Concreto | OK XML + Excel |
| `support2.location` | A-D.- FICHA | J243 | TEST_C5_4 | OK XML + Excel |
| `support2.number` | A-D.- FICHA | J245 | 4 | OK XML + Excel |
| `support3.type` | A-D.- FICHA | H576 | Rocker | OK XML + Excel |
| `support3.material` | A-D.- FICHA | H578 | Flexcel | OK XML + Excel |
| `support3.location` | A-D.- FICHA | H580 | TEST_C5_5 | OK XML + Excel |
| `support3.number` | A-D.- FICHA | H582 | 5 | OK XML + Excel |
| `support4.type` | A-D.- FICHA | J576 | Eslabon y pin | OK XML + Excel |
| `support4.material` | A-D.- FICHA | J578 | Acero | OK XML + Excel |
| `support4.location` | A-D.- FICHA | J580 | TEST_C5_6 | OK XML + Excel |
| `support4.number` | A-D.- FICHA | J582 | 6 | OK XML + Excel |
| `joint2.jointType` | A-D.- FICHA | H588 | Tipo Peine | OK XML + Excel |
| `joint2.jointMaterial` | A-D.- FICHA | H590 | Mastic Epoxico | OK XML + Excel |
| `joint3.jointType` | A-D.- FICHA | H596 | Tipo Comprensible/Expansible | OK XML + Excel |
| `joint3.jointMaterial` | A-D.- FICHA | H598 | Otros | OK XML + Excel |
| `span1.category` | A-D.- FICHA | E76 | DEFINITIVO | OK XML + Excel |
| `span1.type` | A-D.- FICHA | E78 | Losa | OK XML + Excel |
| `span1.secondaryCharacteristic` | A-D.- FICHA | E80 | TEST_C2_3 | OK XML + Excel |
| `span1.edgeCondition` | A-D.- FICHA | E82 | Simp. Apoyado | OK XML + Excel |
| `span1.predominantMaterial` | A-D.- FICHA | E84 | Concreto armado | OK XML + Excel |
| `span1.slabMaterial` | A-D.- FICHA | E98 | Concreto Armado | OK XML + Excel |
| `span1.slabThicknessM` | A-D.- FICHA | E100 | 2.07 | OK XML + Excel |
| `span1.wearingSurface` | A-D.- FICHA | E102 | Asfalto | OK XML + Excel |
| `span1.wearingThicknessM` | A-D.- FICHA | E104 | 2.09 | OK XML + Excel |
| `span1.beamType` | A-D.- FICHA | J98 | Viga Longitudinal | OK XML + Excel |
| `span1.beamCount` | A-D.- FICHA | J100 | 2 | OK XML + Excel |
| `span1.beamMaterial` | A-D.- FICHA | J102 | Concreto Armado | OK XML + Excel |
| `span1.beamShape` | A-D.- FICHA | J104 | Rectangular | OK XML + Excel |
| `span1.beamDepth` | A-D.- FICHA | J106 | 1.35-2.45 | OK XML + Excel |
| `span1.beamSpacingM` | A-D.- FICHA | P104 | 2.15 | OK XML + Excel |
| `span1.beamBaseWidthM` | A-D.- FICHA | P106 | 2.16 | OK XML + Excel |
| `span2.category` | A-D.- FICHA | E606 | DEFINITIVO | OK XML + Excel |
| `span2.type` | A-D.- FICHA | E608 | Losa con Vigas | OK XML + Excel |
| `span2.secondaryCharacteristic` | A-D.- FICHA | E610 | TEST_C2_4 | OK XML + Excel |
| `span2.edgeCondition` | A-D.- FICHA | E612 | Continuo | OK XML + Excel |
| `span2.predominantMaterial` | A-D.- FICHA | E614 | Concreto Pretensado | OK XML + Excel |
| `span2.slabMaterial` | A-D.- FICHA | E628 | Concreto Pretensado | OK XML + Excel |
| `span2.slabThicknessM` | A-D.- FICHA | E630 | 3.07 | OK XML + Excel |
| `span2.wearingSurface` | A-D.- FICHA | E632 | Concreto (Vaciado con Losa) | OK XML + Excel |
| `span2.wearingThicknessM` | A-D.- FICHA | E634 | 3.09 | OK XML + Excel |
| `span2.beamType` | A-D.- FICHA | J628 | Viga Transversal | OK XML + Excel |
| `span2.beamCount` | A-D.- FICHA | J630 | 3 | OK XML + Excel |
| `span2.beamMaterial` | A-D.- FICHA | J632 | Concreto Pretensado | OK XML + Excel |
| `span2.beamShape` | A-D.- FICHA | J634 | I | OK XML + Excel |
| `span2.beamDepth` | A-D.- FICHA | J636 | 1.35-2.45 | OK XML + Excel |
| `span2.beamSpacingM` | A-D.- FICHA | P634 | 3.15 | OK XML + Excel |
| `span2.beamBaseWidthM` | A-D.- FICHA | P636 | 3.16 | OK XML + Excel |
| `span3.category` | A-D.- FICHA | E646 | DEFINITIVO | OK XML + Excel |
| `span3.type` | A-D.- FICHA | E648 | Portico | OK XML + Excel |
| `span3.secondaryCharacteristic` | A-D.- FICHA | E650 | TEST_C2_5 | OK XML + Excel |
| `span3.edgeCondition` | A-D.- FICHA | E652 | Gerber | OK XML + Excel |
| `span3.predominantMaterial` | A-D.- FICHA | E654 | Acero Estructural | OK XML + Excel |
| `span3.slabMaterial` | A-D.- FICHA | E668 | Plancha Metálica corrugada | OK XML + Excel |
| `span3.slabThicknessM` | A-D.- FICHA | E670 | 4.07 | OK XML + Excel |
| `span3.wearingSurface` | A-D.- FICHA | E672 | Concreto Pobre | OK XML + Excel |
| `span3.wearingThicknessM` | A-D.- FICHA | E674 | 4.09 | OK XML + Excel |
| `span3.beamType` | A-D.- FICHA | J668 | Otros | OK XML + Excel |
| `span3.beamCount` | A-D.- FICHA | J670 | 4 | OK XML + Excel |
| `span3.beamMaterial` | A-D.- FICHA | J672 | Metálico | OK XML + Excel |
| `span3.beamShape` | A-D.- FICHA | J674 | Cajón | OK XML + Excel |
| `span3.beamDepth` | A-D.- FICHA | J676 | 1.35-2.45 | OK XML + Excel |
| `span3.beamSpacingM` | A-D.- FICHA | P674 | 4.15 | OK XML + Excel |
| `span3.beamBaseWidthM` | A-D.- FICHA | P676 | 4.16 | OK XML + Excel |
| `span4.category` | A-D.- FICHA | E686 | DEFINITIVO | OK XML + Excel |
| `span4.type` | A-D.- FICHA | E688 | Arco | OK XML + Excel |
| `span4.secondaryCharacteristic` | A-D.- FICHA | E690 | TEST_C2_6 | OK XML + Excel |
| `span4.edgeCondition` | A-D.- FICHA | E692 | Articulado | OK XML + Excel |
| `span4.predominantMaterial` | A-D.- FICHA | E694 | Cables de Acero | OK XML + Excel |
| `span4.slabMaterial` | A-D.- FICHA | E708 | Madera | OK XML + Excel |
| `span4.slabThicknessM` | A-D.- FICHA | E710 | 5.07 | OK XML + Excel |
| `span4.wearingSurface` | A-D.- FICHA | E712 | Madera | OK XML + Excel |
| `span4.wearingThicknessM` | A-D.- FICHA | E714 | 5.09 | OK XML + Excel |
| `span4.beamType` | A-D.- FICHA | J708 | Viga Longitudinal | OK XML + Excel |
| `span4.beamCount` | A-D.- FICHA | J710 | 5 | OK XML + Excel |
| `span4.beamMaterial` | A-D.- FICHA | J712 | Madera | OK XML + Excel |
| `span4.beamShape` | A-D.- FICHA | J714 | Reticulada | OK XML + Excel |
| `span4.beamDepth` | A-D.- FICHA | J716 | 1.35-2.45 | OK XML + Excel |
| `span4.beamSpacingM` | A-D.- FICHA | P714 | 5.15 | OK XML + Excel |
| `span4.beamBaseWidthM` | A-D.- FICHA | P716 | 5.16 | OK XML + Excel |
| `ACCESS_LEFT.transitionLengthM` | A-D.- FICHA | G268 | 2.0 | OK XML + Excel |
| `ACCESS_LEFT.alignment` | A-D.- FICHA | G270 | Paralelo | OK XML + Excel |
| `ACCESS_LEFT.alignmentDetail` | A-D.- FICHA | H270 | (vacío) | OK XML + Excel |
| `ACCESS_LEFT.roadwayWidthM` | A-D.- FICHA | G272 | 2.03 | OK XML + Excel |
| `ACCESS_LEFT.shoulderWidthM` | A-D.- FICHA | G274 | 2.04 | OK XML + Excel |
| `ACCESS_LEFT.steepSlope` | A-D.- FICHA | G276 | Si | OK XML + Excel |
| `ACCESS_LEFT.visibility` | A-D.- FICHA | G278 | Buena | OK XML + Excel |
| `ACCESS_LEFT.informative` | A-D.- FICHA | G307 | Si | OK XML + Excel |
| `ACCESS_LEFT.informativeDetail` | A-D.- FICHA | H307 | Cartel del Puente | OK XML + Excel |
| `ACCESS_LEFT.preventive` | A-D.- FICHA | G309 | Si | OK XML + Excel |
| `ACCESS_LEFT.preventiveDetail` | A-D.- FICHA | H309 | Cartel Rombo Amarillo-Curvas | OK XML + Excel |
| `ACCESS_LEFT.regulatory` | A-D.- FICHA | G311 | Si | OK XML + Excel |
| `ACCESS_LEFT.regulatoryDetail` | A-D.- FICHA | H311 | Cartel rectangular negro-rojo (Ceda el paso) | OK XML + Excel |
| `ACCESS_LEFT.horizontal` | A-D.- FICHA | G313 | Si | OK XML + Excel |
| `ACCESS_LEFT.horizontalDetail` | A-D.- FICHA | H313 | Marcas en la Calzada | OK XML + Excel |
| `ACCESS_RIGHT.transitionLengthM` | A-D.- FICHA | N268 | 3.0 | OK XML + Excel |
| `ACCESS_RIGHT.alignment` | A-D.- FICHA | N270 | Perpendicular | OK XML + Excel |
| `ACCESS_RIGHT.alignmentDetail` | A-D.- FICHA | P270 | (vacío) | OK XML + Excel |
| `ACCESS_RIGHT.roadwayWidthM` | A-D.- FICHA | N272 | 3.03 | OK XML + Excel |
| `ACCESS_RIGHT.shoulderWidthM` | A-D.- FICHA | N274 | 3.04 | OK XML + Excel |
| `ACCESS_RIGHT.steepSlope` | A-D.- FICHA | N276 | No | OK XML + Excel |
| `ACCESS_RIGHT.visibility` | A-D.- FICHA | N278 | Regular | OK XML + Excel |
| `ACCESS_RIGHT.informative` | A-D.- FICHA | L307 | No | OK XML + Excel |
| `ACCESS_RIGHT.informativeDetail` | A-D.- FICHA | N307 | (vacío) | OK XML + Excel |
| `ACCESS_RIGHT.preventive` | A-D.- FICHA | L309 | No | OK XML + Excel |
| `ACCESS_RIGHT.preventiveDetail` | A-D.- FICHA | N309 | (vacío) | OK XML + Excel |
| `ACCESS_RIGHT.regulatory` | A-D.- FICHA | L311 | No | OK XML + Excel |
| `ACCESS_RIGHT.regulatoryDetail` | A-D.- FICHA | N311 | (vacío) | OK XML + Excel |
| `ACCESS_RIGHT.horizontal` | A-D.- FICHA | L313 | No | OK XML + Excel |
| `ACCESS_RIGHT.horizontalDetail` | A-D.- FICHA | N313 | (vacío) | OK XML + Excel |
| `point1.distanceM` | A-D.- FICHA | C461 | 3.0 | OK XML + Excel |
| `point1.downstreamM` | A-D.- FICHA | G461 | 3.01 | OK XML + Excel |
| `point1.upstreamM` | A-D.- FICHA | J461 | 3.02 | OK XML + Excel |
| `point1.axisM` | A-D.- FICHA | N461 | 3.03 | OK XML + Excel |
| `point2.distanceM` | A-D.- FICHA | C462 | 4.0 | OK XML + Excel |
| `point2.downstreamM` | A-D.- FICHA | G462 | 4.01 | OK XML + Excel |
| `point2.upstreamM` | A-D.- FICHA | J462 | 4.02 | OK XML + Excel |
| `point2.axisM` | A-D.- FICHA | N462 | 4.03 | OK XML + Excel |
| `point3.distanceM` | A-D.- FICHA | C463 | 5.0 | OK XML + Excel |
| `point3.downstreamM` | A-D.- FICHA | G463 | 5.01 | OK XML + Excel |
| `point3.upstreamM` | A-D.- FICHA | J463 | 5.02 | OK XML + Excel |
| `point3.axisM` | A-D.- FICHA | N463 | 5.03 | OK XML + Excel |
| `104.quantity` | G.- CONDICION ESTADISTICA | C14 | 10.0 | OK XML + Excel |
| `104.code` | G.- CONDICION ESTADISTICA | A14 | 104 | OK XML + Excel |
| `104.percent0` | G.- CONDICION ESTADISTICA | F14 | 0 | OK XML + Excel |
| `104.percent1` | G.- CONDICION ESTADISTICA | G14 | 80 | OK XML + Excel |
| `104.percent2` | G.- CONDICION ESTADISTICA | H14 | 20 | OK XML + Excel |
| `104.percent3` | G.- CONDICION ESTADISTICA | I14 | 0 | OK XML + Excel |
| `104.percent4` | G.- CONDICION ESTADISTICA | J14 | 0 | OK XML + Excel |
| `104.percent5` | G.- CONDICION ESTADISTICA | K14 | 0 | OK XML + Excel |
| `104.defects` | sec F | F22 | Observación 104 · Ubicación 0 | OK XML + Excel |
| `110.quantity` | G.- CONDICION ESTADISTICA | C15 | 11.0 | OK XML + Excel |
| `110.code` | G.- CONDICION ESTADISTICA | A15 | 110 | OK XML + Excel |
| `110.percent0` | G.- CONDICION ESTADISTICA | F15 | 0 | OK XML + Excel |
| `110.percent1` | G.- CONDICION ESTADISTICA | G15 | 80 | OK XML + Excel |
| `110.percent2` | G.- CONDICION ESTADISTICA | H15 | 20 | OK XML + Excel |
| `110.percent3` | G.- CONDICION ESTADISTICA | I15 | 0 | OK XML + Excel |
| `110.percent4` | G.- CONDICION ESTADISTICA | J15 | 0 | OK XML + Excel |
| `110.percent5` | G.- CONDICION ESTADISTICA | K15 | 0 | OK XML + Excel |
| `110.defects` | sec F | F23 | Observación 110 · Ubicación 1<br>Segunda falla 110 · Centro | OK XML + Excel |
| `111.quantity` | G.- CONDICION ESTADISTICA | C16 | 12.0 | OK XML + Excel |
| `111.code` | G.- CONDICION ESTADISTICA | A16 | 111 | OK XML + Excel |
| `111.percent0` | G.- CONDICION ESTADISTICA | F16 | 0 | OK XML + Excel |
| `111.percent1` | G.- CONDICION ESTADISTICA | G16 | 80 | OK XML + Excel |
| `111.percent2` | G.- CONDICION ESTADISTICA | H16 | 20 | OK XML + Excel |
| `111.percent3` | G.- CONDICION ESTADISTICA | I16 | 0 | OK XML + Excel |
| `111.percent4` | G.- CONDICION ESTADISTICA | J16 | 0 | OK XML + Excel |
| `111.percent5` | G.- CONDICION ESTADISTICA | K16 | 0 | OK XML + Excel |
| `111.defects` | sec F | F24 | Observación 111 · Ubicación 2 | OK XML + Excel |
| `202.quantity` | G.- CONDICION ESTADISTICA | C17 | 13.0 | OK XML + Excel |
| `202.code` | G.- CONDICION ESTADISTICA | A17 | 202 | OK XML + Excel |
| `202.percent0` | G.- CONDICION ESTADISTICA | F17 | 0 | OK XML + Excel |
| `202.percent1` | G.- CONDICION ESTADISTICA | G17 | 80 | OK XML + Excel |
| `202.percent2` | G.- CONDICION ESTADISTICA | H17 | 20 | OK XML + Excel |
| `202.percent3` | G.- CONDICION ESTADISTICA | I17 | 0 | OK XML + Excel |
| `202.percent4` | G.- CONDICION ESTADISTICA | J17 | 0 | OK XML + Excel |
| `202.percent5` | G.- CONDICION ESTADISTICA | K17 | 0 | OK XML + Excel |
| `202.defects` | sec F | F26 | Observación 202 · Ubicación 3 | OK XML + Excel |
| `205.quantity` | G.- CONDICION ESTADISTICA | C18 | 14.0 | OK XML + Excel |
| `205.code` | G.- CONDICION ESTADISTICA | A18 | 205 | OK XML + Excel |
| `205.percent0` | G.- CONDICION ESTADISTICA | F18 | 0 | OK XML + Excel |
| `205.percent1` | G.- CONDICION ESTADISTICA | G18 | 80 | OK XML + Excel |
| `205.percent2` | G.- CONDICION ESTADISTICA | H18 | 20 | OK XML + Excel |
| `205.percent3` | G.- CONDICION ESTADISTICA | I18 | 0 | OK XML + Excel |
| `205.percent4` | G.- CONDICION ESTADISTICA | J18 | 0 | OK XML + Excel |
| `205.percent5` | G.- CONDICION ESTADISTICA | K18 | 0 | OK XML + Excel |
| `205.defects` | sec F | F27 | Observación 205 · Ubicación 4 | OK XML + Excel |
| `302.quantity` | G.- CONDICION ESTADISTICA | C19 | 15.0 | OK XML + Excel |
| `302.code` | G.- CONDICION ESTADISTICA | A19 | 302 | OK XML + Excel |
| `302.percent0` | G.- CONDICION ESTADISTICA | F19 | 0 | OK XML + Excel |
| `302.percent1` | G.- CONDICION ESTADISTICA | G19 | 80 | OK XML + Excel |
| `302.percent2` | G.- CONDICION ESTADISTICA | H19 | 20 | OK XML + Excel |
| `302.percent3` | G.- CONDICION ESTADISTICA | I19 | 0 | OK XML + Excel |
| `302.percent4` | G.- CONDICION ESTADISTICA | J19 | 0 | OK XML + Excel |
| `302.percent5` | G.- CONDICION ESTADISTICA | K19 | 0 | OK XML + Excel |
| `302.defects` | sec F | F29 | Observación 302 · Ubicación 5 | OK XML + Excel |
| `311.quantity` | G.- CONDICION ESTADISTICA | C20 | 16.0 | OK XML + Excel |
| `311.code` | G.- CONDICION ESTADISTICA | A20 | 311 | OK XML + Excel |
| `311.percent0` | G.- CONDICION ESTADISTICA | F20 | 0 | OK XML + Excel |
| `311.percent1` | G.- CONDICION ESTADISTICA | G20 | 80 | OK XML + Excel |
| `311.percent2` | G.- CONDICION ESTADISTICA | H20 | 20 | OK XML + Excel |
| `311.percent3` | G.- CONDICION ESTADISTICA | I20 | 0 | OK XML + Excel |
| `311.percent4` | G.- CONDICION ESTADISTICA | J20 | 0 | OK XML + Excel |
| `311.percent5` | G.- CONDICION ESTADISTICA | K20 | 0 | OK XML + Excel |
| `311.defects` | sec F | F30 | Observación 311 · Ubicación 6 | OK XML + Excel |
| `353.quantity` | G.- CONDICION ESTADISTICA | C21 | 17.0 | OK XML + Excel |
| `353.code` | G.- CONDICION ESTADISTICA | A21 | 353 | OK XML + Excel |
| `353.percent0` | G.- CONDICION ESTADISTICA | F21 | 0 | OK XML + Excel |
| `353.percent1` | G.- CONDICION ESTADISTICA | G21 | 80 | OK XML + Excel |
| `353.percent2` | G.- CONDICION ESTADISTICA | H21 | 20 | OK XML + Excel |
| `353.percent3` | G.- CONDICION ESTADISTICA | I21 | 0 | OK XML + Excel |
| `353.percent4` | G.- CONDICION ESTADISTICA | J21 | 0 | OK XML + Excel |
| `353.percent5` | G.- CONDICION ESTADISTICA | K21 | 0 | OK XML + Excel |
| `353.defects` | sec F | F31 | Observación 353 · Ubicación 7 | OK XML + Excel |
| `372.quantity` | G.- CONDICION ESTADISTICA | C22 | 18.0 | OK XML + Excel |
| `372.code` | G.- CONDICION ESTADISTICA | A22 | 372 | OK XML + Excel |
| `372.percent0` | G.- CONDICION ESTADISTICA | F22 | 0 | OK XML + Excel |
| `372.percent1` | G.- CONDICION ESTADISTICA | G22 | 80 | OK XML + Excel |
| `372.percent2` | G.- CONDICION ESTADISTICA | H22 | 20 | OK XML + Excel |
| `372.percent3` | G.- CONDICION ESTADISTICA | I22 | 0 | OK XML + Excel |
| `372.percent4` | G.- CONDICION ESTADISTICA | J22 | 0 | OK XML + Excel |
| `372.percent5` | G.- CONDICION ESTADISTICA | K22 | 0 | OK XML + Excel |
| `372.defects` | sec F | F32 | Observación 372 · Ubicación 8 | OK XML + Excel |
| `401.quantity` | G.- CONDICION ESTADISTICA | C23 | 19.0 | OK XML + Excel |
| `401.code` | G.- CONDICION ESTADISTICA | A23 | 401 | OK XML + Excel |
| `401.percent0` | G.- CONDICION ESTADISTICA | F23 | 0 | OK XML + Excel |
| `401.percent1` | G.- CONDICION ESTADISTICA | G23 | 80 | OK XML + Excel |
| `401.percent2` | G.- CONDICION ESTADISTICA | H23 | 20 | OK XML + Excel |
| `401.percent3` | G.- CONDICION ESTADISTICA | I23 | 0 | OK XML + Excel |
| `401.percent4` | G.- CONDICION ESTADISTICA | J23 | 0 | OK XML + Excel |
| `401.percent5` | G.- CONDICION ESTADISTICA | K23 | 0 | OK XML + Excel |
| `401.defects` | sec F | F34 | Observación 401 · Ubicación 9 | OK XML + Excel |
| `402.quantity` | G.- CONDICION ESTADISTICA | C24 | 20.0 | OK XML + Excel |
| `402.code` | G.- CONDICION ESTADISTICA | A24 | 402 | OK XML + Excel |
| `402.percent0` | G.- CONDICION ESTADISTICA | F24 | 0 | OK XML + Excel |
| `402.percent1` | G.- CONDICION ESTADISTICA | G24 | 80 | OK XML + Excel |
| `402.percent2` | G.- CONDICION ESTADISTICA | H24 | 20 | OK XML + Excel |
| `402.percent3` | G.- CONDICION ESTADISTICA | I24 | 0 | OK XML + Excel |
| `402.percent4` | G.- CONDICION ESTADISTICA | J24 | 0 | OK XML + Excel |
| `402.percent5` | G.- CONDICION ESTADISTICA | K24 | 0 | OK XML + Excel |
| `402.defects` | sec F | F35 | Observación 402 · Ubicación 10 | OK XML + Excel |
| `501.quantity` | G.- CONDICION ESTADISTICA | C25 | 21.0 | OK XML + Excel |
| `501.code` | G.- CONDICION ESTADISTICA | A25 | 501 | OK XML + Excel |
| `501.percent0` | G.- CONDICION ESTADISTICA | F25 | 0 | OK XML + Excel |
| `501.percent1` | G.- CONDICION ESTADISTICA | G25 | 80 | OK XML + Excel |
| `501.percent2` | G.- CONDICION ESTADISTICA | H25 | 20 | OK XML + Excel |
| `501.percent3` | G.- CONDICION ESTADISTICA | I25 | 0 | OK XML + Excel |
| `501.percent4` | G.- CONDICION ESTADISTICA | J25 | 0 | OK XML + Excel |
| `501.percent5` | G.- CONDICION ESTADISTICA | K25 | 0 | OK XML + Excel |
| `501.defects` | sec F | F37 | Observación 501 · Ubicación 11 | OK XML + Excel |
| `511.quantity` | G.- CONDICION ESTADISTICA | C26 | 22.0 | OK XML + Excel |
| `511.code` | G.- CONDICION ESTADISTICA | A26 | 511 | OK XML + Excel |
| `511.percent0` | G.- CONDICION ESTADISTICA | F26 | 0 | OK XML + Excel |
| `511.percent1` | G.- CONDICION ESTADISTICA | G26 | 80 | OK XML + Excel |
| `511.percent2` | G.- CONDICION ESTADISTICA | H26 | 20 | OK XML + Excel |
| `511.percent3` | G.- CONDICION ESTADISTICA | I26 | 0 | OK XML + Excel |
| `511.percent4` | G.- CONDICION ESTADISTICA | J26 | 0 | OK XML + Excel |
| `511.percent5` | G.- CONDICION ESTADISTICA | K26 | 0 | OK XML + Excel |
| `511.defects` | sec F | F38 | Observación 511 · Ubicación 12 | OK XML + Excel |
| `530.quantity` | G.- CONDICION ESTADISTICA | C27 | 23.0 | OK XML + Excel |
| `530.code` | G.- CONDICION ESTADISTICA | A27 | 530 | OK XML + Excel |
| `530.percent0` | G.- CONDICION ESTADISTICA | F27 | 0 | OK XML + Excel |
| `530.percent1` | G.- CONDICION ESTADISTICA | G27 | 80 | OK XML + Excel |
| `530.percent2` | G.- CONDICION ESTADISTICA | H27 | 20 | OK XML + Excel |
| `530.percent3` | G.- CONDICION ESTADISTICA | I27 | 0 | OK XML + Excel |
| `530.percent4` | G.- CONDICION ESTADISTICA | J27 | 0 | OK XML + Excel |
| `530.percent5` | G.- CONDICION ESTADISTICA | K27 | 0 | OK XML + Excel |
| `530.defects` | sec F | F39 | Observación 530 · Ubicación 13 | OK XML + Excel |
| `span1.lengthM` | A-D.- FICHA | J62 | 10.0 | OK XML + Excel |
| `span2.lengthM` | A-D.- FICHA | J64 | 11.0 | OK XML + Excel |
| `span3.lengthM` | A-D.- FICHA | J66 | 12.0 | OK XML + Excel |
| `span4.lengthM` | A-D.- FICHA | N686 | 13.0 | OK XML + Excel |
| `general.description` | sec F | F45 | Observación general sintética · Acceso | OK XML + Excel |
| `photo0.category/description/element/defects` | F.2.-PANEL FOTOGRAFICO | D13 | Vista lado derecho · Foto sintética 1 · 104<br>Observación 104 · Ubicación 0<br>Observación 401 · Ubicación 9 | OK XML + Excel |
| `photo0.index` | F.2.-PANEL FOTOGRAFICO | B13 | 1 | OK XML + Excel |
| `photo1.category/description/element/defects` | F.2.-PANEL FOTOGRAFICO | D14 | Vista lado izquierdo · Foto sintética 2 · 104<br>Observación 110 · Ubicación 1<br>Observación 402 · Ubicación 10 | OK XML + Excel |
| `photo1.index` | F.2.-PANEL FOTOGRAFICO | B14 | 2 | OK XML + Excel |
| `photo2.category/description/element/defects` | F.2.-PANEL FOTOGRAFICO | D15 | Aguas arriba · Foto sintética 3 · 104<br>Observación 111 · Ubicación 2<br>Observación 501 · Ubicación 11 | OK XML + Excel |
| `photo2.index` | F.2.-PANEL FOTOGRAFICO | B15 | 3 | OK XML + Excel |
| `photo3.category/description/element/defects` | F.2.-PANEL FOTOGRAFICO | D16 | Aguas abajo · Foto sintética 4 · 104<br>Observación 202 · Ubicación 3<br>Observación 511 · Ubicación 12 | OK XML + Excel |
| `photo3.index` | F.2.-PANEL FOTOGRAFICO | B16 | 4 | OK XML + Excel |
| `photo4.category/description/element/defects` | F.2.-PANEL FOTOGRAFICO | D17 | Carretera / tablero · Foto sintética 5 · 104<br>Observación 205 · Ubicación 4<br>Observación 530 · Ubicación 13 | OK XML + Excel |
| `photo4.index` | F.2.-PANEL FOTOGRAFICO | B17 | 5 | OK XML + Excel |
| `photo5.category/description/element/defects` | F.2.-PANEL FOTOGRAFICO | D18 | Señal informativa · Foto sintética 6 · 104<br>Observación 302 · Ubicación 5 | OK XML + Excel |
| `photo5.index` | F.2.-PANEL FOTOGRAFICO | B18 | 6 | OK XML + Excel |
| `photo6.category/description/element/defects` | F.2.-PANEL FOTOGRAFICO | D19 | Barandas / veredas / tuberías · Foto sintética 7 · 104<br>Observación 311 · Ubicación 6 | OK XML + Excel |
| `photo6.index` | F.2.-PANEL FOTOGRAFICO | B19 | 7 | OK XML + Excel |
| `photo7.category/description/element/defects` | F.2.-PANEL FOTOGRAFICO | D20 | Anomalías o fallas · Foto sintética 8 · 104<br>Observación 353 · Ubicación 7 | OK XML + Excel |
| `photo7.index` | F.2.-PANEL FOTOGRAFICO | B20 | 8 | OK XML + Excel |
| `photo8.category/description/element/defects` | F.2.-PANEL FOTOGRAFICO | D21 | Otros elementos del puente · Foto sintética 9 · 104<br>Observación 372 · Ubicación 8 | OK XML + Excel |
| `photo8.index` | F.2.-PANEL FOTOGRAFICO | B21 | 9 | OK XML + Excel |
