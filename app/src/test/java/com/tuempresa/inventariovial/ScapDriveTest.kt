package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapDriveTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun ack(s: ScapInspectionSnapshot) = JSONObject().put("ok", true).put("protocol", 1)
        .put("recordKind", "SCAP").put("scapInspectionId", s.inspection.id).put("revision", s.inspection.updatedAt)
        .put("sha256", "verified-hash").put("fileId", "zip-id").put("folderId", "version-folder")
        .put("files", org.json.JSONArray((listOf("SCAP.xlsx","manifest.json") + s.photos.map {"fotos/${it.id}.png"} + s.sketches.map {"croquis/${it.id}.png"}).map {
            JSONObject().put("path",it).put("fileId","file-$it").put("folderId","folder-$it")
        }))

    @Test fun onlyMatchingServerAckMarksTheSavedRevisionAndPhotosSynced() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        try {
            val repo = ScapRepository(db, ScapCatalog.load(context)); val id = repo.create("Synthetic", "test", null)
            repo.addPhoto(id, "/test-only/photo", "GENERAL", null)
            val s = repo.dao.snapshot(id)!!
            for (bad in listOf(ack(s).put("ok", false), ack(s).put("revision", 1), ack(s).put("fileId", ""),
                ack(s).put("scapInspectionId", "other"), ack(s).put("sha256", "wrong"), JSONObject().put("ok", true))) {
                assertFalse(ScapDriveSync.validAck(bad, s, "verified-hash"))
                assertTrue(runCatching {ScapDriveSync.acknowledge(db, s, bad, "verified-hash")}.isFailure)
                assertEquals("PENDING", repo.dao.inspection(id)!!.syncStatus)
            }
            assertTrue(ScapDriveSync.acknowledge(db, s, ack(s), "verified-hash"))
            val synced = repo.dao.snapshot(id)!!
            assertEquals("SYNCED", synced.inspection.syncStatus); assertEquals("SYNCED", synced.photos.single().syncStatus)
            assertEquals("zip-id", synced.values("drive")["fileId"])
            assertFalse(synced.values().containsKey("fileId"))
        } finally {db.close()}
    }

    @Test fun editsDuringUploadStayPendingAndOldAckCannotOverwriteNewerData() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        try {
            val repo = ScapRepository(db, ScapCatalog.load(context)); val id = repo.create("Synthetic", "test", null)
            val old = repo.dao.snapshot(id)!!
            repo.dao.syncState(id, old.inspection.updatedAt, "UPLOADING")
            repo.setField(id, "inspection", "bridgeName", "Edited during upload")
            assertFalse(ScapDriveSync.acknowledge(db, old, ack(old), "verified-hash"))
            val current = repo.dao.snapshot(id)!!
            assertEquals("PENDING", current.inspection.syncStatus)
            assertEquals("Edited during upload", current.inspection.bridgeName)
            assertTrue(current.values("drive").isEmpty())
        } finally {db.close()}
    }

    @Test fun bundleContainsOnlyOwnedMarkedPhotosWorkbookSketchesAndVerifiableManifestAndRetriesExactBytes() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        val source = File(context.cacheDir, "source-${UUID.randomUUID()}.png")
        var bundle: File? = null
        try {
            val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply {eraseColor(Color.LTGRAY)}
            source.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)}; bitmap.recycle()
            val original = source.readBytes()
            val repo = ScapRepository(db, ScapCatalog.load(context)); val id = repo.create("Synthetic", "test", null)
            repo.addPhoto(id, source.path, "GENERAL", null)
            val sketch = ScapSketchEntity(UUID.randomUUID().toString(), id, "PLAN", source.path)
            repo.dao.insertSketch(sketch)
            val s = repo.dao.snapshot(id)!!
            bundle = ScapDelivery.bundle(context, s)
            val hash = ScapDelivery.sha256(bundle)
            assertEquals(hash, ScapDelivery.sha256(ScapDelivery.bundle(context, s)))
            ZipFile(bundle).use {zip ->
                val names = zip.entries().asSequence().map {it.name}.toSet()
                assertEquals(setOf("SCAP.xlsx", "manifest.json", "fotos/${s.photos.single().id}.png", "croquis/${sketch.id}.png"), names)
                val manifest = JSONObject(zip.getInputStream(zip.getEntry("manifest.json")).bufferedReader().readText())
                assertEquals(id, manifest.getString("scapInspectionId"))
                assertFalse(manifest.toString().contains(source.absolutePath))
                val files = manifest.getJSONArray("files")
                for (i in 0 until files.length()) {
                    val item = files.getJSONObject(i)
                    assertEquals(item.getString("sha256"), ScapDelivery.sha256(zip.getInputStream(zip.getEntry(item.getString("path"))).readBytes()))
                }
                val image = zip.getInputStream(zip.getEntry("fotos/${s.photos.single().id}.png")).readBytes()
                assertFalse(original.contentEquals(image))
                val workbook = TemplateWorkbook(zip.getInputStream(zip.getEntry("SCAP.xlsx")))
                assertArrayEquals(image, workbook.parts.getValue("xl/media/scap_5_1.png"))
                assertEquals(2, workbook.parts.keys.count {it.startsWith("xl/media/scap_")})
            }
            assertArrayEquals(original, source.readBytes())
            assertEquals("PENDING", repo.dao.inspection(id)!!.syncStatus)
            // A different inspection cannot include the first inspection's photo.
            val foreign = s.copy(inspection = s.inspection.copy(id = UUID.randomUUID().toString()))
            assertTrue(runCatching {ScapDelivery.bundle(context, foreign)}.isFailure)
        } finally {bundle?.delete(); source.delete(); db.close()}
    }

    @Test fun uploadBodyStreamsExactBase64AndEscapesMetadata() {
        val file = File(context.cacheDir, "body-${UUID.randomUUID()}.zip")
        try {
            file.writeBytes(ByteArray(65539) {(it % 256).toByte()})
            val body = ScapUploadBody(JSONObject().put("bridgeName", "Río \"Ñ\"\nPuente"), file)
            val sink = Buffer(); body.writeTo(sink)
            assertEquals(body.contentLength(), sink.size)
            val json = JSONObject(sink.readUtf8())
            assertEquals("Río \"Ñ\"\nPuente", json.getString("bridgeName"))
            assertArrayEquals(file.readBytes(), java.util.Base64.getDecoder().decode(json.getString("base64")))
        } finally {file.delete()}
    }
}
