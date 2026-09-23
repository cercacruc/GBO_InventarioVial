package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey


@Entity(
    tableName = "sic19_details",

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
data class Sic19Entity(

    val recordId: String,

    val classCode: String,

    val typeCode: String,

    val crossSectionCode: String,

    val structuralConditionCode: String,

    val functionalConditionCode: String
)