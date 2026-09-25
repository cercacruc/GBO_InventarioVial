package com.tuempresa.inventariovial.scap.catalog

import android.content.Context
import org.json.JSONObject

data class ScapCatalogElement(val code:String,val name:String,val unit:String,val importanceFactor:Double,val group:String,val source:String)
data class ScapField(val key:String,val section:String,val owner:String,val label:String,val kind:String,
    val catalog:String?,val source:String,val visibleWhen:String?) {
    fun visible(values:Map<String,String>):Boolean {
        val rule=visibleWhen ?: return true
        val separator=if("!=" in rule) "!=" else "="
        val (key,value)=rule.split(separator,limit=2)
        return if(separator=="!=") values[key]!=value else values[key]==value
    }
}
class ScapCatalog(val elements:List<ScapCatalogElement>,val fields:List<ScapField>,
    private val options:Map<String,List<String>>,private val types:Map<String,List<String>>,val sourceHash:String) {
    fun options(name:String?)=options[name].orEmpty()
    fun types(category:String)=types[category].orEmpty()
    fun fields(section:String,owner:String)=fields.filter {it.section==section && it.owner==owner}
    fun element(code:String)=elements.singleOrNull {it.code==code}
    companion object {
        fun load(context:Context)=parse(
            context.assets.open("scap_elements.json").bufferedReader().use {it.readText()},
            context.assets.open("scap_options.json").bufferedReader().use {it.readText()},
            context.assets.open("scap_fields.json").bufferedReader().use {it.readText()})
        fun parse(elementsText:String,optionsText:String,fieldsText:String):ScapCatalog {
            val e=JSONObject(elementsText);val o=JSONObject(optionsText);val f=JSONObject(fieldsText)
            val hash=e.getString("sourceSha256")
            require(o.getString("sourceSha256")==hash && f.getString("sourceSha256")==hash) {"Catálogos SCAP de versiones diferentes."}
            val elements=e.getJSONArray("elements").let {a->List(a.length()){i->a.getJSONObject(i).let {
                ScapCatalogElement(it.getString("code"),it.getString("name"),it.getString("unit"),it.getDouble("importanceFactor"),it.getString("group"),it.getString("source"))
            }}}
            require(elements.isNotEmpty() && elements.map {it.code}.distinct().size==elements.size)
            require(elements.all {it.importanceFactor.isFinite() && it.importanceFactor in 0.0..1.0})
            fun lists(obj:JSONObject)=obj.keys().asSequence().associateWith {k->obj.getJSONObject(k).getJSONArray("options").let {a->List(a.length()){a.getString(it)}}}
            val fields=f.getJSONArray("fields").let {a->List(a.length()){i->a.getJSONObject(i).let {
                ScapField(it.getString("key"),it.getString("section"),it.getString("owner"),it.getString("label"),it.getString("kind"),
                    it.optString("catalog").ifEmpty {null},it.getString("source"),it.optString("visibleWhen").ifEmpty {null})
            }}}
            return ScapCatalog(elements,fields,lists(o.getJSONObject("catalogs")),lists(o.getJSONObject("typesByCategory")),hash)
        }
    }
}
