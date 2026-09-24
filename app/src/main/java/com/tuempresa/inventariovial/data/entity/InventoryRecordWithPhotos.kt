package com.tuempresa.inventariovial.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class InventoryRecordWithPhotos(
    @Embedded val record: InventoryRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId") val photos: List<PhotoEntity>,
    @Relation(parentColumn = "id", entityColumn = "recordId") val sic23: Sic23Entity? = null,
    @Relation(parentColumn = "id", entityColumn = "recordId") val sic18: Sic18Entity? = null
)
