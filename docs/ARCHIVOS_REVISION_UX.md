# Archivos de la revisión funcional y UX

Listado de archivos modificados o añadidos en esta entrega. `local.properties` ya tenía la corrección local del SDK anterior a esta solicitud y no forma parte de los cambios funcionales. `drive.local.properties` no se publica.

## Aplicación

- `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/catalog/EngineeringConditions.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/database/EngineeringMigration.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordWithPhotos.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/PhotoEntity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic19Entity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/Sic20Entity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/Sic18AExporter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/CaptureSummary.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/ConditionSelector.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/CulvertPhotos.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/EngineeringEditScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/Sic18AExportButton.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/InventorySaveRequest.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/Sic19FormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/model/form/Sic20FormState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/repository/EngineeringEdits.kt`
- `app/src/main/java/com/tuempresa/inventariovial/repository/InventoryRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/catalog/ScapCatalog.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapJointEntity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapFieldPolicy.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapToSicMapper.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapValidation.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapExcelExporter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapTemplateMedia.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapEngineeringPanels.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapForms.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapMedia.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapWorkspace.kt`
- `app/src/main/java/com/tuempresa/inventariovial/supplementary/SupplementaryForms.kt`
- `app/src/main/java/com/tuempresa/inventariovial/ui/theme/Color.kt`
- `app/src/main/java/com/tuempresa/inventariovial/ui/theme/Theme.kt`
- `app/src/main/java/com/tuempresa/inventariovial/validation/CaptureValidation.kt`
- `app/src/main/java/com/tuempresa/inventariovial/viewmodel/InventoryViewModel.kt`

## Pruebas unitarias

- `app/src/test/java/com/tuempresa/inventariovial/EngineeringUxTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/InventoryRoomMigrationTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapStorageTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SurveyStorageTest.kt`

## Pruebas instrumentadas

- `app/src/androidTest/java/com/tuempresa/inventariovial/EngineeringUxScreenTest.kt`
- `app/src/androidTest/java/com/tuempresa/inventariovial/InventoryMigrationTest.kt`
- `app/src/androidTest/java/com/tuempresa/inventariovial/SicExportScreenTest.kt`

## Schema Room

- `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/7.json`

## Documentación

- `docs/ARCHIVOS_REVISION_UX.md`
- `docs/CHECKLIST_TABLET_UX.md`
- `docs/REVISION_INGENIERIA_UX.md`
- `docs/SCAP_IMPLEMENTATION.md`

