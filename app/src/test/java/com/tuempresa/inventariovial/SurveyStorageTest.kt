package com.tuempresa.inventariovial

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.field.SurveyPreferences
import com.tuempresa.inventariovial.model.form.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class SurveyStorageTest {
    @Test fun versionFourUpgradeRetainsOldCulvertDimensionsAndMarksNewFieldsUnknown() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="v4.db";context.deleteDatabase(name)
        val file=context.getDatabasePath(name).apply {parentFile!!.mkdirs()}
        SQLiteDatabase.openOrCreateDatabase(file,null).use {old ->
            val schema=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/4.json").readText()).getJSONObject("database")
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val entity=entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}",entity.getString("tableName")))
                val indices=entity.optJSONArray("indices") ?: org.json.JSONArray()
                for(j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}",entity.getString("tableName")))
            }
            old.execSQL("""INSERT INTO inventory_records (id,sicCode,assetType,routeCode,roadbedCode,startPrCode,startDistanceM,latitude,longitude,surveyDate,status,photoSyncStatus,excelSyncStatus,createdAt,updatedAt) VALUES ('old','SIC-18','ALCANTARILLA','R','CD','12',350,-12,-77,'24/09/2026','ACTIVE','PENDING','PENDING',1,1)""")
            old.execSQL("INSERT INTO sic18_details VALUES ('old','06','1',1,'2',1.2,0.8,'2','1')")
            old.version=4
        }
        val db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_4_5,InventoryDatabase.MIGRATION_5_6).build()
        try {
            val snapshot=db.inventoryDao().snapshot("old")!!
            assertEquals(0.8,snapshot.sic18!!.dimension2M!!,0.0)
            assertNull(snapshot.sic18!!.sectionShape);assertNull(snapshot.sic18!!.structuralDamagePercent)
            assertNull(snapshot.record.segment);assertEquals("2",snapshot.sic18!!.structuralConditionCode)
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun catalogsAndContinuitySurviveReopeningAndResetDoesNotRemoveCatalogs() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("survey_context",0).edit().clear().commit()
        val prefs=SurveyPreferences(context)
        prefs.saveRoutes("Tramo 1",listOf("r1","r2"))
        val request=InventorySaveRequest("SIC-19","CUNETA","R1","CD","12","350","12","450","D",-12.0,-77.0,
            1f,"24/09/2026","","",SicFormDetail.Sic19(Sic19FormState()),photoPaths=emptyList(),segment="Tramo 1")
        assertNull(prefs.validate(request));prefs.remember(request)
        val reopened=SurveyPreferences(context)
        assertEquals("12",reopened.last()!!.pr)
        assertNotNull(reopened.validate(request.copy(startDistanceM="300")))
        reopened.reset();assertNull(reopened.validate(request.copy(startDistanceM="300")))
        assertEquals(listOf("R1","R2"),reopened.routes("Tramo 1"))
        assertNotNull(reopened.validate(request.copy(routeCode="NO_EXISTE")))
    }
}
