package com.tuempresa.inventariovial.scap.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ScapMigration {
    val MIGRATION_5_6 = object : Migration(5,6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_inspections` (`id` TEXT NOT NULL, `roadRecordId` TEXT NOT NULL, `bridgeName` TEXT NOT NULL, `bridgeCode` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `createdBy` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `status` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `latitude` REAL, `longitude` REAL, `gpsAccuracyM` REAL, `gpsTimestamp` INTEGER, `locationSource` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`roadRecordId`) REFERENCES `inventory_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scap_inspections_roadRecordId` ON `scap_inspections` (`roadRecordId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_values` (`inspectionId` TEXT NOT NULL, `ownerId` TEXT NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`inspectionId`,`ownerId`,`key`), FOREIGN KEY(`inspectionId`) REFERENCES `scap_inspections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_spans` (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, `spanIndex` INTEGER NOT NULL, `lengthM` REAL, `category` TEXT NOT NULL, `type` TEXT NOT NULL, `secondaryCharacteristic` TEXT NOT NULL, `edgeCondition` TEXT NOT NULL, `predominantMaterial` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`inspectionId`) REFERENCES `scap_inspections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scap_spans_inspectionId_spanIndex` ON `scap_spans` (`inspectionId`,`spanIndex`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_substructures` (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, `kind` TEXT NOT NULL, `elementIndex` INTEGER NOT NULL, `elevationType` TEXT NOT NULL, `elevationMaterial` TEXT NOT NULL, `foundationType` TEXT NOT NULL, `foundationMaterial` TEXT NOT NULL, `soil` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`inspectionId`) REFERENCES `scap_inspections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scap_substructures_inspectionId_kind_elementIndex` ON `scap_substructures` (`inspectionId`,`kind`,`elementIndex`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_supports` (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, `supportIndex` INTEGER NOT NULL, `type` TEXT NOT NULL, `material` TEXT NOT NULL, `location` TEXT NOT NULL, `number` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`inspectionId`) REFERENCES `scap_inspections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scap_supports_inspectionId_supportIndex` ON `scap_supports` (`inspectionId`,`supportIndex`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_elements` (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, `elementCode` TEXT NOT NULL, `description` TEXT NOT NULL, `quantity` REAL, `unit` TEXT NOT NULL, `importanceFactor` REAL NOT NULL, `group` TEXT NOT NULL, `isPresent` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`inspectionId`) REFERENCES `scap_inspections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scap_elements_inspectionId_elementCode` ON `scap_elements` (`inspectionId`,`elementCode`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_conditions` (`id` TEXT NOT NULL, `scapElementId` TEXT NOT NULL, `percent0` REAL, `percent1` REAL, `percent2` REAL, `percent3` REAL, `percent4` REAL, `percent5` REAL, PRIMARY KEY(`id`), FOREIGN KEY(`scapElementId`) REFERENCES `scap_elements`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scap_conditions_scapElementId` ON `scap_conditions` (`scapElementId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_defects` (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, `elementCode` TEXT, `description` TEXT NOT NULL, `locationDescription` TEXT NOT NULL, `photoId` TEXT, `aiSuggested` INTEGER NOT NULL, `validatedByUser` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`inspectionId`) REFERENCES `scap_inspections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_scap_defects_inspectionId` ON `scap_defects` (`inspectionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_sketches` (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, `type` TEXT NOT NULL, `localUri` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`inspectionId`) REFERENCES `scap_inspections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_scap_sketches_inspectionId` ON `scap_sketches` (`inspectionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `scap_profile_points` (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, `pointIndex` INTEGER NOT NULL, `distanceM` REAL, `downstreamM` REAL, `upstreamM` REAL, `axisM` REAL, PRIMARY KEY(`id`), FOREIGN KEY(`inspectionId`) REFERENCES `scap_inspections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_scap_profile_points_inspectionId_pointIndex` ON `scap_profile_points` (`inspectionId`,`pointIndex`)")
            db.execSQL("ALTER TABLE photos ADD COLUMN scapInspectionId TEXT")
            db.execSQL("ALTER TABLE photos ADD COLUMN scapElementCode TEXT")
            db.execSQL("ALTER TABLE photos ADD COLUMN photoCategory TEXT")
        }
    }
}
