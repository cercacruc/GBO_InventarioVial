package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "track_points", foreignKeys = [ForeignKey(entity = InventoryRecordEntity::class,
    parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["recordId", "sequence"], unique = true)])
data class TrackPointEntity(
    @PrimaryKey val id: String, val recordId: String, val sequence: Int,
    val latitude: Double, val longitude: Double, val altitude: Double?,
    val accuracy: Double, val timestamp: Long
)
