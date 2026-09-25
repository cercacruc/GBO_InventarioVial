package com.tuempresa.inventariovial.scap.export

import com.tuempresa.inventariovial.scap.calculator.ScapPercentages
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.ScapNumbers
import org.json.JSONArray
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ScapExportReview(val errors:List<String>,val warnings:List<String>)

/** Copies the supplied SCAP workbook; never uses its example measurements as defaults. */
class ScapExcelExporter(private val template:()->InputStream, fieldMap:String) {
    private data class Field(val key:String,val owner:String,val section:String,val cell:String,val kind:String,val visible:String?)
    private val fields=JSONArray(fieldMap).let{a->List(a.length()){i->a.getJSONObject(i).let{
        Field(it.getString("key"),it.getString("owner"),it.getString("section"),it.getString("valueCell"),it.getString("kind"),it.optString("visibleWhen").ifBlank{null})
    }}}
    fun review(s:ScapInspectionSnapshot):ScapExportReview {
        val errors=mutableListOf<String>();val warnings=mutableListOf<String>()
        val selected=s.elements.filter{it.element.isPresent}
        groupRows.forEach {(group,rows)->if(selected.count{it.element.group==group}>rows.size)
            errors+="$group: la plantilla admite ${rows.size} elementos; amplía y valida la plantilla antes de exportar todos."
        }
        if(selected.any{it.element.group !in groupRows}) errors+="Existe un grupo de elementos sin correspondencia en la plantilla."
        if(s.photos.size>32) errors+="La plantilla admite 32 fotos; esta inspección tiene ${s.photos.size}."
        if(s.profile.size>15) errors+="La plantilla admite 15 puntos de perfil; esta inspección tiene ${s.profile.size}."
        if(s.spans.size>3) errors+="La plantilla admite longitudes de tres tramos; esta inspección tiene ${s.spans.size}."
        if(s.supports.size>2) errors+="La plantilla admite dos grupos de apoyos; esta inspección tiene ${s.supports.size}."
        if(s.substructures.count{it.kind=="PIER"}>3) errors+="La plantilla no distingue individualmente más de tres pilares."
        if(s.sketches.groupBy{it.type}.any{it.value.size>1}) errors+="La plantilla admite un croquis de cada tipo."
        if(s.spans.size>1) warnings+="C.2/C.3: tipologías principal/secundaria y tablero vacíos; falta identificar inequívocamente el tramo principal. Las longitudes se conservan en su orden."
        if(s.spans.size>2) warnings+="C.2: categoría y tipología del tercer tramo no tienen casillero individual; quedan sin exportar."
        if(s.substructures.count{it.kind=="PIER"}>=3) warnings+="C.4: tercer pilar sin exportar en el bloque colectivo «Pilar 3, 4 y 5»; falta confirmar esa equivalencia."
        if(s.defects.any{it.elementCode==null}) warnings+="Defectos generales sin elemento: no hay casillero inequívoco en sec F; permanecen en la ficha local."
        warnings+="Conclusiones y recomendaciones sin mapeo: vacías. No se reutilizan textos del ejemplo."
        warnings+="Excel recalculará las fórmulas al abrir. Una ficha incompleta puede no producir condición global."
        return ScapExportReview(errors,warnings)
    }
    fun write(output:OutputStream,s:ScapInspectionSnapshot,media:(String)->ByteArray) {
        val review=review(s);require(review.errors.isEmpty()){review.errors.joinToString("\n")}
        val w=template().use{TemplateWorkbook(it)}
        val common=s.values()+mapOf("bridgeName" to s.inspection.bridgeName,"bridgeCode" to s.inspection.bridgeCode)
        fun put(field:Field,ref:String,values:Map<String,String>) {
            val rule=field.visible
            val visible=if(rule==null) true else if("!=" in rule) values[rule.substringBefore("!=")]!=rule.substringAfter("!=") else values[rule.substringBefore('=')]==rule.substringAfter('=')
            val raw=values[field.key].orEmpty().takeIf{visible}.orEmpty().trim()
            val value:Any?=when {
                raw.isEmpty()->null
                field.kind=="CHAINAGE"->ScapNumbers.chainage(raw)
                field.kind=="DATE"->runCatching{ChronoUnit.DAYS.between(LocalDate.of(1899,12,30),LocalDate.parse(raw,DateTimeFormatter.ofPattern("dd/MM/uuuu")))}.getOrNull()
                field.kind in setOf("DECIMAL","SIGNED_DECIMAL","INTEGER","YEAR","PERCENT")->ScapNumbers.decimal(raw)
                else->raw
            }
            // E66 is a genuine link to total length. J62 was an example-only single-span assumption.
            if(ref!="E66") w.set(1,ref,value)
        }
        fields.filter{it.owner=="inspection"}.forEach{put(it,it.cell,common)}
        w.set(1,"C4",s.inspection.bridgeName)
        w.set(1,"C47",s.inspection.createdBy.takeIf{it.isNotBlank()}?.let{"Evaluación: $it"})
        w.set(1,"E62",s.spans.size.takeIf{it>0})
        w.set(1,"E447",s.profile.size.takeIf{it>0})
        // Remove supplementary sample observations outside the 141 field coordinates.
        listOf("E173","E179","E189","E195","E247","J249","C483").forEach{w.set(1,it,null)}
        for(r in 455..457) for(col in listOf("U","V","W","X")) w.set(1,"$col$r",null)
        for(r in 461..475) {w.set(1,"U$r",null);w.set(1,"W$r",null)}
        for(r in 482..496) for(col in listOf("G","H","J","L")) w.set(1,"$col$r",null)
        val spans=s.spans.sortedBy{it.spanIndex}
        for(i in 0..2) {
            val span=spans.getOrNull(i)
            w.set(1,"J${62+2*i}",span?.lengthM)
            if(i<2) fields.filter{it.owner=="span" && it.section=="C2" && it.key!="lengthM"}.forEach{
                put(it,if(i==0)it.cell else it.cell.replace("E","L"),span?.takeIf{spans.size==1}?.let{s.values(it.id)}.orEmpty())
            }
        }
        fields.filter{it.owner=="span" && it.section=="C3"}.forEach{put(it,it.cell,spans.singleOrNull()?.let{s.values(it.id)}.orEmpty())}
        val piers=s.substructures.filter{it.kind=="PIER"}.sortedBy{it.elementIndex}
        fun sub(kind:String,index:Int,owner:String,from:String,to:String,soil:String?) {
            val row=if(kind=="PIER")piers.getOrNull(index) else s.substructures.find{it.kind==kind}
            fields.filter{it.owner==owner && it.key!="soil"}.forEach{put(it,it.cell.replace(from,to),row?.let{s.values(it.id)}.orEmpty())}
            if(soil!=null) w.set(1,soil,row?.soil)
        }
        sub("LEFT_ABUTMENT",0,"abutment","J","J","E392");sub("RIGHT_ABUTMENT",0,"abutment","J","N","G392")
        sub("PIER",0,"pier","G","G","J392");sub("PIER",1,"pier","G","J","L392")
        fields.filter{it.owner=="pier" && it.key!="soil"}.forEach{w.set(1,it.cell.replace("G","N"),null)}
        w.set(1,"N392",piers.getOrNull(2)?.soil)
        sub("LEFT_ANCHOR",0,"anchor","G","G",null);sub("RIGHT_ANCHOR",0,"anchor","G","L",null)
        for(i in 0..1) fields.filter{it.owner=="bearing"}.forEach{put(it,if(i==0)it.cell else it.cell.replace("H","J"),s.supports.sortedBy{it.supportIndex}.getOrNull(i)?.let{s.values(it.id)}.orEmpty())}
        for(owner in listOf("ACCESS_LEFT","ACCESS_RIGHT")) fields.filter{it.owner=="access"}.forEach{f->
            val ref=if(owner=="ACCESS_LEFT") f.cell else when {
                f.section=="C6"->f.cell.replace("G","N").replace("H","P")
                else->f.cell.replace("G","L").replace("H","N")
            }
            put(f,ref,s.values(owner))
        }
        for(i in 0..14) fields.filter{it.owner=="profile"}.forEach{put(it,it.cell.replace("461",(461+i).toString()),s.profile.sortedBy{it.pointIndex}.getOrNull(i)?.let{s.values(it.id)}.orEmpty())}
        for(sheet in 2..6) w.set(sheet,"C4",s.inspection.bridgeName)
        w.set(4,"D15",s.inspection.createdBy);w.set(4,"D16",null);w.set(4,"P28",null)
        listOf("B19","B29","E29","B39","E39","B43","E43").forEach{w.set(7,it,null)}
        w.set(7,"D57",s.inspection.createdBy)
        val ordered=groupRows.flatMap{(group,rows)->
            val entries=s.elements.filter{it.element.isPresent && it.element.group==group}.sortedBy{it.element.elementCode}
            rows.forEachIndexed{i,r->
                val e=entries.getOrNull(i)?.element
                w.set(3,"K$r",e?.elementCode?.toInt());w.set(3,"L$r",e?.quantity)
                val defectRow=defectRows.getValue(group)[i]
                w.set(4,"B$defectRow",e?.elementCode?.toInt())
                w.set(4,"F$defectRow",e?.let{el->s.defects.filter{it.elementCode==el.elementCode}.joinToString("\n"){listOf(it.description,it.locationDescription).filter(String::isNotBlank).joinToString(" · ")}})
            };entries
        }
        for(i in 0..16) {
            val item=ordered.getOrNull(i);val row=45+i
            w.set(3,"A$row",item?.element?.description);w.set(3,"B$row",item?.element?.elementCode?.toInt());w.set(3,"C$row",item?.element?.quantity)
            for(p in 0..5) w.set(3,"${'E'+p}$row",item?.condition?.let{ScapPercentages.values(it)[5-p]?.div(100)})
        }
        for(i in 0..13) {
            val item=ordered.getOrNull(i);val row=14+i
            w.set(6,"A$row",item?.element?.elementCode?.toInt());w.set(6,"C$row",item?.element?.quantity)
            for(p in 0..5) w.set(6,"${'F'+p}$row",item?.condition?.let{ScapPercentages.values(it)[p]})
        }
        ScapTemplateMedia.write(w,s,media)
        w.finish(output)
    }
    companion object {
        internal val groupRows=linkedMapOf("SUPERESTRUCTURA" to listOf(14,15,16),"SUBESTRUCTURA" to listOf(19,20),"DETALLES" to listOf(23,24,25,26),"CAUCE" to listOf(29,30),"ACCESOS" to listOf(33,34,35))
        private val defectRows=mapOf("SUPERESTRUCTURA" to listOf(22,23,24),"SUBESTRUCTURA" to listOf(26,27),"DETALLES" to listOf(29,30,31,32),"CAUCE" to listOf(34,35),"ACCESOS" to listOf(37,38,39))
    }
}
