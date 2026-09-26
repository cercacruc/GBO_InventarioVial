package com.tuempresa.inventariovial.scap.export

import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import org.w3c.dom.Element
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal object ScapTemplateMedia {
    private const val REL="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private data class Picture(val path:String,val description:String)
    fun write(w:TemplateWorkbook,s:ScapInspectionSnapshot,read:(String)->ByteArray) {
        val photos=s.photos.sortedBy{it.photoIndex}
        w.set(5,"H1",photos.size);w.set(5,"H5","NO")
        for(i in 0..31) {
            val p=photos.getOrNull(i);val row=i+13
            w.set(5,"B$row",p?.let {i+1})
            w.set(5,"C$row",p?.let{ChronoUnit.DAYS.between(LocalDate.of(1899,12,30),Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate())})
            val description=p?.let{listOfNotNull(com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.label(it.photoCategory),it.description?.takeIf(String::isNotBlank),it.scapElementCode).joinToString(" · ") + s.defects.filter{d->d.photoId==it.id}.joinToString("",prefix=""){d->"\n${d.description} · ${d.locationDescription}"}}
            w.set(5,"D$row",description)
        }
        replace(w,5,photos.map{Picture(DriveUploadPolicy.uploadPath(it),listOfNotNull(com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.label(it.photoCategory),it.description?.takeIf(String::isNotBlank),it.scapElementCode).joinToString(" · "))},read)
        replace(w,2,listOf("ELEVATION","PLAN","CROSS_SECTION").map{type->s.sketches.singleOrNull{it.type==type}?.let{Picture(it.localUri,type)}},read)
    }
    private fun replace(w:TemplateWorkbook,sheet:Int,pictures:List<Picture?>,read:(String)->ByteArray) {
        val sheetRelations=w.document("xl/worksheets/_rels/sheet$sheet.xml.rels")
        val drawing=sheetRelations.nodes("Relationship").single{it.getAttribute("Type").endsWith("/drawing")}.getAttribute("Target").substringAfterLast('/')
        val doc=w.document("xl/drawings/$drawing")
        val rels=w.document("xl/drawings/_rels/$drawing.rels")
        val anchors=doc.documentElement.children().filter{anchor->
            anchor.getElementsByTagNameNS("*","from").item(0)?.let{from->
                (from as Element).getElementsByTagNameNS("*","row").item(0)?.textContent?.toInt()?.let{it>=8}
            }==true
        }.sortedBy{it.getElementsByTagNameNS("*","row").item(0).textContent.toInt()}
        require(pictures.size<=anchors.size){"Faltan espacios para imágenes en la plantilla."}
        anchors.forEachIndexed{i,anchor->
            val picture=pictures.getOrNull(i)
            if(picture==null) doc.documentElement.removeChild(anchor)
            else {
                val bytes=read(picture.path)
                val extension=when {
                    bytes.size>8 && bytes[0]==0x89.toByte() && bytes[1]==0x50.toByte()->"png"
                    bytes.size>3 && bytes[0]==0xff.toByte() && bytes[1]==0xd8.toByte()->"jpg"
                    else->error("Imagen ilegible o no compatible: ${picture.description}")
                }
                val bounds=android.graphics.BitmapFactory.Options().apply {inJustDecodeBounds=true}
                android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                require(bounds.outWidth>0 && bounds.outHeight>0) {"Imagen ilegible: ${picture.description}"}
                val name="scap_${sheet}_${i+1}.$extension";val rid="scapImage${i+1}"
                w.parts["xl/media/$name"]=bytes
                (anchor.getElementsByTagNameNS("*","blip").item(0) as Element).setAttributeNS(REL,"r:embed",rid)
                val props=anchor.getElementsByTagNameNS("*","cNvPr").item(0) as Element
                props.setAttribute("name",name);props.setAttribute("descr",picture.description)
                // Source photos have crop metadata specific to their pixels.
                val crops=anchor.getElementsByTagNameNS("*","srcRect")
                while(crops.length>0) crops.item(0).parentNode.removeChild(crops.item(0))
                rels.documentElement.appendChild(rels.createElementNS("http://schemas.openxmlformats.org/package/2006/relationships","Relationship").apply{
                    setAttribute("Id",rid);setAttribute("Type","$REL/image");setAttribute("Target","../media/$name")
                })
                fitPicture(anchor,bounds.outWidth,bounds.outHeight,w.sheet(sheet))
                val content=w.document("[Content_Types].xml")
                if(content.nodes("Default").none{it.getAttribute("Extension")==extension}) content.documentElement.appendChild(content.createElementNS(content.documentElement.namespaceURI,"Default").apply{
                    setAttribute("Extension",extension);setAttribute("ContentType",if(extension=="jpg")"image/jpeg" else "image/png")
                })
            }
        }
        val used=doc.nodes("blip").map{it.getAttributeNS(REL,"embed")}.toSet()
        rels.nodes("Relationship").filter{it.getAttribute("Type").endsWith("/image") && it.getAttribute("Id") !in used}.forEach{it.parentNode.removeChild(it)}
    }
    /** Fit the full image inside the existing template frame, without crop or stretch. */
    private fun fitPicture(anchor:Element,width:Int,height:Int,worksheet:org.w3c.dom.Document) {
        val doc=anchor.ownerDocument
        val transform=anchor.getElementsByTagNameNS("*","xfrm").item(0) as Element
        val extent=transform.children().single {it.localName=="ext"}
        val from=anchor.children().single {it.localName=="from"}
        val to=anchor.children().singleOrNull {it.localName=="to"}
        val format=worksheet.nodes("sheetFormatPr").single()
        fun coordinate(marker:Element,key:String)=marker.children().single {it.localName==key}.textContent.toLong()
        fun distance(axis:String):Long {
            if(to==null) return anchor.children().single {it.localName=="ext"}.getAttribute(if(axis=="col") "cx" else "cy").toLong()
            val start=coordinate(from,axis).toInt();val end=coordinate(to,axis).toInt()
            val cells=(start until end).sumOf {index->
                if(axis=="row") {
                    val row=worksheet.nodes("row").firstOrNull {it.namespaceURI==TemplateWorkbook.MAIN && it.getAttribute("r")== (index+1).toString()}
                    val points=row?.getAttribute("ht")?.toDoubleOrNull() ?: format.getAttribute("defaultRowHeight").toDouble()
                    (points*12700).toLong()
                } else {
                    val col=worksheet.nodes("col").firstOrNull {index+1 in it.getAttribute("min").toInt()..it.getAttribute("max").toInt()}
                    val characters=col?.getAttribute("width")?.toDoubleOrNull() ?: format.getAttribute("defaultColWidth").toDoubleOrNull() ?: 8.43
                    // Canonical workbook uses Calibri 11 (maximum digit width 7 px at 96 dpi).
                    kotlin.math.floor((256*characters+kotlin.math.floor(128.0/7))/256*7).toLong()*9525
                }
            }
            return cells+coordinate(to,axis+"Off")-coordinate(from,axis+"Off")
        }
        // Source xfrm sizes can be stale after row-height edits; never exceed the actual cell frame.
        val oldWidth=minOf(extent.getAttribute("cx").toLong(),distance("col"))
        val oldHeight=minOf(extent.getAttribute("cy").toLong(),distance("row"))
        val ratio=minOf(oldWidth.toDouble()/width,oldHeight.toDouble()/height)
        val cx=(width*ratio).toLong();val cy=(height*ratio).toLong()
        val dx=(oldWidth-cx)/2;val dy=(oldHeight-cy)/2
        for((key,delta) in listOf("colOff" to dx,"rowOff" to dy)) {
            from.children().single {it.localName==key}.let {it.textContent=(it.textContent.toLong()+delta).toString()}
        }
        transform.children().single {it.localName=="off"}.let {
            it.setAttribute("x",(it.getAttribute("x").toLong()+dx).toString())
            it.setAttribute("y",(it.getAttribute("y").toLong()+dy).toString())
        }
        extent.setAttribute("cx",cx.toString());extent.setAttribute("cy",cy.toString())
        val replacement=doc.createElementNS(anchor.namespaceURI,"xdr:oneCellAnchor")
        replacement.appendChild(from)
        replacement.appendChild(doc.createElementNS(anchor.namespaceURI,"xdr:ext").apply {
            setAttribute("cx",cx.toString());setAttribute("cy",cy.toString())
        })
        anchor.children().filter {it.localName in setOf("pic","clientData")}.forEach {replacement.appendChild(it)}
        anchor.parentNode.replaceChild(replacement,anchor)
    }

}
