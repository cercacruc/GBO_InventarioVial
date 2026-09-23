package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey


@Entity(
    tableName = "sic17_details",

    primaryKeys = [
        "recordId"
    ],

    foreignKeys = [
        ForeignKey(
            entity = InventoryRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Sic17Entity(

    val recordId: String,

    val classCode: String,

    val typeCode: String,

    val bridgeCode: String?,

    val inventoriedCode: String,

    val spans: Int?,

    val dimension1LengthM: Double?,

    val dimension2LowerHeightM: Double?,

    val structuralConditionCode: String,

    val functionalConditionCode: String,

    val serviceTypeCode: String,

    val singularityCode: String,

    val singularityName: String?,

    val dimension3UpperHeightM: Double?
)