package com.tuempresa.inventariovial

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.gis.LocalKmlExport
import com.tuempresa.inventariovial.repository.InventoryRepository
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], shadows = [TestKmlFileProvider::class])
class RoadAxisStorageTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun realVersionEightMigrationPreservesManualPrPhotosAndDetailsAndNewFieldsStartNull() = runBlocking {
        val name = "axis-v8.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/8.json").readText())
                .getJSONObject("database").getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
            }
            old.execSQL("""INSERT INTO inventory_records (id,sicCode,assetType,routeCode,roadbedCode,startPrCode,startDistanceM,
                endPrCode,endDistanceM,latitude,longitude,gpsAccuracyM,surveyDate,status,photoSyncStatus,excelSyncStatus,createdAt,updatedAt)
                VALUES ('old','SIC-19','DITCH','R','CD','9999',777,'9998',555,0,0.005,1,'28/09/2026','ACTIVE','SYNCED','SYNCED',11,22)""")
            old.execSQL("INSERT INTO sic19_details (recordId,classCode,typeCode,crossSectionCode,structuralConditionCode,functionalConditionCode) VALUES ('old','08','2','1','1','1')")
            old.execSQL("INSERT INTO photos (id,recordId,photoIndex,localPath,isPrimary,syncStatus,createdAt) VALUES ('photo','old',1,'/keep.jpg',1,'SYNCED',11)")
            old.version = 8
        }
        var db = Room.databaseBuilder(context, InventoryDatabase::class.java, name).addMigrations(InventoryDatabase.MIGRATION_8_9,InventoryDatabase.MIGRATION_9_10).build()
        try {
            val before = db.inventoryDao().snapshot("old")!!
            assertNull(before.record.axisMeasureM)
            assertNull(before.record.distanceToRoadAxisM)
            assertNull(before.record.roadMatchConfidence)
            assertNull(before.record.matchedSegmentId)
            assertNull(before.record.projectedLatitude)
            assertNull(before.record.projectedLongitude)
            assertEquals(1, RoadAxisStorage(db.inventoryDao()).refresh(testAxis(), MatchConfig()))
            db.close()
            db = Room.databaseBuilder(context, InventoryDatabase::class.java, name).build()
            val after = db.inventoryDao().snapshot("old")!!
            assertEquals(1500.0, after.record.axisMeasureM!!, 0.01)
            assertEquals(0.0, after.record.distanceToRoadAxisM!!, 0.01)
            assertEquals("axis", after.record.matchedSegmentId)
            assertEquals(before.record, after.record.copy(axisMeasureM = null, distanceToRoadAxisM = null,
                roadMatchConfidence = null, matchedSegmentId = null, projectedLatitude = null, projectedLongitude = null))
            assertEquals(before.photos, after.photos)
            assertEquals(before.sic19, after.sic19)
            assertEquals(10, db.openHelper.readableDatabase.version)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun projectionPersistsOnSaveAndRecalculationCannotOverwriteConcurrentRouteEditOrManualFields() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        try {
            val dao = db.inventoryDao()
            val captured = testRecord(sic = "SIC-19").copy(startPrCode = "0042", startDistanceM = 17.0)
            val positioned = RoadAxisPositioner(testAxis()).position(captured)
            val photo = PhotoEntity("photo", captured.id, 1, "/original.jpg", true, null, null, null, "PENDING", 1)
            InventoryRepository(db).saveSic19(positioned, Sic19Entity(captured.id, "08", "1", "1", "1", "1"), listOf(photo))
            assertEquals(positioned, dao.recordById(captured.id))
            dao.updateCoreFields(captured.id, "OTHER", "CD", "0042", 17.0, "0011", 100.0, "D", null, 10)
            assertFalse(RoadAxisStorage(dao).persist(positioned))
            RoadAxisStorage(dao).refresh(testAxis(), MatchConfig())
            val edited = dao.snapshot(captured.id)!!
            assertNull(edited.record.axisMeasureM)
            assertEquals("OTHER", edited.record.routeCode)
            assertEquals("0042", edited.record.startPrCode)
            assertEquals(17.0, edited.record.startDistanceM, 0.0)
            assertEquals(captured.createdAt, edited.record.createdAt)
            assertEquals(photo, edited.photos.single())
            assertEquals("CUN-1", RoadOrdering.orderAssetsForRoute(dao.observeHistory().first()).single().shortCode)
        } finally { db.close() }
    }

    @Test fun localKmzExportFiltersSelectedRouteAndIncludesAxisEvenWithoutElements(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        try {
            val ref = RoadReferenceRepository(context).loadFromAssets()
            val p = ref.forSelectedRoute("PE-3N").segments.single().points[100]
            db.inventoryDao().insertRecord(testRecord("inside", route = "PE-3N", sic = "SIC-18").copy(latitude = p.latitude, longitude = p.longitude))
            db.inventoryDao().insertRecord(testRecord("outside", route = "PE-22A", sic = "SIC-18"))
            val file = LocalKmlExport(context, db).prepare(ChainageCalculator(), "PE-3N", kmz = true)
            try {
                ZipFile(file).use { zip ->
                    val kml = zip.getInputStream(zip.getEntry("doc.kml")).bufferedReader().readText()
                    assertTrue(kml.contains("ALC-1")); assertTrue(kml.contains("INICIO PE-3N"))
                    assertFalse(kml.contains("PE-22A")); assertFalse(kml.contains("outside"))
                    assertNotNull(zip.getEntry("legend.png"))
                    assertEquals(151669.9, ref.forSelectedRoute("PE-3N").segments.single().points.last().chainageM, 0.02)
                }
                assertNotNull(db.inventoryDao().recordById("inside")!!.axisMeasureM)
                assertNull(db.inventoryDao().recordById("outside")!!.axisMeasureM)
                val intent = LocalKmlExport(context, db).intent(file, true)
                assertEquals("application/vnd.google-earth.kmz", intent.type)
                assertEquals("${context.packageName}.fileprovider", intent.data!!.authority)
                assertTrue(intent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            } finally { file.delete() }
            val empty = LocalKmlExport(context, db).prepare(ChainageCalculator(), "PE-28H", kmz = true)
            assertTrue(empty.length() > 0); empty.delete()
        } finally { db.close() }
    }
}

// AndroidX FileProvider checks Android '/' separators; Robolectric runs on Windows '\\'.
// Keep this platform adapter in tests only, so intent payloads are exercised without changing provider security.
@Implements(FileProvider::class)
class TestKmlFileProvider {
    companion object {
        @JvmStatic @Implementation
        fun getUriForFile(context: Context, authority: String, file: File): Uri {
            require(file.parentFile!!.canonicalFile == File(context.cacheDir, "kml").canonicalFile)
            return Uri.Builder().scheme("content").authority(authority).appendPath("kml_exports").appendPath(file.name).build()
        }
    }
}
