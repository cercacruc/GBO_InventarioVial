package com.tuempresa.inventariovial

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import com.tuempresa.inventariovial.camera.*
import com.tuempresa.inventariovial.location.GeoLocation
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
class PhotoStampServiceTest {
    @get:Rule val temporary=TemporaryFolder()
    @Test fun processedCopyPreservesOriginalAndOrientationAndWritesGps() = runBlocking {
        val original=temporary.newFile("original.jpg")
        Bitmap.createBitmap(1200,800,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.LTGRAY) }.let { bitmap ->
            original.outputStream().use {bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle()
        }
        ExifInterface(original.absolutePath).apply {setAttribute(ExifInterface.TAG_ORIENTATION,"6");saveAttributes()}
        val before=original.readBytes()
        val data=PhotoStampData("RUTA "+"muy extensa ".repeat(15),"CD","0045 + 273 m",GeoLocation(-12.123456,-77.123456,0.5f),1_750_000_000_000,"Ingeniero","Cuneta")
        val stamped=PhotoStampService().process(original,temporary.newFolder("processed"),data,PhotoStampConfig(fields=PhotoStampField.entries.toSet()))
        assertArrayEquals(before,original.readBytes());assertNotEquals(original.canonicalPath,stamped.canonicalPath)
        val output=BitmapFactory.decodeFile(stamped.absolutePath)
        assertEquals(800,output.width);assertEquals(1200,output.height)
        // Bottom-right background differs from the untouched upper image.
        assertNotEquals(output.getPixel(50,50),output.getPixel(output.width-40,output.height-40))
        val exif=ExifInterface(stamped.absolutePath);val coords=FloatArray(2)
        assertTrue(exif.getLatLong(coords));assertEquals(-12.123456,coords[0].toDouble(),0.00001)
        assertEquals(-77.123456,coords[1].toDouble(),0.00001)
        assertEquals(1,exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,0));assertNotNull(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        output.recycle()
    }
    @Test fun fieldsAreOptionalAndTextDoesNotInventMissingAccuracy() {
        val data=PhotoStampData("R","CD","0010 + 0",null,1000,"","Señal")
        assertEquals(emptyList<String>(),PhotoStampText.lines(data,PhotoStampConfig(fields=emptySet())))
        assertEquals(listOf("Ruta: R"),PhotoStampText.lines(data,PhotoStampConfig(fields=setOf(PhotoStampField.ROUTE,PhotoStampField.ACCURACY))))
    }
}
