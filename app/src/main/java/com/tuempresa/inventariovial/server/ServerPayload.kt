package com.tuempresa.inventariovial.server

import com.tuempresa.inventariovial.data.entity.*
import org.json.JSONArray
import org.json.JSONObject

object ServerPayload {
    private fun obj(vararg pairs: Pair<String,Any?>) = JSONObject().apply {
        pairs.forEach { (key,value) ->
            // Unknown GNSS accuracy must be JSON null; JSON does not permit Infinity/NaN.
            val finite=when(value) { is Double -> value.takeIf { it.isFinite() }; is Float -> value.takeIf { it.isFinite() }; else -> value }
            put(key,finite ?: JSONObject.NULL)
        }
    }
    fun encode(snapshot: InventorySnapshot, session: FieldSession? = null): JSONObject {
        val r=snapshot.record
        return obj(
            "schemaVersion" to 1, "id" to r.id, "updatedAt" to r.updatedAt, "status" to r.status,
            "record" to record(r),
            "location" to obj("latitude" to r.latitude,"longitude" to r.longitude,"altitudeM" to r.altitudeM,
                "horizontalAccuracyM" to r.gpsAccuracyM,"verticalAccuracyM" to r.verticalAccuracyM,
                "timestamp" to r.gpsTimestamp,"provider" to r.gnssProvider,"fixType" to r.gnssFixType,
                "satellites" to r.satellites,"hdop" to r.hdop,"correctionAge" to r.correctionAge,"isRtkFixed" to r.isRtkFixed,
                "source" to r.locationSource,"sideSource" to r.sideSource,
                "endLatitude" to r.endLatitude,"endLongitude" to r.endLongitude,
                "endAccuracyM" to r.endGpsAccuracyM,"endTimestamp" to r.endGpsTimestamp),
            "sic" to obj("SIC-17" to snapshot.sic17?.let { detail(it) },"SIC-18" to snapshot.sic18?.let { detail(it) },
                "SIC-19" to snapshot.sic19?.let { detail(it) },"SIC-20" to snapshot.sic20?.let { detail(it) },
                "SIC-21" to snapshot.sic21?.let { detail(it) },"SIC-22" to snapshot.sic22?.let { detail(it) },
                "SIC-23" to snapshot.sic23?.let { detail(it) },
                "SIC-17A" to snapshot.sic17a?.let { JSONObject(it.values()) },
                "SIC-17B" to snapshot.sic17b?.let { JSONObject(it.values()) },
                "SIC-18A" to snapshot.sic18a?.let { JSONObject(it.values()) }),
            "photos" to JSONArray(snapshot.photos.sortedBy { it.photoIndex }.map { photo ->
                obj("id" to photo.id,"recordId" to photo.recordId,"photoIndex" to photo.photoIndex,
                    "isPrimary" to photo.isPrimary,"fileName" to photo.generatedFileName,
                    "createdAt" to photo.createdAt,"hasOriginal" to photo.originalPath.isNotEmpty(),
                    "hasStampedCopy" to (photo.stampedPath!=null))
            }),
            "track" to JSONArray(snapshot.track.sortedBy { it.sequence }.map { point ->
                obj("id" to point.id,"recordId" to point.recordId,"sequence" to point.sequence,
                    "latitude" to point.latitude,"longitude" to point.longitude,"altitude" to point.altitude,
                    "accuracy" to point.accuracy,"timestamp" to point.timestamp)
            }),
            "session" to session?.let { obj("sessionId" to it.sessionId,"project" to it.project,"operator" to it.operator,
                "device" to it.device,"road" to it.road,"segment" to it.segment,"roadbed" to it.roadbed,
                "direction" to it.direction,"startTime" to it.startTime,"endTime" to it.endTime) }
        )
    }
    private fun record(r: InventoryRecordEntity) = obj(
        "id" to r.id,"sicCode" to r.sicCode,"assetType" to r.assetType,"routeCode" to r.routeCode,
        "roadbedCode" to r.roadbedCode,"startPrCode" to r.startPrCode,"startDistanceM" to r.startDistanceM,
        "endPrCode" to r.endPrCode,"endDistanceM" to r.endDistanceM,"sideCode" to r.sideCode,
        "surveyDate" to r.surveyDate,"observations" to r.observations,"createdAt" to r.createdAt,"sessionId" to r.sessionId,
        "segment" to r.segment,"surveyDirection" to r.surveyDirection
    )
    private fun detail(d: Sic17Entity) = obj(
        "recordId" to d.recordId,
        "classCode" to d.classCode,
        "typeCode" to d.typeCode,
        "bridgeCode" to d.bridgeCode,
        "inventoriedCode" to d.inventoriedCode,
        "spans" to d.spans,
        "dimension1LengthM" to d.dimension1LengthM,
        "dimension2LowerHeightM" to d.dimension2LowerHeightM,
        "structuralConditionCode" to d.structuralConditionCode,
        "functionalConditionCode" to d.functionalConditionCode,
        "serviceTypeCode" to d.serviceTypeCode,
        "singularityCode" to d.singularityCode,
        "singularityName" to d.singularityName,
        "dimension3UpperHeightM" to d.dimension3UpperHeightM
    )
    private fun detail(d: Sic18Entity) = obj(
        "sectionShape" to d.sectionShape,
        "structuralDamagePercent" to d.structuralDamagePercent,
        "functionalObstructionPercent" to d.functionalObstructionPercent,
        "recordId" to d.recordId,
        "classCode" to d.classCode,
        "typeCode" to d.typeCode,
        "spans" to d.spans,
        "crossSectionCode" to d.crossSectionCode,
        "dimension1M" to d.dimension1M,
        "dimension2M" to d.dimension2M,
        "structuralConditionCode" to d.structuralConditionCode,
        "functionalConditionCode" to d.functionalConditionCode
    )
    private fun detail(d: Sic19Entity) = obj(
        "recordId" to d.recordId,
        "classCode" to d.classCode,
        "typeCode" to d.typeCode,
        "crossSectionCode" to d.crossSectionCode,
        "structuralConditionCode" to d.structuralConditionCode,
        "functionalConditionCode" to d.functionalConditionCode
    )
    private fun detail(d: Sic20Entity) = obj(
        "recordId" to d.recordId,
        "classCode" to d.classCode,
        "typeCode" to d.typeCode,
        "dimension1M" to d.dimension1M,
        "dimension2M" to d.dimension2M,
        "structuralConditionCode" to d.structuralConditionCode,
        "functionalConditionCode" to d.functionalConditionCode
    )
    private fun detail(d: Sic21Entity) = obj(
        "recordId" to d.recordId,
        "classCode" to d.classCode,
        "typeCode" to d.typeCode,
        "materialCode" to d.materialCode,
        "conditionCode" to d.conditionCode
    )
    private fun detail(d: Sic22Entity) = obj(
        "recordId" to d.recordId,
        "classCode" to d.classCode,
        "typeCode" to d.typeCode,
        "materialCode" to d.materialCode,
        "signalCode" to d.signalCode,
        "kilometerPostNumber" to d.kilometerPostNumber,
        "conditionCode" to d.conditionCode,
        "signWidthM" to d.signWidthM,
        "signHeightM" to d.signHeightM,
        "lowerEdgeHeightM" to d.lowerEdgeHeightM
    )
    private fun detail(d: Sic23Entity) = obj(
        "recordId" to d.recordId,
        "classCode" to d.classCode,
        "typeCode" to d.typeCode,
        "widthM" to d.widthM,
        "description" to d.description
    )
}
