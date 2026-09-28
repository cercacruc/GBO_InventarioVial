package com.tuempresa.inventariovial.gis

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.tuempresa.inventariovial.road.AssetPresentationCatalog
import java.io.ByteArrayOutputStream

/** Small local PNG resources; all category colors come from the presentation catalog. */
object KmlMapImages {
    fun resources(): Map<String, ByteArray> = mapOf("legend.png" to legend(), "marker.png" to marker())
    private fun png(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use {
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        bitmap.recycle()
        it.toByteArray()
    }
    private fun legend(): ByteArray {
        val bitmap = Bitmap.createBitmap(720, 390, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.argb(235, 255, 255, 255))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 28f; color = Color.BLACK; isFakeBoldText = true }
        canvas.drawText("LEYENDA · INVENTARIO VIAL", 20f, 38f, paint)
        paint.isFakeBoldText = false
        KmlPresentation.legend.forEachIndexed { i, entry ->
            val y = 82f + i * 46f
            paint.color = Color.BLACK or entry.rgb
            canvas.drawCircle(30f, y - 8f, 12f, paint)
            paint.color = Color.BLACK
            canvas.drawText(entry.text, 56f, y, paint)
        }
        return png(bitmap)
    }
    private fun marker(): ByteArray {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY }
        canvas.drawCircle(16f, 16f, 15f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(16f, 16f, 12f, paint)
        return png(bitmap)
    }
}
