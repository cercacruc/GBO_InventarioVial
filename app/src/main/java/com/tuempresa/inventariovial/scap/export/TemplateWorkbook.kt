package com.tuempresa.inventariovial.scap.export

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Preserves original parts and can append copies of bounded template blocks. */
internal class TemplateWorkbook(input: InputStream) {
    val parts = linkedMapOf<String, ByteArray>()
    private val documents = mutableMapOf<String, Document>()
    init {
        ZipInputStream(input).use { zip ->
            while (true) { val entry=zip.nextEntry ?: break; if (!entry.isDirectory) parts[entry.name]=zip.readBytes() }
        }
    }
    fun document(path:String):Document=documents.getOrPut(path) {
        val factory=DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware=true
            setFeature("http://xml.org/sax/features/external-general-entities",false)
            setFeature("http://xml.org/sax/features/external-parameter-entities",false)
        }
        factory.newDocumentBuilder().parse(parts.getValue(path).inputStream())
    }
    fun sheet(index:Int)=document("xl/worksheets/sheet$index.xml")
    fun cell(sheet:Int,ref:String):Element {
        val doc=sheet(sheet)
        doc.nodes("c").firstOrNull {it.getAttribute("r")==ref}?.let {return it}
        val rowNumber=ref.filter(Char::isDigit).toInt()
        val data=doc.nodes("sheetData").single()
        val row=doc.nodes("row").filter {it.namespaceURI==MAIN}.firstOrNull{it.getAttribute("r").toInt()==rowNumber}
            ?: doc.createElementNS(MAIN,"row").apply {
                setAttribute("r",rowNumber.toString())
                data.insertBefore(this,data.children().firstOrNull{it.getAttribute("r").toInt()>rowNumber})
            }
        return doc.createElementNS(MAIN,"c").apply {
            setAttribute("r",ref)
            row.insertBefore(this,row.children().firstOrNull{column(it.getAttribute("r"))>column(ref)})
        }
    }
    fun set(sheet:Int,ref:String,value:Any?) {
        val c=cell(sheet,ref);val doc=c.ownerDocument
        c.children().filter{it.localName in setOf("f","v","is")}.forEach(c::removeChild)
        c.removeAttribute("t")
        if(value==null || value=="") return
        if(value is Number) {
            require(value.toDouble().isFinite()) {"Número inválido en $ref"}
            c.appendChild(doc.createElementNS(MAIN,"v").apply{textContent=value.toString()})
        } else {
            c.setAttribute("t","inlineStr")
            c.appendChild(doc.createElementNS(MAIN,"is").apply {
                appendChild(doc.createElementNS(MAIN,"t").apply{
                    setAttributeNS("http://www.w3.org/XML/1998/namespace","xml:space","preserve");textContent=value.toString()
                })
            })
        }
    }
    fun formula(sheet:Int,ref:String)=cell(sheet,ref).children().firstOrNull{it.localName=="f"}?.textContent
    fun setFormula(sheet:Int,ref:String,formula:String) {
        set(sheet,ref,null)
        val c=cell(sheet,ref)
        c.appendChild(c.ownerDocument.createElementNS(MAIN,"f").apply{textContent=formula})
    }
    /** Appending leaves every original cell address, drawing and cross-sheet reference intact. */
    fun copyRows(sheet:Int,first:Int,last:Int,destination:Int,lastColumn:Int=18) {
        val doc=sheet(sheet);val data=doc.nodes("sheetData").single();val delta=destination-first
        fun columnName(number:Int):String=if(number<=26) ('A'+number-1).toString() else columnName((number-1)/26)+('A'+(number-1)%26)
        val endColumn=columnName(lastColumn)
        require(destination>doc.nodes("row").filter {it.namespaceURI==MAIN}.maxOf {it.getAttribute("r").toInt()}) {"El bloque debe agregarse después del contenido existente."}
        require(destination+last-first<=1_048_576) {"Se excede el límite de filas de Excel."}
        fun shift(ref:String)=Regex("([A-Z]+)([0-9]+)").replace(ref) {it.groupValues[1]+(it.groupValues[2].toInt()+delta)}
        fun localFormula(formula:String):String = formula.split('"').mapIndexed {i,part->
            if(i%2==1) part else Regex("(?<![A-Za-z0-9_!])([A-Z]{1,3})([0-9]+)").replace(part) {
                val row=it.groupValues[2].toInt()
                if(row in first..last) it.groupValues[1]+(row+delta) else it.value
            }
        }.joinToString("\"")
        doc.nodes("row").filter {it.namespaceURI==MAIN}.filter {it.getAttribute("r").toInt() in first..last}.forEach {source->
            val copy=source.cloneNode(true) as Element
            copy.setAttribute("r",(source.getAttribute("r").toInt()+delta).toString())
            copy.children().filter {it.localName=="c"}.forEach {c->
                if(column(c.getAttribute("r"))>lastColumn) copy.removeChild(c) else {
                    c.setAttribute("r",shift(c.getAttribute("r")))
                    c.children().filter {it.localName=="f"}.forEach {f->
                        require(!f.hasAttribute("t")) {"El bloque contiene una fórmula compartida que requiere expansión explícita."}
                        f.textContent=localFormula(f.textContent)
                    }
                }
            }
            data.appendChild(copy)
        }
        fun contained(ref:String):Boolean {
            val ends=ref.split(':')
            return ends.all {it.filter(Char::isDigit).toInt() in first..last && column(it)<=lastColumn}
        }
        doc.nodes("mergeCell").filter {contained(it.getAttribute("ref"))}.forEach {m->
            m.parentNode.appendChild((m.cloneNode(true) as Element).apply {setAttribute("ref",shift(m.getAttribute("ref")))})
        }
        doc.nodes("mergeCells").forEach {it.setAttribute("count",it.children().size.toString())}
        doc.nodes("dataValidation").toList().forEach {v->
            // Intersect multi-cell validations with the copied block, including boundary ranges.
            val targets=v.getAttribute("sqref").split(' ').mapNotNull {ref->
                val ends=ref.split(':');val a=ends.first();val b=ends.last()
                val lo=maxOf(first,a.filter(Char::isDigit).toInt());val hi=minOf(last,b.filter(Char::isDigit).toInt())
                if(lo>hi || column(a)>lastColumn) null else {
                    val ca=a.takeWhile(Char::isLetter);val cb=if(column(b)>lastColumn) endColumn else b.takeWhile(Char::isLetter)
                    shift("$ca$lo")+if(ca==cb && lo==hi) "" else ":"+shift("$cb$hi")
                }
            }
            if(targets.isNotEmpty()) v.parentNode.appendChild((v.cloneNode(true) as Element).apply {
                setAttribute("sqref",targets.joinToString(" "))
                removeAttributeNS("http://schemas.microsoft.com/office/spreadsheetml/2014/revision","uid")
                children().filter {it.localName in setOf("formula1","formula2")}.forEach {it.textContent=localFormula(it.textContent)}
            })
        }
        doc.nodes("dataValidations").forEach {it.setAttribute("count",it.children().size.toString())}
        val end=destination+last-first
        doc.nodes("dimension").firstOrNull()?.let {
            val old=it.getAttribute("ref")
            val lastCol=columnName(maxOf(column(old.substringAfter(':')),lastColumn))
            it.setAttribute("ref",old.substringBefore(':')+":$lastCol$end")
        }
        val workbook=document("xl/workbook.xml")
        val name=workbook.nodes("sheet")[sheet-1].getAttribute("name").replace("'","''")
        workbook.nodes("definedName").single {it.getAttribute("name")=="_xlnm.Print_Area" && it.getAttribute("localSheetId")== (sheet-1).toString()}.let {
            it.textContent += ",'$name'!\$A\$$destination:\$$endColumn\$$end"
        }
    }

    fun removeValidationAt(sheet:Int,ref:String) {
        sheet(sheet).nodes("dataValidation").toList().forEach {v->
            val remaining=v.getAttribute("sqref").split(' ').filter {it!=ref}
            if(remaining.isEmpty()) v.parentNode.removeChild(v) else v.setAttribute("sqref",remaining.joinToString(" "))
        }
        sheet(sheet).nodes("dataValidations").forEach {it.setAttribute("count",it.children().size.toString())}
    }
    fun finish(output:OutputStream) {
        // Shared strings are compacted, retaining rich text but removing unused example data.
        val strings=document("xl/sharedStrings.xml")
        val old=strings.nodes("si")
        val used=linkedMapOf<Int,Int>();var count=0
        for (path in parts.keys.filter{it.startsWith("xl/worksheets/") && it.endsWith(".xml")}) {
            val doc=document(path)
            doc.nodes("c").forEach {c->
                if(c.children().any{it.localName=="f"}) c.children().filter{it.localName=="v"}.forEach(c::removeChild)
                if(c.getAttribute("t")=="s") c.children().firstOrNull{it.localName=="v"}?.let{v->
                    val index=v.textContent.toInt();v.textContent=used.getOrPut(index){used.size}.toString();count++
                }
            }
        }
        old.forEach {strings.documentElement.removeChild(it)}
        used.keys.forEach{strings.documentElement.appendChild(old[it])}
        strings.documentElement.setAttribute("count",count.toString())
        strings.documentElement.setAttribute("uniqueCount",used.size.toString())
        val workbook=document("xl/workbook.xml")
        workbook.nodes("absPath").forEach{it.parentNode.removeChild(it)}
        val calc=workbook.nodes("calcPr").firstOrNull() ?: workbook.createElementNS(MAIN,"calcPr").also{workbook.documentElement.appendChild(it)}
        calc.setAttribute("calcMode","auto");calc.setAttribute("fullCalcOnLoad","1");calc.setAttribute("forceFullCalc","1")
        parts.remove("xl/calcChain.xml")
        document("xl/_rels/workbook.xml.rels").nodes("Relationship").filter{it.getAttribute("Type").endsWith("/calcChain")}.forEach{it.parentNode.removeChild(it)}
        document("[Content_Types].xml").nodes("Override").filter{it.getAttribute("PartName")=="/xl/calcChain.xml"}.forEach{it.parentNode.removeChild(it)}
        // Delete orphaned source photos after replacing anchors. Logos/methodology images remain referenced.
        val media=mutableSetOf<String>()
        for(path in parts.keys.filter{it.endsWith(".rels")}) {
            document(path).nodes("Relationship").filter{it.getAttribute("Type").endsWith("/image")}.forEach {
                val target=it.getAttribute("Target")
                media += if(target.startsWith("/")) target.drop(1) else "xl/media/"+target.substringAfterLast('/')
            }
        }
        parts.keys.filter{it.startsWith("xl/media/") && it !in media}.toList().forEach(parts::remove)
        ZipOutputStream(output).use {zip->
            parts.forEach {(path,bytes)->
                zip.putNextEntry(ZipEntry(path))
                val doc=documents[path]
                if(doc==null) zip.write(bytes) else TransformerFactory.newInstance().newTransformer().transform(DOMSource(doc),StreamResult(zip))
                zip.closeEntry()
            }
        }
    }
    companion object {
        const val MAIN="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        fun column(ref:String)=ref.takeWhile(Char::isLetter).fold(0){n,c->n*26+(c-'A'+1)}
    }
}
internal fun Document.nodes(name:String):List<Element> = getElementsByTagNameNS("*",name).let{nodes->List(nodes.length){nodes.item(it) as Element}}
internal fun Element.children():List<Element> = (0 until childNodes.length).mapNotNull{childNodes.item(it) as? Element}
