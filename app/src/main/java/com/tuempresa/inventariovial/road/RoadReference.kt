package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import java.util.Locale
import kotlin.math.*

data class RoadRoute(val routeCode: String, val name: String = "")
data class RoadPolylinePoint(
    val routeCode: String, val roadbedCode: String, val segmentId: String,
    val sequence: Int, val latitude: Double, val longitude: Double, val chainageM: Double
)
data class RoadSegment(
    val routeCode: String, val roadbedCode: String, val segmentId: String,
    val points: List<RoadPolylinePoint>
)
data class RoadPr(
    val routeCode: String, val roadbedCode: String, val prCode: String,
    val latitude: Double, val longitude: Double, val chainageM: Double
)
data class RoadReferenceData(
    val routes: List<RoadRoute> = emptyList(),
    val segments: List<RoadSegment> = emptyList(),
    val prs: List<RoadPr> = emptyList()
)

enum class PositionSource { OFFICIAL_PR, ESTIMATED_FROM_PR_CODE, KILOMETRIC, UNKNOWN }
enum class CaptureSource { MANUAL, GNSS_MAP_MATCH, SERVER, IMPORT }
data class RoadPosition(val chainageM: Double?, val source: PositionSource)

fun normalizedRoadCode(value: String) = value.trim().uppercase(Locale.ROOT)
fun normalizedPr(value: String) = value.trim().padStart(4, '0')

class ChainageCalculator(private val prs: List<RoadPr> = emptyList()) {
    fun calculate(record: com.tuempresa.inventariovial.data.entity.InventoryRecordEntity): RoadPosition =
        if(record.surveyDirection != null) {
            val chainage = com.tuempresa.inventariovial.field.SurveyOrder.chainage(record.startPrCode,record.startDistanceM.toString())
            RoadPosition(chainage,if(chainage == null) PositionSource.UNKNOWN else PositionSource.KILOMETRIC)
        } else calculate(record.routeCode,record.roadbedCode,record.startPrCode,record.startDistanceM)
    fun calculate(routeCode: String, roadbedCode: String, prCode: String, distanceM: Double): RoadPosition {
        if (!distanceM.isFinite() || distanceM < 0 || prCode.isBlank()) return RoadPosition(null, PositionSource.UNKNOWN)
        val catalog = prs.filter { normalizedRoadCode(it.routeCode) == normalizedRoadCode(routeCode) &&
            normalizedRoadCode(it.roadbedCode) == normalizedRoadCode(roadbedCode) }
        val official = catalog.singleOrNull { normalizedPr(it.prCode) == normalizedPr(prCode) }
        if (official != null) return RoadPosition(official.chainageM + distanceM, PositionSource.OFFICIAL_PR)
        // An existing catalog with a missing PR must not silently fall back to an invented kilometer.
        if (catalog.isNotEmpty()) return RoadPosition(null, PositionSource.UNKNOWN)
        val km = prCode.trim().takeIf { it.matches(Regex("[0-9]{1,4}")) }?.toIntOrNull()
            ?: return RoadPosition(null, PositionSource.UNKNOWN)
        return RoadPosition(km * 1000.0 + distanceM, PositionSource.ESTIMATED_FROM_PR_CODE)
    }
}

data class SequencedRecord(val item: InventoryRecordWithPhotos, val computedSequence: Int?, val position: RoadPosition)

object RoadOrdering {
    fun order(records: List<InventoryRecordWithPhotos>, calculator: ChainageCalculator): List<SequencedRecord> {
        val sorted = records.filter { it.record.status != "DRAFT" }.map { item ->
            val r = item.record
            SequencedRecord(item, null, calculator.calculate(r))
        }.sortedWith(compareBy<SequencedRecord> { normalizedRoadCode(it.item.record.routeCode) }
            .thenBy { normalizedRoadCode(it.item.record.roadbedCode) }
            .thenBy { it.position.chainageM ?: Double.POSITIVE_INFINITY }
            .thenBy { it.item.record.createdAt }.thenBy { it.item.record.id })
        val counters = mutableMapOf<Pair<String, String>, Int>()
        return sorted.map {
            val r = it.item.record
            if (r.status != "ACTIVE") it else {
                val key = normalizedRoadCode(r.routeCode) to normalizedRoadCode(r.roadbedCode)
                val sequence = (counters[key] ?: 0) + 1
                counters[key] = sequence
                it.copy(computedSequence = sequence)
            }
        }
    }
}

data class MatchConfig(val maxDistanceToAxisM: Double = 30.0, val maxAccuracyM: Double = 15.0,
    val ambiguityDistanceM: Double = 3.0, val sideDeadbandM: Double = 1.0)
