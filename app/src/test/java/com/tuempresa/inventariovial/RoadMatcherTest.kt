package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.road.*
import org.junit.Assert.*
import org.junit.Test

class RoadMatcherTest {
    @Test fun projectsInterpolatesAndUsesPreviousPr() {
        val result=LinearReferenceEngine(testAxis()).locate(0.00005,0.0075,1.0)!!
        assertEquals("R",result.routeCode);assertEquals("CD",result.roadbedCode)
        assertEquals(1750.0,result.chainageM,0.01);assertEquals("0020",result.prCode)
        assertEquals(250.0,result.distanceFromPrM!!,0.01);assertEquals(5.56,result.distanceToRoadAxisM,0.05)
        assertEquals("I",result.suggestedSide)
    }
    @Test fun sideIsIndependentOfStoredPolylineDirection() {
        for(reversed in listOf(false,true)) {
            val engine=LinearReferenceEngine(testAxis(reversed))
            assertEquals("I",engine.locate(0.00005,0.005,1.0)!!.suggestedSide)
            assertEquals("D",engine.locate(-0.00005,0.005,1.0)!!.suggestedSide)
            assertNull(engine.locate(0.000001,0.005,1.0)!!.suggestedSide)
            assertNull(engine.locate(0.0,0.0101,1.0)!!.suggestedSide)
        }
    }
    @Test fun previousPrIsRouteAndRoadbedScopedAndIncludesExactBoundary() {
        val prs=testAxis().prs+RoadPr("OTHER","CD","9999",0.0,0.0,1499.0)
        assertEquals("0020",LinearReferenceEngine.previousPr(prs,"R","CD",1500.0)!!.prCode)
        assertEquals("0010",LinearReferenceEngine.previousPr(prs,"R","CD",1499.9)!!.prCode)
        assertNull(LinearReferenceEngine.previousPr(prs,"R","CD",999.0))
        assertNull(LinearReferenceEngine.previousPr(prs,"R","UC",1800.0))
    }
    @Test fun refusesBadAccuracyFarPointsAndAmbiguousCarriageways() {
        val engine=LinearReferenceEngine(testAxis())
        assertNull(engine.locate(0.01,0.005,1.0));assertNull(engine.locate(0.0,0.005,20.0))
        assertNull(engine.locate(0.0,0.005,Double.NaN));assertNull(engine.locate(91.0,0.005,1.0))
        val reference=testAxis();val parallel=reference.segments.single().copy(roadbedCode="UC",
            points=reference.segments.single().points.map {it.copy(roadbedCode="UC",latitude=0.00005)})
        assertNull(LinearReferenceEngine(reference.copy(segments=reference.segments+parallel)).locate(0.000025,0.005,1.0))
    }
    @Test fun importJsonAndGeoJsonValidateRatherThanInventChainages() {
        val importer=JsonRoadReferenceImporter()
        val json="""{"segments":[{"routeCode":"R","roadbedCode":"CD","segmentId":"s","points":[{"sequence":0,"latitude":0,"longitude":0,"chainageM":50},{"sequence":1,"latitude":0,"longitude":0.01,"chainageM":75}]}],"prs":[]}"""
        assertEquals(50.0,importer.parse(json).segments.single().points.first().chainageM,0.0)
        val geo="""{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"routeCode":"R","roadbedCode":"CD","segmentId":"s","chainageM":[50,75]},"geometry":{"type":"LineString","coordinates":[[0,0],[0.01,0]]}}]}"""
        assertEquals(75.0,importer.parse(geo).segments.single().points.last().chainageM,0.0)
        assertTrue(runCatching { importer.parse(geo.replace("[50,75]","[50]")) }.isFailure)
        assertTrue(runCatching { importer.parse(json.replace("\"sequence\":1","\"sequence\":0")) }.isFailure)
    }
}
