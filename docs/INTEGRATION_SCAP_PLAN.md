# Comparación previa a la integración SCAP

Base oficial: cdbe5e79df440fdbdfec071f7c0e1c214b9d1639; coincide con origin/main local. Base común del ZIP: 393567166f68837e85415465683c5ef981073876. ZIP verificado: 405b9fc4a0dc4be12ffbd4e0712e93399cbef2946abda41dcf58b474d7e43d17.

Inspección completada antes de cambiar código: **53 nuevos, 26 modificados solo en ZIP, 124 iguales, 0 conflictos de cambios simultáneos**. Las diferencias de fin de línea no se consideran cambios de contenido.

Los tres archivos nuevos de google-apps-script se conservan byte por byte. Las cachés retiradas en GitHub no se restauran. No se extrae una carpeta completa: se aplican solo rutas permitidas, comprobando sus hashes antes de cada escritura. .gitignore se integra por adición de reglas, conservando las existentes. Se mantendrá el contrato SIC del servidor y la exclusión SCAP del uploader.

| Estado | Archivo | Acción |
|---|---|---|
| ZIP_ONLY_CHANGE | .gitignore | MERGE_IGNORE_RULES |
| IDENTICAL | app/.gitignore | KEEP_OUTSIDE_SCOPE |
| ZIP_ONLY_CHANGE | app/build.gradle.kts | PATCH_REVIEWED |
| IDENTICAL | app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/4.json | KEEP |
| IDENTICAL | app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/5.json | KEEP |
| ADD_ZIP | app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/6.json | ADD |
| IDENTICAL | app/src/androidTest/assets/inventory-v1.sql | KEEP |
| IDENTICAL | app/src/androidTest/java/com/tuempresa/inventariovial/ExampleInstrumentedTest.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/androidTest/java/com/tuempresa/inventariovial/InventoryMigrationTest.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/androidTest/java/com/tuempresa/inventariovial/SicExportScreenTest.kt | KEEP |
| IDENTICAL | app/src/androidTest/java/com/tuempresa/inventariovial/SurveyFormsScreenTest.kt | KEEP |
| IDENTICAL | app/src/main/AndroidManifest.xml | KEEP |
| IDENTICAL | app/src/main/assets/allowed_devices.json | KEEP |
| IDENTICAL | app/src/main/assets/road_reference.json | KEEP |
| ADD_ZIP | app/src/main/assets/scap_elements.json | ADD |
| ADD_ZIP | app/src/main/assets/scap_fields.json | ADD |
| ADD_ZIP | app/src/main/assets/scap_options.json | ADD |
| IDENTICAL | app/src/main/ic_launcher-playstore.png | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/access/DeviceAccessManager.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/ai/AiAnalyzer.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/camera/PhotoStampService.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/camera/PhotoUtils.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/catalog/SicCatalogRepository.kt | PATCH_REVIEWED |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/data/dao.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryMigrations.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/FieldSession.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordEntity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordForExport.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordWithPhotos.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/InventorySnapshot.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/data/entity/PhotoEntity.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic17AEntity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic17BEntity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic17Entity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic18AEntity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic18Entity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic19Entity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic20Entity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic21Entity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic22Entity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic23Entity.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/data/entity/TrackPointEntity.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/DriveConfig.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/DriveFolderRouter.kt | KEEP |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/DriveStatusPanel.kt | ADD |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/DriveSync.kt | PATCH_REVIEWED |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/DriveUploadPolicy.kt | ADD |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/DriveUploadWorker.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/export/LocalSicExport.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/export/ObservationReport.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/export/SicExcelWriter.kt | PATCH_REVIEWED |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/export/SicExportFormat.kt | PATCH_REVIEWED |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/field/CaptureSummary.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/field/CaptureToolsUi.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/field/FieldController.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/field/FieldSettings.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/field/ObservationReportButton.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/field/SurveyHeader.kt | PATCH_REVIEWED |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/field/SurveyPreferences.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/gis/KmlExporter.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/gis/LocalKmlExport.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/location/ExternalGnssProvider.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/location/GpsUtils.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/location/LocationProvider.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/location/TabletLocationProvider.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/CommonInventoryFormState.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/InventoryFormState.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/InventorySaveRequest.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/Sic17FormState.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/Sic18FormState.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/Sic19FormState.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/Sic20FormState.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/Sic21FormState.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/Sic22FormState.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/form/Sic23FormState.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/model/RoadAssetType.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/model/SignalizationType.kt | PATCH_REVIEWED |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/repository/InventoryRepository.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/road/RoadReference.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/road/RoadReferenceRepository.kt | KEEP |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/calculator/ScapConditionCalculator.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/catalog/ScapCatalog.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapDao.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapEntities.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapMigration.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapAiAnalyzer.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapToSicMapper.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapValidation.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapDrivePayload.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapExcelExporter.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapSicExporter.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapTemplateMedia.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/export/TemplateWorkbook.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapController.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapElements.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapExportPanel.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapForms.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapMedia.kt | ADD |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapWorkspace.kt | ADD |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/server/ServerApi.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/server/ServerPayload.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/server/ServerSyncWorker.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/Sic23Fields.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/SicExportScreen.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/supplementary/SupplementaryForms.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/tracking/TrackCalculator.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/tracking/TrackCaptureService.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/ui/theme/Color.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/ui/theme/Theme.kt | KEEP |
| IDENTICAL | app/src/main/java/com/tuempresa/inventariovial/ui/theme/Type.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/validation/CaptureValidation.kt | PATCH_REVIEWED |
| ADD_ZIP | app/src/main/java/com/tuempresa/inventariovial/validation/EndLocationPolicy.kt | ADD |
| ZIP_ONLY_CHANGE | app/src/main/java/com/tuempresa/inventariovial/viewmodel/InventoryViewModel.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/main/keepRules/rules.keep | KEEP |
| IDENTICAL | app/src/main/res/drawable/ic_launcher_background.xml | KEEP |
| IDENTICAL | app/src/main/res/drawable/ic_launcher_foreground.xml | KEEP |
| IDENTICAL | app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml | KEEP |
| IDENTICAL | app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml | KEEP |
| IDENTICAL | app/src/main/res/mipmap-hdpi/ic_launcher.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-hdpi/ic_launcher_foreground.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-hdpi/ic_launcher_round.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-mdpi/ic_launcher.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-mdpi/ic_launcher_foreground.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-mdpi/ic_launcher_round.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-xhdpi/ic_launcher.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-xhdpi/ic_launcher_foreground.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-xxhdpi/ic_launcher.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp | KEEP |
| IDENTICAL | app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp | KEEP |
| IDENTICAL | app/src/main/res/values/colors.xml | KEEP |
| IDENTICAL | app/src/main/res/values/ic_launcher_background.xml | KEEP |
| IDENTICAL | app/src/main/res/values/strings.xml | KEEP |
| IDENTICAL | app/src/main/res/values/themes.xml | KEEP |
| IDENTICAL | app/src/main/res/xml/backup_rules.xml | KEEP |
| IDENTICAL | app/src/main/res/xml/data_extraction_rules.xml | KEEP |
| IDENTICAL | app/src/main/res/xml/file_paths.xml | KEEP |
| ADD_ZIP | app/src/test/java/com/tuempresa/inventariovial/EngineeringDecisionsTest.kt | ADD |
| IDENTICAL | app/src/test/java/com/tuempresa/inventariovial/ExampleUnitTest.kt | KEEP |
| ADD_ZIP | app/src/test/java/com/tuempresa/inventariovial/FieldReadinessTest.kt | ADD |
| IDENTICAL | app/src/test/java/com/tuempresa/inventariovial/FieldTestFixtures.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/test/java/com/tuempresa/inventariovial/InventoryRoomMigrationTest.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/test/java/com/tuempresa/inventariovial/PhotoStampServiceTest.kt | KEEP |
| IDENTICAL | app/src/test/java/com/tuempresa/inventariovial/RoadMatcherTest.kt | KEEP |
| IDENTICAL | app/src/test/java/com/tuempresa/inventariovial/RoadPositionTest.kt | KEEP |
| ADD_ZIP | app/src/test/java/com/tuempresa/inventariovial/ScapCalculationTest.kt | ADD |
| ADD_ZIP | app/src/test/java/com/tuempresa/inventariovial/ScapExportTest.kt | ADD |
| ADD_ZIP | app/src/test/java/com/tuempresa/inventariovial/ScapStorageTest.kt | ADD |
| IDENTICAL | app/src/test/java/com/tuempresa/inventariovial/Sic23Test.kt | KEEP |
| IDENTICAL | app/src/test/java/com/tuempresa/inventariovial/SicExcelWriterTest.kt | KEEP |
| ZIP_ONLY_CHANGE | app/src/test/java/com/tuempresa/inventariovial/SurveyCorrectionsTest.kt | PATCH_REVIEWED |
| ZIP_ONLY_CHANGE | app/src/test/java/com/tuempresa/inventariovial/SurveyStorageTest.kt | PATCH_REVIEWED |
| IDENTICAL | app/src/test/java/com/tuempresa/inventariovial/TrackAndKmlTest.kt | KEEP |
| IDENTICAL | app/src/test/java/com/tuempresa/inventariovial/ValidationAndServerTest.kt | KEEP |
| ADD_ZIP | app/src/test/resources/scap_agua_blanca.json | ADD |
| IDENTICAL | build.gradle.kts | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | docs/ARCHITECTURE_REVIEW.md | KEEP |
| ADD_ZIP | docs/DRIVE_SCAP_CONTRACT.md | ADD |
| ADD_ZIP | docs/ENGINEERING_CHANGED_FILES.md | ADD |
| ADD_ZIP | docs/ENGINEERING_RELEASE.md | ADD |
| ADD_ZIP | docs/FIELD_CHANGED_FILES.md | ADD |
| ADD_ZIP | docs/FIELD_READINESS.md | ADD |
| IDENTICAL | docs/PRESERVACION_BASE.json | KEEP |
| ADD_ZIP | docs/reference/SCAP_EXTRACTION.json | ADD |
| ADD_ZIP | docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx | ADD |
| IDENTICAL | docs/ROAD_REFERENCE_FORMAT.md | KEEP |
| ADD_ZIP | docs/SCAP_ARCHITECTURE_REVIEW.md | ADD |
| ADD_ZIP | docs/SCAP_CALCULATION.md | ADD |
| ADD_ZIP | docs/SCAP_EXCEL_VALIDATION.json | ADD |
| ADD_ZIP | docs/SCAP_IMPLEMENTATION.md | ADD |
| ADD_ZIP | docs/SCAP_PENDING_CLIENT_DECISIONS.md | ADD |
| ADD_ZIP | docs/SCAP_TO_SIC_MAPPING.md | ADD |
| ADD_ZIP | docs/SCAP_VERIFICATION.json | ADD |
| IDENTICAL | docs/SERVER_API_CONTRACT.md | KEEP |
| IDENTICAL | docs/VALIDACION_ENTREGA.md | KEEP |
| ADD_ZIP | drive.local.properties.example | ADD |
| IDENTICAL | EXPORTACION_SIC.md | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | gradle/gradle-daemon-jvm.properties | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | gradle/libs.versions.toml | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | gradle/wrapper/gradle-wrapper.jar | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | gradle/wrapper/gradle-wrapper.properties | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | gradle.properties | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | gradlew | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | gradlew.bat | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | INTEGRACION_SEPTIEMBRE_2026.md | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | INTEGRACION_ZIP.md | KEEP_OUTSIDE_SCOPE |
| ZIP_ONLY_CHANGE | server.local.properties.example | PATCH_REVIEWED |
| IDENTICAL | settings.gradle.kts | KEEP_OUTSIDE_SCOPE |
| IDENTICAL | SIC23.md | KEEP_OUTSIDE_SCOPE |
| ADD_ZIP | tools/extract_scap_catalog.py | ADD |
| ADD_ZIP | tools/package_field_delivery.py | ADD |
| ADD_ZIP | tools/report_engineering_changes.py | ADD |
| ADD_ZIP | tools/scap_field_map.json | ADD |
| ADD_ZIP | tools/verify_excel_export.ps1 | ADD |

