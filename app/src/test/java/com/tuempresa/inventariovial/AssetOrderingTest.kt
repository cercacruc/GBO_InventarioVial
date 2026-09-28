package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.road.*
import org.junit.Assert.*
import org.junit.Test

class AssetOrderingTest {
    private fun asset(id: String, axis: Double?, created: Long, sic: String = "SIC-18", route: String = "PE-22A") =
        InventoryRecordWithPhotos(testRecord(id, created = created, sic = sic, route = route).copy(axisMeasureM = axis), emptyList())
    private fun photo(id: String, record: String, index: Int) = PhotoEntity(id, record, index, "/$id.jpg", index == 1,
        null, null, null, "PENDING", 1)

    @Test fun retroactiveCulvertRenumbersOnlyItsFamilyAndKeepsIdentityAndPhotos() {
        val captured = listOf(asset("A", 10000.0, 1), asset("B", 15000.0, 2), asset("C", 20000.0, 3),
            asset("D", 17000.0, 4)).map { it.copy(photos = listOf(photo("photo-${it.record.id}", it.record.id, 1))) }
        val before = RoadOrdering.orderAssetsForRoute(captured.take(3))
        val after = RoadOrdering.orderAssetsForRoute(captured)
        assertEquals(listOf("A", "B", "C"), before.map { it.recordId })
        assertEquals(listOf("A", "B", "D", "C"), after.map { it.recordId })
        assertEquals(listOf("ALC-1", "ALC-2", "ALC-3", "ALC-4"), after.map { it.shortCode })
        for (original in captured) {
            val actual = after.single { it.recordId == original.record.id }.item
            assertEquals(original, actual)
            assertEquals(original.record.id, actual.photos.single().recordId)
            assertEquals(original.record.createdAt, actual.record.createdAt)
        }
    }

    @Test fun mixedFamiliesUseSeparateCountersAndIgnoreCaptureTimeAndManualPr() {
        val records = listOf(asset("p1", 5000.0, 90, "SIC-17"), asset("a1", 6000.0, 50),
            asset("p2", 10000.0, 10, "SIC-17"), asset("a2", 12000.0, 1))
            .mapIndexed { i, item -> item.copy(record = item.record.copy(startPrCode = "${100 - i}")) }
        val sorted = RoadOrdering.orderAssetsForRoute(records.reversed())
        assertEquals(listOf("P-1", "ALC-1", "P-2", "ALC-2"), sorted.map { it.shortCode })
        val inserted = RoadOrdering.orderAssetsForRoute(records + asset("a0", 5500.0, 999))
        assertEquals(sorted.filter { it.assetFamily == AssetFamily.PUENTE }.map { it.shortCode },
            inserted.filter { it.assetFamily == AssetFamily.PUENTE }.map { it.shortCode })
    }

    @Test fun fallbackIsExplicitAndDoesNotMixContractualAndGeometricOrigins() {
        val axis = asset("axis", 900000.0, 90)
        val legacy = asset("legacy", null, 50)
        val unknown = asset("unknown", null, 1).let { it.copy(record = it.record.copy(startPrCode = "?")) }
        val nan = asset("nan", Double.NaN, 2).let { it.copy(record = it.record.copy(startPrCode = "?")) }
        val rows = RoadOrdering.orderAssetsForRoute(listOf(unknown, legacy, axis, nan))
        assertEquals(listOf("axis", "legacy", "unknown", "nan"), rows.map { it.recordId })
        assertEquals(listOf(AssetOrderSource.GNSS_MAP_MATCH, AssetOrderSource.MANUAL_CHAINAGE_FALLBACK,
            AssetOrderSource.CREATED_AT_FALLBACK, AssetOrderSource.CREATED_AT_FALLBACK), rows.map { it.orderSource })
        assertNull(rows[1].axisMeasureM)
    }

    @Test fun tiesAreDeterministicAndRouteSibFamilyDefineCountersNotRoadbed() {
        val a = asset("a", 10.0, 1)
        val b = asset("b", 10.0, 1).let { it.copy(record = it.record.copy(roadbedCode = "UC")) }
        val r = asset("r", 10.0, 1, route = "PE-3N")
        val rows = RoadOrdering.orderAssetsForRoute(listOf(r, b, a))
        assertEquals(listOf("a", "b", "r"), rows.map { it.recordId })
        assertEquals(listOf("ALC-1", "ALC-2", "ALC-1"), rows.map { it.shortCode })
        val sameFamilyDifferentSib = listOf(asset("x", 1.0, 1, "SIC-21"), asset("y", 2.0, 2, "SIC-20"))
            .map { it.copy(record = it.record.copy(assetType = "MURO")) }
        assertEquals(listOf("MUR-1", "MUR-1"), RoadOrdering.orderAssetsForRoute(sameFamilyDifferentSib).map { it.shortCode })
        assertEquals(1, RoadOrdering.orderAssetsForRoute(listOf(a, b.copy(record = b.record.copy(status = "ANNULLED")))) .size)
    }

