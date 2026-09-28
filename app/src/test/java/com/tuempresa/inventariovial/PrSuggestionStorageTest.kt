package com.tuempresa.inventariovial

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.field.SurveyPreferences
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrSuggestionStorageTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun migrationNineToTenRetainsRecordsPhotosAndStartSourceAndDefaultsEndToManual() = runBlocking {
        val name = "pr-v9.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/9.json").readText())
                .getJSONObject("database").getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
            }
            old.execSQL("""INSERT INTO inventory_records (id,sicCode,assetType,routeCode,roadbedCode,startPrCode,startDistanceM,
                endPrCode,endDistanceM,latitude,longitude,surveyDate,status,photoSyncStatus,excelSyncStatus,createdAt,updatedAt,axisMeasureM,locationSource)
                VALUES ('old','SIC-19','DITCH','R','CD','0042',684,'0050',12,0,0.0053,'28/09/2026','ACTIVE','SYNCED','SYNCED',11,22,12650,'MANUAL')""")
            old.execSQL("INSERT INTO photos (id,recordId,photoIndex,localPath,isPrimary,syncStatus,createdAt) VALUES ('photo','old',1,'/keep.jpg',1,'SYNCED',11)")
            old.version = 9
        }
        val db = Room.databaseBuilder(context, InventoryDatabase::class.java, name).addMigrations(InventoryDatabase.MIGRATION_9_10).build()
        try {
            val snapshot = db.inventoryDao().snapshot("old")!!
            assertEquals("0042", snapshot.record.startPrCode)
            assertEquals(684.0, snapshot.record.startDistanceM, 0.0)
            assertEquals("0050", snapshot.record.endPrCode)
            assertEquals(12.0, snapshot.record.endDistanceM!!, 0.0)
            assertEquals("MANUAL", snapshot.record.locationSource)
            assertEquals("MANUAL", snapshot.record.endLocationSource)
            assertEquals(12650.0, snapshot.record.axisMeasureM!!, 0.0)
            assertEquals(11L, snapshot.record.createdAt)
            assertEquals(22L, snapshot.record.updatedAt)
            assertEquals("photo", snapshot.photos.single().id)
            assertEquals("/keep.jpg", snapshot.photos.single().localPath)
            assertEquals(10, db.openHelper.readableDatabase.version)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun evaluatingSuggestionsCannotWriteManualPrAndConfirmedSourcesPersistIndependently() = runBlocking {
        val name = "pr-confirm.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, InventoryDatabase::class.java, name).build()
        try {
            val manual = testRecord().copy(startPrCode = "0042", startDistanceM = 684.0, endPrCode = "0050", endDistanceM = 12.0,
                longitude = .0053, endLatitude = 0.0, endLongitude = .012, endGpsAccuracyM = 1.0)
            val photo = PhotoEntity("photo", manual.id, 1, "/original.jpg", true, null, null, null, "PENDING", 1)
            db.inventoryDao().insertRecord(manual)
            db.inventoryDao().insertPhoto(photo)
            val engine = PrSuggestionEngine(prSuggestionReference())
            val initial = engine.suggest(manual.routeCode, manual.roadbedCode, manual.latitude, manual.longitude, manual.gpsAccuracyM).suggestion!!
            val final = engine.suggest(manual.routeCode, manual.roadbedCode, manual.endLatitude!!, manual.endLongitude!!, manual.endGpsAccuracyM).suggestion!!
            assertEquals(manual, db.inventoryDao().recordById(manual.id))
            val start = PrEntry(manual.startPrCode, manual.startDistanceM.toString()).confirm(initial)
            db.inventoryDao().updateCoreFields(manual.id, "R", "CD", start.prCode, start.distanceM.toDouble(), manual.endPrCode,
                manual.endDistanceM, "D", null, 2, start.source, "MANUAL")
            val partial = db.inventoryDao().recordById(manual.id)!!
            assertEquals("0050", partial.endPrCode)
            assertEquals(12.0, partial.endDistanceM!!, 0.0)
            assertEquals("MANUAL", partial.endLocationSource)
            val end = PrEntry(partial.endPrCode!!, partial.endDistanceM.toString()).confirm(final)
            db.inventoryDao().updateCoreFields(manual.id, "R", "CD", partial.startPrCode, partial.startDistanceM, end.prCode,
                end.distanceM.toDouble(), "D", null, 3, partial.locationSource, end.source)
            db.close()
            db = Room.databaseBuilder(context, InventoryDatabase::class.java, name).build()
            val saved = db.inventoryDao().snapshot(manual.id)!!
            assertEquals("0010", saved.record.startPrCode)
            assertEquals(2650.0, saved.record.startDistanceM, 0.0)
            assertEquals("0015", saved.record.endPrCode)
            assertEquals(1000.0, saved.record.endDistanceM!!, 0.0)
            assertEquals(PrEntrySource.CONFIRMED, saved.record.locationSource)
            assertEquals(PrEntrySource.CONFIRMED, saved.record.endLocationSource)
            assertEquals("CD", saved.record.roadbedCode)
            assertEquals(manual.createdAt, saved.record.createdAt)
            assertEquals(photo, saved.photos.single())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun surveyValidationAllowsCatalogOffsetsAbove1000WithoutInterpretingCodesAsKilometres() {
        context.getSharedPreferences("survey_context", 0).edit().clear().commit()
        val request = InventorySaveRequest("SIC-19", "DITCH", "R", "CD", "0042", "2650", "0007", "1000", "D",
            0.0, .0053, 1f, "28/09/2026", "", "", SicFormDetail.Sic19(Sic19FormState()), segment = "Tramo 1")
        val prs = prSuggestionReference().prs.map { it.copy(prCode = if (it.prCode == "0010") "0042" else "0007") }
        val prefs = SurveyPreferences(context)
        assertNull(prefs.validate(request, prs)) // 12650 -> 16000; PR labels themselves decrease.
        assertNotNull(prefs.validate(request)) // Existing manual-only kilometre rules stay in place.
        assertNotNull(prefs.validate(request.copy(direction = "DECREASING"), prs))
        assertNull(prefs.continuityWarning(request, prs))
    }
}
