package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.repository.InventoryRepository
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.supplementary.*
import com.tuempresa.inventariovial.export.Sic18AExporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class Engineering2DataTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun exporter()=ScapExcelExporter({context.assets.open("scap_template.xlsx")},context.assets.open("scap_field_map.json").bufferedReader().use {it.readText()})
    @Test fun sevenPiersFixedStructuresAndObservationsSurviveReopenWithoutMaterializingReminders()=runBlocking {
        val name="engineering2.db";context.deleteDatabase(name)
        var db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {
            var repo=ScapRepository(db,ScapCatalog.load(context));val id=repo.create("I","D",null)
            val empty=repo.dao.snapshot(id)!!
            assertEquals(14,ScapPresentation.visibleElements(empty).size)
            assertTrue(ScapPresentation.visibleElements(empty).all {ScapPresentation.presence(empty,it)=="Sin completar"})
            assertEquals(4,ScapPresentation.withVisibleStructures(empty).substructures.size)
            assertTrue(repo.dao.snapshot(id)!!.substructures.isEmpty());assertTrue(repo.dao.snapshot(id)!!.elements.isEmpty())
            for(kind in ScapPresentation.fixedStructures) repo.setField(id,ScapPresentation.fixedId(id,kind),"elevationType",ScapFieldPolicy.NOT_APPLICABLE)
            repeat(7) {repo.addRow(id,"PIER")}
            repo.dao.snapshot(id)!!.substructures.filter {it.kind=="PIER"}.forEach {repo.setField(id,it.id,"soil","Suelo distinto ${it.elementIndex}")}
            repeat(3) {repo.addRow(id,"JOINT");repo.addRow(id,"BEARING")}
            repo.addElementForReview(id,"112");repo.addElementForReview(id,"112")
            repo.selectElement(id,"104",true)
            val element=repo.dao.snapshot(id)!!.elements.single {it.element.elementCode=="104"}.element
            for(i in 0..5) repo.setField(id,element.id,"percent$i",if(i==2) "100" else "0")
            repo.selectElement(id,"104",false)
            repo.addPhoto(id,"/original/one.jpg","DEFECT","104")
            val photo=repo.dao.snapshot(id)!!.photos.single()
            val observation=ScapDefectEntity("draft-observation",id,"104","","",photo.id,false,true,0)
            repo.setDefectField(id,observation.id,"description","Fisura longitudinal",observation)
            repo.setDefectField(id,observation.id,"locationDescription","Aguas abajo",observation)
            repo.saveDefect(id,observation.copy(id="other",description="Otra observación"))
            repo.saveDefect(id,observation.copy(id="historical-general",elementCode=null,description="Histórica sin elemento"))
            for(category in ScapPhotoCategories.labels.keys) repo.addPhoto(id,"/original/$category.jpg",category,null)
            db.close();db=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build();repo=ScapRepository(db,ScapCatalog.load(context))
            val s=repo.dao.snapshot(id)!!
            assertEquals((1..7).map {"Suelo distinto $it"},s.substructures.filter {it.kind=="PIER"}.sortedBy {it.elementIndex}.map {it.soil})
            assertEquals(11,s.substructures.size);assertEquals(3,s.supports.size);assertEquals(3,s.joints.size)
            assertEquals(15,ScapPresentation.visibleElements(s).size);assertEquals(2,s.elements.size)
            assertEquals("Sin completar",ScapPresentation.presence(s,"112"));assertNull(s.elements.single {it.element.elementCode=="112"}.condition)
            assertEquals(100.0,s.elements.single {it.element.elementCode=="104"}.condition!!.percent2!!,0.0)
            assertEquals("Fisura longitudinal",s.defects.single {it.id==observation.id}.description)
            assertEquals("Aguas abajo",s.defects.single {it.id==observation.id}.locationDescription)
            assertEquals(3,s.defects.size);assertTrue(s.defects.any {it.elementCode==null})
            assertTrue(s.photos.mapNotNull {it.photoCategory}.containsAll(ScapPhotoCategories.labels.keys))
            assertEquals("/original/one.jpg",s.photos.single {it.id==photo.id}.originalPath)
            assertEquals(7,db.openHelper.readableDatabase.version)
        } finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun complementarySic17RequiredFieldsPersistEnableExportAndNeverLeakIntoScap()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val repo=ScapRepository(db,ScapCatalog.load(context));val id=repo.create("I","D",null)
            mapOf("bridgeName" to "PUENTE SIC TEST","bridgeCode" to "P-7","route" to "PE-3N","progressive" to "45+120","lastInspection" to "25/09/2026","totalLengthM" to "20").forEach {(k,v)->repo.setField(id,"inspection",k,v)}
            repo.addRow(id,"SPAN")
            var s=repo.dao.snapshot(id)!!
            assertEquals(8,ScapSicExporter.previews(s).single {it.format=="SIC-17"}.missingRequired.size)
            val complement=mapOf("roadbedCode" to "UC","classCode" to "01","typeCode" to "2","inventoriedCode" to "S","structuralConditionCode" to "2","functionalConditionCode" to "3","serviceTypeCode" to "1","singularityCode" to "2","singularityName" to "SIC_ONLY_SENTINEL")
            complement.forEach {(key,value)->repo.setField(id,"inspection",ScapSicSupplement.PREFIX+key,value)}
            s=repo.dao.snapshot(id)!!
            assertTrue(ScapSicExporter.previews(s).all {it.missingRequired.isEmpty()})
            val out=ByteArrayOutputStream();ScapSicExporter.write(out,s,"SIC-17")
            val w=TemplateWorkbook(out.toByteArray().inputStream())
            assertTrue(w.sheet(1).documentElement.textContent.contains("SIC_ONLY_SENTINEL"))
            val scap=ByteArrayOutputStream();exporter().write(scap,s){error("media")}
            assertFalse(TemplateWorkbook(scap.toByteArray().inputStream()).parts.filterKeys {it.endsWith(".xml")}.values.any {String(it).contains("SIC_ONLY_SENTINEL")})
            repo.setField(id,"inspection","sic17.typeCode","99")
            assertTrue(ScapSicExporter.previews(repo.dao.snapshot(id)!!).single {it.format=="SIC-17"}.missingRequired.contains("Tipo"))
        } finally {db.close()}
    }
    @Test fun sic18aHasThreeStatesEditsSameRecordAndExportsCurrentInheritedData()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val record=testRecord().copy(sicCode="SIC-18");db.inventoryDao().insertRecord(record)
            val parent=Sic18Entity(record.id,"06","1",2,"2",1.0,null,"3","1","CIRCULAR")
            db.inventoryDao().insertSic18(parent)
            assertEquals("Pendiente",Sic18AStatus.label(parent,null))
            assertEquals("No requerido",Sic18AStatus.label(parent.copy(structuralConditionCode="1"),null))
            val form=SupplementaryFormState(SupplementaryFormat.SIC18A,Sic18AStatus.inherited(parent,null)+mapOf("functionCode" to "1","failureLocationCode" to "4","failureTypeCode" to "5","functionalStateCode" to "2","probableCauseCode" to "3"))
            val repo=InventoryRepository(db);repo.saveSupplementary(record.id,form)
            var saved=db.inventoryDao().snapshot(record.id)!!.sic18a!!
            assertEquals("Completo",Sic18AStatus.label(parent,saved))
            repo.saveSupplementary(record.id,form.copy(values=form.values+("probableCauseCode" to "4")))
            saved=db.inventoryDao().snapshot(record.id)!!.sic18a!!;assertEquals("4",saved.probableCauseCode)
            val out=ByteArrayOutputStream();Sic18AExporter.write(out,record,form.copy(values=saved.values()))
            val w=TemplateWorkbook(out.toByteArray().inputStream());assertTrue(w.sheet(1).documentElement.textContent.contains(record.routeCode))
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sic18a_details").use {it.moveToFirst();assertEquals(1,it.getInt(0))}
        } finally {db.close()}
    }
    @Test fun editingOnePercentagePreservesHistoricalConditionsWithoutValueRows():Unit=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val repo=ScapRepository(db,ScapCatalog.load(context));val id=repo.create("I","D",null)
            repo.selectElement(id,"104",true)
            val element=repo.dao.snapshot(id)!!.elements.single().element
            repo.dao.putCondition(ScapElementConditionEntity("legacy",element.id,0.0,80.0,20.0,0.0,0.0,0.0))
            repo.setField(id,element.id,"percent0","5")
            val c=repo.dao.snapshot(id)!!.elements.single().condition!!
            assertEquals(5.0,c.percent0!!,0.0);assertEquals(80.0,c.percent1!!,0.0);assertEquals(20.0,c.percent2!!,0.0)
        } finally {db.close()}
    }

}
