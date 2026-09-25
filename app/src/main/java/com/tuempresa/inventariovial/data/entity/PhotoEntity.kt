package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "photos",

    foreignKeys = [
        ForeignKey(
            entity = InventoryRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index("recordId"),

        Index(
            value = [
                "recordId",
                "photoIndex"
            ],
            unique = true
        )
    ]
)
data class PhotoEntity(

    @PrimaryKey
    val id: String,

    val recordId: String,

    val photoIndex: Int,

    val localPath: String,

    val isPrimary: Boolean,

    val generatedFileName: String?,

    val driveFolderId: String?,

    val driveFileId: String?,

    val syncStatus: String,

    val createdAt: Long,
    @androidx.room.ColumnInfo(defaultValue = "''") val originalPath: String = localPath,
    val stampedPath: String? = null,
    val scapInspectionId: String? = null,
    val scapElementCode: String? = null,
    val photoCategory: String? = null
)
