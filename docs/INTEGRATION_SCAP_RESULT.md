# Resultado de la integración SCAP

Verificado: 2026-09-25T11:31:17.073359+00:00.

Proyecto final: `D:\Downloads\GBO_InventarioVial`. Integración aplicada sobre la base oficial, sin commit, push, reset, staging ni sustitución de `.git`. No se creó otra copia final del proyecto.

Base oficial y `origin/main` local: `cdbe5e79df440fdbdfec071f7c0e1c214b9d1639`. Base común del ZIP: `393567166f68837e85415465683c5ef981073876`. Se compararon los 203 archivos del ZIP antes de editar: 53 nuevos, 26 con cambios solo en el ZIP y 124 iguales. No hubo archivos modificados por ambos lados ni conflictos reales. La comparación previa está en [INTEGRATION_SCAP_PLAN.md](INTEGRATION_SCAP_PLAN.md) y los hashes en [INTEGRATION_SCAP_PLAN.json](INTEGRATION_SCAP_PLAN.json).

## A. Archivos añadidos

53 archivos de la versión de campo y 4 documentos de auditoría de esta integración: 57 archivos nuevos sin seguimiento.

- `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/6.json`
- `app/src/main/assets/scap_elements.json`
- `app/src/main/assets/scap_fields.json`
- `app/src/main/assets/scap_options.json`
- `app/src/main/java/com/tuempresa/inventariovial/DriveStatusPanel.kt`
- `app/src/main/java/com/tuempresa/inventariovial/DriveUploadPolicy.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/calculator/ScapConditionCalculator.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/catalog/ScapCatalog.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapEntities.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapMigration.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapAiAnalyzer.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapToSicMapper.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapValidation.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapDrivePayload.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapExcelExporter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapSicExporter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapTemplateMedia.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/TemplateWorkbook.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapController.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapElements.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapExportPanel.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapForms.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapMedia.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapWorkspace.kt`
- `app/src/main/java/com/tuempresa/inventariovial/validation/EndLocationPolicy.kt`
- `app/src/test/java/com/tuempresa/inventariovial/EngineeringDecisionsTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/FieldReadinessTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapCalculationTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapExportTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapStorageTest.kt`
- `app/src/test/resources/scap_agua_blanca.json`
- `docs/DRIVE_SCAP_CONTRACT.md`
- `docs/ENGINEERING_CHANGED_FILES.md`
- `docs/ENGINEERING_RELEASE.md`
- `docs/FIELD_CHANGED_FILES.md`
- `docs/FIELD_READINESS.md`
- `docs/SCAP_ARCHITECTURE_REVIEW.md`
- `docs/SCAP_CALCULATION.md`
- `docs/SCAP_EXCEL_VALIDATION.json`
- `docs/SCAP_IMPLEMENTATION.md`
- `docs/SCAP_PENDING_CLIENT_DECISIONS.md`
- `docs/SCAP_TO_SIC_MAPPING.md`
- `docs/SCAP_VERIFICATION.json`
- `docs/reference/SCAP_EXTRACTION.json`
- `docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx`
- `drive.local.properties.example`
- `tools/extract_scap_catalog.py`
- `tools/package_field_delivery.py`
- `tools/report_engineering_changes.py`
- `tools/scap_field_map.json`
- `tools/verify_excel_export.ps1`

Documentos de auditoría:

- `docs/INTEGRATION_GIT_STATUS.txt`
- `docs/INTEGRATION_SCAP_PLAN.json`
- `docs/INTEGRATION_SCAP_PLAN.md`
- `docs/INTEGRATION_SCAP_RESULT.md`

## B. Archivos modificados

26 archivos existentes de la base oficial:

