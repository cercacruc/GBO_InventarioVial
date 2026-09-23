package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document

class SicExcelWriterTest {
    private fun record(sic: Int, route: String = "PE-22B", pr: String = "0005", distance: Double = 12.5,
                       status: String = "ACTIVE", id: String = "record-$sic") = InventoryRecordEntity(
        id = id, sicCode = "SIC-$sic", assetType = "TEST", routeCode = route, roadbedCode = "CD",
        startPrCode = pr, startDistanceM = distance, endPrCode = "0006", endDistanceM = 20.0, sideCode = "D",
        latitude = -12.0, longitude = -77.0, altitudeM = null, gpsAccuracyM = 3.0,
        surveyDate = "23/09/2026", observations = null, status = status, photoSyncStatus = "PENDING",
        excelSyncStatus = "PENDING", createdAt = 1L, updatedAt = 1L)

    private fun fixtures() = listOf(
        InventoryRecordForExport(record(17), sic17 = Sic17Entity("record-17", "01", "2", "PE-22B.001", "S", 3,
            25.5, 4.25, "1", "2", "1", "1", "RÍO PRUEBA", 0.0)),
        InventoryRecordForExport(record(18), sic18 = Sic18Entity("record-18", "06", "3", 2, "2", 1.25, null, "2", "1")),
        InventoryRecordForExport(record(19), sic19 = Sic19Entity("record-19", "08", "2", "1", "1", "3")),
        InventoryRecordForExport(record(20), sic20 = Sic20Entity("record-20", "14", "2", 3.5, 9.99, "2", null)),
        InventoryRecordForExport(record(21), sic21 = Sic21Entity("record-21", "18", "3", "5", "1")),
        InventoryRecordForExport(record(22), sic22 = Sic22Entity("record-22", "20", "3", "2", "I-5", "99", "2", 1.0, 2.0, 3.0)),
        InventoryRecordForExport(record(23), sic23 = Sic23Entity("record-23", "21", "1", 12.5, "ANCHO TOTAL"))
    )
    private val heading = ExportHeading("ENSAYO DE EXPORTACIÓN", "DATOS SINTÉTICOS", "TRAMO DE PRUEBA")
    private fun bytes(items: List<InventoryRecordForExport>, format: SicExportFormat?): ByteArray =
        ByteArrayOutputStream().also { SicExcelWriter.write(it, items, format, heading) }.toByteArray()
    private fun unzip(bytes: ByteArray): Map<String, ByteArray> = buildMap {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) { val entry = zip.nextEntry ?: break; put(entry.name, zip.readBytes()) }
        }
    }
    private fun xml(bytes: ByteArray): Document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    private fun cells(doc: Document): Map<String, org.w3c.dom.Element> = buildMap {
        val nodes = doc.getElementsByTagName("c")
        for (i in 0 until nodes.length) { val e = nodes.item(i) as org.w3c.dom.Element; put(e.getAttribute("r"), e) }
    }

    @Test fun allSevenFormatsProduceReadablePackagesWithOfficialColumns() {
        val expectedSizes = listOf(20, 13, 13, 14, 12, 14, 12)
        val dir = File("build/sic-export-verification").apply { mkdirs() }
        SicExportFormat.entries.forEachIndexed { index, format ->
            val data = bytes(fixtures(), format)
            File(dir, "${format.code}.xlsx").writeBytes(data)
            val entries = unzip(data)
            entries.filterKeys { it.endsWith(".xml") || it.endsWith(".rels") }.values.forEach { xml(it) }
            val sheet = xml(entries.getValue("xl/worksheets/sheet1.xml"))
            val rows = sheet.getElementsByTagName("row")
            val dataRow = (0 until rows.length).map { rows.item(it) as org.w3c.dom.Element }.single { it.getAttribute("r") == "14" }
            assertEquals(format.code, expectedSizes[index], dataRow.getElementsByTagName("c").length)
            val cells = cells(sheet)
            assertEquals("PE-22B", cells.getValue("A14").textContent)
            assertEquals("0005", cells.getValue("C14").textContent)
            assertEquals("inlineStr", cells.getValue("C14").getAttribute("t"))
            assertEquals("12.5", cells.getValue("D14").textContent)
            assertEquals("4", cells.getValue("D14").getAttribute("s"))
            assertEquals(0, sheet.getElementsByTagName("f").length)
        }
    }

    @Test fun officialFieldsAndNotApplicableValuesAreNotLostOrInvented() {
        fun sheet(format: SicExportFormat) = cells(xml(unzip(bytes(fixtures(), format)).getValue("xl/worksheets/sheet1.xml")))
        val bridge = sheet(SicExportFormat.SIC17)
        assertEquals("01", bridge.getValue("G14").textContent)
        assertEquals("PE-22B.001", bridge.getValue("I14").textContent)
        assertEquals("3", bridge.getValue("K14").textContent)
        assertEquals("25.5", bridge.getValue("L14").textContent)
        assertEquals("RÍO PRUEBA", bridge.getValue("S14").textContent)
        assertEquals("0.0", bridge.getValue("T14").textContent)
        assertEquals("6", bridge.getValue("P14").getAttribute("s"))
        assertEquals("", sheet(SicExportFormat.SIC18).getValue("J14").textContent)
        assertEquals("", sheet(SicExportFormat.SIC20).getValue("K14").textContent)
        val vertical = sheet(SicExportFormat.SIC22)
        assertEquals("2", vertical.getValue("J14").textContent)
        assertEquals("I-5", vertical.getValue("K14").textContent)
        assertEquals("", vertical.getValue("L14").textContent)
        assertEquals("ANCHO TOTAL", sheet(SicExportFormat.SIC23).getValue("K14").textContent)
    }

    @Test fun allZipContainsOnlyNonemptyFormatsAndPreservesRouteOrdering() {
        val item = fixtures().last()
        val records = listOf(item.copy(record = record(23, pr = "0010", id = "late")),
            item.copy(record = record(23, route = "PE-5S", id = "other")),
            item.copy(record = record(23, status = "ANNULLED", id = "void")), item)
        val zip = unzip(bytes(records, null))
        assertEquals(setOf("SIC-23.xlsx"), zip.keys)
        val workbook = unzip(zip.getValue("SIC-23.xlsx"))
        val first = cells(xml(workbook.getValue("xl/worksheets/sheet1.xml")))
        assertEquals("0005", first.getValue("C14").textContent)
        assertEquals("0010", first.getValue("C15").textContent)
        assertFalse(first.containsKey("A16"))
        assertEquals("PE-5S", cells(xml(workbook.getValue("xl/worksheets/sheet2.xml"))).getValue("A14").textContent)
        val all = unzip(bytes(fixtures(), null))
        assertEquals(7, all.size)
        all.values.forEach { assertTrue(unzip(it).containsKey("xl/workbook.xml")) }
    }

    @Test fun arbitraryTextCannotBecomeFormulasOrBreakXmlAndSheetNamesStayUnique() {
        val item = fixtures().last()
        val special = item.copy(record = record(23, route = "A/B"), sic23 = item.sic23!!.copy(description = "=1+1 & <á>"))
        val other = item.copy(record = record(23, route = "a?b", id = "other"))
        val workbook = unzip(bytes(listOf(special, other), SicExportFormat.SIC23))
        val names = xml(workbook.getValue("xl/workbook.xml")).getElementsByTagName("sheet")
        assertEquals("A_B", (names.item(0) as org.w3c.dom.Element).getAttribute("name"))
        assertEquals("a_b (2)", (names.item(1) as org.w3c.dom.Element).getAttribute("name"))
        val sheet = xml(workbook.getValue("xl/worksheets/sheet1.xml"))
        assertEquals("=1+1 & <á>", cells(sheet).getValue("K14").textContent)
        assertEquals("inlineStr", cells(sheet).getValue("K14").getAttribute("t"))
        assertEquals(0, sheet.getElementsByTagName("f").length)
    }

    @Test fun invalidOrIncompleteDataFailsInsteadOfProducingASuccessfulExport() {
        val item = fixtures().last()
        for (items in listOf(emptyList(), listOf(item.copy(sic23 = null)),
            listOf(item.copy(record = record(23, distance = Double.NaN))),
            listOf(item.copy(record = record(23).copy(surveyDate = "31/02/2026"))))) {
            try { bytes(items, SicExportFormat.SIC23); fail("La exportación inválida debió fallar") }
            catch (expected: IllegalArgumentException) { assertNotNull(expected.message) }
        }
    }
}
