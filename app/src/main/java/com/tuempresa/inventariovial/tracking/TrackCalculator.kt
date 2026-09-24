package com.tuempresa.inventariovial.tracking

import com.tuempresa.inventariovial.data.entity.TrackPointEntity
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import kotlin.math.abs
import kotlin.math.max

data class TrackConfig(val maxAccuracyM: Double = 10.0, val minDistanceM: Double = 3.0,
    val maxAgeMs: Long = 15_000, val maxSpeedMps: Double = 45.0, val sampleIntervalMs: Long = 3_000)

object TrackFilter {
    fun accepts(previous: TrackPointEntity?, fix: GeoLocation, now: Long, config: TrackConfig = TrackConfig()): Boolean {
        if (!GeoMath.validCoordinate(fix.latitude,fix.longitude) || !fix.accuracyHorizontal.isFinite() ||
            fix.accuracyHorizontal < 0 || fix.accuracyHorizontal > config.maxAccuracyM ||
            fix.timestamp<=0 || now-fix.timestamp !in 0..config.maxAgeMs) return false
        if (previous==null) return true
        val elapsed=(fix.timestamp-previous.timestamp)/1000.0
        if (elapsed<=0) return false
        val distance=GeoMath.distanceM(previous.latitude,previous.longitude,fix.latitude,fix.longitude)
        return distance >= max(config.minDistanceM, max(previous.accuracy,fix.accuracyHorizontal.toDouble())) && distance/elapsed <= config.maxSpeedMps
    }
}

data class TrackLengths(val rawTrackLength: Double, val matchedRoadLength: Double?, val startEndStraightDistance: Double,
    val isEstimate: Boolean = true, val hasGaps: Boolean = false)

object TrackContinuity {
    fun runs(points: List<TrackPointEntity>): List<List<TrackPointEntity>> {
        val runs=mutableListOf<MutableList<TrackPointEntity>>()
        for(point in points.sortedBy { it.sequence }) {
            val previous=runs.lastOrNull()?.lastOrNull()
            if(previous==null || point.sequence!=previous.sequence+1) runs.add(mutableListOf())
            runs.last().add(point)
        }
        return runs
    }
}

object TrackCalculator {
    fun lengths(points: List<TrackPointEntity>, reference: RoadReferenceData = RoadReferenceData(), config: MatchConfig = MatchConfig()): TrackLengths {
        val sorted=points.sortedBy { it.sequence }
        val runs=TrackContinuity.runs(sorted)
        if(sorted.size<2) return TrackLengths(0.0,null,0.0)
        val matches=sorted.map { RoadMatcher(reference,config).match(it.latitude,it.longitude,it.accuracy) }
        // A partial match must not be shown as a complete road length. No bridging gaps/branches.
        val continuous=runs.size==1 && matches.all { it!=null } && matches.filterNotNull().map { Triple(it.routeCode,it.roadbedCode,it.segmentId) }.distinct().size==1
        val chainages=matches.filterNotNull().map { it.chainageM }
        val differences=chainages.zipWithNext().map { (a,b)->b-a }
        val monotone=differences.all { it>=0 } || differences.all { it<=0 }
        val matched=if(continuous && monotone) abs(chainages.last()-chainages.first()) else null
        val first=sorted.first(); val last=sorted.last()
        return TrackLengths(runs.sumOf { run -> GeoMath.polylineLength(run.map { it.latitude to it.longitude }) },matched,
            GeoMath.distanceM(first.latitude,first.longitude,last.latitude,last.longitude),hasGaps=runs.size>1)
    }
}
