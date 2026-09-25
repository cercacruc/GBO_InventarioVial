package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.catalog.*
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.calculator.ScapPercentages
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class ScapReview(val errors:List<String>,val pending:List<String>,val present:Int,val unevaluated:Int)
object ScapNumbers {
    fun decimal(value:String?)=value?.trim()?.replace(',','.')?.toDoubleOrNull()?.takeIf {it.isFinite()}
    fun chainage(value:String?):Double? {
        val text=value?.trim()?.replace(',','.') ?: return null
        if('+' !in text) return decimal(text)?.takeIf {it>=0}
        val p=text.split('+');if(p.size!=2) return null
        val km=p[0].trim().toIntOrNull() ?: return null;val m=decimal(p[1]) ?: return null
        return if(km>=0 && m>=0 && m<1000) km*1000.0+m else null
    }
}
object ScapValidation {
    fun ownerType(snapshot:ScapInspectionSnapshot,owner:String):String? = when {
        owner=="inspection" -> "inspection"
        owner in setOf("ACCESS_LEFT","ACCESS_RIGHT") -> "access"
        snapshot.spans.any {it.id==owner} -> "span"
        snapshot.supports.any {it.id==owner} -> "bearing"
        snapshot.profile.any {it.id==owner} -> "profile"
        else -> snapshot.substructures.find {it.id==owner}?.kind?.let {when {it=="PIER"->"pier";"ANCHOR" in it->"anchor";else->"abutment"}}
    }
    fun fieldError(field:ScapField,value:String,values:Map<String,String>,catalog:ScapCatalog):String? {
        if(value.isBlank() || !field.visible(values)) return null
        val number=ScapNumbers.decimal(value)
        val valid=when(field.kind) {
            "DECIMAL" -> number!=null && number>=0
            "SIGNED_DECIMAL" -> number!=null
            "INTEGER" -> value.toIntOrNull()?.let {it>=0}==true
            "YEAR" -> value.matches(Regex("[0-9]{4}")) && value.toInt()>0
            "PERCENT" -> number!=null && number in 0.0..100.0
            "CHAINAGE" -> ScapNumbers.chainage(value)!=null
            "DATE" -> runCatching {LocalDate.parse(value,DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT))}.isSuccess
            "CATALOG" -> value in catalog.options(field.catalog)
            "DEPENDENT" -> value in catalog.types(values["category"].orEmpty())
            "APPLICABILITY" -> value in catalog.options("yesNo")
            "DIMENSION_RANGE" -> {
                val numbers=value.trim().removeSuffix("m").trim().split('-').map {ScapNumbers.decimal(it)}
                numbers.size in 1..2 && numbers.all {it!=null && it>=0} && (numbers.size==1 || numbers[0]!!<=numbers[1]!!)
            }
            else -> true
        }
        return if(valid) null else "${field.label}: valor inválido${if(field.kind=="DATE") "; usa dd/mm/aaaa" else ""}."
    }
    fun review(s:ScapInspectionSnapshot,catalog:ScapCatalog):ScapReview {
        val errors=mutableListOf<String>();val pending=mutableListOf<String>();val common=s.values()
        for(key in listOf("bridgeName","bridgeCode","route","progressive")) if(common[key].isNullOrBlank())
            errors+="Falta ${catalog.fields.first {it.key==key}.label}."
        if(s.inspection.createdBy.isBlank()) pending+="Inspector no identificado."
        if(s.inspection.latitude==null || s.inspection.longitude==null) pending+="Ubicación GNSS sin capturar; las coordenadas UTM manuales se conservan sin conversión."
        if(s.spans.isEmpty()) errors+="Agrega al menos un tramo."
        s.spans.forEach {span->
            if(span.category.isBlank() || span.type.isBlank()) errors+="Tramo ${span.spanIndex}: categoría y tipo pendientes."
            if(span.type.isNotBlank() && span.type !in catalog.types(span.category)) errors+="Tramo ${span.spanIndex}: tipo incompatible con categoría."
            if(span.category=="ALCANTARILLA" && span.secondaryCharacteristic.isNotBlank() && span.secondaryCharacteristic.toIntOrNull()?.let {it>=0}!=true)
                errors+="Tramo ${span.spanIndex}: ojos/vanos debe ser entero."
        }
        val owners=listOf("inspection","ACCESS_LEFT","ACCESS_RIGHT")+s.spans.map {it.id}+s.substructures.map {it.id}+s.supports.map {it.id}+s.profile.map {it.id}
        for(owner in owners) {
            val type=ownerType(s,owner);val values=s.values(owner)
            catalog.fields.filter {it.owner==type}.distinctBy {it.key}.filter {it.visible(values)}.forEach {field->
                val value=values[field.key].orEmpty()
                fieldError(field,value,values,catalog)?.let {errors+="${field.section} · $it"}
                if(value.isBlank()) pending+="${field.section} · ${if(owner=="inspection") "" else "$type · "}${field.label}"
            }
        }
        val selected=s.elements.filter {it.element.isPresent}
        if(selected.isEmpty()) errors+="Selecciona al menos un elemento presente en el puente."
        var unevaluated=0
        selected.forEach {entry->
            val e=entry.element;val official=catalog.element(e.elementCode)
            if(official==null || official.unit!=e.unit || official.importanceFactor!=e.importanceFactor) errors+="${e.elementCode}: catálogo o factor inconsistente."
            val raw=s.values(e.id)["quantity"]
            if(!raw.isNullOrBlank() && (ScapNumbers.decimal(raw)?.let {it>=0}!=true)) errors+="${e.elementCode}: metrado inválido."
            if(e.quantity==null) pending+="${e.elementCode}: metrado pendiente."
            val c=entry.condition
            if(c==null || ScapPercentages.values(c).any {it==null}) unevaluated++
            val err=if(c==null) "Falta evaluación de condición." else ScapPercentages.error(ScapPercentages.values(c))
            if(err!=null) errors+="${e.elementCode}: $err"
        }
        s.defects.forEach {d->
            if(d.description.isBlank()) errors+="Defecto ${d.id.take(8)}: falta descripción."
            if(d.elementCode!=null && catalog.element(d.elementCode)==null) errors+="Defecto con código SCAP desconocido."
            if(d.aiSuggested && !d.validatedByUser) errors+="Defecto sugerido por IA pendiente de confirmación."
            if(d.photoId!=null && s.photos.none {it.id==d.photoId}) errors+="Fotografía de defecto no pertenece a la inspección."
        }
        if(s.photos.isEmpty()) pending+="Panel fotográfico sin imágenes."
        for(type in listOf("ELEVATION","PLAN","CROSS_SECTION")) if(s.sketches.none {it.type==type}) pending+="Croquis pendiente: $type."
        return ScapReview(errors.distinct(),pending.distinct(),selected.size,unevaluated)
    }
}
