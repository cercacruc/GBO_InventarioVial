package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey


@Entity(
    tableName = "sic22_details",

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
data class Sic22Entity(

    val recordId: String,

    val classCode: String,

    val typeCode: String,

    val materialCode: String,

    val signalCode: String?,

    val kilometerPostNumber: String?,

    val conditionCode: String,


    // Campos complementarios del cliente

    val signWidthM: Double?,

    val signHeightM: Double?,

    val lowerEdgeHeightM: Double?
)