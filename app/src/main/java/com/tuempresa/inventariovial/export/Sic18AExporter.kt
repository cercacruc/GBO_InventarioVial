package com.tuempresa.inventariovial.export

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.supplementary.*
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object Sic18AExporter {
    val columns=listOf(SicColumn("Ruta"),SicColumn("Calzada"),SicColumn("Código PR"),SicColumn("Distancia (m)",CellKind.DECIMAL),
        SicColumn("Clase"),SicColumn("Tipo"),SicColumn("Ojos / Vanos",CellKind.INTEGER),SicColumn("Función"),
        SicColumn("Ubicación de falla estructural"),SicColumn("Tipo de falla"),SicColumn("Estado falla funcional"),SicColumn("Causa probable"),SicColumn("Fecha",CellKind.DATE))
    fun values(r:InventoryRecordEntity,state:SupplementaryFormState):List<Any?> {
        require(r.sicCode=="SIC-18" && r.status=="ACTIVE") {"SIC-18A requiere una alcantarilla activa."}
        require(state.format==SupplementaryFormat.SIC18A)
        require(state.validate().isEmpty()) {state.validate().joinToString("\n")}
        val v=state.values
        return listOf(r.routeCode,r.roadbedCode,r.startPrCode.padStart(4,'0'),r.startDistanceM,v["classCode"],v["typeCode"],v["spans"]?.toInt(),
            v["functionCode"],v["failureLocationCode"],v["failureTypeCode"],v["functionalStateCode"],v["probableCauseCode"],
            LocalDate.parse(r.surveyDate,DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)))
    }
    fun write(out:OutputStream,r:InventoryRecordEntity,state:SupplementaryFormState)=
        SicExcelWriter.writeMappedRow(out,"SIC-18A",r.routeCode,columns,values(r,state))
}
