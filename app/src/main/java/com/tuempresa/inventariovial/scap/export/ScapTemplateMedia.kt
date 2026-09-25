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
            w.set(5,"B$row",p?.photoIndex)
            w.set(5,"C$row",p?.let{ChronoUnit.DAYS.between(LocalDate.of(1899,12,30),Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate())})
            val description=p?.let{listOfNotNull(it.photoCategory,it.scapElementCode).joinToString(" · ") + s.defects.filter{d->d.photoId==it.id}.joinToString("",prefix=""){d->"\n${d.description} · ${d.locationDescription}"}}
            w.set(5,"D$row",description)
        }
        replace(w,5,photos.map{Picture(DriveUploadPolicy.uploadPath(it),listOfNotNull(it.photoCategory,it.scapElementCode).joinToString(" · "))},read)
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
                val content=w.document("[Content_Types].xml")
                if(content.nodes("Default").none{it.getAttribute("Extension")==extension}) content.documentElement.appendChild(content.createElementNS(content.documentElement.namespaceURI,"Default").apply{
                    setAttribute("Extension",extension);setAttribute("ContentType",if(extension=="jpg")"image/jpeg" else "image/png")
                })
            }
        }
        val used=doc.nodes("blip").map{it.getAttributeNS(REL,"embed")}.toSet()
        rels.nodes("Relationship").filter{it.getAttribute("Type").endsWith("/image") && it.getAttribute("Id") !in used}.forEach{it.parentNode.removeChild(it)}
    }
}
