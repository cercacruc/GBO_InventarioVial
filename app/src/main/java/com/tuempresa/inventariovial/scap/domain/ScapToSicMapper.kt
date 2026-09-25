package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

enum class ScapMappingStatus { DIRECT, TRANSFORM, NO_MAPPING, PENDING_CLIENT_CONFIRMATION }
data class ScapMappedField(val scapField:String,val sicField:String,val value:String,val status:ScapMappingStatus,val notes:String="")
data class ScapSicMapping(val format:String,val roadRecordId:String,val fields:List<ScapMappedField>,val pending:List<String>)

/** Safe projections. Does not write SIC tables, alter catalog codes or schedule Drive. */
object ScapToSicMapper {
    private fun direct(s:ScapInspectionSnapshot,pairs:List<Pair<String,String>>)=pairs.mapNotNull {(from,to)->
        ScapFieldPolicy.exportValue(from,"inspection",s.values()).takeIf {it.isNotBlank()}?.let {ScapMappedField(from,to,it,ScapMappingStatus.DIRECT)}
    }
    fun mapToSic17(s:ScapInspectionSnapshot):ScapSicMapping {
        val fields=direct(s,listOf("bridgeCode" to "bridgeCode","route" to "routeCode","lastInspection" to "surveyDate","totalLengthM" to "dimension1LengthM",
            "lowerClearanceM" to "dimension2LowerHeightM","upperClearanceM" to "dimension3UpperHeightM","singularityName" to "singularityName"))+
            (if(s.spans.isEmpty()) emptyList() else listOf(ScapMappedField("spans.count","spans",s.spans.size.toString(),ScapMappingStatus.TRANSFORM,"Cuenta tramos registrados; requiere revisión al confirmar la ficha."))) +
            (ScapNumbers.chainage(s.values()["progressive"])?.let {m->listOf(
                ScapMappedField("progressive","startPrCode",(m/1000).toInt().toString().padStart(4,'0'),ScapMappingStatus.TRANSFORM),
                ScapMappedField("progressive","startDistanceM",(m%1000).toString(),ScapMappingStatus.TRANSFORM))} ?: emptyList())
        return ScapSicMapping("SIC-17",s.inspection.roadRecordId,fields,listOf("Clase/tipo y códigos de condición no equivalen automáticamente a SCAP.","Calzada, servicio, singularidad y código inventariado requieren confirmación."))
    }
    fun mapToSic17A(s:ScapInspectionSnapshot):ScapSicMapping {
        val fields=direct(s,listOf("bridgeName" to "bridgeName","bridgeCode" to "bridgeCode","constructionYear" to "constructionYear",
            "politicalDepartment" to "department","province" to "province","district" to "district","nearbyTown" to "nearbyTown",
            "altitudeM" to "altitude","roadwayWidthM" to "roadwayWidthM","sidewalkWidthM" to "sidewalkWidthM"))+
            listOfNotNull(s.inspection.latitude?.let {ScapMappedField("GNSS.latitude","latitude",it.toString(),ScapMappingStatus.DIRECT)},
                s.inspection.longitude?.let {ScapMappedField("GNSS.longitude","longitude",it.toString(),ScapMappingStatus.DIRECT)})
        return ScapSicMapping("SIC-17A",s.inspection.roadRecordId,fields,listOf("Vías de tránsito versus carriles: confirmar.","Alineamiento numérico, ancho de tablero y superestructura: pendientes.","UTM 19K no se transforma sin conversión validada."))
    }
    fun mapToSic17B(s:ScapInspectionSnapshot)=ScapSicMapping("SIC-17B",s.inspection.roadRecordId,
        direct(s,listOf("bridgeCode" to "bridgeCode","designLoad" to "designLoad","mainSpanM" to "mainSpanM")),
        listOf("Capacidad máxima no se infiere de cargas de diseño/actual/futura.","Materiales, borde, sección, losa, vigas, estribos y pilares requieren tabla de equivalencias; no usar índices del dropdown como códigos SIC."))
}
