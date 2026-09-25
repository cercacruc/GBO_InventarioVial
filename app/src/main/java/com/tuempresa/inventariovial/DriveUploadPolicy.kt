package com.tuempresa.inventariovial

import android.graphics.BitmapFactory
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import java.io.File

object DriveUploadPolicy {
    const val SCAP_LOCAL_MESSAGE = "SCAP guardado localmente. Sincronización de puente pendiente de configuración del servidor."
    private val sicFormats=setOf("SIC-17","SIC-18","SIC-19","SIC-20","SIC-21","SIC-22","SIC-23")
    fun configurationError(token:String=DriveConfig.API_TOKEN,url:String=DriveConfig.WEB_APP_URL):String? = when {
        token.isBlank()->"Drive no configurado: falta DRIVE_API_TOKEN en drive.local.properties."
        url.isBlank()->"Drive no configurado: falta DRIVE_WEB_APP_URL en drive.local.properties."
        !Regex("https://script\\.google\\.com/macros/s/[A-Za-z0-9_-]+/exec").matches(url)->
            "Drive no configurado: DRIVE_WEB_APP_URL debe ser una URL de implementación https://script.google.com/macros/s/.../exec; no una URL de editor."
        else->null
    }
    fun eligible(sicCode:String,status:String,scapInspectionId:String?)=
        sicCode in sicFormats && status=="ACTIVE" && scapInspectionId==null
    fun requireConfiguration() {configurationError()?.let{throw IllegalStateException(it)}}
    fun requireSic(sicCode:String) {require(sicCode in sicFormats){SCAP_LOCAL_MESSAGE}}
    fun readableImage(path:String):Boolean = runCatching {
        val f=File(path)
        if(!f.isFile || !f.canRead() || f.length()==0L) false else {
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeFile(path,bounds);bounds.outWidth>0 && bounds.outHeight>0
        }
    }.getOrDefault(false)
    fun uploadPath(photo:PhotoEntity):String=photo.stampedPath?.takeIf(::readableImage) ?: photo.localPath
    fun statusLabel(status:String):String=when(status) {
        "PENDING"->"PENDIENTE";"QUEUED","ENQUEUED"->"PROGRAMADA";"UPLOADING"->"SUBIENDO"
        "SYNCED"->"SINCRONIZADA";"ERROR","FAILED"->"ERROR";else->"PENDIENTE"
    }
}
