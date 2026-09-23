package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "sic23_details",
    primaryKeys = ["recordId"],
    foreignKeys = [ForeignKey(entity = InventoryRecordEntity::class,
        parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE)]
)
data class Sic23Entity(
    val recordId: String,
    val classCode: String,
    val typeCode: String?,
    val widthM: Double?,
    val description: String
)
