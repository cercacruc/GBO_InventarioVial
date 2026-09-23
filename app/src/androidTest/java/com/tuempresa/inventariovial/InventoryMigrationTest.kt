package com.tuempresa.inventariovial

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class InventoryMigrationTest {
    @Test
    fun migrationPreservesRecordsDetailsAndMultiplePhotos() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "migration-" + UUID.randomUUID() + ".db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            instrumentation.context.assets.open("inventory-v1.sql").bufferedReader().use { reader ->
                reader.readText().split(';').filter { it.isNotBlank() }.forEach { old.execSQL(it) }
            }
            old.execSQL("""INSERT INTO inventory_records
                (id,sicCode,assetType,routeCode,roadbedCode,startPrCode,startDistanceM,
                 latitude,longitude,surveyDate,status,photoSyncStatus,excelSyncStatus,createdAt,updatedAt)
                VALUES ('record-1','SIC-21','SAFETY','PE-1N','CD','0002',100,
                -12,-77,'23/09/2026','ACTIVE','PENDING','PENDING',1,1)""")
            old.execSQL("INSERT INTO sic21_details VALUES ('record-1','19','1','1','1')")
            old.execSQL("""INSERT INTO photos
                (id,recordId,photoIndex,localPath,isPrimary,syncStatus,createdAt)
                VALUES ('photo-1','record-1',1,'/photo1.jpg',1,'SYNCED',1)""")
            old.execSQL("""INSERT INTO photos
                (id,recordId,photoIndex,localPath,isPrimary,syncStatus,createdAt)
                VALUES ('photo-2','record-1',2,'/photo2.jpg',0,'PENDING',2)""")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, InventoryDatabase::class.java, name)
            .addMigrations(InventoryDatabase.MIGRATION_1_2).build()
        try {
            val dao = db.inventoryDao()
            val item = dao.observeHistory().first().single()
            assertEquals("record-1", item.record.id)
            assertEquals(2, item.photos.size)
            assertNull(item.record.endLatitude)
            assertNull(item.record.endLongitude)
            assertEquals(1, dao.pendingPhotos().size)
            db.openHelper.readableDatabase.query("SELECT classCode FROM sic21_details").use {
                assertTrue(it.moveToFirst())
                assertEquals("19", it.getString(0))
            }
            dao.setRecordStatus("record-1", "ANNULLED", 3)
            assertTrue(dao.pendingPhotos().isEmpty())
            assertEquals(2, dao.observeHistory().first().single().photos.size)
            dao.setRecordStatus("record-1", "ACTIVE", 4)
            assertEquals(1, dao.pendingPhotos().size)
            dao.refreshRecordPhotoStatus("record-1")
            assertEquals("PENDING", dao.observeHistory().first().single().record.photoSyncStatus)
            dao.completePhotoUpload("/photo2.jpg", "drive-2")
            assertEquals("SYNCED", dao.observeHistory().first().single().record.photoSyncStatus)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
