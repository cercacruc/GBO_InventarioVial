package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey


@Entity(
    tableName = "sic18_details",

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
data class Sic18Entity(

    val recordId: String,

    val classCode: String,

    val typeCode: String,

    val spans: Int?,

    val crossSectionCode: String,

    val dimension1M: Double?,

    val dimension2M: Double?,

    val structuralConditionCode: String,

    val functionalConditionCode: String,
    val sectionShape: String? = null,
    val structuralDamagePercent: Double? = null,
    val functionalObstructionPercent: Double? = null
)