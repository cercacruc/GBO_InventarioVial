package com.tuempresa.inventariovial.scap.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tuempresa.inventariovial.camera.createPhotoFile
import com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal val photoLabels=com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.labels
internal val sketchLabels=linkedMapOf("ELEVATION" to "Elevación", "PLAN" to "Planta", "CROSS_SECTION" to "Sección transversal")

@Composable
fun ScapMedia(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean,sketch:Boolean,initialElementCode:String?=null) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var category by rememberSaveable(sketch){mutableStateOf(if(sketch) "ELEVATION" else "GENERAL")}
    var code by rememberSaveable(initialElementCode) {mutableStateOf(initialElementCode.orEmpty())}
    var additional by rememberSaveable {mutableStateOf(false)}
    var cameraPath by rememberSaveable {mutableStateOf<String?>(null)}
    var captureCategory by rememberSaveable {mutableStateOf("")}
    var captureCode by rememberSaveable {mutableStateOf("")}
    var importing by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf<String?>(null)}
    var removing by remember {mutableStateOf<String?>(null)}
    val labels=if(sketch) sketchLabels else photoLabels
    fun attach(path:String,selectedCategory:String,selectedCode:String) {
        c.submit {if(sketch)it.addSketch(s.inspection.id,selectedCategory,path) else {
            val marked=com.tuempresa.inventariovial.camera.RequiredWatermark.prepare(context,path)
            it.addPhoto(s.inspection.id,path,selectedCategory,selectedCode.ifBlank{null},marked.absolutePath)
        }}
    }
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ok->
        val path=cameraPath
        if(ok && path!=null) attach(path,captureCategory,captureCode)
        else if(path!=null) File(path).delete()
        cameraPath=null
    }
    val takePhoto:()->Unit={
        runCatching {
            val photo=createPhotoFile(context,"SCAP")
            cameraPath=photo.file.absolutePath
            camera.launch(photo.uri)
        }.onFailure{message="No se pudo abrir la cámara: ${it.message}"}
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)takePhoto() else message="Permiso de cámara no concedido. Puedes importar una imagen."}
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null) {
            val selectedCategory=captureCategory;val selectedCode=captureCode
            importing=true
            scope.launch {
                try {
                    val path=withContext(Dispatchers.IO) {
                        val directory=File(context.filesDir,"images").apply{mkdirs()}
                        val mime=context.contentResolver.getType(uri)
                        val suffix=when(mime){"image/png"->".png";"image/webp"->".webp";else->".jpg"}
                        val file=File.createTempFile("SCAP_IMPORT_",suffix,directory)
                        try {
                            context.contentResolver.openInputStream(uri).use{input->requireNotNull(input){"No se pudo leer la imagen."};file.outputStream().use{input.copyTo(it)}}
                            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.absolutePath,bounds)
                            require(bounds.outWidth>0 && bounds.outHeight>0){"El archivo no es una imagen compatible."}
                            file.absolutePath
                        } catch(e:Exception){file.delete();throw e}
                    }
                    attach(path,selectedCategory,selectedCode)
                } catch(e:Exception){message="No se pudo importar: ${e.message}"} finally {importing=false}
            }
        }
    }
    val elementOptions=catalog.elements.map {"${it.code} · ${it.name}"}
    @Composable fun captureButtons(cat:String,element:String="") {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled=editable && !importing,onClick={
                captureCategory=cat;captureCode=element
                if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) takePhoto() else permission.launch(Manifest.permission.CAMERA)
            }) {Text("Tomar foto")}
            OutlinedButton(enabled=editable && !importing,onClick={captureCategory=cat;captureCode=element;importer.launch(arrayOf("image/*"))}) {Text("Importar")}
        }
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(sketch) item {ScapSketchDimensions(s,c,editable)}
        item {
            Text(if(sketch) "Croquis del puente" else "Panel fotográfico",style=MaterialTheme.typography.titleLarge)
            if(!sketch) Text("Estas tomas son recordatorios; puedes registrar varias fotos por categoría.")
            if(importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        }
        if(sketch) {
            sketchLabels.forEach {(cat,label)->
                item {ScapElementCard(label) {captureButtons(cat)}}
                items(s.sketches.filter {it.type==cat},key={it.id}) {item->ScapElementCard(label) {
                    ScapImage(item.localUri)
                    TextButton(enabled=editable,onClick={removing=item.id}) {Text("Retirar croquis")}
                }}
            }
        } else {
            val categories=com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.primary.keys.toList() +
                s.photos.map {it.photoCategory.orEmpty()}.distinct().filter {it !in com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.primary}
            categories.forEach {cat->
                val photos=s.photos.filter {it.photoCategory.orEmpty()==cat}.sortedBy {it.photoIndex}
                item(key="category:$cat") {ScapElementCard("${com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.label(cat)} · ${photos.size} fotos") {
                    if(cat=="DEFECT") ScapChoice("Elemento SCAP (opcional)",catalog.element(code)?.let {"${it.code} · ${it.name}"}.orEmpty(),elementOptions,editable) {code=it.substringBefore(" · ")}
                    captureButtons(cat.ifBlank {"OTHER"},if(cat=="DEFECT") code else "")
                }}
                items(photos,key={it.id}) {photo->
                    ScapPhotoCard(s,c,catalog,photo,editable) {removing=photo.id}
                }
            }
            item {
                OutlinedButton(enabled=editable,onClick={additional=!additional}) {Text("+ AÑADIR FOTO / CATEGORÍA ADICIONAL")}
                if(additional) {
                    ScapChoice("Categoría adicional",photoLabels[category].orEmpty(),photoLabels.values.toList(),editable) {v->category=(photoLabels.entries.firstOrNull {it.value==v}?.key ?: "OTHER")}
                    captureButtons(category)
                }
            }
        }
        item {Text(if(sketch) "Croquis guardados: ${s.sketches.size}" else "Fotografías guardadas: ${s.photos.size}")}
    }
    removing?.let{id->AlertDialog(onDismissRequest={removing=null},title={Text(if(sketch) "Retirar croquis" else "Eliminar fotografía")},text={Text("Se retirará esta imagen de la ficha SCAP.")},
        confirmButton={TextButton(onClick={c.submit{if(sketch) it.removeSketch(s.inspection.id,id) else it.removePhoto(s.inspection.id,id)};removing=null}){Text("Retirar")}},dismissButton={TextButton(onClick={removing=null}){Text("Cancelar")}})}
}

