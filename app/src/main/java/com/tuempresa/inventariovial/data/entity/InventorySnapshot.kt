package com.tuempresa.inventariovial.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class InventorySnapshot(
    @Embedded val record: InventoryRecordEntity,
    @Relation(parentColumn="id",entityColumn="recordId") val photos: List<PhotoEntity>,
    @Relation(parentColumn="id",entityColumn="recordId") val track: List<TrackPointEntity>,
    @Relation(parentColumn="id",entityColumn="recordId") val sic17: Sic17Entity?,
    @Relation(parentColumn="id",entityColumn="recordId") val sic18: Sic18Entity?,
    @Relation(parentColumn="id",entityColumn="recordId") val sic19: Sic19Entity?,
    @Relation(parentColumn="id",entityColumn="recordId") val sic20: Sic20Entity?,
    @Relation(parentColumn="id",entityColumn="recordId") val sic21: Sic21Entity?,
    @Relation(parentColumn="id",entityColumn="recordId") val sic22: Sic22Entity?,
    @Relation(parentColumn="id",entityColumn="recordId") val sic23: Sic23Entity?,
    @Relation(parentColumn="id",entityColumn="recordId") val sic17a: Sic17AEntity?,
    @Relation(parentColumn="id",entityColumn="recordId") val sic17b: Sic17BEntity?,
    @Relation(parentColumn="id",entityColumn="recordId") val sic18a: Sic18AEntity?
)
