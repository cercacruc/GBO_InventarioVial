package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey


@Entity(
    tableName = "sic21_details",

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
data class Sic21Entity(

    val recordId: String,

    val classCode: String,

    val typeCode: String,

    val materialCode: String,

    val conditionCode: String
)