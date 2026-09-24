package com.tuempresa.inventariovial.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object InventoryMigrations {
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN segment TEXT")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN surveyDirection TEXT")
            db.execSQL("ALTER TABLE sic18_details ADD COLUMN sectionShape TEXT")
            db.execSQL("ALTER TABLE sic18_details ADD COLUMN structuralDamagePercent REAL")
            db.execSQL("ALTER TABLE sic18_details ADD COLUMN functionalObstructionPercent REAL")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN locationSource TEXT NOT NULL DEFAULT 'MANUAL'")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN sideSource TEXT NOT NULL DEFAULT 'MANUAL'")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN sessionId TEXT")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN gpsTimestamp INTEGER")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN endGpsTimestamp INTEGER")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN gnssProvider TEXT NOT NULL DEFAULT 'TABLET'")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN gnssFixType TEXT")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN verticalAccuracyM REAL")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN satellites INTEGER")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN hdop REAL")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN correctionAge REAL")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN isRtkFixed INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN serverSyncStatus TEXT NOT NULL DEFAULT 'PENDING'")
            db.execSQL("ALTER TABLE inventory_records ADD COLUMN serverSyncError TEXT")
            db.execSQL("ALTER TABLE photos ADD COLUMN originalPath TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE photos ADD COLUMN stampedPath TEXT")
            db.execSQL("UPDATE photos SET originalPath = localPath")
            db.execSQL("CREATE TABLE IF NOT EXISTS track_points (id TEXT NOT NULL, recordId TEXT NOT NULL, sequence INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, altitude REAL, accuracy REAL NOT NULL, timestamp INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(recordId) REFERENCES inventory_records(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_points_recordId_sequence ON track_points(recordId, sequence)")
            db.execSQL("CREATE TABLE IF NOT EXISTS field_sessions (sessionId TEXT NOT NULL, project TEXT NOT NULL, operator TEXT NOT NULL, device TEXT NOT NULL, road TEXT, segment TEXT, roadbed TEXT, direction TEXT NOT NULL, startTime INTEGER NOT NULL, endTime INTEGER, PRIMARY KEY(sessionId))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `sic17a_details` (`recordId` TEXT NOT NULL, `bridgeName` TEXT NOT NULL, `bridgeCode` TEXT NOT NULL, `constructionYear` TEXT NOT NULL, `department` TEXT NOT NULL, `province` TEXT NOT NULL, `district` TEXT NOT NULL, `nearbyTown` TEXT NOT NULL, `latitude` TEXT NOT NULL, `longitude` TEXT NOT NULL, `altitude` TEXT NOT NULL, `lanes` TEXT NOT NULL, `roadwayWidthM` TEXT NOT NULL, `sidewalkWidthM` TEXT NOT NULL, `deckWidthM` TEXT NOT NULL, `superstructureWidthM` TEXT NOT NULL, `alignmentCode` TEXT NOT NULL, PRIMARY KEY(`recordId`), FOREIGN KEY(`recordId`) REFERENCES `inventory_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `sic17b_details` (`recordId` TEXT NOT NULL, `bridgeCode` TEXT NOT NULL, `designLoad` TEXT NOT NULL, `maximumCapacity` TEXT NOT NULL, `wearingSurface` TEXT NOT NULL, `vehicleRestraintCode` TEXT NOT NULL, `mainSpanM` TEXT NOT NULL, `boundaryCode` TEXT NOT NULL, `crossSectionCode` TEXT NOT NULL, `beams` TEXT NOT NULL, `slabMaterialCode` TEXT NOT NULL, `beamMaterialCode` TEXT NOT NULL, `abutmentElevationCode` TEXT NOT NULL, `abutmentMaterialCode` TEXT NOT NULL, `abutmentFoundationCode` TEXT NOT NULL, `pierElevationCode` TEXT NOT NULL, `pierMaterialCode` TEXT NOT NULL, `pierFoundationCode` TEXT NOT NULL, `comments` TEXT NOT NULL, PRIMARY KEY(`recordId`), FOREIGN KEY(`recordId`) REFERENCES `inventory_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `sic18a_details` (`recordId` TEXT NOT NULL, `classCode` TEXT NOT NULL, `typeCode` TEXT NOT NULL, `spans` TEXT NOT NULL, `functionCode` TEXT NOT NULL, `failureLocationCode` TEXT NOT NULL, `failureTypeCode` TEXT NOT NULL, `functionalStateCode` TEXT NOT NULL, `probableCauseCode` TEXT NOT NULL, PRIMARY KEY(`recordId`), FOREIGN KEY(`recordId`) REFERENCES `inventory_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        }
    }
}
