package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.dao.InventoryDao
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoadAxisStorage(private val dao: InventoryDao) {
    suspend fun refresh(reference: RoadReferenceData, config: MatchConfig, routeCode: String? = null): Int = withContext(Dispatchers.Default) {
        val positioner = RoadAxisPositioner(reference, config)
        var matched = 0
        dao.recordsForAxis().filter { routeCode == null || normalizedRoadCode(it.routeCode) == normalizedRoadCode(routeCode) }.forEach {
            val positioned = positioner.position(it)
            if (persist(positioned) && positioned.axisMeasureM != null) matched++
        }
        matched
    }

    suspend fun persist(r: InventoryRecordEntity): Boolean = dao.updateAxisPosition(r.id, r.routeCode,
        r.latitude, r.longitude, r.gpsAccuracyM, r.axisMeasureM, r.distanceToRoadAxisM, r.roadMatchConfidence,
        r.matchedSegmentId, r.projectedLatitude, r.projectedLongitude) == 1
}
