package com.tuempresa.inventariovial.field

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.*
import com.tuempresa.inventariovial.camera.*
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.tracking.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale

@Composable
fun RoadSuggestionPanel(viewModel: InventoryViewModel,location: GeoLocation?,
    onAccept: (RoadMatchResult)->Unit,onManual: ()->Unit,onSide: ((String,String)->Unit)? = null) {
    val reference by viewModel.field.reference.collectAsState()
    val settings by viewModel.field.settings.collectAsState()
    val match by produceState<RoadMatchResult?>(null,location,reference,settings) {
        value=withContext(Dispatchers.Default) { location?.takeIf { System.currentTimeMillis()-it.timestamp in 0..30_000 }?.let {
            LinearReferenceEngine(reference,settings.matchConfig()).locate(it.latitude,it.longitude,it.horizontalAccuracy.toDouble())
        } }
    }
    var dismissed by remember(location,reference) { mutableStateOf(false) }
    if(dismissed) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            val suggestion=match
            if(suggestion==null) {
                Text(if(reference.segments.isEmpty()) "Sin cartografía disponible: selecciona ruta, calzada y PR manualmente."
                    else "Sin coincidencia GNSS confiable: revisa precisión y selecciona la ubicación vial manualmente.")
            } else {
                Text("Ruta sugerida: ${suggestion.routeCode}")
                Text("Calzada sugerida: ${suggestion.roadbedCode.ifEmpty { "No diferenciada" }}")
                Text("Progresiva sugerida sobre el eje: ${String.format(Locale.US,"%.2f m",suggestion.chainageM)}")
                Text("Distancia al eje: ${String.format(Locale.US,"%.1f m",suggestion.distanceToRoadAxisM)}")
                Row {
                    TextButton(enabled=suggestion.chainageM in 0.0..<9_999_999.99 && suggestion.roadbedCode.isNotBlank(),onClick={onAccept(suggestion)}) {Text("ACEPTAR")}
                    TextButton(onClick={onManual();dismissed=true}) {Text("EDITAR")}
                }
                if(onSide!=null && suggestion.suggestedSide!=null) {
                    Text("Lado sugerido: ${if(suggestion.suggestedSide=="D") "DERECHO" else "IZQUIERDO"}")
                    TextButton(onClick={onSide(suggestion.suggestedSide,"GNSS_MAP_MATCH")}) {Text("CONFIRMAR LADO")}
                    Row { listOf("D" to "DERECHO","I" to "IZQUIERDO","S" to "SIN OBJETO").forEach { (code,label) ->
                        TextButton(onClick={onSide(code,"MANUAL")}) {Text(label)}
                    } }
                }
            }
        }
    }
}

