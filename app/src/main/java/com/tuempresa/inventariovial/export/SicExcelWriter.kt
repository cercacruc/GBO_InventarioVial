package com.tuempresa.inventariovial.export

import com.tuempresa.inventariovial.data.entity.InventoryRecordForExport
import java.io.FilterOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** XLSX Open XML sin dependencias de escritorio. Escritura secuencial para uso offline en Android. */
object SicExcelWriter {
    const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PACKAGE_REL = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"

    /** Single reviewed bridge projection. Reuses SIC cell encoding/styles without scheduling uploads. */
    fun writeMappedRow(output:OutputStream, format:String, route:String, columns:List<SicColumn>, values:List<Any?>) {
        require(columns.size==values.size && columns.isNotEmpty())
        ZipOutputStream(output).use {zip->
            fun entry(path:String,body:String) {
                zip.putNextEntry(ZipEntry(path));zip.write((XML+body).toByteArray(Charsets.UTF_8));zip.closeEntry()
            }
            entry("[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>")
            entry("_rels/.rels","<Relationships xmlns=\"$PACKAGE_REL\"><Relationship Id=\"rId1\" Type=\"$REL/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            entry("xl/workbook.xml","<workbook xmlns=\"$MAIN\" xmlns:r=\"$REL\"><sheets><sheet name=\"${xml(format)}\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
            entry("xl/_rels/workbook.xml.rels","<Relationships xmlns=\"$PACKAGE_REL\"><Relationship Id=\"rId1\" Type=\"$REL/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"styles\" Type=\"$REL/styles\" Target=\"styles.xml\"/></Relationships>")
            entry("xl/styles.xml",styles())
            entry("xl/worksheets/sheet1.xml",buildString {
                append("<worksheet xmlns=\"$MAIN\"><sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\"/></sheetViews><cols>")
                columns.forEachIndexed{i,c->append("<col min=\"${i+1}\" max=\"${i+1}\" width=\"${c.width}\" customWidth=\"1\"/>")}
                append("</cols><sheetData><row r=\"1\" ht=\"28\" customHeight=\"1\">${cell("A1",format,1)}</row>")
                append("<row r=\"2\" ht=\"28\" customHeight=\"1\">${cell("A2","Ruta: $route",1)}</row>")
                append("<row r=\"3\" ht=\"70\" customHeight=\"1\">")
                columns.forEachIndexed{i,c->append(cell("${col(i+1)}3",c.label,2))};append("</row><row r=\"4\" ht=\"60\" customHeight=\"1\">")
                values.forEachIndexed{i,v->append(cell("${col(i+1)}4",v,when(columns[i].kind){CellKind.TEXT->3;CellKind.DECIMAL->4;CellKind.INTEGER->5;CellKind.DATE->6}))}
                append("</row></sheetData><mergeCells count=\"2\"><mergeCell ref=\"A1:${col(columns.size)}1\"/><mergeCell ref=\"A2:${col(columns.size)}2\"/></mergeCells></worksheet>")
            })
        }
    }

    fun write(output: OutputStream, records: List<InventoryRecordForExport>,
              format: SicExportFormat?, heading: ExportHeading) {
        val active = records.filter { it.record.status == "ACTIVE" &&
            SicExportFormat.entries.any { f -> f.code == it.record.sicCode } &&
            (format == null || it.record.sicCode == format.code) }
        require(active.isNotEmpty()) { "No hay registros activos para exportar con esta selección." }
        if (format != null) writeWorkbook(output, active, format, heading)
        else ZipOutputStream(output).use { zip ->
            for (sic in SicExportFormat.entries) {
                val items = active.filter { it.record.sicCode == sic.code }
                if (items.isEmpty()) continue
                zip.putNextEntry(ZipEntry("${sic.code}.xlsx"))
                writeWorkbook(object : FilterOutputStream(zip) {
                    override fun write(bytes: ByteArray, offset: Int, length: Int) { zip.write(bytes, offset, length) }
                    override fun close() { flush() } // No cerrar el ZIP contenedor.
                }, items, sic, heading)
                zip.closeEntry()
            }
        }
    }

    private fun writeWorkbook(output: OutputStream, records: List<InventoryRecordForExport>,
                              format: SicExportFormat, heading: ExportHeading) {
        val groups = records.groupBy { it.record.routeCode }.toSortedMap()
        val used = mutableSetOf<String>()
        val sheets = groups.entries.map { (route, rows) ->
            Triple(sheetName(route, used), route, rows.sortedWith(compareBy<InventoryRecordForExport>(
                { it.record.roadbedCode },
                { (it.record.startPrCode.toDoubleOrNull() ?: 0.0) * 1000 + it.record.startDistanceM },
                { it.record.createdAt }, { it.record.id })))
        }
        ZipOutputStream(output).use { zip ->
            fun entry(path: String, body: String) {
                zip.putNextEntry(ZipEntry(path)); zip.write((XML + body).toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            entry("[Content_Types].xml", buildString {
                append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
                sheets.indices.forEach { append("<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>") }
                append("</Types>")
            })
            entry("_rels/.rels", "<Relationships xmlns=\"$PACKAGE_REL\"><Relationship Id=\"rId1\" Type=\"$REL/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            entry("xl/workbook.xml", buildString {
                append("<workbook xmlns=\"$MAIN\" xmlns:r=\"$REL\"><bookViews><workbookView/></bookViews><sheets>")
                sheets.forEachIndexed { i, s -> append("<sheet name=\"${xml(s.first)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>") }
                append("</sheets><definedNames>")
                sheets.forEachIndexed { i, s ->
                    val ref = xml("'${s.first.replace("'", "''")}'")
                    append("<definedName name=\"_xlnm.Print_Titles\" localSheetId=\"$i\">$ref!\$1:\$13</definedName>")
                    append("<definedName name=\"_xlnm.Print_Area\" localSheetId=\"$i\">$ref!\$A\$5:\$${col(format.columns.size)}\$${13 + s.third.size}</definedName>")
                }
                append("</definedNames></workbook>")
            })
            entry("xl/_rels/workbook.xml.rels", buildString {
                append("<Relationships xmlns=\"$PACKAGE_REL\">")
                sheets.indices.forEach { append("<Relationship Id=\"rId${it + 1}\" Type=\"$REL/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>") }
                append("<Relationship Id=\"styles\" Type=\"$REL/styles\" Target=\"styles.xml\"/></Relationships>")
            })
            entry("xl/styles.xml", styles())
            sheets.forEachIndexed { i, sheet ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${i + 1}.xml"))
                val writer = zip.writer(Charsets.UTF_8)
                fun emit(s: String) { writer.write(s) }
                val columns = format.columns
                val last = col(columns.size)
                require(sheet.third.size <= 1_048_563) { "La ruta ${sheet.second} supera el límite de filas de Excel." }
                emit(XML + "<worksheet xmlns=\"$MAIN\"><sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
                emit("<dimension ref=\"A5:$last${13 + sheet.third.size}\"/><sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\"><pane xSplit=\"2\" ySplit=\"13\" topLeftCell=\"C14\" activePane=\"bottomRight\" state=\"frozen\"/></sheetView></sheetViews>")
                emit("<sheetFormatPr defaultRowHeight=\"20\"/><cols>")
                columns.forEachIndexed { n, c -> emit("<col min=\"${n + 1}\" max=\"${n + 1}\" width=\"${c.width}\" customWidth=\"1\"/>") }
                emit("</cols><sheetData>")
                listOf(5 to heading.project, 6 to heading.road, 7 to "INVENTARIO VIAL CALIFICADO DE LA SITUACIÓN ACTUAL",
                    8 to listOf(heading.section, "RUTA: ${sheet.second}").filter { it.isNotBlank() }.joinToString(" · "),
                    10 to "${format.code}: ${format.title}").forEach { (row, text) ->
                    emit("<row r=\"$row\" ht=\"30\" customHeight=\"1\">${cell("A$row", text, 1)}</row>")
                }
                emit("<row r=\"12\" ht=\"24\" customHeight=\"1\">${cell("C12", "Ubicación inicio", 2)}")
                if (format != SicExportFormat.SIC18) emit(cell("E12", "Ubicación fin", 2))
                emit("</row><row r=\"13\" ht=\"60\" customHeight=\"1\">")
                columns.forEachIndexed { n, c -> emit(cell("${col(n + 1)}13", c.label, 2)) }
                emit("</row>")
                sheet.third.forEachIndexed { index, item ->
                    val row = index + 14
                    val values = format.values(item)
                    check(values.size == columns.size) { "Columnas incompatibles en ${format.code}." }
                    val lineCount = values.indices.maxOf { c ->
                        val value = values[c]?.toString().orEmpty()
                        value.lines().sumOf { ((it.length / (columns[c].width * 0.85)).toInt() + 1) }
                    }
                    val height = (lineCount * 15.0 + 6).coerceIn(22.0, 409.0)
                    emit("<row r=\"$row\" ht=\"$height\" customHeight=\"1\">")
                    values.forEachIndexed { c, value ->
                        emit(cell("${col(c + 1)}$row", value, when (columns[c].kind) {
                            CellKind.TEXT -> 3; CellKind.DECIMAL -> 4; CellKind.INTEGER -> 5; CellKind.DATE -> 6
                        }))
                    }
                    emit("</row>")
                }
                emit("</sheetData><autoFilter ref=\"A13:$last${13 + sheet.third.size}\"/><mergeCells>")
                for (r in listOf(5, 6, 7, 8, 10)) emit("<mergeCell ref=\"A$r:$last$r\"/>")
                emit("<mergeCell ref=\"C12:D12\"/>")
                if (format != SicExportFormat.SIC18) emit("<mergeCell ref=\"E12:F12\"/>")
                emit("</mergeCells><printOptions horizontalCentered=\"1\"/><pageMargins left=\"0.25\" right=\"0.25\" top=\"0.35\" bottom=\"0.35\" header=\"0.15\" footer=\"0.15\"/>")
                emit("<pageSetup paperSize=\"${if (format == SicExportFormat.SIC17) 8 else 9}\" orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
                emit("<headerFooter><oddFooter>&amp;CPágina &amp;P de &amp;N</oddFooter></headerFooter></worksheet>")
                writer.flush(); zip.closeEntry()
            }
        }
    }

    internal fun sheetName(route: String, used: MutableSet<String>): String {
        val clean = route.map { if (it in "[]:*?/\\" || it.code < 32) '_' else it }.joinToString("").trim().trim('\'')
            .ifBlank { "Ruta" }.take(31).trimEnd('\'').ifBlank { "Ruta" }
        var name = clean
        var index = 2
        while (!used.add(name.lowercase(Locale.ROOT))) {
            val suffix = " (${index++})"
            name = clean.take(31 - suffix.length) + suffix
        }
        return name
    }

    private fun col(index: Int): String {
        var n = index; var result = ""
        while (n > 0) { n--; result = ('A' + n % 26) + result; n /= 26 }
        return result
    }

    private fun cell(ref: String, value: Any?, style: Int): String {
        if (value == null) return "<c r=\"$ref\" s=\"$style\"/>"
        val number = when (value) {
            is LocalDate -> {
                require(!value.isBefore(LocalDate.of(1900, 3, 1))) { "Fecha fuera del rango admitido por Excel: $value." }
                ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), value)
            }
            is Number -> value.also { require(it.toDouble().isFinite()) { "Valor numérico inválido en $ref." } }
            else -> null
        }
        if (number != null) return "<c r=\"$ref\" s=\"$style\"><v>$number</v></c>"
        val text = value.toString()
        require(text.length <= 32767) { "El texto de $ref supera el límite de Excel (32767 caracteres)." }
        // Inline strings evitan interpretar códigos/descripciones como fórmulas.
        return "<c r=\"$ref\" s=\"$style\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(text)}</t></is></c>"
    }

    private fun xml(value: String): String = buildString {
        value.codePoints().forEach { c ->
            when (c) {
                38 -> append("&amp;"); 60 -> append("&lt;"); 62 -> append("&gt;")
                34 -> append("&quot;"); 39 -> append("&apos;")
                9, 10, 13 -> appendCodePoint(c)
                in 32..0xD7FF, in 0xE000..0xFFFD, in 0x10000..0x10FFFF -> appendCodePoint(c)
                else -> append(' ')
            }
        }
    }

    private fun styles(): String = """
        <styleSheet xmlns="$MAIN">
        <numFmts count="2"><numFmt numFmtId="164" formatCode="0.00"/><numFmt numFmtId="165" formatCode="dd/mm/yyyy"/></numFmts>
        <fonts count="2"><font><sz val="10"/><name val="Arial"/></font><font><b/><sz val="10"/><name val="Arial"/></font></fonts>
        <fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFE7E6E6"/><bgColor indexed="64"/></patternFill></fill></fills>
        <borders count="2"><border/><border><left style="thin"><color rgb="FF808080"/></left><right style="thin"><color rgb="FF808080"/></right><top style="thin"><color rgb="FF808080"/></top><bottom style="thin"><color rgb="FF808080"/></bottom></border></borders>
        <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
        <cellXfs count="7">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
        <xf numFmtId="49" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
        <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
        <xf numFmtId="1" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        </cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
    """.trimIndent()
}
