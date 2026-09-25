package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.road.RoadReferenceData
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.validation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FieldReadinessTest {
    @get:Rule val temp=TemporaryFolder()
    @Test fun driveRejectsBlankTokenAndNonDeploymentUrlsWithoutLeakingConfiguration() {
        val valid="https://script.google.com/macros/s/test-deployment_123/exec"
        assertEquals("Drive no configurado: falta DRIVE_API_TOKEN en drive.local.properties.",DriveUploadPolicy.configurationError("",valid))
        assertNotNull(DriveUploadPolicy.configurationError("   ",valid))
        for(url in listOf("","https://script.google.com/d/editor-id/edit","http://script.google.com/macros/s/x/exec",
            "https://script.google.com/macros/s/x/dev","https://example.com/macros/s/x/exec","https://script.google.com/macros/s/x/exec?token=test-secret",
            "https://script.google.com.evil.test/macros/s/x/exec","https://script.google.com/macros/s//exec")) {
            val error=DriveUploadPolicy.configurationError("test-secret",url)
            assertNotNull(error);assertFalse(error!!.contains("test-secret"))
        }
        assertNull(DriveUploadPolicy.configurationError("test-secret",valid))
    }
    @Test fun stampedReadableImageWinsAndInvalidStampFallsBackWithoutRenaming() {
        fun image(name:String)=temp.newFile(name).also{f->
            val bitmap=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888)
            f.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle()
        }
        val local=image("local.jpg");val stamped=image("stamp.jpg")
        val photo=PhotoEntity("p","r",1,local.path,true,"FINAL-SIC.jpg",null,null,"PENDING",0,originalPath="/original.jpg",stampedPath=stamped.path)
        assertEquals(stamped.path,DriveUploadPolicy.uploadPath(photo));assertEquals("FINAL-SIC.jpg",photo.generatedFileName)
        val invalid=temp.newFile("invalid.jpg").apply{writeText("not an image")}
        for(path in listOf<String?>(null,"/missing.jpg",invalid.path,temp.newFolder("folder").path,temp.newFile("empty.jpg").path)) {
            assertEquals(local.path,DriveUploadPolicy.uploadPath(photo.copy(stampedPath=path)))
        }
        assertEquals("/original.jpg",photo.originalPath);assertEquals(local.path,photo.localPath)
    }
    @Test fun scapCannotEnterQueueEvenIfItsTechnicalParentIsIncorrectlyMarkedActive() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val scap=ScapRepository(db,testScapCatalog());val id=scap.create("I","device",null)
            scap.addPhoto(id,"/scap.jpg","GENERAL",null)
            val s=scap.dao.snapshot(id)!!;val parent=db.inventoryDao().recordById(s.inspection.roadRecordId)!!
            db.inventoryDao().updateRecord(parent.copy(status="ACTIVE"))
            db.inventoryDao().insertRecord(testRecord())
            db.inventoryDao().insertPhoto(PhotoEntity("sic-photo","id",1,"/sic.jpg",true,"keep.jpg",null,null,"PENDING",0))
            assertEquals(listOf("sic-photo"),db.inventoryDao().pendingPhotos().map{it.id})
            assertFalse(DriveUploadPolicy.eligible("SCAP","ACTIVE",null))
            assertFalse(DriveUploadPolicy.eligible("SIC-17","ACTIVE",id))
            assertFalse(DriveUploadPolicy.eligible("SIC-18","ANNULLED",null))
            assertFalse(DriveUploadPolicy.eligible("SIC-18","DRAFT",null))
            assertTrue(DriveUploadPolicy.eligible("SIC-18","ACTIVE",null))
            // Guard must run before accessing WorkManager, regardless of local credentials.
            val error=runCatching{scheduleDriveUpload(context,"/scap.jpg","unchanged.jpg","R","SIB","SCAP")}.exceptionOrNull()
            assertTrue(error is IllegalArgumentException);assertEquals(DriveUploadPolicy.SCAP_LOCAL_MESSAGE,error!!.message)
        } finally {db.close()}
    }
    @Test fun uploadCompletionUsesRoomIdentityWhenPhysicalFileIsStamped() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),InventoryDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao=db.inventoryDao();dao.insertRecord(testRecord())
            dao.insertPhoto(PhotoEntity("p","id",1,"/local.jpg",true,"final.jpg",null,null,"PENDING",0,originalPath="/original.jpg",stampedPath="/stamp.jpg"))
            dao.setPhotoSyncStatus("p","QUEUED");assertEquals("PROGRAMADA",DriveUploadPolicy.statusLabel(dao.photoById("p")!!.syncStatus))
            dao.setPhotoSyncStatus("p","UPLOADING");assertEquals("SUBIENDO",DriveUploadPolicy.statusLabel(dao.photoById("p")!!.syncStatus))
            dao.completePhotoUpload("/local.jpg","remote-id")
            val saved=dao.photoById("p")!!
            assertEquals("SINCRONIZADA",DriveUploadPolicy.statusLabel(saved.syncStatus));assertEquals("remote-id",saved.driveFileId)
            assertEquals("final.jpg",saved.generatedFileName);assertEquals("/stamp.jpg",saved.stampedPath)
            assertEquals("SYNCED",dao.recordById("id")!!.photoSyncStatus)
            assertEquals("ERROR",DriveUploadPolicy.statusLabel("ERROR"));assertEquals("PENDIENTE",DriveUploadPolicy.statusLabel("PENDING"))
        } finally {db.close()}
    }
    @Test fun endLocationPolicyPreservesPointLineRulesAndMatchesWarnings() {
        for(sic in listOf("SIC-17","SIC-18","SIC-22","SCAP")) assertFalse(requiresEndLocation(sic))
        for(sic in listOf("SIC-19","SIC-21")) assertTrue(requiresEndLocation(sic))
        assertFalse(requiresEndLocation("SIC-20",sic20Class="12"))
        for(code in listOf("13","14")) assertTrue(requiresEndLocation("SIC-20",sic20Class=code))
        for(asset in listOf("MURO","TUNEL")) assertTrue(requiresEndLocation("SIC-20",asset))
        for(code in listOf("21","22")) assertTrue(requiresEndLocation("SIC-23",sic23Class=code))
        for(code in listOf("23","24")) assertFalse(requiresEndLocation("SIC-23",sic23Class=code))
        val base=InventorySaveRequest("SIC-20","BADEN","R","CD","1","0",null,null,null,
            -12.0,-77.0,1f,"24/09/2026","","",SicFormDetail.Sic20(Sic20FormState(classCode="12")),photoPaths=emptyList())
        val cases=listOf(base,base.copy(detail=SicFormDetail.Sic20(Sic20FormState(classCode="13"))),
            base.copy(sicCode="SIC-22",detail=SicFormDetail.Sic22(Sic22FormState())),
            base.copy(sicCode="SIC-23",detail=SicFormDetail.Sic23(Sic23FormState(classCode="23"))),
            base.copy(sicCode="SIC-23",detail=SicFormDetail.Sic23(Sic23FormState(classCode="21"))))
        for(r in cases) {
            assertEquals(requiresEndLocation(r),CaptureValidation.warnings(r,RoadReferenceData(),0).any{it.code=="MISSING_END_GPS"})
            val summary=com.tuempresa.inventariovial.field.captureSummary(r.copy(endLatitude=-12.1,endLongitude=-77.1))
            assertTrue(summary.any{it.first=="GPS inicial"})
            assertEquals(requiresEndLocation(r),summary.any{it.first=="GPS final"})
        }
    }
}
