package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.entity.InventorySnapshot
import com.tuempresa.inventariovial.field.SurveyPreferences
import com.tuempresa.inventariovial.gis.*
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.w3c.dom.Element
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoadReferenceDeliveryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun xml(text: String) = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(text.byteInputStream())

    @Test fun repositoryLoadsExactlySixRealMonotoneAxesAndGeneratesRouteDeliveries() = runBlocking {
        val data = RoadReferenceRepository(context).loadFromAssets()
        assertEquals(SurveyPreferences.initialRoutes.toSet(), data.routes.map { it.routeCode }.toSet())
        assertEquals(6, data.segments.size)
        assertTrue(data.prs.isEmpty())
        val lengths = mapOf("PE-3N" to 151669.90, "PE-22A" to 193079.11, "PE-28C" to 105950.03,
            "PE-12A" to 239503.58, "PE-3NG" to 114064.33, "PE-28H" to 150231.89)
        val output = File("build/outputs/road-reference").apply { mkdirs() }
        val resources = KmlMapImages.resources()
        File(output, "legend.png").writeBytes(resources.getValue("legend.png"))
        for (segment in data.segments) {
            val points = segment.points
            assertTrue(points.size >= 2)
            assertEquals(points.size, points.map { it.sequence }.distinct().size)
            assertTrue(points.zipWithNext().all { (a, b) -> a.chainageM < b.chainageM })
            assertTrue(points.all { GeoMath.validCoordinate(it.latitude, it.longitude) })
            val length = points.last().chainageM - points.first().chainageM
            assertEquals(lengths.getValue(segment.routeCode), length, 0.02)
            assertEquals(length, GeoMath.polylineLength(points.map { it.latitude to it.longitude }), length * 0.001)
            val kml = KmlExporter.render(emptyList(), reference = data.forSelectedRoute(segment.routeCode), legendHref = "legend.png", iconHref = "marker.png")
            val document = xml(kml)
            assertEquals(4, document.getElementsByTagName("Placemark").length)
            assertEquals(2, document.getElementsByTagName("LineString").length)
            assertEquals(document.getElementsByTagName("coordinates").item(0).textContent, document.getElementsByTagName("coordinates").item(1).textContent)
            assertEquals(points.size, document.getElementsByTagName("coordinates").item(0).textContent.trim().split(Regex("\\s+")).size)
            assertEquals("${points.first().longitude},${points.first().latitude},0", document.getElementsByTagName("coordinates").item(2).textContent)
            assertEquals("${points.last().longitude},${points.last().latitude},0", document.getElementsByTagName("coordinates").item(3).textContent)
            val kmz = File(output, "${segment.routeCode}.kmz")
            kmz.outputStream().use { KmlExporter.writeKmz(it, kml, resources) }
            // Both exports use the compatible sidebar legend; PNG remains an optional resource.
            File(output, "${segment.routeCode}.kml").writeText(KmlExporter.render(emptyList(), reference = data.forSelectedRoute(segment.routeCode)))
            ZipFile(kmz).use { zip ->
                assertEquals(setOf("doc.kml", "legend.png", "marker.png"), zip.entries().asSequence().map { it.name }.toSet())
                val packed = zip.getInputStream(zip.getEntry("doc.kml")).bufferedReader().readText()
                assertEquals(0, xml(packed).getElementsByTagName("ScreenOverlay").length)
                assertTrue(packed.contains("<name>LEYENDA</name>"))
                for (name in resources.keys) {
                    val bytes = zip.getInputStream(zip.getEntry(name)).readBytes()
                    assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                }
            }
        }
        File(output, "longitudes.txt").writeText(data.segments.joinToString("\n") {
            "${it.routeCode}: ${it.points.size} vértices; ${it.points.last().chainageM - it.points.first().chainageM} m"
        })
    }

    @Test fun categoryStylesShortNamesAndExtendedDataPreserveManualAndGeometricValues() {
        val rows = listOf(testRecord("p", sic = "SIC-17").copy(assetType = "BRIDGE"),
            testRecord("a", sic = "SIC-18").copy(assetType = "CULVERT"),
            testRecord("c", sic = "SIC-19").copy(assetType = "DITCH"))
            .map { it.copy(axisMeasureM = 1234.5, distanceToRoadAxisM = 3.2, projectedLatitude = 0.0,
                projectedLongitude = 0.004, roadMatchConfidence = 0.9, matchedSegmentId = "axis", sessionId = "session") }
        val kml = KmlExporter.render(rows.map(::testSnapshot), reference = testAxis(), inspectors = mapOf("session" to "Inspector de prueba"))
        val document = xml(kml)
        val names = document.getElementsByTagName("name").let { list -> (0 until list.length).map { list.item(it).textContent } }
        assertTrue(names.containsAll(listOf("EJE axis", "INICIO R", "FIN R", "P-1", "ALC-1", "CUN-1")))
        assertFalse(names.any { it.contains("SIC-") })
        assertEquals(7, document.getElementsByTagName("Placemark").length)
        val placemarks = document.getElementsByTagName("Placemark")
        val culvert = (0 until placemarks.length).map { placemarks.item(it) as Element }
            .single { it.getElementsByTagName("name").item(0).textContent == "ALC-1" }
        val values = culvert.getElementsByTagName("Data").let { list -> (0 until list.length).map { list.item(it) as Element }.associate {
            it.getAttribute("name") to it.textContent } }
        assertEquals("a", values["UUID"])
        assertEquals("0010", values["manualPrCode"])
        assertEquals("300.0", values["manualDistanceM"])
        assertEquals("1234.5", values["axisMeasureM"])
        assertEquals("3.2", values["distanceToRoadAxisM"])
        assertEquals("SIB-02", values["SIB"])
        assertEquals("Inspector de prueba", values["inspector"])
        assertTrue(values.keys.containsAll(listOf("shortCode", "assetFamily", "SIC", "routeCode", "roadbedCode", "latitude", "longitude",
            "projectedLatitude", "projectedLongitude", "gpsAccuracyM", "surveyDate")))
        assertEquals("ffff6600", AssetFamily.ALCANTARILLA.kmlColor)
        assertEquals("ff0000ff", AssetFamily.SENALIZACION_VERTICAL.kmlColor)
        assertEquals("ff228b22", AssetFamily.PUENTE.kmlColor)
        assertEquals("ff0088ff", AssetFamily.CUNETA.kmlColor)
        assertEquals("ffcc3399", AssetFamily.BADEN.kmlColor)
        assertEquals(6, AssetPresentationCatalog.legend.map { it.kmlColor }.distinct().size)
        AssetFamily.entries.forEach { assertTrue(kml.contains("<color>${it.kmlColor}</color>")) }
    }

    @Test fun realPe22AxisSupportsRetroactiveCaptureAndProducesClearlyLabeledDemo() = runBlocking {
        val data = RoadReferenceRepository(context).loadFromAssets().forSelectedRoute("PE-22A")
        val segment = data.segments.single()
        fun snapshot(id: String, measure: Double, created: Long, sic: String = "SIC-18"): InventorySnapshot {
            val pair = segment.points.zipWithNext().first { (a, b) -> measure >= a.chainageM && measure <= b.chainageM }
            val t = (measure - pair.first.chainageM) / (pair.second.chainageM - pair.first.chainageM)
            val r = testRecord("DEMO-$id", created = created, route = "PE-22A", sic = sic).copy(
                latitude = pair.first.latitude + t * (pair.second.latitude - pair.first.latitude),
                longitude = pair.first.longitude + t * (pair.second.longitude - pair.first.longitude),
                observations = "DEMO DE PRUEBA: elemento sintético, no es inventario de campo.")
            return testSnapshot(RoadAxisPositioner(data).position(r))
        }
        val snapshots = listOf(snapshot("A", 10000.0, 1), snapshot("B", 15000.0, 2), snapshot("C", 20000.0, 3), snapshot("D", 17000.0, 4),
            snapshot("P", 11000.0, 5, "SIC-17"), snapshot("CUN", 16000.0, 6, "SIC-19"))
        val culverts = RoadOrdering.orderAssetsForRoute(snapshots.map { com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos(it.record, emptyList()) })
            .filter { it.assetFamily == AssetFamily.ALCANTARILLA }
        assertEquals(listOf("DEMO-A", "DEMO-B", "DEMO-D", "DEMO-C"), culverts.map { it.recordId })
        assertEquals(listOf("ALC-1", "ALC-2", "ALC-3", "ALC-4"), culverts.map { it.shortCode })
        val output = File("build/outputs/road-reference").apply { mkdirs() }
        val kml = KmlExporter.render(snapshots, reference = data, legendHref = "legend.png", iconHref = "marker.png")
        File(output, "DEMO-PE-22A.kmz").outputStream().use { KmlExporter.writeKmz(it, kml, KmlMapImages.resources()) }
        assertEquals(10, xml(kml).getElementsByTagName("Placemark").length)
    }

    @Test fun compatibleAxisHasYellowTopBlackBorderIdenticalGeometryAndSidebarLegend() {
        val kml = KmlExporter.render(emptyList(), reference = testAxis(), legendHref = "legend.png", iconHref = "marker.png")
        val document = xml(kml)
        val styles = document.getElementsByTagName("Style").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }.associateBy { it.getAttribute("id") } }
        val axis = styles.getValue("axis")
        val border = styles.getValue("axis_outline")
        assertEquals("ff00ffff", axis.getElementsByTagName("color").item(0).textContent)
        assertEquals("ff000000", border.getElementsByTagName("color").item(0).textContent)
        assertTrue(axis.getElementsByTagName("width").item(0).textContent.toDouble() >= 5)
        assertEquals("8", border.getElementsByTagName("width").item(0).textContent)
        assertEquals("1.4", styles.getValue("start").getElementsByTagName("scale").item(0).textContent)
        assertEquals("1.4", styles.getValue("end").getElementsByTagName("scale").item(0).textContent)
        assertEquals(AssetFamily.PUENTE.kmlColor, styles.getValue("start").getElementsByTagName("color").item(0).textContent)
        assertEquals(AssetFamily.SENALIZACION_VERTICAL.kmlColor, styles.getValue("end").getElementsByTagName("color").item(0).textContent)
        val lines = document.getElementsByTagName("LineString")
        assertEquals(2, lines.length)
        assertTrue(lines.item(0).isEqualNode(lines.item(1)))
        val marks = document.getElementsByTagName("Placemark")
        assertEquals("#axis_outline", (marks.item(0) as Element).getElementsByTagName("styleUrl").item(0).textContent)
        assertEquals("#axis", (marks.item(1) as Element).getElementsByTagName("styleUrl").item(0).textContent)
        assertEquals(0, document.getElementsByTagName("ScreenOverlay").length)
        val folders = document.getElementsByTagName("Folder")
        val legend = (0 until folders.length).map { folders.item(it) as Element }
            .single { it.getElementsByTagName("name").item(0).textContent == "LEYENDA" }
        assertEquals(7, legend.getElementsByTagName("Folder").length)
        for (entry in KmlPresentation.legend) {
            assertTrue(legend.textContent.contains(entry.text))
            assertTrue(document.getElementsByTagName("description").item(0).textContent.contains(entry.text))
        }
        assertTrue(AssetFamily.entries.none { it.kmlColor == KmlPresentation.AXIS_COLOR })
    }
}
