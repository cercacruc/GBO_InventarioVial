package com.tuempresa.inventariovial.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

import androidx.core.content.FileProvider

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class CapturedPhoto(
    val file: File,
    val uri: Uri
)


fun createPhotoFile(
    context: Context,
    prefix: String
): CapturedPhoto {

    val directory = File(
        context.filesDir,
        "images"
    )

    if (!directory.exists()) {
        directory.mkdirs()
    }

    val timestamp =
        SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(Date())

    val safePrefix =
        prefix
            .uppercase()
            .replace("Ñ", "N")
            .replace("Á", "A")
            .replace("É", "E")
            .replace("Í", "I")
            .replace("Ó", "O")
            .replace("Ú", "U")
            .replace(" ", "_")

    val file =
        File.createTempFile(
            "${safePrefix}_${timestamp}_",
            ".jpg",
            directory
        )

    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

    return CapturedPhoto(
        file = file,
        uri = uri
    )
}


fun loadCorrectlyOrientedBitmap(
    path: String
): Bitmap? {

    val bitmap =
        BitmapFactory.decodeFile(path)
            ?: return null

    return try {

        val exif =
            ExifInterface(path)

        val orientation =
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

        val matrix =
            Matrix()

        when (orientation) {

            ExifInterface.ORIENTATION_ROTATE_90 ->
                matrix.postRotate(90f)

            ExifInterface.ORIENTATION_ROTATE_180 ->
                matrix.postRotate(180f)

            ExifInterface.ORIENTATION_ROTATE_270 ->
                matrix.postRotate(270f)

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
                matrix.postScale(-1f, 1f)

            ExifInterface.ORIENTATION_FLIP_VERTICAL ->
                matrix.postScale(1f, -1f)
        }

        if (
            orientation ==
            ExifInterface.ORIENTATION_NORMAL
        ) {

            bitmap

        } else {

            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }

    } catch (_: Exception) {

        bitmap
    }
}