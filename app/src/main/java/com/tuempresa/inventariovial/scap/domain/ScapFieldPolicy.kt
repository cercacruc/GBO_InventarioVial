package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.catalog.ScapField

/** Internal sentinel; never a replacement for a source catalog or an invented numeric code. */
object ScapFieldPolicy {
    const val NOT_APPLICABLE="NOT_APPLICABLE"
    val detailGroups=linkedMapOf(
        "A · Barandas" to listOf("railingType","railingMaterial","railingSecondary"),
        "B · Veredas y Sardineles" to listOf("sidewalkMaterial","sidewalkWidthM","curbHeightM"),
        "C · Apoyos" to listOf("type","material","location","number"),
        "D · Juntas de Expansión" to listOf("jointType","jointMaterial"),
        "E · Drenaje de Calzada" to listOf("drainType","drainMaterial"))
    fun allows(f:ScapField)=f.kind in setOf("CATALOG","CHOICE_TEXT") &&
        (f.section in setOf("C3","C4","C5","C7") || f.catalog=="wearingSurface")
    fun controller(key:String,owner:String):String? = when {
        owner=="bearing" && key!="type" -> "type"
        key in listOf("railingMaterial","railingSecondary") -> "railingType"
        key in listOf("sidewalkWidthM","curbHeightM") -> "sidewalkMaterial"
        key=="jointMaterial" -> "jointType"
        key=="drainMaterial" -> "drainType"
        key=="elevationMaterial" -> "elevationType"
        key=="foundationMaterial" -> "foundationType"
        else -> null
    }
    fun absent(value:String?)=value==NOT_APPLICABLE || value in setOf("No hay","No tiene")
    fun enabled(key:String,owner:String,values:Map<String,String>):Boolean {
        val parent=controller(key,owner) ?: return true
        return !values[parent].isNullOrBlank() && !absent(values[parent])
    }
    fun hasDependents(key:String,owner:String,values:Map<String,String>)=
        values.any {(k,v)->controller(k,owner)==key && v.isNotBlank()}
    fun exportValue(key:String,owner:String,values:Map<String,String>):String {
        if(!enabled(key,owner,values)) return ""
        val v=values[key].orEmpty()
        // H249 in the canonical workbook explicitly contains this literal, without a numeric code.
        return if(v==NOT_APPLICABLE) {if(key=="jointType") "No Aplica" else ""} else v
    }
    fun state(keys:List<String>,owner:String,values:Map<String,String>,valid:(String)->Boolean):String {
        if(values[keys.first()]==NOT_APPLICABLE) return "— No aplica"
        val required=keys.filter {enabled(it,owner,values)}
        return when {
            required.all {values[it].isNullOrBlank()} -> "○ Sin completar"
            required.all {!values[it].isNullOrBlank() && valid(it)} -> "✓ Completado"
            else -> "! Revisión"
        }
    }
}
