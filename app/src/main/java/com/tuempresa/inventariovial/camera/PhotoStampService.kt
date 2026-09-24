package com.tuempresa.inventariovial.camera

import android.graphics.*
import android.media.ExifInterface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.tuempresa.inventariovial.location.GeoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

enum class PhotoStampField(val label: String) {
    ROUTE("Ruta"), ROADBED("Calzada"), CHAINAGE("Progresiva"), LATITUDE("Latitud"), LONGITUDE("Longitud"),
    ACCURACY("Precisión GNSS"), DATE_TIME("Fecha/hora"), OPERATOR("Usuario"), ASSET("Elemento")
}
data class PhotoStampConfig(
    val fields: Set<PhotoStampField> = setOf(PhotoStampField.ROUTE,PhotoStampField.CHAINAGE,PhotoStampField.DATE_TIME),
    val maxEdgePx: Int = 2560, val marginFraction: Float = .025f, val fontFraction: Float = .022f,
    val backgroundAlpha: Int = 160, val jpegQuality: Int = 92
)
data class PhotoStampData(val route: String, val roadbed: String, val chainage: String,
    val location: GeoLocation?, val timestamp: Long, val operator: String, val asset: String)

object PhotoStampText {
    fun lines(data: PhotoStampData,config: PhotoStampConfig): List<String> = config.fields.mapNotNull { field ->
        when(field) {
            PhotoStampField.ROUTE -> "Ruta: ${data.route}"
            PhotoStampField.ROADBED -> "Calzada: ${data.roadbed}"
            PhotoStampField.CHAINAGE -> "Progresiva: ${data.chainage}"
            PhotoStampField.LATITUDE -> data.location?.let { "Latitud: ${String.format(Locale.US,"%.8f",it.latitude)}" }
            PhotoStampField.LONGITUDE -> data.location?.let { "Longitud: ${String.format(Locale.US,"%.8f",it.longitude)}" }
            PhotoStampField.ACCURACY -> data.location?.let { "Precisión GNSS: ${if(it.horizontalAccuracy.isFinite()) String.format(Locale.US,"± %.2f m",it.horizontalAccuracy) else "desconocida"}" }
            PhotoStampField.DATE_TIME -> SimpleDateFormat("dd/MM/yyyy HH:mm:ss XXX",Locale.US).format(Date(data.timestamp))
            PhotoStampField.OPERATOR -> "Usuario: ${data.operator}"
            PhotoStampField.ASSET -> "Elemento: ${data.asset}"
        }
    }
}

