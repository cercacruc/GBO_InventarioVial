package com.tuempresa.inventariovial

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.export.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.graphics.Bitmap
import android.graphics.Color
import com.tuempresa.inventariovial.data.entity.PhotoEntity

/** Uses Android's actual XML implementation, not the desktop JVM parser. No database access. */
@RunWith(AndroidJUnit4::class)
class ScapAndroidExportTest {
    @Test fun exportsAndReopensRealTemplateOnAndroid() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val exporter=ScapExcelExporter({context.assets.open("scap_template.xlsx")},
            context.assets.open("scap_field_map.json").bufferedReader().use {it.readText()})
        val inspection=ScapInspectionEntity("export-test","test-road","Puente de prueba Ñ","TEST",0,0,
            "Prueba","test","IN_PROGRESS","PENDING",null,null,null,null,"MANUAL")
        val s=ScapInspectionSnapshot(inspection,values=listOf(
            ScapFieldValueEntity(inspection.id,"inspection","route","PE-28H","MANUAL")),
            photos=(1..2).map {PhotoEntity("photo$it",inspection.roadRecordId,it,"synthetic-photo$it",it==1,
                null,null,null,"PENDING",0)},
            sketches=listOf("ELEVATION","PLAN","CROSS_SECTION").map {
                ScapSketchEntity("sketch-$it",inspection.id,it,"synthetic-$it")
            },
            spans=(1..3).map {ScapSpanEntity("span$it",inspection.id,it,10.0,"DEFINITIVO","Losa","","","")})
        val bitmap=Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.BLUE)}
        val image=ByteArrayOutputStream().also {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray()
        bitmap.recycle()
        val output=ByteArrayOutputStream()
        exporter.write(output,s) {image}
        val workbook=TemplateWorkbook(output.toByteArray().inputStream())
        assertTrue(workbook.parts.containsKey("xl/workbook.xml"))
        assertTrue(workbook.sheet(1).documentElement.textContent.contains("Puente de prueba Ñ"))
        assertEquals(5,workbook.parts.keys.count {it.startsWith("xl/media/scap_")})
        assertEquals("2",workbook.cell(5,"H1").textContent)
        workbook.parts.keys.filter {it.startsWith("xl/drawings/") && it.endsWith(".xml")}.forEach {path ->
            val drawing=workbook.document(path)
            drawing.nodes("blip").filter {it.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","embed").startsWith("scapImage")}.forEach {blip ->
                val fill=blip.parentNode as org.w3c.dom.Element
                assertEquals(0,fill.getElementsByTagNameNS("*","srcRect").length)
            }
        }
        workbook.parts.keys.filter {it.endsWith(".xml") || it.endsWith(".rels")}.forEach {workbook.document(it)}
    }

    private fun xmlWorkbook(xml:String,encoding:String="UTF-8"):TemplateWorkbook {
        val bytes=ByteArrayOutputStream()
        ZipOutputStream(bytes).use {zip->
            zip.putNextEntry(ZipEntry("test.xml"))
            zip.write(xml.toByteArray(charset(encoding)))
            zip.closeEntry()
        }
        return TemplateWorkbook(bytes.toByteArray().inputStream())
    }

    @Test fun rejectsInternalAndExternalDtdInUtf8AndUtf16() {
        for(encoding in listOf("UTF-8","UTF-16")) {
            for(dtd in listOf("<!DOCTYPE root SYSTEM 'file:///never-read.dtd'>",
                "<!DOCTYPE root [<!ENTITY example 'not allowed'>]>")) {
                val result=runCatching {xmlWorkbook("<?xml version='1.0' encoding='$encoding'?>$dtd<root/>",encoding).document("test.xml")}
                assertTrue("DTD must be rejected in $encoding",result.exceptionOrNull() is IllegalArgumentException)
            }
        }
    }

    @Test fun preservesNamespacesUnicodeAndEscapedText() {
        val doc=xmlWorkbook("<s:root xmlns:s='urn:test'><s:value>Vía &amp; río</s:value></s:root>").document("test.xml")
        assertEquals("urn:test",doc.documentElement.namespaceURI)
        assertEquals("Vía & río",doc.documentElement.textContent)
    }
}
