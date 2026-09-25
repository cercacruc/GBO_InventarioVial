# Archivos de la estabilización

Comparación de fuentes con el commit `393567166f68837e85415465683c5ef981073876`. A = añadido; M = modificado. No incluye cachés ni configuración local. La comparación abarca SCAP, estabilidad y decisiones confirmadas de ingeniería; la estructura del módulo está en SCAP_IMPLEMENTATION.md.

| Estado | Archivo |
|---|---|
| M | `.gitignore` |
| M | `app/build.gradle.kts` |
| A | `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/6.json` |
| M | `app/src/androidTest/java/com/tuempresa/inventariovial/InventoryMigrationTest.kt` |
| A | `app/src/main/assets/scap_elements.json` |
| A | `app/src/main/assets/scap_fields.json` |
| A | `app/src/main/assets/scap_options.json` |
| M | `app/src/main/java/com/tuempresa/inventariovial/catalog/SicCatalogRepository.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordWithPhotos.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/data/entity/PhotoEntity.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/DriveConfig.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/DriveStatusPanel.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/DriveSync.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/DriveUploadPolicy.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/DriveUploadWorker.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/export/SicExcelWriter.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/export/SicExportFormat.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/field/CaptureSummary.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/field/SurveyHeader.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/field/SurveyPreferences.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/model/SignalizationType.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/calculator/ScapConditionCalculator.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/catalog/ScapCatalog.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapDao.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapEntities.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapMigration.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapAiAnalyzer.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapToSicMapper.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapValidation.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapDrivePayload.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapExcelExporter.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapSicExporter.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapTemplateMedia.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/export/TemplateWorkbook.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapController.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapElements.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapExportPanel.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapForms.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapMedia.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapWorkspace.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/validation/CaptureValidation.kt` |
| A | `app/src/main/java/com/tuempresa/inventariovial/validation/EndLocationPolicy.kt` |
| M | `app/src/main/java/com/tuempresa/inventariovial/viewmodel/InventoryViewModel.kt` |
| A | `app/src/test/java/com/tuempresa/inventariovial/EngineeringDecisionsTest.kt` |
| A | `app/src/test/java/com/tuempresa/inventariovial/FieldReadinessTest.kt` |
| M | `app/src/test/java/com/tuempresa/inventariovial/InventoryRoomMigrationTest.kt` |
| A | `app/src/test/java/com/tuempresa/inventariovial/ScapCalculationTest.kt` |
| A | `app/src/test/java/com/tuempresa/inventariovial/ScapExportTest.kt` |
| A | `app/src/test/java/com/tuempresa/inventariovial/ScapStorageTest.kt` |
| M | `app/src/test/java/com/tuempresa/inventariovial/SurveyCorrectionsTest.kt` |
| M | `app/src/test/java/com/tuempresa/inventariovial/SurveyStorageTest.kt` |
| A | `app/src/test/resources/scap_agua_blanca.json` |
| A | `docs/DRIVE_SCAP_CONTRACT.md` |
| A | `docs/ENGINEERING_CHANGED_FILES.md` |
| A | `docs/ENGINEERING_RELEASE.md` |
| A | `docs/FIELD_READINESS.md` |
| A | `docs/reference/SCAP_EXTRACTION.json` |
| A | `docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx` |
| A | `docs/SCAP_ARCHITECTURE_REVIEW.md` |
| A | `docs/SCAP_CALCULATION.md` |
| A | `docs/SCAP_EXCEL_VALIDATION.json` |
| A | `docs/SCAP_IMPLEMENTATION.md` |
| A | `docs/SCAP_PENDING_CLIENT_DECISIONS.md` |
| A | `docs/SCAP_TO_SIC_MAPPING.md` |
| A | `drive.local.properties.example` |
| M | `server.local.properties.example` |
| A | `tools/extract_scap_catalog.py` |
| A | `tools/package_field_delivery.py` |
| A | `tools/report_engineering_changes.py` |
| A | `tools/scap_field_map.json` |
| A | `tools/verify_excel_export.ps1` |
| A / generado | `docs/SCAP_VERIFICATION.json` |
| A / generado | `docs/FIELD_CHANGED_FILES.md` |

Artefactos fuera del código: output/GBO_InventarioVial_campo.zip, output/GBO_InventarioVial_campo-debug.apk y output/DELIVERY.json. Los archivos generados que ya estuvieran versionados no se retiran del índice.
