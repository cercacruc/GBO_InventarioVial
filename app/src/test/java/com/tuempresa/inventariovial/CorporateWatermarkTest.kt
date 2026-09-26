package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.camera.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CorporateWatermarkTest {
    @get:Rule val temp=TemporaryFolder()
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val service get()=PhotoStampService(context)
    private val data=PhotoStampData("PE-3N","UC","30+123",null,1_750_000_000_000,"Inspector","Puente")
    @Test fun requiredLogoIsAutomaticReusableAndKeepsOriginalUntouched()=runBlocking {
        val original=source(1200,800)
        val bytes=original.readBytes()
        val marked=RequiredWatermark.prepare(context,original.path,directory=temp.root)
        assertNotEquals(original.path,marked.path)
        assertEquals(RequiredWatermark.MARKER,ExifInterface(marked.path).getAttribute(ExifInterface.TAG_USER_COMMENT))
        val bitmap=BitmapFactory.decodeFile(marked.path)
        var changed=0
        for(y in 30..180 step 3) for(x in 960..1150 step 3) {
            val pixel=bitmap.getPixel(x,y)
            if(abs(Color.red(pixel)-Color.red(Color.LTGRAY))>25 ||
                abs(Color.green(pixel)-Color.green(Color.LTGRAY))>25 ||
                abs(Color.blue(pixel)-Color.blue(Color.LTGRAY))>25) changed++
        }
        assertTrue("The delivery copy must contain visible logo pixels",changed>100)
        bitmap.recycle()
        assertEquals(marked.path,RequiredWatermark.prepare(context,original.path,marked.path,temp.root).path)
        assertArrayEquals(bytes,original.readBytes())
    }
    @Test fun missingCopyIsRegeneratedButMissingSourceFailsClosed()=runBlocking {
        val original=source(1200,800)
        val missing=File(temp.root,"missing.jpg").path
        val marked=RequiredWatermark.prepare(context,original.path,missing,temp.root)
        assertTrue(marked.isFile)
        assertTrue(runCatching {RequiredWatermark.prepare(context,missing,null,temp.root)}.isFailure)
    }
    @Test fun optionalTextCopyKeepsLogoWhenPreparedForDelivery()=runBlocking {
        val original=source(1200,800)
        val textCopy=service.process(original,temp.root,data)
        assertEquals(textCopy.path,RequiredWatermark.prepare(context,original.path,textCopy.path,temp.root).path)
        val disabled=service.process(original,temp.root,data,PhotoStampConfig(showCorporateLogo=false))
        val repaired=RequiredWatermark.prepare(context,original.path,disabled.path,temp.root)
        assertNotEquals(disabled.path,repaired.path)
        assertEquals(RequiredWatermark.MARKER,ExifInterface(repaired.path).getAttribute(ExifInterface.TAG_USER_COMMENT))
    }
    private fun source(width:Int,height:Int,marker:Boolean=false):File {
        val file=temp.newFile("source-${System.nanoTime()}.jpg")
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).apply {eraseColor(Color.LTGRAY)}
        if(marker) Canvas(bitmap).drawRect(0f,0f,width/2f,height/2f,Paint().apply {color=Color.RED})
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.JPEG,100,it)};bitmap.recycle()
        return file
    }
    @Test fun officialPngIsByteIdenticalAndHasTransparentPixels() {
        val bytes=context.resources.openRawResource(R.drawable.gbo_logo_watermark).use {it.readBytes()}
        assertEquals("4f126a163787bf58f00ec5a7e226d120ab924f37bd6ae539015fff0645dda0dc",MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)})
        val logo=BitmapFactory.decodeByteArray(bytes,0,bytes.size)
        assertEquals(1448,logo.width);assertEquals(1086,logo.height);assertTrue(logo.hasAlpha())
        assertEquals(0,Color.alpha(logo.getPixel(0,0)));logo.recycle()
    }
    @Test fun portraitAndLandscapeCopiesContainOnlyProportionalUpperRightLogoAndAlphaWorks()=runBlocking {
        for((width,height) in listOf(1200 to 800,800 to 1200)) {
            val original=source(width,height);val before=original.readBytes()
            suspend fun stamp(alpha:Int,show:Boolean=true):Bitmap {
                val file=service.process(original,temp.root,data,PhotoStampConfig(fields=emptySet(),logoAlpha=alpha,showCorporateLogo=show,jpegQuality=100))
                assertNotEquals(original.canonicalPath,file.canonicalPath)
                return BitmapFactory.decodeFile(file.absolutePath)
            }
            val clear=stamp(0);val half=stamp(100);val full=stamp(200);val disabled=stamp(200,false)
            var differenceHalf=0L;var differenceFull=0L;var changed=0
            for(y in 0 until height step 2) for(x in 0 until width step 2) {
                val base=clear.getPixel(x,y);val f=full.getPixel(x,y);val h=half.getPixel(x,y)
                val diff=abs(Color.red(base)-Color.red(f))+abs(Color.green(base)-Color.green(f))+abs(Color.blue(base)-Color.blue(f))
                differenceFull+=diff;differenceHalf+=abs(Color.red(base)-Color.red(h))+abs(Color.green(base)-Color.green(h))+abs(Color.blue(base)-Color.blue(h))
                assertEquals(base,disabled.getPixel(x,y))
                if(diff>25) {
                    changed++
                    assertTrue("Logo x=$x",x>=width*.795f-8 && x<=width*.975f+8)
                    assertTrue("Logo y=$y",y>=height*.025f-8 && y<=height*.025f+width*.18f*.75f+8)
                }
            }
            assertTrue(changed>500)
            assertEquals(.5,differenceHalf.toDouble()/differenceFull,.035)
            val rect=service.corporateLogoBounds(width,height,1448,1086,PhotoStampConfig())
            assertEquals(width*.18f,rect.width(),.001f)
            assertEquals(1448f/1086,rect.width()/rect.height(),.0001f)
            assertEquals(width*.025f,width-rect.right,.001f);assertEquals(height*.025f,rect.top,.001f)
            assertArrayEquals(before,original.readBytes())
            listOf(clear,half,full,disabled).forEach {it.recycle()}
        }
    }
    @Test fun allExifOrientationsAreAppliedBeforeLogoAndOriginalBytesNeverChange()=runBlocking {
        // Expected quadrant of the original red upper-left marker after each EXIF transform.
        val corners=listOf(0 to 0,1 to 0,1 to 1,0 to 1,0 to 0,1 to 0,1 to 1,0 to 1)
        for(orientation in 1..8) {
            val original=source(1200,800,true)
            ExifInterface(original.path).apply {setAttribute(ExifInterface.TAG_ORIENTATION,orientation.toString());saveAttributes()}
            val before=original.readBytes()
            val file=service.process(original,temp.root,data,PhotoStampConfig(fields=emptySet()))
            val bitmap=BitmapFactory.decodeFile(file.path)
            assertEquals(if(orientation>=5) 800 else 1200,bitmap.width)
            assertEquals(if(orientation>=5) 1200 else 800,bitmap.height)
            val corner=corners[orientation-1]
            val pixel=bitmap.getPixel((bitmap.width*(if(corner.first==0) .25 else .75)).toInt(),(bitmap.height*(if(corner.second==0) .25 else .75)).toInt())
            assertTrue("EXIF $orientation",Color.red(pixel)>220 && Color.green(pixel)<25)
            assertEquals(ExifInterface.ORIENTATION_NORMAL,ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION,0))
            assertArrayEquals(before,original.readBytes());bitmap.recycle()
        }
    }
    @Test fun textRemainsBottomRightAndPanoramasFitLogoWithoutCropping()=runBlocking {
        val source=source(1600,300)
        val file=service.process(source,temp.root,data,PhotoStampConfig(fields=PhotoStampField.entries.toSet()))
        val bitmap=BitmapFactory.decodeFile(file.path)
        val rect=service.corporateLogoBounds(bitmap.width,bitmap.height,1448,1086,PhotoStampConfig())
        assertEquals(1448f/1086,rect.width()/rect.height(),.0001f)
        assertTrue(rect.bottom<bitmap.height/2f)
        assertNotEquals(bitmap.getPixel(10,10),bitmap.getPixel(bitmap.width-12,bitmap.height-12))
        bitmap.recycle()
    }
}
