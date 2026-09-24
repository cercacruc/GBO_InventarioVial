package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "sic17a_details", primaryKeys = ["recordId"], foreignKeys = [ForeignKey(
    entity = InventoryRecordEntity::class, parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE)])
data class Sic17AEntity(
    val recordId: String,
    val bridgeName: String = "",
    val bridgeCode: String = "",
    val constructionYear: String = "",
    val department: String = "",
    val province: String = "",
    val district: String = "",
    val nearbyTown: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val altitude: String = "",
    val lanes: String = "",
    val roadwayWidthM: String = "",
    val sidewalkWidthM: String = "",
    val deckWidthM: String = "",
    val superstructureWidthM: String = "",
    val alignmentCode: String = ""
) {
    fun values(): Map<String,String> = mapOf(
        "bridgeName" to bridgeName,
        "bridgeCode" to bridgeCode,
        "constructionYear" to constructionYear,
        "department" to department,
        "province" to province,
        "district" to district,
        "nearbyTown" to nearbyTown,
        "latitude" to latitude,
        "longitude" to longitude,
        "altitude" to altitude,
        "lanes" to lanes,
        "roadwayWidthM" to roadwayWidthM,
        "sidewalkWidthM" to sidewalkWidthM,
        "deckWidthM" to deckWidthM,
        "superstructureWidthM" to superstructureWidthM,
        "alignmentCode" to alignmentCode
    )
    companion object {
        fun from(recordId: String, values: Map<String,String>) = Sic17AEntity(recordId,
            bridgeName = values["bridgeName"].orEmpty().trim(),
            bridgeCode = values["bridgeCode"].orEmpty().trim(),
            constructionYear = values["constructionYear"].orEmpty().trim(),
            department = values["department"].orEmpty().trim(),
            province = values["province"].orEmpty().trim(),
            district = values["district"].orEmpty().trim(),
            nearbyTown = values["nearbyTown"].orEmpty().trim(),
            latitude = values["latitude"].orEmpty().trim(),
            longitude = values["longitude"].orEmpty().trim(),
            altitude = values["altitude"].orEmpty().trim(),
            lanes = values["lanes"].orEmpty().trim(),
            roadwayWidthM = values["roadwayWidthM"].orEmpty().trim(),
            sidewalkWidthM = values["sidewalkWidthM"].orEmpty().trim(),
            deckWidthM = values["deckWidthM"].orEmpty().trim(),
            superstructureWidthM = values["superstructureWidthM"].orEmpty().trim(),
            alignmentCode = values["alignmentCode"].orEmpty().trim()
        )
    }
}
