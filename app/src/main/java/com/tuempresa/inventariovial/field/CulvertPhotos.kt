package com.tuempresa.inventariovial.field

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.camera.createPhotoFile
import com.tuempresa.inventariovial.catalog.EngineeringConditions
import com.tuempresa.inventariovial.scap.ui.ScapImage
import com.tuempresa.inventariovial.scap.ui.ScapChoice
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

@Composable
fun CulvertPhotos(paths:List<String>,categories:Map<String,String>,onChange:(List<String>,Map<String,String>)->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var category by rememberSaveable {mutableStateOf("CULVERT_PANORAMIC")}
    var cameraPath by rememberSaveable {mutableStateOf<String?>(null)}
    var remove by remember {mutableStateOf<String?>(null)}
    var error by remember {mutableStateOf<String?>(null)}
    var importing by remember {mutableStateOf(false)}
    fun attach(path:String) {onChange(paths+path,categories+(path to category))}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {ok->
        cameraPath?.let {if(ok)attach(it) else File(it).delete()};cameraPath=null
    }
    val take:()->Unit={runCatching {val photo=createPhotoFile(context,"ALCANTARILLA");cameraPath=photo.file.absolutePath;camera.launch(photo.uri)}.onFailure {error=it.message}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {if(it)take() else error="Permiso de cámara denegado."}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->if(uri!=null) {
        importing=true
        scope.launch {try {
            val path=withContext(Dispatchers.IO) {
                val file=createPhotoFile(context,"ALCANTARILLA_IMPORT").file
                try {
                    context.contentResolver.openInputStream(uri).use {input->requireNotNull(input);file.outputStream().use {input.copyTo(it)}}
                    val options=android.graphics.BitmapFactory.Options().apply {inJustDecodeBounds=true}
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath,options)
                    require(options.outWidth>0) {"Imagen no compatible"};file.absolutePath
                } catch(e:Exception){file.delete();throw e}
            };attach(path)
        } catch(e:Exception){error=e.message} finally {importing=false}}
    }}
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        EngineeringConditions.culvertPhotos.forEach {(key,title)->Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                val photos=paths.filter {categories[it]==key}
                Text(title,style=MaterialTheme.typography.titleLarge)
                Text("${photos.size} fotografías · ${if(photos.isEmpty()) "○ Pendiente" else "✓ Capturadas"}")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled=!importing && cameraPath==null,onClick={category=key;if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)take() else permission.launch(Manifest.permission.CAMERA)}) {Text("Cámara")}
                    OutlinedButton(enabled=!importing && cameraPath==null,onClick={category=key;picker.launch("image/*")}) {Text("Seleccionar")}
                }
                photos.forEach {path->ScapImage(path);TextButton(onClick={remove=path}){Text("Eliminar fotografía")}}
            }
        }}
        val historical=paths.filter {categories[it] !in EngineeringConditions.culvertPhotos.keys}
        if(historical.isNotEmpty()) Text("Fotos históricas sin categoría: ${historical.size}. Clasifica cada una; no se deduce del nombre.")
        historical.forEach {path->ScapImage(path);ScapChoice("Categoría","",EngineeringConditions.culvertPhotos.values.toList()) {label->
            EngineeringConditions.culvertPhotos.entries.find {it.value==label}?.let {onChange(paths,categories+(path to it.key))}
        }}
        if(importing) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }
    remove?.let {path->AlertDialog(onDismissRequest={remove=null},title={Text("Eliminar fotografía de la ficha")},
        text={Text("Podrás tomar o seleccionar otra fotografía en el mismo apartado.")},
        confirmButton={TextButton(onClick={onChange(paths-path,categories-path);remove=null}){Text("Eliminar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
