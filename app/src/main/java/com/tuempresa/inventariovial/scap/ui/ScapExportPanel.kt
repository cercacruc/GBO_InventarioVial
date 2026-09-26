package com.tuempresa.inventariovial.scap.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.export.SicExcelWriter
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun ScapExportPanel(s:ScapInspectionSnapshot,ready:Boolean,controller:ScapController) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val exporter=remember{ScapExcelExporter({context.assets.open("scap_template.xlsx")},context.assets.open("scap_field_map.json").bufferedReader().use{it.readText()})}
    val review=remember(s){exporter.review(s)}
    val sic=remember(s){ScapSicExporter.previews(s)}
    var busy by remember{mutableStateOf(false)}
    var prepared by rememberSaveable{mutableStateOf<String?>(null)}
    var message by rememberSaveable{mutableStateOf<String?>(null)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SicExcelWriter.XLSX_MIME)){uri->
        val path=prepared;prepared=null
        if(uri==null) {path?.let{File(it).delete()};message="Exportación cancelada; la ficha sigue guardada localmente."}
        else if(path!=null) scope.launch {
            busy=true
            try {
                withContext(Dispatchers.IO){
                    context.contentResolver.openOutputStream(uri,"wt").use{out->
                        requireNotNull(out){"No se pudo abrir el destino."}
                        File(path).inputStream().use{it.copyTo(out)}
                    }
                }
                message="Excel guardado. Ábrelo en Excel para recalcular sus fórmulas y revisar los campos pendientes."
            } catch(e:CancellationException){throw e}
            catch(e:Exception){message="No se pudo guardar el Excel: ${e.message}"}
            finally{File(path).delete();busy=false}
        }
    }
    fun export(format:String) {
        scope.launch {
            busy=true;message="Preparando $format…"
            var file:File?=null
            val temporaryPhotos=mutableListOf<File>()
            try {
                val timestamp=SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())
                // Local document suggestion only. Does not touch final photo names or Drive routing.
                val target=File(context.cacheDir,"${format}_${timestamp}_${UUID.randomUUID().toString().take(8)}.xlsx")
                file=target
                val saved=controller.savedSnapshot(s.inspection.id)
                val delivery = if(format=="SCAP") saved.copy(photos=saved.photos.map { photo ->
                    val directory=File(context.cacheDir,"watermark-export")
                    val marked=com.tuempresa.inventariovial.camera.RequiredWatermark.prepare(
                        context,photo.originalPath ?: photo.localPath,photo.stampedPath,directory)
                    if(marked.parentFile?.canonicalPath==directory.canonicalPath) temporaryPhotos.add(marked)
                    // If the prepared copy disappears, fail instead of exporting the original.
                    photo.copy(localPath=marked.absolutePath,stampedPath=marked.absolutePath)
                }) else saved
                withContext(Dispatchers.IO){
                    target.outputStream().use{out->if(format=="SCAP") exporter.write(out,delivery){readExportImage(context,it)} else ScapSicExporter.write(out,delivery,format)}
                }
                prepared=target.absolutePath;message="Elige dónde guardar $format.";launcher.launch(target.name)
            } catch(e:CancellationException){file?.delete();throw e}
            catch(e:Exception){file?.delete();message="No se pudo preparar $format: ${e.message}"}
            finally{temporaryPhotos.forEach {it.delete()};busy=false}
        }
    }
    HorizontalDivider()
    Text("Exportación Excel",style=MaterialTheme.typography.titleLarge)
    Text(DriveUploadPolicy.SCAP_LOCAL_MESSAGE)
    if(!ready) Text("Espera a que termine el guardado local y resuelve cualquier error antes de exportar.")
    review.errors.forEach{Text(it,color=MaterialTheme.colorScheme.error)}
    review.warnings.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall)}
    Button(enabled=ready && !busy && prepared==null && review.errors.isEmpty(),onClick={export("SCAP")}){Text("Guardar Excel SCAP")}
    sic.forEach{p->
        Card(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(p.format,style=MaterialTheme.typography.titleMedium)
                if(p.missingRequired.isNotEmpty()) Text("Exportación bloqueada. Faltan: ${p.missingRequired.joinToString()}.",color=MaterialTheme.colorScheme.error)
                if(p.missingOptional.isNotEmpty()) Text("Se exportan vacíos: ${p.missingOptional.joinToString()}.")
                p.pending.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall)}
                Text("Revisa estos faltantes. Esta exportación no modifica ni completa automáticamente la ficha.",style=MaterialTheme.typography.bodySmall)
                OutlinedButton(enabled=ready && !busy && prepared==null && p.missingRequired.isEmpty(),onClick={export(p.format)}){Text("Guardar ${p.format}")}
            }
        }
    }
    message?.let{Text(it)}
}

/** Bounded image size in the workbook; original files and their names remain untouched. */
private fun readExportImage(context:Context,path:String):ByteArray {
    fun stream()=if(path.startsWith("content:")) requireNotNull(context.contentResolver.openInputStream(Uri.parse(path))) else File(path.removePrefix("file://")).inputStream()
    val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
    stream().use{BitmapFactory.decodeStream(it,null,bounds)}
    require(bounds.outWidth>0 && bounds.outHeight>0){"Una imagen local no puede leerse."}
    var sample=1
    while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1800) sample*=2
    val bitmap=stream().use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})} ?: error("Imagen ilegible.")
    return try {ByteArrayOutputStream().use{out->check(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));out.toByteArray()}} finally{bitmap.recycle()}
}
