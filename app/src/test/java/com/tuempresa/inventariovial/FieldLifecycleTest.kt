package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.gis.KmlExporter
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.*
import com.tuempresa.inventariovial.scap.export.TemplateWorkbook
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class FieldLifecycleTest {
    @Test fun fieldRecordsReopenEditAndExport() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="lifecycle.db"
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        var db=open()
        try {
            val repo=InventoryRepository(db)
            repo.saveSic18(testRecord("culvert",sic="SIC-18"),Sic18Entity("culvert","06","1",1,"2",1.2,0.8,"1","1","CIRCULAR"),emptyList())
            repo.saveSic19(testRecord("ditch",sic="SIC-19"),Sic19Entity("ditch","08","1","1","1","1"),emptyList())
            repo.saveSic20(testRecord("wall",sic="SIC-20"),Sic20Entity("wall","14","2",3.0,null,"1","1",35.0),emptyList())
            repo.saveSic20(testRecord("tunnel",sic="SIC-20"),Sic20Entity("tunnel","13","1",4.0,5.0,"1","1"),emptyList())
            repo.saveSic21(testRecord("horizontal",sic="SIC-21"),Sic21Entity("horizontal","18","1","5","1"),emptyList())
            repo.saveSic21(testRecord("security",sic="SIC-21"),Sic21Entity("security","19","1","2","1"),emptyList())
            repo.saveSic22(testRecord("vertical",sic="SIC-22"),Sic22Entity("vertical","20","3","2","I-1",null,"1",null,null,null),emptyList())
            repo.saveSic22(testRecord("legacy",sic="SIC-22"),Sic22Entity("legacy","20","4","3",null,"54","1",1.0,2.0,3.0),emptyList())
            db.close();db=open()
            val ids=listOf("culvert","ditch","wall","tunnel","horizontal","security","vertical","legacy")
            for(id in ids) {
                val s=db.inventoryDao().snapshot(id)!!
                val changed=when(val d=EngineeringEdits.form(s)) {
                    is SicFormDetail.Sic18->d.copy(state=d.state.copy(functionalConditionCode="2"))
                    is SicFormDetail.Sic19->d.copy(state=d.state.copy(functionalConditionCode="2"))
                    is SicFormDetail.Sic20->d.copy(state=d.state.copy(functionalConditionCode="2"))
                    is SicFormDetail.Sic21->d.copy(state=d.state.copy(conditionCode="2"))
                    is SicFormDetail.Sic22->d.copy(state=d.state.copy(conditionCode="2"))
                    else->error("fixture")
                }
                EngineeringEdits.save(db,id,changed,emptyList(),emptyMap())
            }
            db.close();db=open()
            val rows=db.inventoryDao().recordsForExport(null,null)
            assertEquals(ids.toSet(),rows.map {it.record.id}.toSet())
            rows.forEach {item->
                val format=SicExportFormat.entries.single {it.code==item.record.sicCode}
                val values=format.values(item)
                val condition=if(format in listOf(SicExportFormat.SIC21,SicExportFormat.SIC22)) "Condición" else "Condición funcional"
                assertEquals("2",values[format.columns.indexOfFirst {it.label==condition}])
                val output=ByteArrayOutputStream();SicExcelWriter.write(output,listOf(item),format,ExportHeading())
                val workbook=TemplateWorkbook(output.toByteArray().inputStream())
                workbook.parts.keys.filter {it.endsWith(".xml")}.forEach {workbook.document(it)}
                assertEquals(0.005,item.record.longitude,0.0)
            }
            val culvert=rows.single {it.record.id=="culvert"}; assertEquals(0.8,culvert.sic18!!.dimension2M!!,0.0)
            assertNull(SicExportFormat.SIC18.values(culvert)[9])
            val horizontal=rows.single {it.record.id=="horizontal"}
            assertEquals("5",horizontal.sic21!!.materialCode); assertNull(SicExportFormat.SIC21.values(horizontal)[9])
            assertEquals("2",SicExportFormat.SIC21.values(rows.single {it.record.id=="security"})[9])
            val legacy=rows.single {it.record.id=="legacy"}.sic22!!
            assertEquals("4",legacy.typeCode);assertEquals("54",legacy.kilometerPostNumber);assertEquals(3.0,legacy.lowerEdgeHeightM!!,0.0)
            assertEquals(35.0,rows.single {it.record.id=="wall"}.sic20!!.wallLengthMeters!!,0.0)
        } finally {db.close();context.deleteDatabase(name)}
    }

    @Test fun gisSharesNormalizedMaterialAndAssociatesPhotosWithoutExposingPrivatePaths() {
        val r=testRecord("sign",sic="SIC-21")
        val p=PhotoEntity("photo",r.id,1,"/private/original.jpg",true,null,"folder","drive-photo-id","SYNCED",1,photoCategory="GENERAL")
        val s=testSnapshot(r).copy(sic19=null,sic21=Sic21Entity(r.id,"18","1","5","2"),photos=listOf(p))
        val xml=KmlExporter.render(listOf(s))
        assertTrue(xml.contains("drive-photo-id"));assertTrue(xml.contains("foto_1_UUID"))
        assertFalse(xml.contains("/private/"));assertFalse(xml.contains("SIC_10_Material"))
        assertTrue(xml.contains("SIC_11_Condición"));assertTrue(xml.contains("<value>2</value>"))
    }
}