class PhotoStampService {
    /** Returns a new file. The input is opened read-only and never replaced, renamed or deleted. */
    suspend fun process(original: File, outputDirectory: File, data: PhotoStampData,
        config: PhotoStampConfig = PhotoStampConfig()): File = withContext(Dispatchers.IO) {
        require(original.isFile) { "No se encontró la fotografía original." }
        require(config.maxEdgePx in 640..8192 && config.fontFraction in .005f..0.1f && config.marginFraction in .005f..0.1f)
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeFile(original.absolutePath,bounds)
        require(bounds.outWidth>0 && bounds.outHeight>0) { "La fotografía no se puede decodificar." }
        var sample=1
        while(max(bounds.outWidth,bounds.outHeight)/sample>config.maxEdgePx*2) sample*=2
        val decoded=BitmapFactory.decodeFile(original.absolutePath,BitmapFactory.Options().apply { inSampleSize=sample })
            ?: error("No se pudo leer la imagen.")
        val exif=ExifInterface(original.absolutePath)
        val oriented=orient(decoded,exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL))
        if(oriented!==decoded) decoded.recycle()
        val ratio=min(1f,config.maxEdgePx.toFloat()/max(oriented.width,oriented.height))
        val scaled=if(ratio<1) Bitmap.createScaledBitmap(oriented,max(1,(oriented.width*ratio).toInt()),max(1,(oriented.height*ratio).toInt()),true) else oriented
        if(scaled!==oriented) oriented.recycle()
        val bitmap=scaled.copy(Bitmap.Config.ARGB_8888,true)
        scaled.recycle()
        requireNotNull(bitmap)
        var output: File? = null
        try {
            val lines=PhotoStampText.lines(data,config)
            if(lines.isNotEmpty()) drawStamp(bitmap,lines.joinToString("\n"),config)
            require(outputDirectory.exists() || outputDirectory.mkdirs()) { "No se pudo crear la carpeta de copias." }
            output=File(outputDirectory,"${UUID.randomUUID()}.jpg")
            require(output.canonicalPath!=original.canonicalPath)
            output.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.JPEG,config.jpegQuality.coerceIn(1,100),it)) }
            val copyExif=ExifInterface(output.absolutePath)
            copyExif.setAttribute(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL.toString())
            copyExif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,SimpleDateFormat("yyyy:MM:dd HH:mm:ss",Locale.US).format(Date(data.timestamp)))
            data.location?.let { location ->
                if(com.tuempresa.inventariovial.road.GeoMath.validCoordinate(location.latitude,location.longitude)) {
                    copyExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exifCoordinate(location.latitude))
                    copyExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if(location.latitude<0) "S" else "N")
                    copyExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exifCoordinate(location.longitude))
                    copyExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if(location.longitude<0) "W" else "E")
                }
            }
            copyExif.saveAttributes()
            output
        } catch(error: Throwable) {
            output?.delete() // Only this invocation's newly generated copy.
            throw error
        } finally { bitmap.recycle() }
    }

    private fun exifCoordinate(value: Double): String {
        val absolute=kotlin.math.abs(value); val degrees=absolute.toInt()
        val minutes=((absolute-degrees)*60).toInt()
        val seconds=((absolute-degrees)*3600-minutes*60)*1_000_000
        return "$degrees/1,$minutes/1,${kotlin.math.round(seconds).toLong()}/1000000"
    }

    private fun drawStamp(bitmap: Bitmap,text: String,config: PhotoStampConfig) {
        val edge=min(bitmap.width,bitmap.height).toFloat()
        val margin=max(2f,edge*config.marginFraction)
        val padding=max(2f,edge*.012f)
        val availableWidth=max(1,(bitmap.width-2*margin-2*padding).toInt())
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE;typeface=Typeface.create(Typeface.SANS_SERIF,Typeface.NORMAL);textSize=max(8f,edge*config.fontFraction) }
        fun layout(width: Int)=StaticLayout.Builder.obtain(text,0,text.length,paint,width).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
        var width=min(availableWidth,max(1,text.lines().maxOf { paint.measureText(it).toInt()+1 }))
        var layout=layout(width)
        while(layout.height>bitmap.height-2*margin-2*padding && paint.textSize>1f) {
            paint.textSize*=.85f
            width=min(availableWidth,max(1,text.lines().maxOf { paint.measureText(it).toInt()+1 }))
            layout=layout(width)
        }
        require(layout.height<=bitmap.height-2*margin-2*padding) { "El texto del sello es demasiado extenso para esta fotografía." }
        val left=bitmap.width-margin-width-2*padding
        val top=bitmap.height-margin-layout.height-2*padding
        val canvas=Canvas(bitmap)
        canvas.drawRect(left,top,bitmap.width-margin,bitmap.height-margin,Paint().apply { color=Color.argb(config.backgroundAlpha.coerceIn(0,255),0,0,0) })
        canvas.save();canvas.translate(left+padding,top+padding);layout.draw(canvas);canvas.restore()
    }

    internal fun orient(bitmap: Bitmap,orientation: Int): Bitmap {
        val matrix=Matrix()
        when(orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f,1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f,-1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f);matrix.postScale(-1f,1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f);matrix.postScale(-1f,1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
    }
}
