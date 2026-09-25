package com.tuempresa.inventariovial.scap.export

import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.supplementary.*
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ScapSicPreview(val format:String,val columns:List<SicColumn>,val values:List<Any?>,
    val missingRequired:List<String>,val missingOptional:List<String>,val pending:List<String>)

object ScapSicExporter {
    private val accepted=setOf(ScapMappingStatus.DIRECT,ScapMappingStatus.TRANSFORM)
    fun previews(s:ScapInspectionSnapshot)=listOf(ScapToSicMapper.mapToSic17(s),ScapToSicMapper.mapToSic17A(s),ScapToSicMapper.mapToSic17B(s)).map(::preview)
    fun preview(mapping:ScapSicMapping):ScapSicPreview {
        val safe=mapping.fields.filter{it.status in accepted && it.value.isNotBlank()}.associate{it.sicField to it.value}
        val keys:List<String>;val columns:List<SicColumn>;val required:Set<String>
        if(mapping.format=="SIC-17") {
            keys=listOf("routeCode","roadbedCode","startPrCode","startDistanceM","endPrCode","endDistanceM","classCode","typeCode","bridgeCode","inventoriedCode","spans","dimension1LengthM","dimension2LowerHeightM","structuralConditionCode","functionalConditionCode","surveyDate","serviceTypeCode","singularityCode","singularityName","dimension3UpperHeightM")
            columns=SicExportFormat.SIC17.columns
            required=setOf("routeCode","roadbedCode","startPrCode","startDistanceM","classCode","typeCode","bridgeCode","inventoriedCode","spans","dimension1LengthM","structuralConditionCode","functionalConditionCode","surveyDate","serviceTypeCode","singularityCode")
        } else {
            val format=when(mapping.format){"SIC-17A"->SupplementaryFormat.SIC17A;"SIC-17B"->SupplementaryFormat.SIC17B;else->error("Formato no soportado")}
            val fields=SupplementaryForms.fields(format);keys=fields.map{it.key}
            columns=fields.map {SicColumn(it.label,when(it.kind){FieldKind.YEAR,FieldKind.INTEGER->CellKind.INTEGER;FieldKind.DECIMAL,FieldKind.SIGNED_DECIMAL,FieldKind.LATITUDE,FieldKind.LONGITUDE->CellKind.DECIMAL;else->CellKind.TEXT},24.0)}
            // Existing supplementary forms have optional content. Identity is the technical export minimum.
            required=if(format==SupplementaryFormat.SIC17A) setOf("bridgeCode","bridgeName") else setOf("bridgeCode")
        }
        val values=keys.mapIndexed{i,key->safe[key]?.let{raw->when(columns[i].kind){
            CellKind.TEXT->raw
            CellKind.INTEGER->raw.toIntOrNull()
            CellKind.DECIMAL->ScapNumbers.decimal(raw)
            CellKind.DATE->runCatching{LocalDate.parse(raw,DateTimeFormatter.ofPattern("dd/MM/uuuu"))}.getOrNull()
        }}}
        val missing=keys.indices.filter{values[it]==null}
        return ScapSicPreview(mapping.format,columns,values,missing.filter{keys[it] in required}.map{columns[it].label},
            missing.filter{keys[it] !in required}.map{columns[it].label},mapping.pending)
    }
    fun write(output:OutputStream,s:ScapInspectionSnapshot,format:String) {
        val preview=previews(s).single{it.format==format}
        require(preview.missingRequired.isEmpty()){"$format: faltan ${preview.missingRequired.joinToString()}"}
        SicExcelWriter.writeMappedRow(output,format,s.values()["route"].orEmpty(),preview.columns,preview.values)
    }
}