@Composable
fun TrackCapturePanel(viewModel: InventoryViewModel,recordId: String?,sicCode: String,asset: String,route: String,
    roadbed: String,pr: String,distance: String,side: String?,location: GeoLocation?,
    onRecordId: (String)->Unit,onEndLocation: (GeoLocation)->Unit,segment: String="",direction: String="INCREASING") {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val active by TrackCaptureService.activeRecordId.collectAsState()
    val message by TrackCaptureService.message.collectAsState()
    val reference by viewModel.field.reference.collectAsState()
    val settings by viewModel.field.settings.collectAsState()
    val points by remember(recordId) {
        if(recordId==null) kotlinx.coroutines.flow.flowOf(emptyList()) else viewModel.field.dao.observeTrack(recordId)
    }.collectAsState(initial=emptyList())
    val lengths by produceState(TrackLengths(0.0,null,0.0),points,reference,settings) {
        value=withContext(Dispatchers.Default) { TrackCalculator.lengths(points,reference,settings.matchConfig()) }
    }
    var error by remember { mutableStateOf<String?>(null) };var starting by remember { mutableStateOf(false) }
    fun start() {
        if(location==null) { error="Obtén primero una posición GNSS.";return }
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            error="El recorrido requiere permiso de ubicación precisa.";return
        }
        starting=true
        viewModel.field.startTrack(recordId,sicCode,asset,route,roadbed,pr,distance,side,location,
            {starting=false;onRecordId(it)},{starting=false;error=it},segment,direction)
    }
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { start() }
    SectionTitle("Recorrido del elemento")
    if(active==recordId && recordId!=null) {
        Button(onClick={
            viewModel.field.stopTrack()
            scope.launch {
                withTimeoutOrNull(5_000) {TrackCaptureService.activeRecordId.first {it==null}}
                viewModel.field.dao.track(recordId).lastOrNull()?.let {
                    onEndLocation(GeoLocation(it.latitude,it.longitude,it.accuracy.toFloat(),it.altitude,timestamp=it.timestamp))
                }
            }
        }) {Text("FINALIZAR RECORRIDO")}
    } else Button(enabled=active==null && !starting,onClick={
        val needed=mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        if(Build.VERSION.SDK_INT>=33) needed+=Manifest.permission.POST_NOTIFICATIONS
        permissions.launch(needed.toTypedArray())
    }) {Text(if(starting) "Iniciando…" else "INICIAR RECORRIDO")}
    if(active!=null && active!=recordId) Text("Otro registro tiene un recorrido activo.")
    Text("Puntos guardados: ${points.size}")
    fun meters(value:Double?)=value?.let {String.format(Locale.US,"%.2f m",it)} ?: "Sin coincidencia continua con el eje"
    Text("ESTIMACIÓN · Longitud sobre eje vial: ${meters(lengths.matchedRoadLength)}")
    Text("ESTIMACIÓN · Longitud trayectoria GNSS: ${meters(lengths.rawTrackLength)}")
    Text("ESTIMACIÓN · Distancia directa: ${meters(lengths.startEndStraightDistance)}")
    if(lengths.hasGaps) Text("Recorrido con interrupciones: la longitud GNSS solo suma los tramos observados. No se unen las pausas.")
    Text("Estas estimaciones no reemplazan la medición validada por el ingeniero.")
    message?.let {Text(it)};error?.let {ErrorText(it)}
}

@Composable
fun PhotoStampPanel(paths: List<String>,data: PhotoStampData,stampedPaths: Map<String,String>,onStamped: (Map<String,String>)->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var expanded by remember {mutableStateOf(false)}
    var fields by remember {mutableStateOf(PhotoStampConfig().fields)}
    var index by remember(paths) {mutableStateOf(0)}
    var preview by remember {mutableStateOf<String?>(null)}
    var previewOriginal by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)}
    if(paths.isEmpty()) return
    OutlinedButton(onClick={expanded=!expanded}) {Text("Sello fotográfico · ${stampedPaths.keys.count { it in paths }} copias seleccionadas")}
    if(expanded) {
        Text("Campos opcionales del sello")
        PhotoStampField.entries.forEach { field -> Row {
            Checkbox(field in fields,{checked->fields=if(checked) fields+field else fields-field})
            Text(field.label)
        } }
        ChoiceSelector(paths.indices.map {"Foto ${it+1}"},"Foto ${index+1}",{index=it.substringAfter("Foto ").toInt()-1})
        Button(enabled=!busy,onClick={scope.launch {
            busy=true;error=null
            try {
                val original=paths[index]
                val file=PhotoStampService().process(File(original),File(context.filesDir,"stamped"),data,PhotoStampConfig(fields=fields))
                previewOriginal=original;preview=file.absolutePath
            } catch(e:Exception) {error=e.message} finally {busy=false}
        }}) {Text(if(busy) "Procesando…" else "Vista previa del sello")}
        stampedPaths[paths[index]]?.let {
            Text("Copia con sello seleccionada. El original se conserva.")
            TextButton(onClick={onStamped(stampedPaths-paths[index])}) {Text("Usar solo original")}
        }
        error?.let {ErrorText(it)}
    }
    preview?.let { path ->
        val bitmap by produceState<android.graphics.Bitmap?>(null,path) {
            value=withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path,BitmapFactory.Options().apply {inSampleSize=2}) }
        }
        AlertDialog(onDismissRequest={preview=null},title={Text("Vista previa de copia procesada")},
            text={ Column(Modifier.verticalScroll(rememberScrollState())) {
                bitmap?.let {Image(it.asImageBitmap(),"Fotografía con sello",Modifier.fillMaxWidth().height(320.dp))}
                Text("Confirma esta copia antes de guardar el registro.")
            } },
            confirmButton={TextButton(enabled=bitmap!=null,onClick={previewOriginal?.let {onStamped(stampedPaths+(it to path))};preview=null}) {Text("Usar esta copia")} },
            dismissButton={TextButton(onClick={preview=null}) {Text("Cancelar")}})
    }
}
