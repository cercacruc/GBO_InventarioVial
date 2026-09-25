package com.tuempresa.inventariovial

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ScapExportTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun exporter()=ScapExcelExporter({context.assets.open("scap_template.xlsx")},context.assets.open("scap_field_map.json").bufferedReader().use{it.readText()})
    private fun snapshot():ScapInspectionSnapshot {
        val inspection=ScapInspectionEntity("12345678-1234-1234-1234-123456789000","road","Puente de prueba Ñ","P-TEST",0,0,"Inspector de prueba","device","IN_PROGRESS","PENDING",-12.0,-77.0,null,null,"MANUAL")
        val values=mapOf("bridgeName" to inspection.bridgeName,"bridgeCode" to inspection.bridgeCode,"route" to "PE-28H","progressive" to "42+125.50","totalLengthM" to "18.4","lastInspection" to "24/09/2026","constructionYear" to "2001")
        val elements=listOf("104","110").mapIndexed{i,code->ScapElementWithCondition(ScapElementEntity("e$i",inspection.id,code,"Elemento $code",10.0+i,"m3",if(i==0)0.6 else 1.0,"SUPERESTRUCTURA",true),ScapElementConditionEntity("c$i","e$i",0.0,90.0,10.0,0.0,0.0,0.0))}
        return ScapInspectionSnapshot(inspection,values.map{ScapFieldValueEntity(inspection.id,"inspection",it.key,it.value,"MANUAL")},elements=elements)
    }
    private fun workbook(s:ScapInspectionSnapshot)=ByteArrayOutputStream().also{exporter().write(it,s){error("No media expected")}}.toByteArray()
    @Test fun runtimeIncludesExactTemplateAndExportPreservesSheetsStylesGeometryAndCalculationFormulas() {
        val template=context.assets.open("scap_template.xlsx").use{it.readBytes()}
        assertEquals("1df9f651722d06d6cd886c9be3edf565df7aae8b6fca10097011f48b12430b13",MessageDigest.getInstance("SHA-256").digest(template).joinToString(""){"%02x".format(it)})
        val bytes=workbook(snapshot());val original=TemplateWorkbook(template.inputStream());val generated=TemplateWorkbook(bytes.inputStream())
        assertArrayEquals(original.parts["xl/styles.xml"],generated.parts["xl/styles.xml"])
        fun geometry(w:TemplateWorkbook,n:Int)=w.sheet(n).nodes("mergeCell").map{it.getAttribute("ref")} + w.sheet(n).nodes("col").map{listOf("min","max","width","hidden").map(it::getAttribute).joinToString()} + w.sheet(n).nodes("row").map{listOf("r","ht","customHeight","hidden").map(it::getAttribute).joinToString()}
        for(i in 1..10) assertEquals("Geometry sheet $i",geometry(original,i),geometry(generated,i))
        assertEquals(original.document("xl/workbook.xml").nodes("sheet").map{it.getAttribute("name")},generated.document("xl/workbook.xml").nodes("sheet").map{it.getAttribute("name")})
        original.sheet(6).nodes("c").filter{it.children().any{c->c.localName=="f"} && !it.getAttribute("r").matches(Regex("C(1[4-9]|2[0-7])"))}.forEach{assertEquals(it.getAttribute("r"),original.formula(6,it.getAttribute("r")),generated.formula(6,it.getAttribute("r")))}
        assertEquals("1",generated.document("xl/workbook.xml").nodes("calcPr").single().getAttribute("fullCalcOnLoad"))
        assertFalse(generated.parts.containsKey("xl/calcChain.xml"))
        assertEquals("18.4",generated.cell(1,"E31").textContent)
        assertEquals("42125.5",generated.cell(1,"P17").textContent)
        assertEquals("Puente de prueba Ñ",generated.cell(1,"J9").textContent)
        assertEquals("",generated.cell(1,"E13").textContent)
        assertEquals("",generated.cell(7,"B39").textContent)
        assertEquals("10.0",generated.cell(6,"C14").textContent)
        val visible=generated.parts.filterKeys{it.endsWith(".xml")}.values.joinToString{String(it)}
        // Retained for independent OpenXML/visual QA; not presented as a real inspection.
        File("../tmp/engineering").mkdirs();File("../tmp/engineering/export-validation.xlsx").writeBytes(bytes)
        assertFalse(visible.contains("PUENTE AGUA BLANCA"));assertFalse(visible.contains("Ing. Martín Pichón"));assertFalse(visible.contains("M.Sc. Ing. Nelson Pareja"))
    }
    @Test fun blankInspectionNeverRetainsExampleInputsOrMedia() {
        val s=snapshot().copy(values=emptyList(),elements=emptyList())
        val w=TemplateWorkbook(workbook(s).inputStream())
        for(ref in listOf("E31","E33","P15","E337","E392","G461","U461")) assertEquals(ref,"",w.cell(1,ref).textContent)
        for(ref in listOf("A14","C14","F14","G14","A27","C27")) assertEquals(ref,"",w.cell(6,ref).textContent)
        for(i in 13..44) assertEquals("",w.cell(5,"D$i").textContent)
        for(path in w.parts.keys.filter{it.startsWith("xl/drawings/") && it.endsWith(".xml")}) {
            val doc=w.document(path)
            assertFalse(doc.nodes("cNvPr").any{it.getAttribute("descr").contains("Agua Blanca",true)})
        }
    }
    @Test fun suppliedPhotoAndSketchReplaceOnlyTemplateBodyImages() {
        val s=snapshot();val p=PhotoEntity("p",s.inspection.roadRecordId,1,"photo",true,null,null,null,"PENDING",0,scapInspectionId=s.inspection.id,photoCategory="GENERAL")
        val image=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7f8AAAAASUVORK5CYII=")
        val out=ByteArrayOutputStream()
        exporter().write(out,s.copy(photos=listOf(p),sketches=listOf(ScapSketchEntity("sk",s.inspection.id,"PLAN","sketch")))){image}
        val w=TemplateWorkbook(out.toByteArray().inputStream())
        assertArrayEquals(image,w.parts["xl/media/scap_5_1.png"]);assertArrayEquals(image,w.parts["xl/media/scap_2_2.png"])
        assertEquals("GENERAL",w.cell(5,"D13").textContent)
        assertEquals("1",w.cell(5,"H1").textContent)
    }
    @Test fun templateCapacityIsCheckedWithoutModifyingOrTruncatingSavedInspection() {
        val s=snapshot();val extended=s.copy(elements=(0..3).map{s.elements[0].copy(element=s.elements[0].element.copy(id="e$it",elementCode=(101+it).toString()))})
        assertTrue(exporter().review(extended).errors.any{it.contains("SUPERESTRUCTURA")})
        assertTrue(runCatching{workbook(extended)}.isFailure);assertEquals(4,extended.elements.size)
    }
    @Test fun multipleSpansDoNotSilentlySelectAPrincipalSpan() {
        val s=snapshot();val spans=(1..2).map{ScapSpanEntity("span$it",s.inspection.id,it,10.0*it,"DEFINITIVO","Losa","","","")}
        val rows=spans.flatMap{e->listOf("category" to e.category,"type" to e.type,"slabMaterial" to "Concreto armado").map{ScapFieldValueEntity(s.inspection.id,e.id,it.first,it.second,"MANUAL")}}
        val multiple=s.copy(spans=spans,values=s.values+rows)
        assertTrue(exporter().review(multiple).warnings.any{it.contains("principal")})
        val w=TemplateWorkbook(workbook(multiple).inputStream())
        assertEquals("10.0",w.cell(1,"J62").textContent);assertEquals("20.0",w.cell(1,"J64").textContent)
        for(ref in listOf("E76","L76","E98")) assertEquals("",w.cell(1,ref).textContent)
    }
    @Test fun pendingMappingsNeverLeakIntoSicAndOnlyAffectedExportIsBlocked() {
        val s=snapshot();val previews=ScapSicExporter.previews(s)
        assertTrue(previews.single{it.format=="SIC-17"}.missingRequired.contains("Clase"))
        assertTrue(previews.single{it.format=="SIC-17A"}.missingRequired.isEmpty())
        assertTrue(previews.single{it.format=="SIC-17B"}.missingRequired.isEmpty())
        for(status in listOf(ScapMappingStatus.PENDING_CLIENT_CONFIRMATION,ScapMappingStatus.NO_MAPPING)) {
            val mapping=ScapSicMapping("SIC-17B","road",listOf(ScapMappedField("test","bridgeCode","DO NOT EXPORT",status)),emptyList())
            assertTrue(ScapSicExporter.preview(mapping).missingRequired.isNotEmpty())
            assertTrue(ScapSicExporter.preview(mapping).values.all{it==null})
        }
        for(format in listOf("SIC-17A","SIC-17B")) {
            val out=ByteArrayOutputStream();ScapSicExporter.write(out,s,format)
            assertTrue(out.size()>1000)
            File("../tmp/engineering").mkdirs();File("../tmp/engineering/$format-validation.xlsx").writeBytes(out.toByteArray())
        }
        assertTrue(runCatching{ScapSicExporter.write(ByteArrayOutputStream(),s,"SIC-17")}.isFailure)
        assertEquals("IN_PROGRESS",s.inspection.status)
    }
    @Test fun bridgePayloadUsesStableIdentityAndRemainsDisconnectedFromSicUploader() {
        val s=snapshot();val p=PhotoEntity("p","road",1,"photo",true,null,null,null,"PENDING",0,scapInspectionId=s.inspection.id,photoCategory="GENERAL")
        val payload=ScapDrivePayload.prepare(s,p,"existing-name.jpg","image/jpeg","AA==")
        assertEquals("SCAP",payload.getString("recordKind"));assertEquals("P-TEST",payload.getString("bridgeCode"))
        assertEquals("code:P-TEST",ScapDrivePayload.bridgeIdentity("P-TEST","other-inspection"))
        assertNotEquals(ScapDrivePayload.bridgeIdentity("",s.inspection.id),ScapDrivePayload.bridgeIdentity("","other-inspection"))
        assertTrue(runCatching{ScapDrivePayload.prepare(s,p.copy(scapInspectionId="other"),"photo","image/png","AA==")}.isFailure)
        assertFalse(ScapDrivePayload.uploadEnabled);assertFalse(DriveUploadPolicy.eligible("SCAP","ACTIVE",s.inspection.id))
        assertFalse(DriveUploadPolicy.eligible("SIC-17","ACTIVE",s.inspection.id))
        assertEquals("SCAP guardado localmente. Sincronización de puente pendiente de configuración del servidor.",DriveUploadPolicy.SCAP_LOCAL_MESSAGE)
    }
}
