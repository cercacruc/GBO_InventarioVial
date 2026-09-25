package com.tuempresa.inventariovial

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.Sic23Entity
import com.tuempresa.inventariovial.repository.InventoryRepository
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
            .addMigrations(InventoryDatabase.MIGRATION_1_2, InventoryDatabase.MIGRATION_2_3, InventoryDatabase.MIGRATION_3_4,InventoryDatabase.MIGRATION_4_5,InventoryDatabase.MIGRATION_5_6).build()
        try {
            val dao = db.inventoryDao()
            val item = dao.observeHistory().first().single()
            assertEquals("record-1", item.record.id)
            assertEquals(2, item.photos.size)
            assertEquals("/photo1.jpg", item.photos.first { it.id == "photo-1" }.originalPath)
            assertNull(item.photos.first().stampedPath)
            assertEquals("PENDING", item.record.serverSyncStatus)
            assertNull(item.record.endLatitude)
            assertNull(item.record.endLongitude)
            assertEquals(1, dao.pendingPhotos().size)
            db.openHelper.readableDatabase.query("SELECT classCode FROM sic21_details").use {
                assertTrue(it.moveToFirst())
                assertEquals("19", it.getString(0))
            }
            // The upgraded database must accept SIC-23 details and keep their photos.
            val newRecord = item.record.copy(id = "sic23-1", sicCode = "SIC-23", assetType = "DERECHO_VIA",
                endPrCode = "0002", endDistanceM = 150.0, sideCode = "D")
            val detail = Sic23Entity("sic23-1", "21", "1", 12.50, "ANCHO TOTAL")
            InventoryRepository(db).saveSic23(newRecord, detail,
                listOf(item.photos.first().copy(id = "sic23-photo", recordId = "sic23-1", localPath = "/sic23.jpg")))
            assertEquals(detail, dao.observeHistory().first().first { it.record.id == "sic23-1" }.sic23)
            val exportRows = dao.recordsForExport(null, null)
            assertEquals(2, exportRows.size)
            assertEquals("19", exportRows.first { it.record.id == "record-1" }.sic21!!.classCode)
            assertEquals(detail, dao.recordsForExport("SIC-23", "PE-1N").single().sic23)
            assertTrue(dao.recordsForExport("SIC-22", null).isEmpty())
            assertTrue(dao.recordsForExport(null, "OTHER").isEmpty())
            val exported = java.io.ByteArrayOutputStream()
            com.tuempresa.inventariovial.export.SicExcelWriter.write(exported, exportRows, null,
                com.tuempresa.inventariovial.export.ExportHeading())
            java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(exported.toByteArray())).use { zip ->
                assertEquals("SIC-21.xlsx", zip.nextEntry.name)
                assertTrue(zip.readBytes().size > 100)
                assertEquals("SIC-23.xlsx", zip.nextEntry.name)
                assertTrue(zip.readBytes().size > 100)
                assertNull(zip.nextEntry)
            }

            db.openHelper.writableDatabase.execSQL("DELETE FROM inventory_records WHERE id = 'sic23-1'")
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sic23_details").use {
                assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
            }
            dao.setRecordStatus("record-1", "ANNULLED", 3)
            assertTrue(dao.pendingPhotos().isEmpty())
            assertTrue(dao.recordsForExport(null, null).isEmpty())
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
