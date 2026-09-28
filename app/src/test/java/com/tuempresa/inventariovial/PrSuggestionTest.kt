package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.road.*
import org.junit.Assert.*
import org.junit.Test

fun prSuggestionReference(): RoadReferenceData {
    val points = listOf(RoadPolylinePoint("R", "EJE", "s", 0, 0.0, 0.0, 10000.0),
        RoadPolylinePoint("R", "EJE", "s", 1, 0.0, 0.02, 20000.0))
    return RoadReferenceData(listOf(RoadRoute("R")), listOf(RoadSegment("R", "EJE", "s", points)),
        listOf(RoadPr("R", "CD", "0010", 0.0, 0.0, 10000.0), RoadPr("R", "CD", "0015", 0.0, 0.01, 15000.0)))
}

class PrSuggestionTest {
    private fun suggest(data: RoadReferenceData = prSuggestionReference(), bed: String = "CD", lon: Double = .0053) =
        PrSuggestionEngine(data).suggest("R", bed, 0.0, lon, 1.0)

    @Test fun previousPrAt12650Is0010Plus2650AndKeepsContractualRoadbed() {
        val result = suggest().suggestion!!
        assertEquals("0010", result.suggestedPrCode)
        assertEquals(2650.0, result.suggestedDistanceFromPrM, 0.000001)
        assertEquals("2650", result.distanceText)
        assertEquals("CD", result.roadbedCode)
        assertEquals("EJE", result.match.roadbedCode)
        assertEquals("0010", result.match.prCode)
        assertEquals(result.suggestedDistanceFromPrM, result.match.distanceFromPrM!!, 0.0)
    }

    @Test fun suggestionDoesNotApplyItselfAndStartAndEndRequireSeparateConfirmations() {
        val start = PrEntry("0042", "684")
        val end = PrEntry("0050", "7")
        val startSuggestion = suggest().suggestion!!
        val endSuggestion = suggest(lon = .012).suggestion!!
        assertEquals(PrEntry("0042", "684", "MANUAL"), start)
        assertEquals(PrEntry("0050", "7", "MANUAL"), end)
        val confirmedStart = start.confirm(startSuggestion)
        assertEquals("0010", confirmedStart.prCode)
        assertEquals("2650", confirmedStart.distanceM)
        assertEquals(PrEntrySource.CONFIRMED, confirmedStart.source)
        assertEquals("MANUAL", end.source)
        assertEquals("0050", end.prCode)
        val confirmedEnd = end.confirm(endSuggestion)
        assertEquals("0015", confirmedEnd.prCode)
        assertEquals("1000", confirmedEnd.distanceM)
        assertEquals(PrEntrySource.CONFIRMED, confirmedEnd.source)
    }

    @Test fun noCatalogNoPreviousPrOrUnknownRoadbedNeverInventsAPr() {
        val data = prSuggestionReference()
        assertNull(suggest(data.copy(prs = emptyList())).suggestion)
        assertNull(suggest(data.copy(prs = data.prs.drop(1))).suggestion)
        assertNull(suggest(bed = "UC").suggestion)
        assertNull(PrSuggestionEngine(data).suggest("OTHER", "CD", 0.0, .0053, 1.0).suggestion)
        assertNull(PrSuggestionEngine(data).suggest("", "CD", 0.0, .0053, 1.0).suggestion)
        assertNull(suggest(data.copy(prs = data.prs.map { it.copy(roadbedCode = "EJE") })).suggestion)
    }

    @Test fun roadbedSpecificCatalogsAreNotMergedEvenWithSharedEje() {
        val data = prSuggestionReference()
        val uc = RoadPr("R", "UC", "0042", 0.0, .005, 12500.0)
        val others = listOf(uc, uc.copy(routeCode = "OTHER", roadbedCode = "CD", prCode = "0088"))
        assertEquals("0010", suggest(data.copy(prs = data.prs + others)).suggestion!!.suggestedPrCode)
        val result = suggest(data.copy(prs = data.prs + others), bed = "UC").suggestion!!
        assertEquals("0042", result.suggestedPrCode)
        assertEquals(150.0, result.suggestedDistanceFromPrM, 0.001)
    }

