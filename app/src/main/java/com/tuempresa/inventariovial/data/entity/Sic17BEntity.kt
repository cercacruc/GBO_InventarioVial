package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "sic17b_details", primaryKeys = ["recordId"], foreignKeys = [ForeignKey(
    entity = InventoryRecordEntity::class, parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE)])
data class Sic17BEntity(
    val recordId: String,
    val bridgeCode: String = "",
    val designLoad: String = "",
    val maximumCapacity: String = "",
    val wearingSurface: String = "",
    val vehicleRestraintCode: String = "",
    val mainSpanM: String = "",
    val boundaryCode: String = "",
    val crossSectionCode: String = "",
    val beams: String = "",
    val slabMaterialCode: String = "",
    val beamMaterialCode: String = "",
    val abutmentElevationCode: String = "",
    val abutmentMaterialCode: String = "",
    val abutmentFoundationCode: String = "",
    val pierElevationCode: String = "",
    val pierMaterialCode: String = "",
    val pierFoundationCode: String = "",
    val comments: String = ""
) {
    fun values(): Map<String,String> = mapOf(
        "bridgeCode" to bridgeCode,
        "designLoad" to designLoad,
        "maximumCapacity" to maximumCapacity,
        "wearingSurface" to wearingSurface,
        "vehicleRestraintCode" to vehicleRestraintCode,
        "mainSpanM" to mainSpanM,
        "boundaryCode" to boundaryCode,
        "crossSectionCode" to crossSectionCode,
        "beams" to beams,
        "slabMaterialCode" to slabMaterialCode,
        "beamMaterialCode" to beamMaterialCode,
        "abutmentElevationCode" to abutmentElevationCode,
        "abutmentMaterialCode" to abutmentMaterialCode,
        "abutmentFoundationCode" to abutmentFoundationCode,
        "pierElevationCode" to pierElevationCode,
        "pierMaterialCode" to pierMaterialCode,
        "pierFoundationCode" to pierFoundationCode,
        "comments" to comments
    )
    companion object {
        fun from(recordId: String, values: Map<String,String>) = Sic17BEntity(recordId,
            bridgeCode = values["bridgeCode"].orEmpty().trim(),
            designLoad = values["designLoad"].orEmpty().trim(),
            maximumCapacity = values["maximumCapacity"].orEmpty().trim(),
            wearingSurface = values["wearingSurface"].orEmpty().trim(),
            vehicleRestraintCode = values["vehicleRestraintCode"].orEmpty().trim(),
            mainSpanM = values["mainSpanM"].orEmpty().trim(),
            boundaryCode = values["boundaryCode"].orEmpty().trim(),
            crossSectionCode = values["crossSectionCode"].orEmpty().trim(),
            beams = values["beams"].orEmpty().trim(),
            slabMaterialCode = values["slabMaterialCode"].orEmpty().trim(),
            beamMaterialCode = values["beamMaterialCode"].orEmpty().trim(),
            abutmentElevationCode = values["abutmentElevationCode"].orEmpty().trim(),
            abutmentMaterialCode = values["abutmentMaterialCode"].orEmpty().trim(),
            abutmentFoundationCode = values["abutmentFoundationCode"].orEmpty().trim(),
            pierElevationCode = values["pierElevationCode"].orEmpty().trim(),
            pierMaterialCode = values["pierMaterialCode"].orEmpty().trim(),
            pierFoundationCode = values["pierFoundationCode"].orEmpty().trim(),
            comments = values["comments"].orEmpty().trim()
        )
    }
}
