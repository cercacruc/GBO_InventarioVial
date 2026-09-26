package com.tuempresa.inventariovial.camera

import android.content.Context
import android.media.ExifInterface
import com.tuempresa.inventariovial.DriveUploadPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** All delivery paths must pass through this policy; never fall back to unmarked bytes. */
object RequiredWatermark {
    internal const val MARKER = "GBO_CORPORATE_LOGO_V1"

    suspend fun prepare(context: Context, original: String, stamped: String? = null,
        directory: File = File(context.filesDir, "stamped")): File = withContext(Dispatchers.IO) {
        val selected = stamped?.takeIf { DriveUploadPolicy.readableImage(it) } ?: original
        val source = File(selected)
        val marked = runCatching {
            ExifInterface(source.path).getAttribute(ExifInterface.TAG_USER_COMMENT) == MARKER
        }.getOrDefault(false)
        if (marked && DriveUploadPolicy.readableImage(selected)) return@withContext source
        // Preserve optional text on a selected legacy copy. The original is never modified.
        val exif = runCatching { ExifInterface(source.path) }.getOrNull()
        val output = PhotoStampService(context).process(source, directory,
            PhotoStampData("", "", "", null, source.lastModified(), "", ""),
            PhotoStampConfig(fields = emptySet()))
        try {
            // Logo-only processing must preserve available capture metadata.
            val result = ExifInterface(output.path)
            listOf(ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF).forEach { tag ->
                result.setAttribute(tag, exif?.getAttribute(tag))
            }
            result.saveAttributes()
            output
        } catch (error: Throwable) {
            output.delete()
            throw error
        }
    }
}