    @Test fun nativeRoadbedsTakePrecedenceOverSharedEjeAndNearbyCarriageway() {
        val data = prSuggestionReference()
        val cd = data.segments.single().copy(roadbedCode = "CD", points = data.segments.single().points.map { it.copy(roadbedCode = "CD") })
        val uc = cd.copy(roadbedCode = "UC", points = cd.points.map { it.copy(roadbedCode = "UC", latitude = .00005) })
        val ref = data.copy(segments = listOf(cd, uc) + data.segments)
        assertEquals("CD", suggest(ref).suggestion!!.match.roadbedCode)
        assertEquals(12650.0, RoadAxisPositioner(ref).position(testRecord().copy(longitude = .0053)).axisMeasureM!!, .001)
    }

    @Test fun validatesPrCoordinatesAndChainageAgainstAxisBeforeUsingCatalog() {
        val data = prSuggestionReference()
        val badCoordinate = data.prs.last().copy(latitude = .1)
        val badMeasure = data.prs.last().copy(chainageM = 15050.0)
        for (bad in listOf(badCoordinate, badMeasure, data.prs.last().copy(chainageM = Double.NaN))) {
            val result = suggest(data.copy(prs = listOf(data.prs.first(), bad)))
            assertNull(result.suggestion)
            assertTrue(result.message!!.contains("no validado"))
        }
        val duplicate = suggest(data.copy(prs = data.prs + data.prs.first()))
        assertNull(duplicate.suggestion)
    }

    @Test fun exactPrBoundaryAndReversedGeometryUseIncreasingReferenceDirection() {
        val data = prSuggestionReference()
        val reversed = data.copy(segments = data.segments.map { segment ->
            segment.copy(points = segment.points.reversed().mapIndexed { i, p -> p.copy(sequence = i) }) })
        assertEquals("0010", suggest(reversed).suggestion!!.suggestedPrCode)
        val exact = suggest(data, lon = .01).suggestion!!
        assertEquals("0015", exact.suggestedPrCode)
        assertEquals(0.0, exact.suggestedDistanceFromPrM, 0.0)
    }

    @Test fun previousPrCannotComeFromAnotherReferenceSegment() {
        val data = prSuggestionReference()
        val other = data.segments.single().copy(segmentId = "other", points = data.segments.single().points.map {
            it.copy(segmentId = "other", latitude = 1.0) })
        val ref = data.copy(segments = data.segments + other, prs = listOf(data.prs.first().copy(latitude = 1.0)))
        assertNull(suggest(ref).suggestion)
    }

    @Test fun badOrMissingFixProducesNoSuggestionAndDoesNotChangeRecord() {
        val data = prSuggestionReference()
        val original = testRecord().copy(startPrCode = "0042", startDistanceM = 684.0)
        for (accuracy in listOf(null, Double.NaN, -1.0, 100.0)) {
            assertNull(PrSuggestionEngine(data).suggest("R", "CD", 0.0, .0053, accuracy).suggestion)
        }
        val positioned = RoadAxisPositioner(data).position(original.copy(longitude = .0053))
        assertEquals(original.startPrCode, positioned.startPrCode)
        assertEquals(original.startDistanceM, positioned.startDistanceM, 0.0)
        assertEquals(PrEntrySource.MANUAL, positioned.locationSource)
    }

    @Test fun contractualMeasureUsesCatalogEvenForSurveyRecordsAndOffsetsAbove1000() {
        val prs = prSuggestionReference().prs.map { if (it.prCode == "0010") it.copy(prCode = "0042") else it }
        val record = testRecord().copy(surveyDirection = "INCREASING", startPrCode = "0042", startDistanceM = 2650.0)
        assertEquals(12650.0, ChainageCalculator(prs).calculate(record).chainageM!!, 0.0)
        assertTrue(ContractualPrInput.valid("0042", "2650", prs, "R", "CD"))
        assertFalse(ContractualPrInput.valid("0042", "2650", emptyList(), "R", "CD"))
        assertFalse(ContractualPrInput.valid("0042", "2650", prs, "R", "UC"))
    }
}