- `.gitignore`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/tuempresa/inventariovial/InventoryMigrationTest.kt`
- `app/src/main/java/com/tuempresa/inventariovial/DriveConfig.kt`
- `app/src/main/java/com/tuempresa/inventariovial/DriveSync.kt`
- `app/src/main/java/com/tuempresa/inventariovial/DriveUploadWorker.kt`
- `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/catalog/SicCatalogRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordWithPhotos.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/PhotoEntity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/SicExcelWriter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/SicExportFormat.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/CaptureSummary.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/SurveyHeader.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/SurveyPreferences.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/SignalizationType.kt`
- `app/src/main/java/com/tuempresa/inventariovial/validation/CaptureValidation.kt`
- `app/src/main/java/com/tuempresa/inventariovial/viewmodel/InventoryViewModel.kt`
- `app/src/test/java/com/tuempresa/inventariovial/InventoryRoomMigrationTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SurveyCorrectionsTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SurveyStorageTest.kt`
- `server.local.properties.example`

La implementación incluye el módulo SCAP, exportación Excel, SIC-17/17A/17B, correcciones SIC-18 y SIC-21, Hito como Informativa en SIC-22, seis rutas iniciales, DriveStatusPanel/DriveUploadPolicy, EndLocationPolicy, migración Room, pruebas, documentación y herramientas.

## C. Archivos de GitHub conservados

Los 162 archivos versionados restantes de la base oficial conservan exactamente su SHA-256 previo. Los 124 archivos idénticos del ZIP no se reescribieron. En particular:

| Archivo remoto | SHA-256 conservado |
|---|---|
| `google-apps-script/Code.gs` | `8a7966e93424084929f632fbb97e09436336473d964962c63c2c72d633ff6792` |
| `google-apps-script/Code.test.cjs` | `29998351a2a4a529f9086d3695acc9f453c367aece3c2236b4607c77c47979d4` |
| `google-apps-script/README.md` | `8085be448ae4680a21e65fc9ac5229d8161a1ce75bef500fac023e9d1362cc4a` |

Se conservaron el bloqueo del servidor, la idempotencia y la organización por ruta/SIB de Google Apps Script, además del contrato SIC y los nombres de fotos. Las cachés que GitHub había retirado no se copiaron desde el ZIP. Los archivos temporales o informes de build que ya estaban versionados en la base se dejaron intactos.

Lista completa de archivos versionados conservados:

- `.idea/.gitignore`
- `.idea/.name`
- `.idea/AndroidProjectSystem.xml`
- `.idea/GBO_InventarioVial.iml`
- `.idea/assetWizardSettings.xml`
- `.idea/caches/deviceStreaming.xml`
- `.idea/codeStyles/Project.xml`
- `.idea/codeStyles/codeStyleConfig.xml`
- `.idea/compiler.xml`
- `.idea/deploymentTargetSelector.xml`
- `.idea/deviceManager.xml`
- `.idea/gradle.xml`
- `.idea/inspectionProfiles/Project_Default.xml`
- `.idea/misc.xml`
- `.idea/runConfigurations.xml`
- `.idea/vcs.xml`
- `EXPORTACION_SIC.md`
- `INTEGRACION_SEPTIEMBRE_2026.md`
- `INTEGRACION_ZIP.md`
- `SIC23.md`
- `app/.gitignore`
- `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/4.json`
- `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/5.json`
- `app/src/androidTest/assets/inventory-v1.sql`
- `app/src/androidTest/java/com/tuempresa/inventariovial/ExampleInstrumentedTest.kt`
- `app/src/androidTest/java/com/tuempresa/inventariovial/SicExportScreenTest.kt`
- `app/src/androidTest/java/com/tuempresa/inventariovial/SurveyFormsScreenTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/allowed_devices.json`
- `app/src/main/assets/road_reference.json`
- `app/src/main/ic_launcher-playstore.png`
- `app/src/main/java/com/tuempresa/inventariovial/DriveFolderRouter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/Sic23Fields.kt`
- `app/src/main/java/com/tuempresa/inventariovial/SicExportScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/access/DeviceAccessManager.kt`
- `app/src/main/java/com/tuempresa/inventariovial/ai/AiAnalyzer.kt`
- `app/src/main/java/com/tuempresa/inventariovial/camera/PhotoStampService.kt`
- `app/src/main/java/com/tuempresa/inventariovial/camera/PhotoUtils.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/data/dao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryMigrations.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/FieldSession.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordEntity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordForExport.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventorySnapshot.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic17AEntity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic17BEntity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic17Entity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic18AEntity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic18Entity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic19Entity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic20Entity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic21Entity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic22Entity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic23Entity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/TrackPointEntity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/LocalSicExport.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/ObservationReport.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/CaptureToolsUi.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/FieldController.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/FieldSettings.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/ObservationReportButton.kt`
- `app/src/main/java/com/tuempresa/inventariovial/gis/KmlExporter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/gis/LocalKmlExport.kt`
- `app/src/main/java/com/tuempresa/inventariovial/location/ExternalGnssProvider.kt`
- `app/src/main/java/com/tuempresa/inventariovial/location/GpsUtils.kt`
- `app/src/main/java/com/tuempresa/inventariovial/location/LocationProvider.kt`
- `app/src/main/java/com/tuempresa/inventariovial/location/TabletLocationProvider.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/RoadAssetType.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/CommonInventoryFormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/InventoryFormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/InventorySaveRequest.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/Sic17FormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/Sic18FormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/Sic19FormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/Sic20FormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/Sic21FormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/Sic22FormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/Sic23FormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/repository/InventoryRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/road/RoadReference.kt`
- `app/src/main/java/com/tuempresa/inventariovial/road/RoadReferenceRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/server/ServerApi.kt`
- `app/src/main/java/com/tuempresa/inventariovial/server/ServerPayload.kt`
- `app/src/main/java/com/tuempresa/inventariovial/server/ServerSyncWorker.kt`
- `app/src/main/java/com/tuempresa/inventariovial/supplementary/SupplementaryForms.kt`
- `app/src/main/java/com/tuempresa/inventariovial/tracking/TrackCalculator.kt`
- `app/src/main/java/com/tuempresa/inventariovial/tracking/TrackCaptureService.kt`
- `app/src/main/java/com/tuempresa/inventariovial/ui/theme/Color.kt`
- `app/src/main/java/com/tuempresa/inventariovial/ui/theme/Theme.kt`
- `app/src/main/java/com/tuempresa/inventariovial/ui/theme/Type.kt`
- `app/src/main/keepRules/rules.keep`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-hdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-hdpi/ic_launcher_foreground.webp`
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher_foreground.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher_foreground.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/ic_launcher_background.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/test/java/com/tuempresa/inventariovial/ExampleUnitTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/FieldTestFixtures.kt`
- `app/src/test/java/com/tuempresa/inventariovial/PhotoStampServiceTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/RoadMatcherTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/RoadPositionTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/Sic23Test.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SicExcelWriterTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/TrackAndKmlTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ValidationAndServerTest.kt`
- `build.gradle.kts`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/1usomb1g28gmlmmfzeie3ha44/configuration-cache-report.html`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/2yuhfxrhh4df33e8ohd9zlkhw/configuration-cache-report.html`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/3ayj8hjeah9rg2enohe3r82gp/configuration-cache-report.html`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/5h3l7vnto4h8y025pdye3jd80/configuration-cache-report.html`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/63sujdg1msbuwys5zds8au6fk/configuration-cache-report.html`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/8fazzm9as9qk2zuqz3z69xrj9/configuration-cache-report.html`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/b08vxr80agwdiulwlswchivwc/configuration-cache-report.html`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/bl994vw4js8rrnm4gunyfbgiu/configuration-cache-report.html`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/deji0o02ureb67knorvyeabuy/configuration-cache-report.html`
- `build/reports/configuration-cache/d79bs6ovxg0vv148usj6qx2bn/ejp72flu2tr8yeqj6wkhsm4nb/configuration-cache-report.html`
- `docs/ARCHITECTURE_REVIEW.md`
- `docs/PRESERVACION_BASE.json`
- `docs/ROAD_REFERENCE_FORMAT.md`
- `docs/SERVER_API_CONTRACT.md`
- `docs/VALIDACION_ENTREGA.md`
- `google-apps-script/Code.gs`
- `google-apps-script/Code.test.cjs`
- `google-apps-script/README.md`
- `gradle.properties`
- `gradle/gradle-daemon-jvm.properties`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`
- `gradlew.bat`
- `local.properties`
- `settings.gradle.kts`
- `tmp/export-review/SIC-17.png`
- `tmp/export-review/SIC-18.png`
- `tmp/export-review/SIC-19.png`
- `tmp/export-review/SIC-20.png`
- `tmp/export-review/SIC-21.png`
- `tmp/export-review/SIC-22.png`
- `tmp/export-review/SIC-23.png`
- `tmp/export-review/review.mjs`

## D. Conflictos y decisiones de integración

- Conflictos reales: **0**. La comparación contra la base común demostró que los 26 archivos divergentes solo habían cambiado en el ZIP. Se revisaron y aplicaron sus diferencias; no se sustituyó ciegamente el proyecto.
- `.gitignore`: unión de las reglas de la base con las del ZIP. Se añadió la exclusión específica del ZIP de entrada para evitar incluirlo accidentalmente en el commit.
- `google-apps-script/`: se conservaron íntegros los tres archivos nuevos de GitHub. El uploader SIC mantiene su contrato; las inspecciones SCAP siguen excluidas de ese endpoint, tal como estaba implementado en la versión de campo.
- `tools/package_field_delivery.py`: se incorporó `google-apps-script/` a los archivos de futuras entregas para no perder el servidor remoto. La herramienta no se ejecutó durante esta integración; no se generó otro ZIP de entrega.
- `tools/report_engineering_changes.py`: se añadió una línea base alternativa desde el plan de integración porque el ZIP no debe incluir `tmp/engineering/baseline.json`. Se conservó el modo anterior cuando ese archivo existe, incluyendo los filtros de temporales y configuración privada.
- Los otros archivos integrados coinciden con el ZIP en contenido; se conservaron los finales de línea de los archivos existentes cuando correspondía.
- No se copiaron cachés, builds, temporales, APK, ZIP ni propiedades privadas. Gradle generó sus propios resultados locales durante la verificación.
- Los hashes de las propiedades locales y del ZIP de entrada siguen iguales. No se ejecutaron operaciones Git de escritura. La aplicación Codex registró objetos y referencias auxiliares bajo `refs/codex`; HEAD, índice y metadatos ajenos a esos objetos/referencias coinciden con la auditoría inicial. No se borraron ni restauraron esos registros internos.

## E. Resultado de pruebas

Comando ejecutado desde el proyecto, con el SDK instalado indicado por entorno:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\gerbc\.gradle'
$env:ANDROID_HOME='D:\Android\Sdk'
$env:ANDROID_SDK_ROOT='D:\Android\Sdk'
.\gradlew.bat testDebugUnitTest assembleDebug --console=plain
```

