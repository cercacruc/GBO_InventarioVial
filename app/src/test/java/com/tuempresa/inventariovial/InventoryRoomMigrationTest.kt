package com.tuempresa.inventariovial

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.repository.InventoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class InventoryRoomMigrationTest {
    @Test fun migrationsFromEveryReleasedVersionPreserveRecordsPhotosAndSic23() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        for(version in 1..3) {
            // Keep the SQLite database/journal path below Windows' legacy MAX_PATH.
            val name="v$version.db"
            context.deleteDatabase(name)
            val file=context.getDatabasePath(name).apply {parentFile!!.mkdirs()}
            SQLiteDatabase.openOrCreateDatabase(file,null).use {old ->
                File("src/androidTest/assets/inventory-v1.sql").readText().split(';').filter {it.isNotBlank()}.forEach {old.execSQL(it)}
                old.execSQL("""INSERT INTO inventory_records (id,sicCode,assetType,routeCode,roadbedCode,startPrCode,startDistanceM,
                    latitude,longitude,surveyDate,status,photoSyncStatus,excelSyncStatus,createdAt,updatedAt)
                    VALUES ('old','SIC-19','DRENAJE','R','CD','0010',300,0,0,'23/09/2026','ANNULLED','SYNCED','PENDING',1,2)""")
                old.execSQL("INSERT INTO sic19_details VALUES ('old','08','2','1','1','1')")
                old.execSQL("INSERT INTO photos (id,recordId,photoIndex,localPath,isPrimary,syncStatus,createdAt) VALUES ('photo','old',1,'/original.jpg',1,'SYNCED',1)")
                if(version>=2) {
                    old.execSQL("ALTER TABLE inventory_records ADD COLUMN endLatitude REAL")
                    old.execSQL("ALTER TABLE inventory_records ADD COLUMN endLongitude REAL")
                    old.execSQL("ALTER TABLE inventory_records ADD COLUMN endGpsAccuracyM REAL")
                    old.execSQL("UPDATE inventory_records SET endLatitude=0.01,endLongitude=0.02")
                }
                if(version>=3) {
                    old.execSQL("CREATE TABLE sic23_details (recordId TEXT NOT NULL,classCode TEXT NOT NULL,typeCode TEXT,widthM REAL,description TEXT NOT NULL,PRIMARY KEY(recordId),FOREIGN KEY(recordId) REFERENCES inventory_records(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    old.execSQL("INSERT INTO sic23_details VALUES ('old','21','1',12.5,'CONSERVAR')")
                }
                old.version=version
            }
            val db=Room.databaseBuilder(context,InventoryDatabase::class.java,name)
                .addMigrations(InventoryDatabase.MIGRATION_1_2,InventoryDatabase.MIGRATION_2_3,InventoryDatabase.MIGRATION_3_4,InventoryDatabase.MIGRATION_4_5,InventoryDatabase.MIGRATION_5_6,InventoryDatabase.MIGRATION_6_7).build()
            try {
                val snapshot=db.inventoryDao().snapshot("old")!! // Opening invokes Room's full schema validator.
                assertEquals("ANNULLED",snapshot.record.status);assertEquals("old",snapshot.record.id)
                assertEquals("08",snapshot.sic19!!.classCode);assertEquals("photo",snapshot.photos.single().id)
                assertEquals("/original.jpg",snapshot.photos.single().originalPath);assertNull(snapshot.photos.single().stampedPath)
                assertEquals("SYNCED",snapshot.photos.single().syncStatus);assertEquals("PENDING",snapshot.record.serverSyncStatus)
                if(version==3) assertEquals(12.5,snapshot.sic23!!.widthM!!,0.0)
                if(version>=2) assertEquals(0.01,snapshot.record.endLatitude!!,0.0)
                assertTrue(db.inventoryDao().recordsForExport(null,null).isEmpty())
            } finally {db.close();context.deleteDatabase(name)}
        }
    }
    @Test fun completingDraftPreservesTrackAndServerAcknowledgementCannotHideNewerEdit() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val dao=db.inventoryDao();val record=testRecord(status="DRAFT",sic="SIC-23")
            dao.insertRecord(record)
            dao.insertTrackPoint(TrackPointEntity("p",record.id,0,0.0,0.0,null,1.0,1000))
            InventoryRepository(db).saveSic23(record.copy(status="ACTIVE",updatedAt=2),Sic23Entity(record.id,"21","1",12.5,"VIA"),emptyList())
            assertEquals("p",dao.snapshot(record.id)!!.track.single().id)
            assertEquals(1,dao.recordsForExport("SIC-23",null).size)
            assertTrue(runCatching {InventoryRepository(db).saveSic23(record.copy(status="ACTIVE"),Sic23Entity(record.id,"21","1",1.0,""),emptyList())}.isFailure)
            dao.setServerStatus(record.id,2,"SYNCING",null)
            dao.setRecordStatus(record.id,"ANNULLED",3)
            assertEquals(0,dao.setServerStatus(record.id,2,"SYNCED",null))
            assertEquals("PENDING",dao.recordById(record.id)!!.serverSyncStatus)
            assertEquals(1,dao.observeHistory().first().size)
            assertEquals(1,dao.track(record.id).size)
            assertFalse(dao.insertDraftTrackPoint(TrackPointEntity("late",record.id,1,0.0,0.0,null,1.0,2000)))
            assertEquals(1,dao.track(record.id).size)
        } finally {db.close()}
    }
    @Test fun endingSessionInvalidatesOldAcknowledgementAndUpdatesVersionAtomically() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val dao=db.inventoryDao()
            dao.insertSession(FieldSession("session","Proyecto","Operador","hash",null,null,null,"INCREASING",1))
            dao.insertRecord(testRecord().copy(sessionId="session",updatedAt=100,serverSyncStatus="SYNCED"))
            dao.closeSessions(50) // Even if the device clock goes backward, the revision must advance.
            assertEquals(50L,dao.sessionById("session")!!.endTime)
            assertEquals(101L,dao.recordById("id")!!.updatedAt)
            assertEquals("PENDING",dao.recordById("id")!!.serverSyncStatus)
            assertEquals(0,dao.setServerStatus("id",100,"SYNCED",null))
            dao.setRecordStatus("id","ANNULLED",10)
            assertEquals(102L,dao.recordById("id")!!.updatedAt)
        } finally {db.close()}
    }
}
