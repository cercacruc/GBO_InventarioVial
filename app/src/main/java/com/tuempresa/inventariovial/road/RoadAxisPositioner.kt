package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity

/** A selected route is a hard scope, even if its geometry is absent. */
fun RoadReferenceData.forSelectedRoute(routeCode: String): RoadReferenceData {
    val route = normalizedRoadCode(routeCode)
    if (route.isEmpty()) return this
    return copy(routes = routes.filter { normalizedRoadCode(it.routeCode) == route },
        segments = segments.filter { normalizedRoadCode(it.routeCode) == route },
        prs = prs.filter { normalizedRoadCode(it.routeCode) == route })
}

fun RoadReferenceData.forSelectedRouteAndRoadbed(routeCode: String, roadbedCode: String): RoadReferenceData {
    val route = forSelectedRoute(routeCode)
    if (routeCode.isBlank() || roadbedCode.isBlank()) return route
    val bed = normalizedRoadCode(roadbedCode)
    val specific = route.segments.filter { normalizedRoadCode(it.roadbedCode) == bed }
    return route.copy(segments = specific.ifEmpty { route.segments.filter { normalizedRoadCode(it.roadbedCode) == "EJE" } })
}

/** Uses the existing matcher and the initial GNSS fix for point and linear assets. */
class RoadAxisPositioner(private val reference: RoadReferenceData, private val config: MatchConfig = MatchConfig()) {
    fun position(record: InventoryRecordEntity): InventoryRecordEntity {
        val match = record.gpsAccuracyM?.let {
            LinearReferenceEngine(reference.forSelectedRouteAndRoadbed(record.routeCode, record.roadbedCode), config)
                .locate(record.latitude, record.longitude, it)
        }
        return record.copy(axisMeasureM = match?.chainageM, distanceToRoadAxisM = match?.distanceToRoadAxisM,
            roadMatchConfidence = match?.confidence, matchedSegmentId = match?.segmentId,
            projectedLatitude = match?.projectedLatitude, projectedLongitude = match?.projectedLongitude)
    }
}