data class RoadMatchResult(
    val routeCode: String, val roadbedCode: String, val prCode: String?, val distanceFromPrM: Double?,
    val chainageM: Double, val distanceToRoadAxisM: Double, val confidence: Double,
    val source: CaptureSource = CaptureSource.GNSS_MAP_MATCH,
    val suggestedSide: String?, val segmentId: String,
    val projectedLatitude: Double, val projectedLongitude: Double
)

object GeoMath {
    private const val R = 6_371_008.8
    fun validCoordinate(lat: Double, lon: Double) = lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0
    fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val a = sin(Math.toRadians(lat2-lat1)/2).pow(2) + cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) * sin(Math.toRadians(lon2-lon1)/2).pow(2)
        return 2 * R * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
    fun polylineLength(points: List<Pair<Double, Double>>) = points.zipWithNext().sumOf { (a,b) ->
        distanceM(a.first, a.second, b.first, b.second)
    }
}

class RoadMatcher(private val data: RoadReferenceData, private val config: MatchConfig = MatchConfig()) {
    private data class Candidate(val segment: RoadSegment, val a: RoadPolylinePoint, val b: RoadPolylinePoint,
        val t: Double, val distance: Double, val cross: Double)

    fun match(latitude: Double, longitude: Double, accuracy: Double): RoadMatchResult? {
        if (!GeoMath.validCoordinate(latitude, longitude) || !accuracy.isFinite() || accuracy < 0 || accuracy > config.maxAccuracyM) return null
        val scaleX = 111_195.08 * cos(Math.toRadians(latitude))
        val scaleY = 111_195.08
        val candidates = data.segments.flatMap { segment ->
            segment.points.sortedBy { it.sequence }.zipWithNext().mapNotNull { (p1,p2) ->
                if (p1.chainageM == p2.chainageM) return@mapNotNull null
                // Orient using chainage, even when the supplied geometry is reversed.
                val (a,b) = if (p1.chainageM < p2.chainageM) p1 to p2 else p2 to p1
                val ax = (a.longitude-longitude)*scaleX; val ay = (a.latitude-latitude)*scaleY
                val dx = (b.longitude-a.longitude)*scaleX; val dy = (b.latitude-a.latitude)*scaleY
                val lengthSquared = dx*dx+dy*dy
                if (lengthSquared < 0.000001) return@mapNotNull null
                val t = (-(ax*dx+ay*dy)/lengthSquared).coerceIn(0.0,1.0)
                Candidate(segment, a,b,t,hypot(ax+t*dx,ay+t*dy),dx*(-ay)-dy*(-ax))
            }
        }.sortedBy { it.distance }
        val c = candidates.firstOrNull() ?: return null
        if (c.distance > config.maxDistanceToAxisM) return null
        // Do not confidently select one of two adjacent carriageways at a junction.
        val competing = candidates.firstOrNull { it.segment.routeCode != c.segment.routeCode || it.segment.roadbedCode != c.segment.roadbedCode }
        if (competing != null && competing.distance-c.distance <= max(config.ambiguityDistanceM, accuracy)) return null
        val chainage = c.a.chainageM+c.t*(c.b.chainageM-c.a.chainageM)
        val pr = LinearReferenceEngine.previousPr(data.prs, c.segment.routeCode,c.segment.roadbedCode,chainage)
        val side = if (abs(c.cross)<1e-9 || c.distance <= max(config.sideDeadbandM, accuracy)) null else if(c.cross>0) "I" else "D"
        return RoadMatchResult(c.segment.routeCode,c.segment.roadbedCode,pr?.prCode,pr?.let { chainage-it.chainageM },
            chainage,c.distance,(1-c.distance/max(config.maxDistanceToAxisM,0.01)).coerceIn(0.0,1.0),
            suggestedSide=side,segmentId=c.segment.segmentId,
            projectedLatitude=c.a.latitude+c.t*(c.b.latitude-c.a.latitude),
            projectedLongitude=c.a.longitude+c.t*(c.b.longitude-c.a.longitude))
    }
}

class LinearReferenceEngine(private val data: RoadReferenceData, private val config: MatchConfig = MatchConfig()) {
    fun locate(latitude: Double, longitude: Double, accuracy: Double) = RoadMatcher(data,config).match(latitude,longitude,accuracy)
    companion object {
        fun previousPr(prs: List<RoadPr>, routeCode: String, roadbedCode: String, chainageM: Double): RoadPr? = prs
            .filter { normalizedRoadCode(it.routeCode)==normalizedRoadCode(routeCode) &&
                normalizedRoadCode(it.roadbedCode)==normalizedRoadCode(roadbedCode) && it.chainageM <= chainageM }
            .maxByOrNull { it.chainageM }
    }
}
