package com.tuempresa.inventariovial.export

import com.tuempresa.inventariovial.data.entity.InventoryRecordForExport
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

enum class CellKind { TEXT, DECIMAL, INTEGER, DATE }
data class SicColumn(val label: String, val kind: CellKind = CellKind.TEXT, val width: Double = 13.0)
data class ExportHeading(val project: String = "", val road: String = "", val section: String = "")

/** Orden oficial del Manual IV-2014/2015, inventario calificado. */
enum class SicExportFormat(val code: String, val title: String) {
    SIC17("SIC-17", "PUENTES Y PONTONES"),
    SIC18("SIC-18", "ALCANTARILLAS"),
    SIC19("SIC-19", "CUNETAS, CANALES, BAJADAS DE AGUA Y ZANJAS DE DRENAJE"),
    SIC20("SIC-20", "BADENES, TÚNELES Y MUROS"),
    SIC21("SIC-21", "SEGURIDAD Y SEÑALIZACIÓN HORIZONTAL"),
    SIC22("SIC-22", "SEÑALIZACIÓN VERTICAL"),
    SIC23("SIC-23", "DERECHO DE VÍA");

    val columns: List<SicColumn> get() {
        fun text(label: String, width: Double = 13.0) = SicColumn(label, width = width)
        fun number(label: String, width: Double = 14.0) = SicColumn(label, CellKind.DECIMAL, width)
        val date = SicColumn("Fecha", CellKind.DATE, 14.0)
        val common = listOf(text("Carretera", 16.0), text("Calzada", 11.0), text("Código PR"), number("Distancia (m)"))
        val ends = if (this == SIC18) emptyList() else listOf(text("Código PR"), number("Distancia (m)"))
        val side = if (this in listOf(SIC17, SIC18)) emptyList() else listOf(text("Lado", 9.0))
        val types = listOf(text("Clase", 9.0), text("Tipo", 9.0))
        val conditions = listOf(text("Condición estructural", 16.0), text("Condición funcional", 16.0))
        return common + ends + side + types + when (this) {
            SIC17 -> listOf(text("Código del puente", 22.0), text("Inventariado"),
                SicColumn("Ojos / Vanos", CellKind.INTEGER), number("Dimensión 1 Longitud (m)", 18.0),
                number("Dimensión 2 Altura inferior (m)", 18.0)) + conditions + listOf(date,
                text("Tipo de servicio", 15.0), text("Singularidad salvada", 15.0),
                text("Nombre singularidad", 26.0), number("Dimensión 3 Altura superior (m)", 18.0))
            SIC18 -> listOf(SicColumn("Ojos / Vanos", CellKind.INTEGER), text("Sección transversal", 16.0),
                number("Dimensión 1 (m)"), number("Dimensión 2 (m)")) + conditions + date
            SIC19 -> listOf(text("Sección transversal", 16.0)) + conditions + date
            SIC20 -> listOf(number("Dimensión 1 (m)"), number("Dimensión 2 (m)")) + conditions + date
            SIC21 -> listOf(text("Material"), text("Condición"), date)
            SIC22 -> listOf(text("Material"), text("Código de señal", 18.0),
                text("Número del poste kilométrico", 20.0), text("Condición"), date)
            SIC23 -> listOf(number("Ancho (m)"), text("Descripción", 45.0), date)
        }
    }

    fun values(item: InventoryRecordForExport): List<Any?> {
        val r = item.record
        require(r.sicCode == code) { "El registro ${r.id} no pertenece a $code." }
        fun pr(value: String?): String? = value?.let {
            require(it.trim().matches(Regex("[0-9]{1,4}"))) { "$code: PR inválido en el registro ${r.id}." }
            it.trim().padStart(4, '0')
        }
        fun cls(value: String) = value.padStart(2, '0')
        fun <T : Any> detail(value: T?): T = requireNotNull(value) { "$code: falta el detalle del registro ${r.id}." }
        val date = try {
            LocalDate.parse(r.surveyDate, DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT))
        } catch (e: Exception) { throw IllegalArgumentException("$code: fecha inválida en el registro ${r.id} (${r.surveyDate}).", e) }
        val common = listOf(r.routeCode, r.roadbedCode, pr(r.startPrCode), r.startDistanceM) +
            (if (this == SIC18) emptyList() else listOf(pr(r.endPrCode), r.endDistanceM)) +
            (if (this in listOf(SIC17, SIC18)) emptyList() else listOf(r.sideCode))
        return common + when (this) {
            SIC17 -> detail(item.sic17).let { d -> listOf(cls(d.classCode), d.typeCode, d.bridgeCode,
                d.inventoriedCode, d.spans, d.dimension1LengthM, d.dimension2LowerHeightM,
                d.structuralConditionCode, d.functionalConditionCode, date, d.serviceTypeCode,
                d.singularityCode, d.singularityName, d.dimension3UpperHeightM) }
            SIC18 -> detail(item.sic18).let { d -> listOf(cls(d.classCode), d.typeCode, d.spans,
                d.crossSectionCode, d.dimension1M, if(d.crossSectionCode=="2" && d.sectionShape=="CIRCULAR") null else d.dimension2M, d.structuralConditionCode, d.functionalConditionCode, date) }
            SIC19 -> detail(item.sic19).let { d -> listOf(cls(d.classCode), d.typeCode, d.crossSectionCode,
                d.structuralConditionCode, d.functionalConditionCode, date) }
            SIC20 -> detail(item.sic20).let { d -> listOf(cls(d.classCode), d.typeCode, d.dimension1M,
                if (d.classCode == "14") null else d.dimension2M, d.structuralConditionCode, d.functionalConditionCode, date) }
            SIC21 -> detail(item.sic21).let { d -> listOf(cls(d.classCode), d.typeCode, if(d.classCode in setOf("18","20")) null else d.materialCode, d.conditionCode, date) }
            SIC22 -> detail(item.sic22).let { d -> listOf(cls(d.classCode), d.typeCode, d.materialCode,
                if (d.typeCode in listOf("1", "2", "3")) d.signalCode else null,
                if (d.typeCode == "4") d.kilometerPostNumber else null, d.conditionCode, date) }
            SIC23 -> detail(item.sic23).let { d -> listOf(cls(d.classCode),
                if (d.classCode in listOf("21", "23")) d.typeCode else null,
                if (d.classCode == "21" && d.typeCode in listOf("1", "2")) d.widthM else null, d.description, date) }
        }
    }
}
