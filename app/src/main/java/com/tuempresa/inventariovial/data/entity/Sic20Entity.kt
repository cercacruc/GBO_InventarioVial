package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey


@Entity(
    tableName = "sic20_details",

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
data class Sic20Entity(

    val recordId: String,

    val classCode: String,

    val typeCode: String,

    val dimension1M: Double?,

    val dimension2M: Double?,

    val structuralConditionCode: String,

    val functionalConditionCode: String?
)