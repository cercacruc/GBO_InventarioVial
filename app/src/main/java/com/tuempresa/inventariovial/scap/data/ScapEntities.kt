package com.tuempresa.inventariovial.scap.data

import androidx.room.*
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity

@Entity(tableName="scap_inspections", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=InventoryRecordEntity::class,parentColumns=["id"],childColumns=["roadRecordId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["roadRecordId"], unique=true)])
data class ScapInspectionEntity(
    val id: String,
    val roadRecordId: String,
    val bridgeName: String,
    val bridgeCode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String,
    val deviceId: String,
    val status: String,
    val syncStatus: String,
    val latitude: Double?,
    val longitude: Double?,
    val gpsAccuracyM: Double?,
    val gpsTimestamp: Long?,
    val locationSource: String
)

@Entity(tableName="scap_values", primaryKeys=["inspectionId","ownerId","key"],
    foreignKeys=[ForeignKey(entity=ScapInspectionEntity::class,parentColumns=["id"],childColumns=["inspectionId"],onDelete=ForeignKey.CASCADE)],
    indices=[])
data class ScapFieldValueEntity(
    val inspectionId: String,
    val ownerId: String,
    val key: String,
    val value: String,
    val source: String
)

@Entity(tableName="scap_spans", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=ScapInspectionEntity::class,parentColumns=["id"],childColumns=["inspectionId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["inspectionId","spanIndex"], unique=true)])
data class ScapSpanEntity(
    val id: String,
    val inspectionId: String,
    val spanIndex: Int,
    val lengthM: Double?,
    val category: String,
    val type: String,
    val secondaryCharacteristic: String,
    val edgeCondition: String,
    val predominantMaterial: String
)

@Entity(tableName="scap_substructures", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=ScapInspectionEntity::class,parentColumns=["id"],childColumns=["inspectionId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["inspectionId","kind","elementIndex"], unique=true)])
data class ScapSubstructureEntity(
    val id: String,
    val inspectionId: String,
    val kind: String,
    val elementIndex: Int,
    val elevationType: String,
    val elevationMaterial: String,
    val foundationType: String,
    val foundationMaterial: String,
    val soil: String
)

@Entity(tableName="scap_supports", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=ScapInspectionEntity::class,parentColumns=["id"],childColumns=["inspectionId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["inspectionId","supportIndex"], unique=true)])
data class ScapSupportEntity(
    val id: String,
    val inspectionId: String,
    val supportIndex: Int,
    val type: String,
    val material: String,
    val location: String,
    val number: Int?
)

@Entity(tableName="scap_elements", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=ScapInspectionEntity::class,parentColumns=["id"],childColumns=["inspectionId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["inspectionId","elementCode"], unique=true)])
data class ScapElementEntity(
    val id: String,
    val inspectionId: String,
    val elementCode: String,
    val description: String,
    val quantity: Double?,
    val unit: String,
    val importanceFactor: Double,
    val group: String,
    val isPresent: Boolean
)

@Entity(tableName="scap_conditions", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=ScapElementEntity::class,parentColumns=["id"],childColumns=["scapElementId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["scapElementId"], unique=true)])
data class ScapElementConditionEntity(
    val id: String,
    val scapElementId: String,
    val percent0: Double?,
    val percent1: Double?,
    val percent2: Double?,
    val percent3: Double?,
    val percent4: Double?,
    val percent5: Double?
)

@Entity(tableName="scap_defects", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=ScapInspectionEntity::class,parentColumns=["id"],childColumns=["inspectionId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["inspectionId"], unique=false)])
data class ScapDefectEntity(
    val id: String,
    val inspectionId: String,
    val elementCode: String?,
    val description: String,
    val locationDescription: String,
    val photoId: String?,
    val aiSuggested: Boolean,
    val validatedByUser: Boolean,
    val createdAt: Long
)

@Entity(tableName="scap_sketches", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=ScapInspectionEntity::class,parentColumns=["id"],childColumns=["inspectionId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["inspectionId"], unique=false)])
data class ScapSketchEntity(
    val id: String,
    val inspectionId: String,
    val type: String,
    val localUri: String
)

@Entity(tableName="scap_profile_points", primaryKeys=["id"],
    foreignKeys=[ForeignKey(entity=ScapInspectionEntity::class,parentColumns=["id"],childColumns=["inspectionId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["inspectionId","pointIndex"], unique=true)])
data class ScapProfilePointEntity(
    val id: String,
    val inspectionId: String,
    val pointIndex: Int,
    val distanceM: Double?,
    val downstreamM: Double?,
    val upstreamM: Double?,
    val axisM: Double?
)

