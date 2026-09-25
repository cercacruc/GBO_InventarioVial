package com.tuempresa.inventariovial.catalog

object EngineeringConditions {
    val functional=mapOf("1" to "Buena (limpia)","2" to "Regular (parcialmente obstruida)","3" to "Mala (totalmente obstruida)")
    fun criterion(material:String,other:String?):String?=when(material) {
        "1"->"EARTH";"2","3"->"PAVED";"4"->other?.takeIf {it in setOf("EARTH","PAVED")};else->null
    }
    fun structural(material:String,other:String?):Map<String,String> = when(criterion(material,other)) {
        "EARTH" -> mapOf("1" to "No tiene problema.","2" to "Problema de erosión.","3" to "Problema grave de erosión.")
        "PAVED" -> mapOf("1" to "No tiene problema.","2" to "Quebrado o destruido en menos del 30% de longitud.","3" to "Quebrado o destruido en más del 30% de longitud.")
        else -> emptyMap()
    }
    fun badCulvert(structural:String?,functional:String?)=structural=="3" || functional=="3"
    val culvertPhotos=linkedMapOf("CULVERT_PANORAMIC" to "FOTO PANORÁMICA","CULVERT_INLET" to "FOTO DE ENTRADA","CULVERT_OUTLET" to "FOTO DE SALIDA")
}
