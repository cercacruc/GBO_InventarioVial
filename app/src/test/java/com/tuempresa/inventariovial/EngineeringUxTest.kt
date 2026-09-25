package com.tuempresa.inventariovial

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.catalog.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.field.SurveyPreferences
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.*
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.supplementary.*
import com.tuempresa.inventariovial.validation.CaptureValidation
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class EngineeringUxTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun exporter()=ScapExcelExporter({context.assets.open("scap_template.xlsx")},context.assets.open("scap_field_map.json").bufferedReader().use {it.readText()})
    private fun request(detail:SicFormDetail)=InventorySaveRequest("SIC-20","MURO","PE-3N","CD","12","350",null,null,null,-12.0,-77.0,1f,"25/09/2026","","",detail)
    private fun form18a()=SupplementaryFormState(SupplementaryFormat.SIC18A,mapOf("classCode" to "06","typeCode" to "1","spans" to "2","functionCode" to "1","failureLocationCode" to "4","failureTypeCode" to "5","functionalStateCode" to "2","probableCauseCode" to "3"))

    @Test fun materialCriteriaCoverAllFourCasesAndOtherIsNeverGuessed() {
        assertEquals("EARTH",EngineeringConditions.criterion("1",null))
        for(type in listOf("2","3")) assertEquals("PAVED",EngineeringConditions.criterion(type,"EARTH"))
        assertNull(EngineeringConditions.criterion("4",null));assertNull(EngineeringConditions.criterion("4","unknown"))
        assertEquals("Problema de erosión.",EngineeringConditions.structural("1",null)["2"])
        assertTrue(EngineeringConditions.structural("3",null)["3"]!!.contains("30%"))
        assertTrue(CaptureValidation.errors(request(SicFormDetail.Sic19(Sic19FormState(typeCode="4")))).any {it.field=="structuralCriterion"})
    }
    @Test fun allThreeSic20ClassesRequireAndExportFunctionalCondition() {
        for(cls in listOf("12","13","14")) {
            val state=Sic20FormState(classCode=cls,functionalConditionCode="3")
            assertTrue(state.usesFunctionalCondition)
            assertTrue(CaptureValidation.errors(request(SicFormDetail.Sic20(state.copy(functionalConditionCode="")))) .any {it.field=="functionalConditionCode"})
            val item=InventoryRecordForExport(testRecord().copy(sicCode="SIC-20"),sic20=Sic20Entity("id",cls,"2",2.0,4.0,"2","3",19.8))
            val values=SicExportFormat.SIC20.values(item)
            assertEquals("3",values[12]);assertEquals(2.0,values[9]);if(cls=="14")assertNull(values[10])
            assertFalse(values.contains(19.8))
        }
    }
    @Test fun noApplyIsAdditionalAndCatalogCodesStayUnchanged() {
        val cat=testScapCatalog()
        val field=cat.fields.first {it.key=="railingType"}
        assertEquals(listOf("Postes y pasamanos","Parapeto","Guardavias","No hay"),cat.options(field.catalog))
        assertTrue(ScapFieldPolicy.allows(field));assertFalse(cat.options(field.catalog).contains(ScapFieldPolicy.NOT_APPLICABLE))
        assertNull(ScapValidation.fieldError(field,ScapFieldPolicy.NOT_APPLICABLE,emptyMap(),cat))
        assertFalse(ScapFieldPolicy.enabled("railingMaterial","inspection",emptyMap()))
        assertTrue(ScapFieldPolicy.enabled("railingMaterial","inspection",mapOf("railingType" to "Parapeto")))
        assertFalse(ScapFieldPolicy.enabled("railingMaterial","inspection",mapOf("railingType" to ScapFieldPolicy.NOT_APPLICABLE)))
    }
    @Test fun detailOrderAndCompletionDistinguishUnselectedIncompleteAndNoApply() {
        assertEquals(listOf("A · Barandas","B · Veredas y Sardineles","C · Apoyos","D · Juntas de Expansión","E · Drenaje de Calzada"),ScapFieldPolicy.detailGroups.keys.toList())
        val keys=ScapFieldPolicy.detailGroups.values.first()
        assertEquals("○ Sin completar",ScapFieldPolicy.state(keys,"inspection",emptyMap()){true})
        assertEquals("! Revisión",ScapFieldPolicy.state(keys,"inspection",mapOf("railingType" to "Parapeto")){true})
        assertEquals("— No aplica",ScapFieldPolicy.state(keys,"inspection",mapOf("railingType" to ScapFieldPolicy.NOT_APPLICABLE)){true})
        assertEquals("✓ Completado",ScapFieldPolicy.state(keys,"inspection",keys.associateWith {"valid"}){true})
    }
    @Test fun noApplyPreservesDependentDataAcrossReopenAndBlanksItsExport()=runBlocking {
        val name="uxscap.db";context.deleteDatabase(name)
        var database=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {
            var repo=ScapRepository(database,testScapCatalog());val id=repo.create("Inspector","device",null)
            repo.setField(id,"inspection","railingType","Parapeto");repo.setField(id,"inspection","railingMaterial","Acero")
            assertTrue(ScapFieldPolicy.hasDependents("railingType","inspection",repo.dao.snapshot(id)!!.values()))
            repo.setField(id,"inspection","railingType",ScapFieldPolicy.NOT_APPLICABLE)
            database.close();database=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build();repo=ScapRepository(database,testScapCatalog())
            val s=repo.dao.snapshot(id)!!;assertEquals("Acero",s.values()["railingMaterial"])
            val out=ByteArrayOutputStream();exporter().write(out,s){error("media")};val w=TemplateWorkbook(out.toByteArray().inputStream())
            assertEquals("",w.cell(1,"E229").textContent);assertEquals("",w.cell(1,"E231").textContent)
            assertFalse(ScapValidation.review(s,repo.catalog).pending.any {it.contains("BARANDAS")})
            repo.setField(id,"inspection","railingType","Parapeto");assertEquals("Acero",repo.dao.snapshot(id)!!.values()["railingMaterial"])
        } finally {database.close();context.deleteDatabase(name)}
    }
    @Test fun multipleJointsAndSupportsPersistAndOnlyExportCapacityIsBlocked()=runBlocking {
        val database=db()
        try {
            val r=ScapRepository(database,testScapCatalog());val id=r.create("I","D",null)
            repeat(3){r.addRow(id,"BEARING");r.addRow(id,"JOINT")}
            var s=r.dao.snapshot(id)!!;assertEquals(3,s.supports.size);assertEquals(3,s.joints.size)
            r.setField(id,s.joints.first().id,"jointType","Vacio");r.setField(id,s.joints.first().id,"jointMaterial","Jebe")
            s=r.dao.snapshot(id)!!;assertEquals("Vacio",s.joints.first().type)
            assertTrue(exporter().review(s).errors.any {it.contains("junta")})
            assertTrue(exporter().review(s).errors.any {it.contains("apoyos")})
            r.removeRow(id,s.joints.last().id);assertEquals(2,r.dao.snapshot(id)!!.joints.size)
        } finally {database.close()}
    }
    @Test fun jointNoApplyUsesTemplateLiteralAndDoesNotExportPreservedMaterial()=runBlocking {
        val database=db()
        try {
            val r=ScapRepository(database,testScapCatalog());val id=r.create("I","D",null);r.addRow(id,"JOINT")
            val j=r.dao.snapshot(id)!!.joints.single();r.setField(id,j.id,"jointMaterial","Jebe");r.setField(id,j.id,"jointType",ScapFieldPolicy.NOT_APPLICABLE)
            val out=ByteArrayOutputStream();exporter().write(out,r.dao.snapshot(id)!!){error("media")}
            val w=TemplateWorkbook(out.toByteArray().inputStream());assertEquals("No Aplica",w.cell(1,"H249").textContent);assertEquals("",w.cell(1,"H251").textContent)
        } finally {database.close()}
    }
    @Test fun sketchDimensionsPersistButNeverEnterScapOrSicProjection()=runBlocking {
        val database=db()
        try {
            val r=ScapRepository(database,testScapCatalog());val id=r.create("I","D",null)
            r.setField(id,"inspection","internal.sketch.bridgeWidth","987.654")
            val s=r.dao.snapshot(id)!!;assertEquals("987.654",s.values()["internal.sketch.bridgeWidth"])
            assertFalse(ScapToSicMapper.mapToSic17(s).fields.any {it.value=="987.654"})
            val out=ByteArrayOutputStream();exporter().write(out,s){error("media")}
            val w=TemplateWorkbook(out.toByteArray().inputStream());assertFalse(w.parts.filterKeys {it.endsWith(".xml")}.values.any {String(it).contains("987.654")})
        } finally {database.close()}
    }
    @Test fun scapRouteUsesProjectCatalogAndPersistsToExport()=runBlocking {
        val database=db();val prefs=SurveyPreferences(context)
        try {
            prefs.saveRoutes("Tramo 1",listOf("PE-3N","PE-28H"))
            val r=ScapRepository(database,testScapCatalog());val id=r.create("I","D",null)
            r.setField(id,"inspection","route",prefs.routes("Tramo 1").last())
            val s=r.dao.snapshot(id)!!;assertEquals("PE-28H",s.values()["route"])
            prefs.saveRoutes("Tramo 1",listOf("PE-22A"));assertEquals(listOf("PE-22A"),prefs.routes("Tramo 1"))
            assertEquals("PE-28H",r.dao.snapshot(id)!!.values()["route"])
            val out=ByteArrayOutputStream();exporter().write(out,s){error("media")};assertTrue(TemplateWorkbook(out.toByteArray().inputStream()).sheet(1).documentElement.textContent.contains("PE-28H"))
        } finally {database.close();context.getSharedPreferences("survey_context",Context.MODE_PRIVATE).edit().clear().commit()}
    }
    @Test fun sic18aIsSuggestedButNeverCreatedWithoutExplicitSave()=runBlocking {
        val database=db()
        try {
            val r=testRecord().copy(sicCode="SIC-18");database.inventoryDao().insertRecord(r)
            database.inventoryDao().insertSic18(Sic18Entity(r.id,"06","1",2,"2",1.0,null,"3","1","CIRCULAR"))
            assertTrue(EngineeringConditions.badCulvert("3","1"));assertTrue(EngineeringConditions.badCulvert("1","3"));assertFalse(EngineeringConditions.badCulvert("1","2"))
            assertNull(database.inventoryDao().snapshot(r.id)!!.sic18a)
            InventoryRepository(database).saveSupplementary(r.id,form18a())
            val a=database.inventoryDao().snapshot(r.id)!!.sic18a!!;assertEquals(r.id,a.recordId);assertEquals("4",a.failureLocationCode)
            assertTrue(runCatching {InventoryRepository(database).saveSupplementary(r.id,form18a().copy(values=form18a().values+("spans" to "9")))}.isFailure)
        } finally {database.close()}
    }
    @Test fun sic18aCatalogValidationAndXlsxKeepOfficialColumnsAndInheritedHeader() {
        assertTrue(form18a().validate().isEmpty());assertFalse(SupplementaryFormState(SupplementaryFormat.SIC18A).validate().isEmpty())
        assertFalse(form18a().copy(values=form18a().values+("functionCode" to "5")).validate().isEmpty())
        val r=testRecord().copy(sicCode="SIC-18")
        val values=Sic18AExporter.values(r,form18a());assertEquals(13,values.size);assertEquals(r.routeCode,values[0]);assertEquals(r.roadbedCode,values[1]);assertEquals(2,values[6])
        val out=ByteArrayOutputStream();Sic18AExporter.write(out,r,form18a())
        val w=TemplateWorkbook(out.toByteArray().inputStream());assertTrue(w.sheet(1).documentElement.textContent.contains("SIC-18A"));assertTrue(w.sheet(1).documentElement.textContent.contains("Causa probable"))
    }
    @Test fun editedWallAndMaterialCriterionRoundTripWithoutExtraSicColumns()=runBlocking {
        val database=db()
        try {
            val dao=database.inventoryDao();val r=testRecord().copy(sicCode="SIC-20");dao.insertRecord(r);dao.insertSic20(Sic20Entity(r.id,"14","2",3.0,null,"1",null))
            EngineeringEdits.save(database,r.id,SicFormDetail.Sic20(Sic20FormState("14","2","3.0","","2","3","123.45")),emptyList(),emptyMap())
            val s=dao.snapshot(r.id)!!;assertEquals(123.45,s.sic20!!.wallLengthMeters!!,0.0);assertEquals("3",s.sic20!!.functionalConditionCode)
            assertEquals("123.45",(EngineeringEdits.form(s) as SicFormDetail.Sic20).state.wallLengthMeters)
            val values=SicExportFormat.SIC20.values(InventoryRecordForExport(s.record,sic20=s.sic20));assertFalse(values.contains(123.45));assertEquals(14,values.size)
            val d=r.copy(id="ditch",sicCode="SIC-19");dao.insertRecord(d);dao.insertSic19(Sic19Entity(d.id,"08","4","1","1","1"))
            EngineeringEdits.save(database,d.id,SicFormDetail.Sic19(Sic19FormState(typeCode="4",structuralCriterion="EARTH",structuralConditionCode="3")),emptyList(),emptyMap())
            val ds=dao.snapshot(d.id)!!;assertEquals("EARTH",ds.sic19!!.structuralCriterion)
            val dv=SicExportFormat.SIC19.values(InventoryRecordForExport(ds.record,sic19=ds.sic19));assertTrue(dv.contains("3"));assertFalse(dv.contains("EARTH"))
        } finally {database.close()}
    }
    @Test fun photoCategoriesRoundTripWithoutRenamingHistoricalDriveFiles()=runBlocking {
        val database=db()
        try {
            val dao=database.inventoryDao();val r=testRecord().copy(sicCode="SIC-18");dao.insertRecord(r)
            dao.insertSic18(Sic18Entity(r.id,"06","1",1,"2",1.0,null,"1","1","CIRCULAR"))
            val original=PhotoEntity("p",r.id,1,"/old.jpg",true,"KEEP.jpg","folder","file","SYNCED",1,originalPath="/raw.jpg",stampedPath="/stamp.jpg")
            dao.insertPhoto(original)
            val d=EngineeringEdits.form(dao.snapshot(r.id)!!)
            EngineeringEdits.save(database,r.id,d,listOf("/old.jpg"),mapOf("/old.jpg" to "CULVERT_INLET"))
            val p=dao.snapshot(r.id)!!.photos.single();assertEquals(original.copy(photoCategory="CULVERT_INLET"),p)
        } finally {database.close()}
    }
    @Test fun photoDescriptionsReorderingAndRemovalKeepDefectsConsistent()=runBlocking {
        val database=db()
        try {
            val r=ScapRepository(database,testScapCatalog());val id=r.create("I","D",null)
            r.addPhoto(id,"/a.jpg","GENERAL",null);r.addPhoto(id,"/b.jpg","GENERAL",null)
            val p=r.dao.snapshot(id)!!.photos.minBy {it.photoIndex};r.describePhoto(id,p.id,"Vista general")
            r.movePhoto(id,p.id,1);assertEquals(2,r.dao.snapshot(id)!!.photos.single {it.id==p.id}.photoIndex)
            assertEquals("Vista general",r.dao.snapshot(id)!!.photos.single {it.id==p.id}.description)
            r.saveDefect(id,ScapDefectEntity("defect",id,null,"Grieta","",p.id,false,true,1))
            r.removePhoto(id,p.id);assertEquals(1,r.dao.snapshot(id)!!.photos.size);assertNull(r.dao.snapshot(id)!!.defects.single().photoId)
        } finally {database.close()}
    }
    @Test fun versionSixMigrationKeepsAllHistoricalDataAndPromotesLegacyJoint()=runBlocking {
        val name="uxv6.db";context.deleteDatabase(name);val file=context.getDatabasePath(name).apply {parentFile!!.mkdirs()}
        SQLiteDatabase.openOrCreateDatabase(file,null).use {old->
            val entities=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/6.json").readText()).getJSONObject("database").getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val e=entities.getJSONObject(i);val table=e.getString("tableName")
                old.execSQL(e.getString("createSql").replace("\u0024{TABLE_NAME}",table))
                val indices=e.optJSONArray("indices") ?: org.json.JSONArray();for(j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}",table))
            }
            old.execSQL("""INSERT INTO inventory_records (id,sicCode,assetType,routeCode,roadbedCode,startPrCode,startDistanceM,latitude,longitude,surveyDate,status,photoSyncStatus,excelSyncStatus,createdAt,updatedAt) VALUES ('road','SCAP','PUENTE','PE-3N','CD','12',350,-12,-77,'25/09/2026','DRAFT','PENDING','PENDING',1,1)""")
            old.execSQL("INSERT INTO scap_inspections VALUES ('s','road','Puente','P',1,1,'I','D','IN_PROGRESS','PENDING',NULL,NULL,NULL,NULL,'MANUAL')")
            old.execSQL("INSERT INTO scap_values VALUES ('s','inspection','jointType','Vacio','MANUAL'),('s','inspection','jointMaterial','Jebe','MANUAL')")
            old.execSQL("INSERT INTO photos(id,recordId,photoIndex,localPath,isPrimary,syncStatus,createdAt,originalPath,stampedPath,generatedFileName,driveFolderId,driveFileId) VALUES ('p','road',1,'/final.jpg',1,'SYNCED',1,'/raw.jpg','/stamp.jpg','KEEP.jpg','folder','file')")
            old.version=6
        }
        val database=Room.databaseBuilder(context,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_6_7).build()
        try {
            val s=database.scapDao().snapshot("s")!!;assertEquals("Vacio",s.joints.single().type);assertEquals("Jebe",s.values(s.joints.single().id)["jointMaterial"])
            assertEquals("Vacio",s.values()["jointType"])
            val p=database.inventoryDao().snapshot("road")!!.photos.single();assertEquals("KEEP.jpg",p.generatedFileName);assertEquals("SYNCED",p.syncStatus);assertEquals("/raw.jpg",p.originalPath);assertEquals("/stamp.jpg",p.stampedPath);assertNull(p.description);assertNull(p.photoCategory)
        } finally {database.close();context.deleteDatabase(name)}
    }
}
