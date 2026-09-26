package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.*
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.scap.catalog.*
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapFullExportTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val catalog get()=ScapCatalog.load(context)
    private val directory=File("../tmp/engineering2").apply {mkdirs()}
    private fun exporter()=ScapExcelExporter({context.assets.open("scap_template.xlsx")},context.assets.open("scap_field_map.json").bufferedReader().use {it.readText()})
    private fun image(name:String):String {
        val f=File(directory,"$name.png")
        val b=Bitmap.createBitmap(480,240,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.LTGRAY)}
        val canvas=Canvas(b)
        canvas.drawRect(20f,20f,220f,190f,Paint().apply {color=Color.BLUE})
        canvas.drawRect(240f,40f,450f,210f,Paint().apply {color=Color.RED})
        canvas.drawText(name,25f,225f,Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.BLACK;textSize=18f})
        f.outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle();return f.absolutePath
    }
    private fun fixture():ScapInspectionSnapshot {
        val cat=catalog;val id="SCAP_COMPLETO_TEST"
        val inspection=ScapInspectionEntity(id,"road-test",id,"P-TEST-7",0,0,"Inspector sintético","Tablet test","IN_PROGRESS","PENDING",-12.25,-77.15,2.0,1000,"MANUAL")
        val values=mutableListOf<ScapFieldValueEntity>()
        fun populate(owner:String,kind:String,index:Int=0):Map<String,String> {
            val fields=if(kind=="joint")cat.fields("C5",kind) else cat.fields.filter {it.owner==kind}
            val map=fields.mapIndexed {i,f->f.key to when(f.kind) {
                "DATE" -> "25/09/2026"; "YEAR" -> "2009"; "CHAINAGE" -> "45+321.50"
                "INTEGER" -> (index+2).toString(); "DECIMAL","SIGNED_DECIMAL","PERCENT" -> (index+2+i/100.0).toString()
                "DIMENSION_RANGE" -> "1.35-2.45"; "APPLICABILITY" -> "Si"; "DEPENDENT" -> cat.types("DEFINITIVO").first()
                "CATALOG","CHOICE_TEXT" -> cat.options(f.catalog).filterNot {it.startsWith("No ",true)}.let {it.getOrElse(index%maxOf(it.size,1)){"Prueba"}}
                else -> "TEST_${f.section}_${i+index}"
            }}.toMap().toMutableMap()
            if(kind=="span") {map["category"]="DEFINITIVO";map["type"]=cat.types("DEFINITIVO")[index%cat.types("DEFINITIVO").size];map["lengthM"]=(10.0+index).toString()}
            if(kind=="inspection") map.putAll(mapOf("bridgeName" to id,"bridgeCode" to "P-TEST-7","route" to "PE-3N","totalLengthM" to "46.0","createdBy" to inspection.createdBy))
            values+=map.map {ScapFieldValueEntity(id,owner,it.key,it.value,"MANUAL")};return map
        }
        populate("inspection","inspection")
        val spans=(1..4).map {i->val owner="span$i";val v=populate(owner,"span",i-1);ScapSpanEntity(owner,id,i,v.getValue("lengthM").toDouble(),v.getValue("category"),v.getValue("type"),v.getValue("secondaryCharacteristic"),v.getValue("edgeCondition"),v.getValue("predominantMaterial"))}
        val structures=(ScapPresentation.fixedStructures+(1..7).map {"PIER"}).mapIndexed {index,kind->
            val owner="structure$index";val v=populate(owner,if(kind=="PIER") "pier" else if("ANCHOR" in kind) "anchor" else "abutment",index)
            ScapSubstructureEntity(owner,id,kind,if(kind=="PIER")index-3 else 1,v.getValue("elevationType"),v.getValue("elevationMaterial"),v.getValue("foundationType"),v.getValue("foundationMaterial"),v["soil"].orEmpty())
        }
        val supports=(1..4).map {i->val v=populate("support$i","bearing",i);ScapSupportEntity("support$i",id,i,v.getValue("type"),v.getValue("material"),v.getValue("location"),v.getValue("number").toInt())}
        val joints=(1..3).map {i->val v=populate("joint$i","joint",i);ScapJointEntity("joint$i",id,i,v.getValue("jointType"),v.getValue("jointMaterial"))}
        listOf("ACCESS_LEFT","ACCESS_RIGHT").forEachIndexed {i,owner->populate(owner,"access",i)}
        val profile=(1..3).map {i->val v=populate("point$i","profile",i);ScapProfilePointEntity("point$i",id,i,v.getValue("distanceM").toDouble(),v.getValue("downstreamM").toDouble(),v.getValue("upstreamM").toDouble(),v.getValue("axisM").toDouble())}
        val elements=ScapPresentation.defaultElements.mapIndexed {i,code->val e=cat.element(code)!!;ScapElementWithCondition(
            ScapElementEntity("e$code",id,code,e.name,10.0+i,e.unit,e.importanceFactor,e.group,true),ScapElementConditionEntity("c$code","e$code",0.0,80.0,20.0,0.0,0.0,0.0))}
        val photos=ScapPhotoCategories.primary.keys.mapIndexed {i,category->PhotoEntity("photo$i","road-test",i+1,image("foto_${i+1}"),i==0,null,null,null,"PENDING",1_790_333_149_000,description="Foto sintética ${i+1}",scapInspectionId=id,scapElementCode="104",photoCategory=category)}
        val defects=elements.mapIndexed {i,e->ScapDefectEntity("d$i",id,e.element.elementCode,"Observación ${e.element.elementCode}","Ubicación $i",photos[i%photos.size].id,false,true,i.toLong())}+
            listOf(ScapDefectEntity("second",id,"110","Segunda falla 110","Centro",null,false,true,100),ScapDefectEntity("general",id,null,"Observación general sintética","Acceso",null,false,true,101))
        val sketches=ScapRepository.SKETCH_TYPES.map {ScapSketchEntity(it,id,it,image(it))}
        return ScapInspectionSnapshot(inspection,values,spans,structures,supports,elements,defects,sketches,photos,profile,joints)
    }
    @Test fun SCAP_COMPLETO_TEST_reopensWithAllFieldsSevenDistinctPiersAndOriginalGeometry() {
        val s=fixture();val out=ByteArrayOutputStream();val review=exporter().review(s)
        assertEquals(emptyList<String>(),review.errors)
        exporter().write(out,s) {File(it).readBytes()}
        val file=File(directory,"SCAP_COMPLETO_TEST.xlsx").apply {writeBytes(out.toByteArray())}
        val w=file.inputStream().use {TemplateWorkbook(it)}
        val template=context.assets.open("scap_template.xlsx").use {TemplateWorkbook(it)}
        val sheetNames=w.document("xl/workbook.xml").nodes("sheet").map {it.getAttribute("name")}
        assertEquals(10,sheetNames.size);assertEquals(template.document("xl/workbook.xml").nodes("sheet").map {it.getAttribute("name")},sheetNames)
        assertArrayEquals(template.parts.getValue("xl/styles.xml"),w.parts.getValue("xl/styles.xml"))
        for(i in 1..10) {
            assertTrue(w.sheet(i).nodes("mergeCell").map {it.getAttribute("ref")}.containsAll(template.sheet(i).nodes("mergeCell").map {it.getAttribute("ref")}))
            template.sheet(i).nodes("row").filter {it.namespaceURI==TemplateWorkbook.MAIN}.forEach {row->val current=w.sheet(i).nodes("row").filter {it.namespaceURI==TemplateWorkbook.MAIN}.single {it.getAttribute("r")==row.getAttribute("r")};assertEquals(row.getAttribute("ht"),current.getAttribute("ht"))}
        }
        w.parts.keys.filter {it.endsWith(".xml")||it.endsWith(".rels")}.forEach {w.document(it)}
        val cells=JSONArray()
        fun text(sheet:Int,ref:String):String {val c=w.cell(sheet,ref);return if(c.getAttribute("t")=="s")w.document("xl/sharedStrings.xml").nodes("si")[c.textContent.toInt()].textContent else c.textContent}
        fun check(owner:String,key:String,ref:String,expected:String,sheet:Int=1) {
            val actual=text(sheet,ref)
            val number=expected.toDoubleOrNull()
            if(number!=null) assertEquals("$owner/$key -> $ref",number,actual.toDouble(),.000001) else assertEquals("$owner/$key -> $ref",expected,actual)
            cells.put(JSONObject(mapOf("owner" to owner,"field" to key,"sheet" to sheetNames[sheet-1],"cell" to ref,"expected" to expected,"actual" to actual,"result" to "OK")))
        }
        val map=JSONArray(context.assets.open("scap_field_map.json").bufferedReader().use {it.readText()})
        val fields=(0 until map.length()).map {map.getJSONObject(it)}
        fun field(owner:String,f:JSONObject,ref:String=f.getString("valueCell")) {
            val key=f.getString("key");val v=s.values(owner);val rule=f.optString("visibleWhen")
            val visible=rule.isBlank() || if("!=" in rule)v[rule.substringBefore("!=")]!=rule.substringAfter("!=") else v[rule.substringBefore('=')]==rule.substringAfter('=')
            val raw=ScapFieldPolicy.exportValue(key,f.getString("owner"),v).takeIf {visible}.orEmpty()
            val expected=when {raw.isBlank()->"";f.getString("kind")=="DATE"->ChronoUnit.DAYS.between(LocalDate.of(1899,12,30),LocalDate.parse(raw,DateTimeFormatter.ofPattern("dd/MM/uuuu"))).toString();f.getString("kind")=="CHAINAGE"->ScapNumbers.chainage(raw).toString();else->raw}
            check(owner,key,ref,expected)
        }
        fields.filter {it.getString("owner")=="inspection" && it.getString("valueCell")!="E66"}.forEach {f->field(if(f.getString("key").startsWith("joint")) "joint1" else "inspection",f)}
        fun structure(owner:String,kind:String,col:String,base:String,soil:String?=null,delta:Int=0) {
            fields.filter {it.getString("owner")==kind && it.getString("section")=="C4"}.forEach {f->val ref=f.getString("valueCell").replace(base,col);field(owner,f,ref.takeWhile(Char::isLetter)+(ref.filter(Char::isDigit).toInt()+delta))}
            soil?.let {check(owner,"soil",it,s.values(owner).getValue("soil"))}
        }
        structure("structure0","abutment","J","J","E392");structure("structure1","abutment","N","J","G392")
        structure("structure2","anchor","G","G");structure("structure3","anchor","L","G")
        structure("structure4","pier","G","G","J392");structure("structure5","pier","J","G","L392");structure("structure6","pier","N","G","N392")
        structure("structure7","pier","G","G","J543",352);structure("structure8","pier","J","G","L543",352);structure("structure9","pier","N","G","N543",352)
        structure("structure10","pier","G","G","J570",379)
        listOf("G173","J173","N173","G525","J525","N525","G552").forEachIndexed {i,ref->check("pier${i+1}","title",ref,"Pilar ${i+1}")}
        assertEquals(7,s.substructures.filter {it.kind=="PIER"}.map {listOf(it.elevationType,it.elevationMaterial,it.foundationType,it.foundationMaterial,it.soil)}.distinct().size)
        for(i in 1..4) fields.filter {it.getString("owner")=="bearing"}.forEach {f->val ref=f.getString("valueCell");field("support$i",f,(if(i%2==1) "H" else "J")+(ref.filter(Char::isDigit).toInt()+if(i<=2)0 else 337))}
        for(i in 2..3) fields.filter {it.getString("key").startsWith("joint")}.forEach {f->val ref=f.getString("valueCell");field("joint$i",f,ref.takeWhile(Char::isLetter)+(ref.filter(Char::isDigit).toInt()+if(i==2)339 else 347))}
        for(i in 1..4) fields.filter {it.getString("owner")=="span" && it.getString("key")!="lengthM"}.forEach {f->val ref=f.getString("valueCell");field("span$i",f,ref.takeWhile(Char::isLetter)+(ref.filter(Char::isDigit).toInt()+if(i==1)0 else 530+(i-2)*40))}
        for(owner in listOf("ACCESS_LEFT","ACCESS_RIGHT")) fields.filter {it.getString("owner")=="access"}.forEach {f->val ref=f.getString("valueCell");field(owner,f,if(owner=="ACCESS_LEFT")ref else if(f.getString("section")=="C6")ref.replace("G","N").replace("H","P") else ref.replace("G","L").replace("H","N"))}
        for(i in 1..3) fields.filter {it.getString("owner")=="profile"}.forEach {f->field("point$i",f,f.getString("valueCell").replace("461",(460+i).toString()))}
        s.elements.forEachIndexed {i,e->
            check(e.element.elementCode,"quantity","C${14+i}",e.element.quantity.toString(),6)
            check(e.element.elementCode,"code","A${14+i}",e.element.elementCode,6)
            listOf(0,80,20,0,0,0).forEachIndexed {level,value->check(e.element.elementCode,"percent$level","${'F'+level}${14+i}",value.toString(),6)}
            val row=listOf(22,23,24,26,27,29,30,31,32,34,35,37,38,39)[i]
            val extra=if(e.element.elementCode=="110") "\nSegunda falla 110 · Centro" else ""
            check(e.element.elementCode,"defects","F$row","Observación ${e.element.elementCode} · Ubicación $i$extra",4)
        }
        listOf("J62","J64","J66","N686").forEachIndexed {i,ref->check("span${i+1}","lengthM",ref,(10.0+i).toString())}
        assertTrue(text(4,"F23").contains("Segunda falla 110"));check("general","description","F45","Observación general sintética · Acceso",4)
        s.photos.forEachIndexed {i,p->
            val defects=s.defects.filter {it.photoId==p.id}.joinToString("") {"\n${it.description} · ${it.locationDescription}"}
            check(p.id,"category/description/element/defects","D${13+i}","${ScapPhotoCategories.label(p.photoCategory)} · Foto sintética ${i+1} · 104$defects",5)
            check(p.id,"index","B${13+i}",(i+1).toString(),5)
        }
        assertEquals(12,w.parts.keys.count {it.startsWith("xl/media/scap_")})
        val mediaAnchors=w.parts.keys.filter {it.startsWith("xl/drawings/drawing") && it.endsWith(".xml")}.flatMap {w.document(it).nodes("oneCellAnchor")}.filter {a->a.getElementsByTagNameNS("*","cNvPr").let {(it.item(0) as org.w3c.dom.Element).getAttribute("name").startsWith("scap_")}}
        assertEquals(12,mediaAnchors.size)
        val elevation=mediaAnchors.single {(it.getElementsByTagNameNS("*","cNvPr").item(0) as org.w3c.dom.Element).getAttribute("name")=="scap_2_1.png"}
        assertTrue("Croquis within original cell frame",elevation.children().single {it.localName=="ext"}.getAttribute("cy").toLong()<=1_952_625L)
        mediaAnchors.forEach {a->val extent=a.children().single {it.localName=="ext"};assertEquals(2.0,extent.getAttribute("cx").toDouble()/extent.getAttribute("cy").toDouble(),.00001)}
        for(row in 45..61) assertEquals("IF(B$row=\"\",\"\",VLOOKUP(B$row,\$C\$73:\$J\$184,7,FALSE))",w.formula(3,"D$row"))
        for(ref in listOf("E186","E187","E191","M12")) assertEquals(template.formula(6,ref),w.formula(6,ref))
        assertTrue(w.sheet(1).nodes("dataValidation").any {it.getAttribute("sqref").contains("G527")})
        assertEquals(template.cell(1,"G175").getAttribute("s"),w.cell(1,"G527").getAttribute("s"))
        assertEquals(template.formula(1,"C80")!!.replace("E76","E606"),w.formula(1,"C610"))
        val printed=w.document("xl/workbook.xml").nodes("definedName").single {it.getAttribute("name")=="_xlnm.Print_Area" && it.getAttribute("localSheetId")=="0"}.textContent
        assertTrue(printed.contains("\$R\$718"))
        val xml=w.parts.filterKeys {it.endsWith(".xml")}.values.joinToString {String(it)}
        assertFalse(xml.contains("PUENTE AGUA BLANCA"));assertFalse(xml.contains("Ing. Martín Pichón"))
        File(directory,"full-export-cells.json").writeText(cells.toString(2))
    }
}
