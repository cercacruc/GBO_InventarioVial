package com.tuempresa.inventariovial.field

import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import com.tuempresa.inventariovial.model.form.*

/** Explicit field mapping keeps review labels independent of storage names. */
fun captureSummary(r: InventorySaveRequest): List<Pair<String,String>> = buildList {
    fun row(label: String, value: Any?) { add(label to (value?.toString()?.takeIf {it.isNotBlank()} ?: "Sin dato / no aplica")) }
    fun option(label: String, key: String, code: String) {
        row(label,SicCatalogRepository.options(key).firstOrNull {it.substringBefore(" - ")==code} ?: code)
    }
    fun conditions(prefix: String, structural: String, functional: String?) {
        option("Condición estructural","$prefix.structural.0",structural)
        if(functional!=null) option("Condición funcional","$prefix.functional.0",functional)
    }
    row("Formato",r.sicCode);row("Elemento",r.assetType);row("Tramo",r.segment)
    row("Ruta",r.routeCode);row("Calzada",r.roadbedCode)
    row("Sentido",if(r.direction=="DECREASING") "Decreciente" else "Creciente")
    row("Progresiva inicial","${r.startPrCode} + ${r.startDistanceM} m")
    if(!r.endPrCode.isNullOrBlank()) row("Progresiva final","${r.endPrCode} + ${r.endDistanceM} m")
    row("Lado",when(r.sideCode) {"D"->"Derecho";"I"->"Izquierdo";else->"Sin objeto"})
    when(val d=r.detail) {
        is SicFormDetail.Sic22 -> with(d.state) {
            option("Tipo","sic22.type.0",typeCode);option("Material","sic22.material.0",materialCode)
            row("Código de señal",signalCode);option("Condición","sic22.condition.0",conditionCode)
        }
        is SicFormDetail.Sic21 -> with(d.state) {
            row("Clase",when(classCode){"18"->"Marcas horizontales";"20"->"Tachas";else->"Seguridad vial"})
            val index=when(classCode){"18"->0;"19"->1;else->2}
            option("Tipo","sic21.type.$index",typeCode)
            if(classCode=="19") option("Material","sic21.material.0",materialCode)
            option("Condición","sic21.condition.${if(classCode=="18") 0 else if(classCode=="20") 1 else 2}",conditionCode)
        }
        is SicFormDetail.Sic18 -> with(d.state) {
            option("Clase","sic18.class.0",classCode);option("Tipo / material","sic18.type.${if(classCode=="06") 0 else 1}",typeCode)
            row("Ojos / vanos",spans);option("Sección transversal","sic18.section.0",crossSectionCode)
            if(crossSectionCode=="2") row("Forma",if(sectionShape=="CIRCULAR") "Circular" else "Ovalada")
            row(if(usesDimension2) "Ancho (m)" else "Diámetro (m)",dimension1M)
            if(usesDimension2) row("Altura (m)",dimension2M)
            conditions("sic18",structuralConditionCode,functionalConditionCode)
        }
        is SicFormDetail.Sic19 -> with(d.state) {
            option("Clase","sic19.class.0",classCode);option("Tipo","sic19.type.0",typeCode)
            option("Sección","sic19.section.0",crossSectionCode);conditions("sic19",structuralConditionCode,functionalConditionCode)
        }
        is SicFormDetail.Sic20 -> with(d.state) {
            option("Clase","sic20.class.0",classCode)
            option("Tipo","sic20.type.${when(classCode){"12"->0;"13"->1;else->2}}",typeCode)
            row(if(classCode=="14") "Altura promedio (m)" else "Ancho (m)",dimension1M)
            if(usesDimension2) row("Dimensión 2 (m)",dimension2M)
            conditions("sic20",structuralConditionCode,if(usesFunctionalCondition) functionalConditionCode else null)
        }
        is SicFormDetail.Sic17 -> with(d.state) {
            option("Clase","sic17.class.0",classCode)
            option("Tipo","sic17.type.${classCode.toIntOrNull()?.minus(1) ?: 4}",typeCode)
            row("Código de puente",bridgeCode);option("Inventariado","sic17.inventoried.0",inventoriedCode)
            row("Vanos",spans);row("Longitud (m)",dimension1LengthM);row("Altura inferior (m)",dimension2LowerHeightM)
            row("Altura superior (m)",dimension3UpperHeightM);conditions("sic17",structuralConditionCode,functionalConditionCode)
            option("Servicio","sic17.service.0",serviceTypeCode);option("Singularidad","sic17.singularity.0",singularityCode)
            row("Nombre de singularidad",singularityName)
        }
        is SicFormDetail.Sic23 -> with(d.state) {
            row("Clase",Sic23FormState.classOptions.firstOrNull {it.substringBefore(" - ")==classCode})
            row("Tipo",typeOptions.firstOrNull {it.substringBefore(" - ")==typeCode})
            if(usesWidth) row("Ancho (m)",widthM)
            row("Descripción",normalizedDescription())
        }
    }
    row("Fecha",r.surveyDate);row("Observaciones para informe",r.observations)
    row("GPS inicial","${r.latitude}, ${r.longitude}");row("Precisión GPS (m)",r.gpsAccuracyM)
    if(com.tuempresa.inventariovial.validation.requiresEndLocation(r) && r.endLatitude!=null) row("GPS final","${r.endLatitude}, ${r.endLongitude}")
    row("Fotografías",r.photoPaths.size);row("Fotografías con sello confirmado",r.stampedPaths.size)
}