**BUILD SUCCESSFUL** en 7 min 2 s; 51 tareas ejecutadas. `testDebugUnitTest` y `assembleDebug` completados.

- Pruebas JVM/Android unitarias: **82 aprobadas**, 0 fallos, 0 errores, 0 omitidas, en 16 suites. Se verificaron los XML generados por esta ejecución.
- Google Apps Script: **8 aprobadas**, 0 fallos, 0 omitidas, con `node --test google-apps-script/Code.test.cjs`, ejecutado durante esta integración. El código del servidor permaneció intacto después de esa ejecución.
- `git diff --check`: código de salida 0.
- Sintaxis Python de las herramientas correcta; generación del informe con la línea base alternativa verificada en memoria, sin sustituir los informes históricos.
- Se verificó que el APK y los resultados de pruebas son posteriores a los archivos de código, y que el APK contiene exactamente la plantilla canónica y el mapa SCAP.

| Suite | Pruebas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| EngineeringDecisionsTest | 7 | 0 | 0 | 0 |
| ExampleUnitTest | 1 | 0 | 0 | 0 |
| FieldReadinessTest | 5 | 0 | 0 | 0 |
| InventoryRoomMigrationTest | 3 | 0 | 0 | 0 |
| PhotoStampServiceTest | 2 | 0 | 0 | 0 |
| RoadMatcherTest | 5 | 0 | 0 | 0 |
| RoadPositionTest | 6 | 0 | 0 | 0 |
| ScapCalculationTest | 7 | 0 | 0 | 0 |
| ScapExportTest | 7 | 0 | 0 | 0 |
| ScapStorageTest | 7 | 0 | 0 | 0 |
| Sic23Test | 5 | 0 | 0 | 0 |
| SicExcelWriterTest | 5 | 0 | 0 | 0 |
| SurveyCorrectionsTest | 7 | 0 | 0 | 0 |
| SurveyStorageTest | 2 | 0 | 0 | 0 |
| TrackAndKmlTest | 8 | 0 | 0 | 0 |
| ValidationAndServerTest | 5 | 0 | 0 | 0 |

