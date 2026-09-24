package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.data.entity.TrackPointEntity
import com.tuempresa.inventariovial.gis.KmlExporter
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.tracking.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class TrackAndKmlTest {
    private fun point(i:Int,lat:Double=0.0,lon:Double=i*.0025)=TrackPointEntity("p$i","id",i,lat,lon,null,1.0,1000L+i*10000L)
    @Test fun polylineLengthFollowsCurveRatherThanChord() {
        val curve=listOf(point(0),point(1,0.005,0.0),point(2,0.005,0.005))
        val lengths=TrackCalculator.lengths(curve)
        assertEquals(1111.95,lengths.rawTrackLength,0.1)
        assertEquals(786.27,lengths.startEndStraightDistance,0.1)
        assertTrue(lengths.rawTrackLength>lengths.startEndStraightDistance);assertNull(lengths.matchedRoadLength)
    }
    @Test fun lengthPrefersOfficialChainagesAndSupportsDecreasingTrack() {
        val points=(0..3).map {point(it)}
        assertEquals(750.0,TrackCalculator.lengths(points,testAxis()).matchedRoadLength!!,0.001)
        val reversed=points.reversed().mapIndexed {i,p->p.copy(sequence=i)}
        assertEquals(750.0,TrackCalculator.lengths(reversed,testAxis()).matchedRoadLength!!,0.001)
        assertNull(TrackCalculator.lengths(points+point(4,0.5,0.5),testAxis()).matchedRoadLength)
        assertEquals(0.0,TrackCalculator.lengths(emptyList()).rawTrackLength,0.0)
    }
    @Test fun noiseStaleFutureAndImpossibleJumpsAreRejected() {
        val previous=point(0)
        fun fix(lon:Double=.0001,accuracy:Float=1f,timestamp:Long=2000)=GeoLocation(0.0,lon,accuracy,timestamp=timestamp)
        assertTrue(TrackFilter.accepts(previous,fix(),2000))
        assertFalse(TrackFilter.accepts(previous,fix(lon=.000001),2000))
        assertFalse(TrackFilter.accepts(previous,fix(accuracy=30f),2000))
        assertFalse(TrackFilter.accepts(previous,fix(timestamp=1000),2000))
        assertFalse(TrackFilter.accepts(previous,fix(timestamp=3000),2000))
        assertFalse(TrackFilter.accepts(previous,fix(),100_000))
        assertFalse(TrackFilter.accepts(previous,fix(lon=1.0),2000))
        assertFalse(TrackFilter.accepts(null,fix(accuracy=Float.NaN),2000))
    }
    @Test fun kmlIsValidXmlHasNamespaceEscapesTextAndCoordinatesAreLongitudeFirst() {
        val r=testRecord(route="R & <Sur>").copy(observations="x",endLatitude=.01,endLongitude=.02)
        val kml=KmlExporter.render(listOf(testSnapshot(r),testSnapshot(testRecord("void",status="ANNULLED"))))
        val factory=DocumentBuilderFactory.newInstance().apply {isNamespaceAware=true}
        val document=factory.newDocumentBuilder().parse(ByteArrayInputStream(kml.toByteArray()))
        assertEquals("http://www.opengis.net/kml/2.2",document.documentElement.namespaceURI)
        assertEquals(1,document.getElementsByTagName("Placemark").length)
        assertEquals(1,document.getElementsByTagName("LineString").length)
        assertTrue(document.getElementsByTagName("coordinates").item(0).textContent.startsWith("0.005,0.0,0.0"))
        assertTrue(kml.contains("R &amp; &lt;Sur&gt;"));assertFalse(kml.contains("drive.google"))
    }
    @Test fun pointAssetsRemainPointsAndLinearUsesTrackThenEndpoints() {
        val point=testSnapshot(testRecord(sic="SIC-17").copy(assetType="PUENTE"))
        assertTrue(KmlExporter.render(listOf(point)).contains("<Point>"))
        val tracked=KmlExporter.render(listOf(testSnapshot(points=(0..2).map {point(it)})))
        assertTrue(tracked.contains("<LineString>"));assertTrue(tracked.contains("RECORRIDO_GNSS"))
        assertTrue(KmlExporter.render(listOf(testSnapshot())).contains("PUNTO_INCOMPLETO_SIN_GPS_FINAL"))
    }
    @Test fun kmzContainsValidDocKml() {
        val output=ByteArrayOutputStream();KmlExporter.writeKmz(output,KmlExporter.render(listOf(testSnapshot())))
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use {zip ->
            assertEquals("doc.kml",zip.nextEntry.name)
            assertTrue(zip.readBytes().toString(Charsets.UTF_8).contains("<kml"));assertNull(zip.nextEntry)
        }
    }
    @Test fun resumedTrackDoesNotInventConnectingLengthOrKmlSegment() {
        val points=listOf(point(0),point(1),point(3),point(4))
        val lengths=TrackCalculator.lengths(points,testAxis())
        assertEquals(555.98,lengths.rawTrackLength,0.1)
        assertNull(lengths.matchedRoadLength);assertTrue(lengths.hasGaps)
        val kml=KmlExporter.render(listOf(testSnapshot(points=points)))
        val document=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(kml.toByteArray()))
        assertEquals(1,document.getElementsByTagName("MultiGeometry").length)
        assertEquals(2,document.getElementsByTagName("LineString").length)
    }
    @Test fun sic20ClassIdentifiesLinearStructuresEvenWithSharedAssetSelector() {
        val record=testRecord(sic="SIC-20").copy(assetType="FORD",endLatitude=.01,endLongitude=.02)
        val detail=com.tuempresa.inventariovial.data.entity.Sic20Entity("id","13","1",2.0,3.0,"1","1")
        assertTrue(KmlExporter.render(listOf(testSnapshot(record).copy(sic20=detail))).contains("<LineString>"))
        assertTrue(KmlExporter.render(listOf(testSnapshot(record).copy(sic20=detail.copy(classCode="12")))).contains("<Point>"))
    }
}