    @Test fun folderPlanUsesCurrentOrderAndRealPhotoIndexWithoutMutatingAnything() {
        val original = asset("id", 1.0, 1, route = "PE-3N").copy(photos = listOf(photo("second", "id", 9), photo("first", "id", 2)))
        val plan = DrivePathPlanner.plan(listOf(original))
        assertTrue(plan.warnings.isEmpty())
        val photos = plan.folders.single().photos
        assertEquals(listOf("first", "second"), photos.map { it.photoId })
        assertEquals(listOf("PE-3N/SIB-02/ALCANTARILLA 1/Foto 01.jpg", "PE-3N/SIB-02/ALCANTARILLA 1/Foto 02.jpg"), photos.map { it.plannedPath })
        assertEquals(listOf("second", "first"), original.photos.map { it.id })
        val next = DrivePathPlanner.plan(listOf(original, asset("earlier", 0.0, 50, route = "PE-3N")))
        assertEquals("PE-3N/SIB-02/ALCANTARILLA 2", next.folders.single { it.asset.recordId == "id" }.plannedPath)
    }

    @Test fun catalogUsesDetailClassesAndReportsUnknownsWithoutInventingSib() {
        val drainage = asset("d", 1.0, 1, "SIC-19")
        for ((code, abbreviation) in mapOf("08" to "CUN", "09" to "CAN", "10" to "BAJ", "11" to "ZD", "12" to "ELM", "13" to "CUN")) {
            val row = drainage.copy(sic19 = Sic19Entity("d", code, "1", "1", "1", "1"))
            assertEquals(abbreviation, AssetPresentationCatalog.resolve(row).abbreviation)
        }
        for ((code, abbreviation) in mapOf("12" to "BAD", "13" to "TUN", "14" to "MUR")) {
            val row = asset("s", 1.0, 1, "SIC-20").copy(sic20 = Sic20Entity("s", code, "1", 1.0, 1.0, "1", "1"))
            assertEquals(abbreviation, AssetPresentationCatalog.resolve(row).abbreviation)
        }
        val unknown = asset("u", 1.0, 1, "UNKNOWN")
        val plan = DrivePathPlanner.plan(listOf(unknown))
        assertEquals("ELM-1", plan.folders.single().asset.shortCode)
        assertNull(plan.folders.single().plannedPath)
        assertTrue(plan.warnings.any { it.contains("SIB no configurado") })
        assertTrue(plan.warnings.any { it.contains("ELM") })
        assertEquals("SV", AssetPresentationCatalog.resolve(asset("v", 1.0, 1, "SIC-22")).abbreviation)
        assertEquals("SH", AssetPresentationCatalog.resolve(asset("h", 1.0, 1, "SIC-21").let { it.copy(record = it.record.copy(assetType = "HORIZONTAL_MARKS")) }).abbreviation)
        assertEquals("DV", AssetPresentationCatalog.resolve(asset("v", 1.0, 1, "SIC-23").copy(sic23 = Sic23Entity("v", "21", "1", 3.0, ""))).abbreviation)
    }

    @Test fun selectedRouteIsMatchedBeforeNearerRoadAndContractualDataIsUntouched() {
        val reference = testAxis()
        val nearby = reference.segments.single().copy(routeCode = "OTHER", points = reference.segments.single().points.map { it.copy(routeCode = "OTHER", latitude = 0.00005) })
        val data = reference.copy(segments = reference.segments + nearby)
        val record = testRecord().copy(latitude = 0.00005, startPrCode = "9999", startDistanceM = 777.0)
        val matched = RoadAxisPositioner(data).position(record)
        assertEquals("R", matched.routeCode)
        assertEquals(1500.0, matched.axisMeasureM!!, 0.01)
        assertEquals(5.56, matched.distanceToRoadAxisM!!, 0.05)
        assertEquals(record, matched.copy(axisMeasureM = null, distanceToRoadAxisM = null, roadMatchConfidence = null,
            matchedSegmentId = null, projectedLatitude = null, projectedLongitude = null))
        assertNull(RoadAxisPositioner(data).position(record.copy(routeCode = "MISSING")).axisMeasureM)
        assertNull(RoadAxisPositioner(data).position(record.copy(routeCode = "", latitude = 0.000025, gpsAccuracyM = 5.0)).axisMeasureM)
        val global = RoadAxisPositioner(reference).position(record.copy(routeCode = ""))
        assertNotNull(global.axisMeasureM)
        assertEquals("", global.routeCode)
    }

    @Test fun invalidFixOrDistantPointClearsDerivedValuesButRetainsRecord() {
        val positioner = RoadAxisPositioner(testAxis())
        for (bad in listOf(testRecord().copy(gpsAccuracyM = null), testRecord().copy(gpsAccuracyM = 100.0),
            testRecord().copy(latitude = 80.0), testRecord().copy(longitude = Double.NaN))) {
            val result = positioner.position(bad.copy(axisMeasureM = 123.0))
            assertNull(result.axisMeasureM)
            assertEquals(bad, result)
        }
    }
}