APK generado por Gradle: `app/build/outputs/apk/debug/app-debug.apk` (ignorado por Git, sin copiarlo a otra entrega).
SHA-256: `d3bfd37d56b79437a25f9bedd062efcfc400b3e369310f90f22a0772dfc9b886`.

Plantilla canónica: `docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx`.
SHA-256: `1df9f651722d06d6cd886c9be3edf565df7aae8b6fca10097011f48b12430b13`.

El primer intento se detuvo por la ruta de SDK de otra máquina en `local.properties`. La ejecución final usó `D:\Android\Sdk` mediante variables de entorno, conservando ese archivo privado. Permanecen advertencias no bloqueantes sobre esa ruta, opciones experimentales, JDK, `Divider` obsoleto y bibliotecas nativas. No hubo errores de compilación ni pruebas fallidas.

No se ejecutaron pruebas instrumentadas en dispositivo, cámara/GNSS físicos ni subida real a Drive. Las decisiones y limitaciones funcionales previas siguen documentadas en [SCAP_PENDING_CLIENT_DECISIONS.md](SCAP_PENDING_CLIENT_DECISIONS.md). Los informes históricos del ZIP se conservaron; este informe contiene la evidencia nueva de la integración.

## F. Git status final

Rama `main`, mismo HEAD y `origin/main` local. **26 modificados y 57 nuevos sin seguimiento; 0 preparados para commit y 0 eliminados**. No se hizo commit ni push. Lista íntegra: [INTEGRATION_GIT_STATUS.txt](INTEGRATION_GIT_STATUS.txt).
