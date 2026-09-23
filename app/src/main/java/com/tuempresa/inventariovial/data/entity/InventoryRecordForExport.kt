package com.tuempresa.inventariovial.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/** Snapshot transaccional de los datos oficiales; independiente de fotos y sincronización. */
data class InventoryRecordForExport(
    @Embedded val record: InventoryRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId") val sic17: Sic17Entity? = null,
    @Relation(parentColumn = "id", entityColumn = "recordId") val sic18: Sic18Entity? = null,
    @Relation(parentColumn = "id", entityColumn = "recordId") val sic19: Sic19Entity? = null,
    @Relation(parentColumn = "id", entityColumn = "recordId") val sic20: Sic20Entity? = null,
    @Relation(parentColumn = "id", entityColumn = "recordId") val sic21: Sic21Entity? = null,
    @Relation(parentColumn = "id", entityColumn = "recordId") val sic22: Sic22Entity? = null,
    @Relation(parentColumn = "id", entityColumn = "recordId") val sic23: Sic23Entity? = null
)