## Cambios de GitHub preservados

- D	.gradle/9.6.0/checksums/checksums.lock
- D	.gradle/9.6.0/checksums/md5-checksums.bin
- D	.gradle/9.6.0/checksums/sha1-checksums.bin
- D	.gradle/9.6.0/executionHistory/executionHistory.bin
- D	.gradle/9.6.0/executionHistory/executionHistory.lock
- D	.gradle/9.6.0/fileChanges/last-build.bin
- D	.gradle/9.6.0/fileHashes/fileHashes.bin
- D	.gradle/9.6.0/fileHashes/fileHashes.lock
- D	.gradle/9.6.0/fileHashes/resourceHashesCache.bin
- D	.gradle/9.6.0/gc.properties
- D	.gradle/9.6.0/kotlin-dsl-plugin-entries/jar-entries.bin
- D	.gradle/9.6.0/kotlin-dsl-plugin-entries/kotlin-dsl-plugin-entries.lock
- D	.gradle/buildOutputCleanup/buildOutputCleanup.lock
- D	.gradle/buildOutputCleanup/cache.properties
- D	.gradle/buildOutputCleanup/outputFiles.bin
- D	.gradle/configuration-cache/5bdfad8c-16be-4f84-bce9-665b2ec1f8a8/classloaderscopes5291575381278088961.tmp
- D	.gradle/configuration-cache/909813cb-c21f-4a9b-9696-f4f19f7e6391/classloaderscopes3604632775440361874.tmp
- D	.gradle/configuration-cache/aeaca469-a6df-4125-b86c-e95a2d416ac5/classloaderscopes11743832555248986221.tmp
- D	.gradle/configuration-cache/configuration-cache.lock
- D	.gradle/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/candidates.bin
- D	.gradle/configuration-cache/gc.properties
- D	.gradle/file-system.probe
- D	.gradle/vcs-1/gc.properties
- A	google-apps-script/Code.gs
- A	google-apps-script/Code.test.cjs
- A	google-apps-script/README.md

No se ha ejecutado fetch, checkout, merge de Git, add, commit, push ni reset. Las comprobaciones Git usan GIT_OPTIONAL_LOCKS=0.
