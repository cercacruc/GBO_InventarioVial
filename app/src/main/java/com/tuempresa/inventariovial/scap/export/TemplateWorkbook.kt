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

/** Edits cell contents only. Original style ids, row/column geometry and other ZIP parts survive. */
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
        val row=doc.nodes("row").firstOrNull{it.getAttribute("r").toInt()==rowNumber}
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
