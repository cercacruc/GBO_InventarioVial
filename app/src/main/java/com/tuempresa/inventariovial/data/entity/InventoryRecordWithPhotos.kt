package com.tuempresa.inventariovial.data.entity
import androidx.room.Embedded
import androidx.room.Relation
data class InventoryRecordWithPhotos(
    @Embedded val record: InventoryRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId") val photos: List<PhotoEntity>
)
