package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "sic18a_details", primaryKeys = ["recordId"], foreignKeys = [ForeignKey(
    entity = InventoryRecordEntity::class, parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE)])
data class Sic18AEntity(
    val recordId: String,
    val classCode: String = "",
    val typeCode: String = "",
    val spans: String = "",
    val functionCode: String = "",
    val failureLocationCode: String = "",
    val failureTypeCode: String = "",
    val functionalStateCode: String = "",
    val probableCauseCode: String = ""
) {
    fun values(): Map<String,String> = mapOf(
        "classCode" to classCode,
        "typeCode" to typeCode,
        "spans" to spans,
        "functionCode" to functionCode,
        "failureLocationCode" to failureLocationCode,
        "failureTypeCode" to failureTypeCode,
        "functionalStateCode" to functionalStateCode,
        "probableCauseCode" to probableCauseCode
    )
    companion object {
        fun from(recordId: String, values: Map<String,String>) = Sic18AEntity(recordId,
            classCode = values["classCode"].orEmpty().trim(),
            typeCode = values["typeCode"].orEmpty().trim(),
            spans = values["spans"].orEmpty().trim(),
            functionCode = values["functionCode"].orEmpty().trim(),
            failureLocationCode = values["failureLocationCode"].orEmpty().trim(),
            failureTypeCode = values["failureTypeCode"].orEmpty().trim(),
            functionalStateCode = values["functionalStateCode"].orEmpty().trim(),
            probableCauseCode = values["probableCauseCode"].orEmpty().trim()
        )
    }
}
