package com.tuempresa.inventariovial

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.location.GeoLocation
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ScapStorageTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun database()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private suspend fun validInspection(repo:ScapRepository):String {
        val id=repo.create("Inspector","device",null)
        mapOf("bridgeName" to "Puente de prueba","bridgeCode" to "P-01","route" to "R-1","progressive" to "30+298").forEach{(k,v)->repo.setField(id,"inspection",k,v)}
        repo.addRow(id,"SPAN")
        val span=repo.dao.snapshot(id)!!.spans.single()
        repo.setField(id,span.id,"category","DEFINITIVO");repo.setField(id,span.id,"type","Losa")
        repo.selectElement(id,"104",true)
        val element=repo.dao.snapshot(id)!!.elements.single().element
        for(i in 0..5)repo.setField(id,element.id,"percent$i",if(i==1)"100" else "0")
        return id
    }
    @Test fun draftSurvivesDatabaseReopenWithNRowsRawInputAndIndependentAccesses() = runBlocking {
        val name="scap.db";context.deleteDatabase(name)
        var db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {
            val r=ScapRepository(db,testScapCatalog());val id=r.create("Inspector","device",null)
            assertEquals("DRAFT",r.dao.inspection(id)!!.status)
            repeat(5){r.addRow(id,"SPAN");r.addRow(id,"PIER");r.addRow(id,"BEARING");r.addRow(id,"PROFILE")}
            r.addRow(id,"LEFT_ABUTMENT");r.addRow(id,"RIGHT_ABUTMENT")
            assertTrue(runCatching{r.addRow(id,"LEFT_ABUTMENT")}.isFailure)
            r.setField(id,"inspection","bridgeName","Sin terminar")
            r.setField(id,"inspection","totalLengthM","-") // Raw intermediate typing must not disappear.
            r.setField(id,"ACCESS_LEFT","transitionLengthM","10.25")
            r.setField(id,"ACCESS_RIGHT","transitionLengthM","25.5")
            r.selectElement(id,"104",true);r.selectElement(id,"104",true);r.selectElement(id,"110",true)
            r.addPhoto(id,"/images/one.jpg","ELEMENT","104")
            val photo=r.dao.snapshot(id)!!.photos.single()
            repeat(4){n->r.saveDefect(id,ScapDefectEntity(UUID.randomUUID().toString(),id,"104","Defecto $n","Lado izquierdo",photo.id,false,true,n.toLong()))}
            r.addSketch(id,"ELEVATION","/images/elevation.png")
            db.close();db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
            val restored=db.scapDao().snapshot(id)!!
            assertEquals("IN_PROGRESS",restored.inspection.status);assertEquals("Sin terminar",restored.inspection.bridgeName)
            assertEquals("-",restored.values()["totalLengthM"])
            assertEquals(5,restored.spans.size);assertEquals(5,restored.substructures.count{it.kind=="PIER"})
            assertEquals(7,restored.substructures.size);assertEquals(5,restored.supports.size);assertEquals(5,restored.profile.size)
            assertEquals(2,restored.elements.size);assertEquals(4,restored.defects.size)
            assertEquals("10.25",restored.values("ACCESS_LEFT")["transitionLengthM"])
            assertEquals("25.5",restored.values("ACCESS_RIGHT")["transitionLengthM"])
            assertEquals(id,restored.photos.single().scapInspectionId);assertEquals("104",restored.photos.single().scapElementCode)
            assertEquals(photo.id,restored.defects.first().photoId);assertEquals("ELEVATION",restored.sketches.single().type)
            assertTrue(restored.elements.all{it.element.inspectionId==id})
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun completionRejectsMissingOutOfBoundsAndWrongTotalsButAllowsResuming() = runBlocking {
        val db=database()
        try {
            val r=ScapRepository(db,testScapCatalog());val id=validInspection(r)
            val e=r.dao.snapshot(id)!!.elements.single().element
            for(value in listOf("", "99", "101", "-1", "NaN")) {
                r.setField(id,e.id,"percent1",value)
                assertTrue("Cannot close percentage $value",runCatching{r.complete(id)}.isFailure)
                assertEquals("IN_PROGRESS",r.dao.inspection(id)!!.status)
            }
            r.setField(id,e.id,"percent1","100")
            r.selectElement(id,"110",true)
            assertTrue(runCatching{r.complete(id)}.isFailure) // A selected, unevaluated element blocks closure.
            r.selectElement(id,"110",false)
            assertTrue(ScapValidation.review(r.dao.snapshot(id)!!,r.catalog).errors.isEmpty())
            r.complete(id);assertEquals("COMPLETE",r.dao.inspection(id)!!.status)
            assertTrue(runCatching{r.setField(id,"inspection","bridgeName","Closed edit")}.isFailure)
            r.reopen(id);r.setField(id,"inspection","bridgeName","Corrected")
            assertEquals("Corrected",r.dao.snapshot(id)!!.inspection.bridgeName)
        } finally {db.close()}
    }
    @Test fun categoryChangesClearIncompatibleTypeAndUnselectedConditionsArePreserved() = runBlocking {
        val db=database()
        try {
            val r=ScapRepository(db,testScapCatalog());val id=validInspection(r)
            val span=r.dao.snapshot(id)!!.spans.single()
            r.setField(id,span.id,"category","PROVISIONALES")
            val changed=r.dao.snapshot(id)!!
            assertEquals("",changed.spans.single().type);assertEquals("",changed.values(span.id)["type"])
            assertTrue(ScapValidation.review(changed,r.catalog).errors.any{it.contains("categoría y tipo")})
            r.setField(id,span.id,"type","Losa")
            assertTrue(runCatching{r.complete(id)}.isFailure)
            r.setField(id,span.id,"type","Modular")
            r.selectElement(id,"104",false);r.selectElement(id,"104",true)
            assertEquals(100.0,r.dao.snapshot(id)!!.elements.single().condition!!.percent1!!,0.0)
            r.complete(id)
        } finally {db.close()}
    }
    @Test fun localScapClosureDoesNotEnqueueUnknownDriveOrServerRoutesAndMapperIsPartial() = runBlocking {
        val db=database()
        try {
            val r=ScapRepository(db,testScapCatalog());val id=validInspection(r)
            r.setField(id,"inspection","totalLengthM","52.5")
            r.setField(id,"inspection","politicalDepartment","Departamento político")
            r.setField(id,"inspection","roadDepartment","Departamento vial diferente")
            r.setField(id,"inspection","designLoad","HL-93")
            r.setField(id,"inspection","utmEasting","300000")
            r.addPhoto(id,"/images/new.jpg","GENERAL",null)
            r.complete(id)
            val s=r.dao.snapshot(id)!!;val record=db.inventoryDao().recordById(s.inspection.roadRecordId)!!
            assertEquals("COMPLETE",s.inspection.status);assertEquals("DRAFT",record.status)
            assertTrue(db.inventoryDao().pendingPhotos().isEmpty());assertTrue(db.inventoryDao().pendingServerIds().isEmpty())
            assertTrue(db.inventoryDao().recordsForExport(null,null).isEmpty())
            assertNull(s.photos.single().generatedFileName);assertEquals("/images/new.jpg",s.photos.single().originalPath)
            val m17=ScapToSicMapper.mapToSic17(s)
            assertEquals("52.5",m17.fields.single{it.sicField=="dimension1LengthM"}.value)
            assertEquals(ScapMappingStatus.TRANSFORM,m17.fields.single{it.sicField=="spans"}.status)
            assertFalse(m17.fields.any{it.sicField=="structuralConditionCode"})
            val m17a=ScapToSicMapper.mapToSic17A(s)
            assertEquals("Departamento político",m17a.fields.single{it.sicField=="department"}.value)
            assertFalse(m17a.fields.any{it.sicField in setOf("latitude","longitude","deckWidthM")})
            val m17b=ScapToSicMapper.mapToSic17B(s)
            assertEquals("HL-93",m17b.fields.single{it.sicField=="designLoad"}.value)
            assertFalse(m17b.fields.any{it.sicField=="maximumCapacity"});assertTrue(m17b.pending.isNotEmpty())
            assertNull(db.inventoryDao().snapshot(record.id)!!.sic17)
        } finally {db.close()}
    }
    @Test fun foreignKeysOwnershipAndCascadeProtectIndependentInspections() = runBlocking {
        val db=database()
        try {
            val r=ScapRepository(db,testScapCatalog());val a=validInspection(r);val b=validInspection(r)
            val snapshot=r.dao.snapshot(a)!!;val e=snapshot.elements.single().element
            assertTrue(runCatching{r.dao.putElement(e.copy(id="orphan",inspectionId="missing"))}.isFailure)
            // Room upsert can treat a conflict on a secondary unique key as a no-op.
            runCatching{r.dao.putElement(e.copy(id="duplicate"))}
            assertEquals(e.id,r.dao.snapshot(a)!!.elements.single().element.id)
            r.addPhoto(a,"/images/a.jpg","DEFECT","104")
            val photo=r.dao.snapshot(a)!!.photos.single()
            assertTrue(runCatching{r.saveDefect(b,ScapDefectEntity("foreign",b,"104","d","",photo.id,false,true,0))}.isFailure)
            r.saveDefect(a,ScapDefectEntity("ai",a,"104","Fisura visible","",photo.id,true,false,0))
            assertTrue(runCatching{r.complete(a)}.isFailure)
            r.setDefectField(a,"ai","validatedByUser","true");r.complete(a)
            db.openHelper.writableDatabase.execSQL("DELETE FROM inventory_records WHERE id=?",arrayOf(snapshot.inspection.roadRecordId))
            assertNull(r.dao.snapshot(a));assertNotNull(r.dao.snapshot(b))
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM scap_conditions WHERE scapElementId=?",arrayOf(e.id)).use{it.moveToFirst();assertEquals(0,it.getInt(0))}
        } finally {db.close()}
    }
    @Test fun acceptedGnssRetainsManualUtmAndRecordsSource() = runBlocking {
        val db=database()
        try {
            val r=ScapRepository(db,testScapCatalog());val id=validInspection(r)
            r.setField(id,"inspection","utmEasting","300001.2")
            r.setLocation(id,GeoLocation(-12.0,-77.0,accuracyHorizontal=2f,timestamp=123L))
            val s=r.dao.snapshot(id)!!
            assertEquals("300001.2",s.values()["utmEasting"]);assertEquals(-12.0,s.inspection.latitude!!,0.0)
            assertEquals(123L,s.inspection.gpsTimestamp);assertEquals(2.0,s.inspection.gpsAccuracyM!!,0.0)
            assertEquals("-77.0",ScapToSicMapper.mapToSic17A(s).fields.single{it.sicField=="longitude"}.value)
            assertTrue(runCatching{r.setLocation(id,GeoLocation(91.0,-77.0,accuracyHorizontal=2f))}.isFailure)
        } finally {db.close()}
    }
    @Test fun versionFiveMigrationPreservesSicRecordsPhotosMetadataAndNewCulvertFields() = runBlocking {
        val name="v5scap.db";context.deleteDatabase(name)
        val file=context.getDatabasePath(name).apply{parentFile!!.mkdirs()}
        SQLiteDatabase.openOrCreateDatabase(file,null).use{old->
            val entities=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/5.json").readText()).getJSONObject("database").getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val e=entities.getJSONObject(i);val table=e.getString("tableName")
                old.execSQL(e.getString("createSql").replace("\u0024{TABLE_NAME}",table))
                val indices=e.optJSONArray("indices") ?: org.json.JSONArray()
                for(j in 0 until indices.length())old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}",table))
            }
            for((id,status) in listOf("bridge" to "ACTIVE","annulled" to "ANNULLED","draft" to "DRAFT")) {
                old.execSQL("""INSERT INTO inventory_records (id,sicCode,assetType,routeCode,roadbedCode,startPrCode,startDistanceM,latitude,longitude,surveyDate,status,photoSyncStatus,excelSyncStatus,createdAt,updatedAt,segment,surveyDirection) VALUES (?, 'SIC-18','ALCANTARILLA','R','CD','12',350,-12,-77,'24/09/2026',?,'SYNCED','PENDING',1,1,'Tramo 1','DECREASING')""",arrayOf(id,status))
            }
            old.execSQL("INSERT INTO sic18_details VALUES ('bridge','06','1',2,'2',1.2,0.8,'2','1','Rectangular',10,20)")
            old.execSQL("INSERT INTO photos(id,recordId,photoIndex,localPath,isPrimary,syncStatus,createdAt,originalPath,stampedPath,generatedFileName,driveFolderId,driveFileId) VALUES ('oldphoto','bridge',1,'/final.jpg',1,'SYNCED',1,'/original.jpg','/stamp.jpg','keep.jpg','folder','file')")
            old.version=5
        }
        val db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_5_6,InventoryDatabase.MIGRATION_6_7).build()
        try {
            val s=db.inventoryDao().snapshot("bridge")!!
            assertEquals("ACTIVE",s.record.status);assertEquals("Tramo 1",s.record.segment);assertEquals("DECREASING",s.record.surveyDirection)
            assertEquals("Rectangular",s.sic18!!.sectionShape);assertEquals(10.0,s.sic18!!.structuralDamagePercent!!,0.0)
            assertEquals(20.0,s.sic18!!.functionalObstructionPercent!!,0.0)
            assertEquals("ANNULLED",db.inventoryDao().recordById("annulled")!!.status);assertEquals("DRAFT",db.inventoryDao().recordById("draft")!!.status)
            val p=s.photos.single();assertEquals("/original.jpg",p.originalPath);assertEquals("/stamp.jpg",p.stampedPath)
            assertEquals("keep.jpg",p.generatedFileName);assertEquals("folder",p.driveFolderId);assertEquals("file",p.driveFileId)
            assertEquals("SYNCED",p.syncStatus);assertNull(p.scapInspectionId);assertNull(p.scapElementCode);assertNull(p.photoCategory)
            assertNotNull(ScapRepository(db,testScapCatalog()).create("New inspector","device",null))
        } finally {db.close();context.deleteDatabase(name)}
    }
}
