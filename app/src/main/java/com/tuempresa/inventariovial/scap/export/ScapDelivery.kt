package com.tuempresa.inventariovial.scap.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.tuempresa.inventariovial.camera.RequiredWatermark
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One export path for local delivery and Drive. Never changes capture files. */
object ScapDelivery {
    fun exporter(context: Context) = ScapExcelExporter({context.assets.open("scap_template.xlsx")},
        context.assets.open("scap_field_map.json").bufferedReader().use {it.readText()})

    suspend fun writeExcel(context: Context, snapshot: ScapInspectionSnapshot, target: File) {
        val temporary = mutableListOf<File>()
        try {
            val directory = File(context.cacheDir, "watermark-export")
            val delivery = snapshot.copy(photos = snapshot.photos.map { photo ->
                require(photo.scapInspectionId == snapshot.inspection.id && photo.recordId == snapshot.inspection.roadRecordId)
                val marked = RequiredWatermark.prepare(context, photo.originalPath, photo.stampedPath, directory)
                if (marked.parentFile?.canonicalPath == directory.canonicalPath) temporary += marked
                photo.copy(localPath = marked.absolutePath, stampedPath = marked.absolutePath)
            })
            withContext(Dispatchers.IO) {target.outputStream().use {exporter(context).write(it, delivery) {path -> readImage(context, path)}}}
        } finally { temporary.forEach {it.delete()} }
    }

    /** Cache by saved revision so a lost response/restart retries identical bytes. */
    suspend fun bundle(context: Context, s: ScapInspectionSnapshot): File = withContext(Dispatchers.IO) {
        require(s.inspection.id.matches(Regex("[a-fA-F0-9-]{36}")))
        val directory = File(context.filesDir, "scap-drive/${s.inspection.id}").apply {mkdirs()}
        val target = File(directory, "${s.inspection.updatedAt}.zip")
        if (target.isFile) return@withContext target
        val work = File(context.cacheDir, "scap-bundle-${java.util.UUID.randomUUID()}").apply {mkdirs()}
        val partial = File(directory, "${s.inspection.updatedAt}.partial")
        try {
            val files = JSONArray()
            ZipOutputStream(partial.outputStream().buffered()).use {zip ->
                fun add(path: String, bytes: ByteArray, metadata: JSONObject = JSONObject()) {
                    zip.putNextEntry(ZipEntry(path).apply {time = 0}); zip.write(bytes); zip.closeEntry()
                    files.put(metadata.put("path", path).put("sha256", sha256(bytes)))
                }
                // Prepare each photo once and reuse those exact pixels in the workbook and separate files.
                val delivery = s.copy(photos = s.photos.sortedBy {it.photoIndex}.map {p ->
                    require(p.scapInspectionId == s.inspection.id && p.recordId == s.inspection.roadRecordId)
                    val marked = RequiredWatermark.prepare(context, p.originalPath, p.stampedPath, work)
                    val png = readImage(context, marked.absolutePath)
                    add("fotos/${p.id}.png", png, JSONObject().put("photoId", p.id).put("order", p.photoIndex)
                        .put("category", p.photoCategory).put("elementCode", p.scapElementCode).put("description", p.description))
                    p.copy(localPath = marked.absolutePath, stampedPath = marked.absolutePath)
                })
                val workbook = File(work, "SCAP.xlsx")
                workbook.outputStream().use {exporter(context).write(it, delivery) {path -> readImage(context, path)}}
                add("SCAP.xlsx", workbook.readBytes())
                s.sketches.sortedBy {it.id}.forEach {sketch ->
                    add("croquis/${sketch.id}.png", readImage(context, sketch.localUri), JSONObject().put("type", sketch.type))
                }
                val manifest = JSONObject().put("protocol", 1).put("recordKind", "SCAP")
                    .put("scapInspectionId", s.inspection.id).put("revision", s.inspection.updatedAt)
                    .put("bridgeCode", s.inspection.bridgeCode).put("bridgeName", s.inspection.bridgeName)
                    .put("routeCode", s.values()["route"].orEmpty()).put("files", files)
                zip.putNextEntry(ZipEntry("manifest.json").apply {time = 0})
                zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            check(partial.renameTo(target)) {"No se pudo guardar el paquete SCAP."}
            target
        } finally {partial.delete(); work.deleteRecursively()}
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {"%02x".format(it)}
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use {stream -> val buffer = ByteArray(65536); while (true) {
            val count = stream.read(buffer); if (count < 0) break; digest.update(buffer, 0, count)
        }}
        return digest.digest().joinToString("") {"%02x".format(it)}
    }

    /** Bound workbook image resolution; preserve original files and orientation prepared by watermark. */
    fun readImage(context: Context, path: String): ByteArray {
        fun stream() = if (path.startsWith("content:")) requireNotNull(context.contentResolver.openInputStream(Uri.parse(path))) else File(path.removePrefix("file://")).inputStream()
        val bounds = BitmapFactory.Options().apply {inJustDecodeBounds = true}
        stream().use {BitmapFactory.decodeStream(it, null, bounds)}
        require(bounds.outWidth > 0 && bounds.outHeight > 0) {"Una imagen local no puede leerse."}
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1800) sample *= 2
        val bitmap = stream().use {BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {inSampleSize = sample})} ?: error("Imagen ilegible.")
        return try {ByteArrayOutputStream().use {out -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); out.toByteArray()}} finally {bitmap.recycle()}
    }
}