@Composable
private fun ScapPhotoCard(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,photo:com.tuempresa.inventariovial.data.entity.PhotoEntity,editable:Boolean,onRemove:()->Unit) {
    val context=LocalContext.current
    ScapElementCard("Foto ${photo.photoIndex} · ${com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.label(photo.photoCategory)}") {
        Text("Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy",java.util.Locale.getDefault()).format(java.util.Date(photo.createdAt))}")
        var description by remember(photo.id) {mutableStateOf(photo.description.orEmpty())}
        OutlinedTextField(description,{description=it;c.submit {repo->repo.describePhoto(s.inspection.id,photo.id,it)}},label={Text("Descripción")},enabled=editable,modifier=Modifier.fillMaxWidth())
        ScapImage(com.tuempresa.inventariovial.DriveUploadPolicy.uploadPath(photo))
        ScapChoice("Categoría",com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.label(photo.photoCategory),photoLabels.values.toList(),editable) {v->
            c.submit {it.setPhotoMetadata(s.inspection.id,photo.id,(photoLabels.entries.firstOrNull {it.value==v}?.key ?: "OTHER"),photo.scapElementCode)}
        }
        ScapChoice("Elemento SCAP",catalog.element(photo.scapElementCode.orEmpty())?.let {"${it.code} · ${it.name}"}.orEmpty(),catalog.elements.map {"${it.code} · ${it.name}"},editable) {v->
            c.submit {it.setPhotoMetadata(s.inspection.id,photo.id,photo.photoCategory ?: "OTHER",v.substringBefore(" · ").ifBlank {null})}
        }
        if(photo.photoCategory=="DEFECT") {
            s.defects.filter {it.photoId==photo.id}.forEach {d->key(d.id) {ScapDefectFields(s,c,catalog,d,editable)}}
            OutlinedButton(enabled=editable,onClick={c.submit {it.saveDefect(s.inspection.id,com.tuempresa.inventariovial.scap.data.ScapDefectEntity(
                java.util.UUID.randomUUID().toString(),s.inspection.id,photo.scapElementCode,description,"",photo.id,false,true,System.currentTimeMillis()))}}) {Text("+ Asociar observación / ubicación")}
        }
        if(editable) {
            val original=photo.originalPath ?: photo.localPath
            val v=s.values()
            val location=s.inspection.latitude?.let {lat->s.inspection.longitude?.let {lon->com.tuempresa.inventariovial.location.GeoLocation(lat,lon,s.inspection.gpsAccuracyM?.toFloat() ?: Float.NaN)}}
            com.tuempresa.inventariovial.field.PhotoStampPanel(listOf(original),com.tuempresa.inventariovial.camera.PhotoStampData(
                v["route"].orEmpty(),v["sic17.roadbedCode"].orEmpty(),v["progressive"].orEmpty(),location,photo.createdAt,s.inspection.createdBy,s.inspection.bridgeName),
                photo.stampedPath?.let {mapOf(original to it)} ?: emptyMap()) {paths->c.submit {
                    val marked=com.tuempresa.inventariovial.camera.RequiredWatermark.prepare(context,original,paths[original])
                    it.stampPhoto(s.inspection.id,photo.id,marked.absolutePath)
                }}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(enabled=editable && photo!=s.photos.minByOrNull {it.photoIndex},onClick={c.submit {it.movePhoto(s.inspection.id,photo.id,-1)}}) {Text("↑ Subir")}
            TextButton(enabled=editable && photo!=s.photos.maxByOrNull {it.photoIndex},onClick={c.submit {it.movePhoto(s.inspection.id,photo.id,1)}}) {Text("↓ Bajar")}
            TextButton(enabled=editable,onClick=onRemove) {Text("Eliminar")}
        }
    }
}

@Composable
internal fun ScapImage(path:String) {
    val context=LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null,path) {value=withContext(Dispatchers.IO){runCatching{loadCorrectlyOrientedBitmap(path)}.getOrNull()}}
    var error by remember{mutableStateOf<String?>(null)}
    bitmap?.let{Image(it.asImageBitmap(),contentDescription="Imagen de la inspección SCAP",modifier=Modifier.fillMaxWidth().height(220.dp),contentScale=ContentScale.Fit)} ?: Text("Vista previa no disponible")
    TextButton(onClick={runCatching {
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",File(path))
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }.onFailure{error="No hay un visor de imágenes disponible."}}){Text("Abrir imagen")}
    error?.let{Text(it)}
}
