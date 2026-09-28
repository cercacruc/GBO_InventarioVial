package com.tuempresa.inventariovial

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

/** Synthetic data in an isolated in-memory database; never edits the tablet's field records. */
class ScapDriveDeviceTest {
    @Test fun realAndroidBundleAndAckTransaction() = runBlocking {exercise(false)}

    @Test fun realConfiguredDriveUploadAndLostResponseRetry() = runBlocking {
        // Network test is explicitly opt-in; no credentials in arguments, source or reports.
        assumeTrue(InstrumentationRegistry.getArguments().getString("scapDriveE2E") == "true")
        exercise(true)
    }

    private suspend fun exercise(remote: Boolean) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        val image=File(context.cacheDir,"scap-device-test-${UUID.randomUUID()}.png")
        var bundle:File?=null
        try {
            val bitmap=Bitmap.createBitmap(800,600,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.LTGRAY)}
            image.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            val repo=ScapRepository(db,ScapCatalog.load(context));val id=repo.create("PRUEBA SINTÉTICA", "instrumentation",null)
            repo.setField(id,"inspection","bridgeName","PRUEBA TÉCNICA SCAP · NO ES INVENTARIO DE CAMPO")
            repo.setField(id,"inspection","bridgeCode","GBO-PRUEBA-SCAP-20260927")
            repo.setField(id,"inspection","route","PE-3N")
            repeat(5) {repo.addRow(id,"SPAN")};repeat(7) {repo.addRow(id,"PIER")};repeat(3) {repo.addRow(id,"BEARING")}
            repo.addPhoto(id,image.path,"GENERAL",null)
            val s=repo.dao.snapshot(id)!!; bundle=ScapDelivery.bundle(context,s)
            val hash=ScapDelivery.sha256(bundle)
            ZipFile(bundle).use {zip->
                assertNotNull(zip.getEntry("SCAP.xlsx"));assertNotNull(zip.getEntry("manifest.json"))
                val workbook=TemplateWorkbook(zip.getInputStream(zip.getEntry("SCAP.xlsx")))
                assertEquals("5",workbook.cell(1,"E62").textContent)
                assertEquals(10,workbook.document("xl/workbook.xml").nodes("sheet").size)
            }
            assertEquals("PENDING",repo.dao.inspection(id)!!.syncStatus)
            if(remote) {
                val first=ScapDriveClient.upload(s,bundle,hash)
                assertTrue("El endpoint publicado no confirmó el contrato completo SCAP",ScapDriveSync.validAck(first,s,hash))
                val retry=ScapDriveClient.upload(s,bundle,hash)
                assertTrue(ScapDriveSync.validAck(retry,s,hash));assertTrue(retry.getBoolean("alreadyExists"))
                assertEquals(first.getString("fileId"),retry.getString("fileId"))
                assertTrue(ScapDriveSync.acknowledge(db,s,retry,hash))
                assertEquals("SYNCED",repo.dao.inspection(id)!!.syncStatus)
                assertTrue(repo.dao.snapshot(id)!!.photos.single().driveFileId!!.isNotBlank())
            }
        } finally {bundle?.delete();image.delete();db.close()}
    }
}
