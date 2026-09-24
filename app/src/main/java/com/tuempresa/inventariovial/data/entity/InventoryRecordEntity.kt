package com.tuempresa.inventariovial.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "inventory_records",

    indices = [
        Index("routeCode"),
        Index("sicCode"),
        Index("surveyDate")
    ]
)
data class InventoryRecordEntity(

    @PrimaryKey
    val id: String,

    // SIC-17, SIC-18, ...
    val sicCode: String,

    // PUENTE, ALCANTARILLA, CUNETA,
    // BADEN, TUNEL, MURO, etc.
    val assetType: String,


    // =====================================================
    // UBICACIÓN VIAL
    // =====================================================

    val routeCode: String,

    val roadbedCode: String,

    val startPrCode: String,

    val startDistanceM: Double,

    val endPrCode: String?,

    val endDistanceM: Double?,

    val sideCode: String?,


    // =====================================================
    // GPS
    // =====================================================

    val latitude: Double,

    val longitude: Double,

    val altitudeM: Double?,

    val gpsAccuracyM: Double?,


    // =====================================================
    // REGISTRO
    // =====================================================

    val surveyDate: String,

    val observations: String?,

    val status: String,

    val photoSyncStatus: String,

    val excelSyncStatus: String,


    // =====================================================
    // AUDITORÍA
    // =====================================================

    val createdAt: Long,

    val updatedAt: Long,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val endGpsAccuracyM: Double? = null,
    @androidx.room.ColumnInfo(defaultValue = "'MANUAL'") val locationSource: String = "MANUAL",
    @androidx.room.ColumnInfo(defaultValue = "'MANUAL'") val sideSource: String = "MANUAL",
    val sessionId: String? = null,
    val gpsTimestamp: Long? = null,
    val endGpsTimestamp: Long? = null,
    @androidx.room.ColumnInfo(defaultValue = "'TABLET'") val gnssProvider: String = "TABLET",
    val gnssFixType: String? = null,
    val verticalAccuracyM: Double? = null,
    val satellites: Int? = null,
    val hdop: Double? = null,
    val correctionAge: Double? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val isRtkFixed: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "'PENDING'") val serverSyncStatus: String = "PENDING",
    val serverSyncError: String? = null,
    val segment: String? = null,
    val surveyDirection: String? = null
)
