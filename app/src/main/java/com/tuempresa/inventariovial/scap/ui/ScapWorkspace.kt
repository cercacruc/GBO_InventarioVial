package com.tuempresa.inventariovial.scap.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.access.DeviceAccessManager
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.location.TabletLocationProvider
import com.tuempresa.inventariovial.scap.calculator.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val scapSections=linkedMapOf("A" to "Identificación", "B" to "Datos generales", "C1" to "Resumen del puente",
    "C2" to "Tramos", "C3" to "Tablero", "C4" to "Subestructura", "C5" to "Detalles", "C6" to "Accesos",
    "C7" to "Seguridad vial", "C8" to "Cargas", "C9" to "Cruces alternativos", "C10" to "Estado de la vía",
    "D1" to "Suelos", "D2" to "Niveles de agua", "D3" to "Hidráulica", "D4" to "Perfil longitudinal",
    "E" to "Croquis", "F1" to "Elementos y condición", "F2" to "Fotografías", "F3" to "Defectos", "G" to "Revisión y condición global")

@Composable
fun ScapWorkspace(vm:InventoryViewModel,onBack:()->Unit) {
    val c=vm.scap
    val catalog by c.catalog.collectAsState()
    val selected by c.selected.collectAsState()
    val snapshot by c.snapshot.collectAsState()
    val inspections by c.inspections.collectAsState()
    val session by vm.field.session.collectAsState()
    val pending by c.pending.collectAsState()
    val error by c.error.collectAsState()
    val saved by c.saveState.collectAsState()
    val context=LocalContext.current
    val goBack:()->Unit={if(selected!=null)c.select(null) else onBack()}
    BackHandler(onBack=goBack)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick=goBack){Text(if(selected==null) "← Inicio" else "← Inspecciones")}
            Text("SCAP · Puentes",modifier=Modifier.padding(12.dp),style=MaterialTheme.typography.titleMedium)
        }
        if(error!=null) Card(Modifier.fillMaxWidth().padding(horizontal=12.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)) {
            Text(error.orEmpty(),Modifier.padding(12.dp))
            TextButton(onClick=c::retry,enabled=pending==0){Text("Reintentar guardado")}
        }
        val cat=catalog
        if(cat==null) {Text("Cargando catálogo SCAP…",Modifier.padding(16.dp));return@Column}
        if(selected==null) {
            Button(enabled=pending==0,onClick={c.submit {repo->
                val id=repo.create(session?.operator.orEmpty(),session?.device ?: DeviceAccessManager(context).fingerprint(),session?.sessionId)
                c.select(id)
            }},modifier=Modifier.padding(horizontal=16.dp)){Text("Nueva inspección SCAP")}
            Text("Las inspecciones se guardan en este dispositivo. Abre una ficha para continuarla o revisar su cierre.",Modifier.padding(16.dp))
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                if(inspections.isEmpty()) item {Text("Aún no hay inspecciones SCAP.")}
                items(inspections,key={it.id}) {i->Card(onClick={c.select(i.id)},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(i.bridgeName.ifBlank {"Puente sin nombre"},style=MaterialTheme.typography.titleMedium)
                        Text("${i.bridgeCode.ifBlank {"Código pendiente"}} · ${statusLabel(i.status)}")
                        Text("Último cambio: ${SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(i.updatedAt))}")
                    }
                }}
            }
        } else {
            val s=snapshot?.takeIf {it.inspection.id==selected}
            if(s==null) Text("Abriendo inspección…",Modifier.padding(16.dp))
            else ScapEditor(s,c,cat,vm,saved,pending,error)
        }
    }
}

internal fun statusLabel(status:String)=when(status){"COMPLETE"->"Completa";"IN_PROGRESS"->"En progreso";else->"Borrador"}

@Composable
private fun ScapEditor(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,vm:InventoryViewModel,saved:String,pending:Int,error:String?) {
    var section by rememberSaveable(s.inspection.id){mutableStateOf("A")}
    var confirm by remember {mutableStateOf(false)}
    val values=scapValues(s,c,"inspection")
    val editable=s.inspection.status!="COMPLETE"
    val review=remember(s,catalog){ScapValidation.review(s,catalog)}
    Column(Modifier.fillMaxSize()) {
        Surface(tonalElevation=3.dp) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(values["bridgeName"].orEmpty().ifBlank {"Puente sin nombre"},style=MaterialTheme.typography.titleLarge)
                Text("Ruta: ${values["route"].orEmpty().ifBlank {"Pendiente"}} · Progresiva: ${values["progressive"].orEmpty().ifBlank {"Pendiente"}}")
                Text("$section · ${scapSections[section]} · ${statusLabel(s.inspection.status)}")
                Text(saved,style=MaterialTheme.typography.labelMedium)
            }
        }
        LazyRow(contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("A","B","C","D","E","F","G")) {group->FilterChip(section.startsWith(group),{
                section=scapSections.keys.first{it.startsWith(group)}
            },label={Text(group)})}
        }
        val subsections=scapSections.keys.filter{it.startsWith(section.take(1))}
        if(subsections.size>1) LazyRow(contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(subsections) {key->FilterChip(section==key,{section=key},label={Text("$key · ${scapSections[key]}")})}
        }
        if(!editable) Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Ficha cerrada",Modifier.weight(1f).padding(top=12.dp))
            OutlinedButton(enabled=pending==0,onClick={c.submit {it.reopen(s.inspection.id)}}){Text("Reabrir para editar")}
        }
        if(!editable) Text(com.tuempresa.inventariovial.DriveUploadPolicy.SCAP_LOCAL_MESSAGE,Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.bodySmall)
        Box(Modifier.weight(1f)) {
            when(section) {
                "A"->ScapIdentification(s,c,catalog,vm,editable)
                "E"->ScapMedia(s,c,catalog,editable,true)
                "F1"->ScapElements(s,c,catalog,editable)
                "F2"->ScapMedia(s,c,catalog,editable,false)
                "F3"->ScapDefects(s,c,catalog,editable)
                "G"->ScapReviewPanel(s,catalog,review,pending==0 && error==null,c)
                else->ScapTechnicalSection(s,c,catalog,section,editable)
            }
        }
        if(editable) Button(enabled=pending==0 && error==null,onClick={section="G";confirm=true},modifier=Modifier.fillMaxWidth().padding(12.dp)) {Text("Revisar y completar inspección")}
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Confirmar cierre SCAP")},
        text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
            Text("${s.inspection.bridgeName} · ${s.inspection.bridgeCode}")
            Text("${s.spans.size} tramos · ${review.present} elementos · ${review.unevaluated} sin evaluar")
            Text("${s.defects.size} defectos · ${s.photos.size} fotografías · ${s.sketches.size} croquis")
            if(review.errors.isNotEmpty()) {Text("Corrige antes de cerrar:",color=MaterialTheme.colorScheme.error);review.errors.forEach{Text("• $it")}}
            Text("Datos pendientes: ${review.pending.size}")
            review.pending.forEach{Text("• $it")}
            Text("Al confirmar reconoces los datos pendientes y cierras la captura local. Puedes reabrirla para corregirla.")
        }},confirmButton={TextButton(enabled=review.errors.isEmpty() && pending==0 && error==null,onClick={c.submit {it.complete(s.inspection.id)};confirm=false}){Text("Confirmar cierre")}},
        dismissButton={TextButton(onClick={confirm=false}){Text("Volver a la ficha")}})
}

@Composable
private fun ScapIdentification(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,vm:InventoryViewModel,editable:Boolean) {
    val context=LocalContext.current
    val provider=remember {TabletLocationProvider(context)}
    var fix by remember {mutableStateOf<GeoLocation?>(null)}
    var locationMessage by remember {mutableStateOf<String?>(null)}
    var locating by remember {mutableStateOf(false)}
    val requestFix:()->Unit={locating=true;locationMessage=null;provider.getCurrentLocation({fix=it;locating=false},{locationMessage=it;locating=false})}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)requestFix() else locationMessage="Permiso de ubicación no concedido. Puedes seguir completando la ficha."}
    val values=scapValues(s,c,"inspection")
    val session by vm.field.session.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(values["createdBy"] ?: s.inspection.createdBy,{c.edit(s.inspection.id,"inspection","createdBy",it)},label={Text("Inspector")},enabled=editable,modifier=Modifier.fillMaxWidth())
        if(session?.road!=null) OutlinedButton(enabled=editable,onClick={
            session?.road?.let{c.edit(s.inspection.id,"inspection","route",it,"SESSION_ACCEPTED")}
        }) {Text("Usar ruta de la sesión: ${session?.road}")}
        Text("GPS inicial",style=MaterialTheme.typography.titleMedium)
        OutlinedButton(enabled=editable && !locating,onClick={
            if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) requestFix()
            else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }){Text(if(locating) "Obteniendo ubicación…" else "Obtener ubicación GNSS")}
        locationMessage?.let{Text(it)}
        fix?.let {f->
            Text("Propuesta GNSS: ${f.latitude}, ${f.longitude} · precisión ${f.horizontalAccuracy} m")
            Button(enabled=editable,onClick={c.submit{it.setLocation(s.inspection.id,f)};fix=null}){Text("Aceptar ubicación GNSS")}
        }
        s.inspection.latitude?.let{Text("GNSS guardado: $it, ${s.inspection.longitude} · precisión ${s.inspection.gpsAccuracyM} m")}
        Text("Las coordenadas UTM de la ficha se ingresan manualmente; la captura GNSS conserva latitud y longitud.")
        ScapFields(s,c,catalog,"A",editable=editable)
    }
}

@Composable
private fun ScapReviewPanel(s:ScapInspectionSnapshot,catalog:ScapCatalog,review:ScapReview,ready:Boolean,controller:ScapController) {
    val v=s.values()
    val result=remember(s){ScapConditionCalculator.calculate(s.elements.filter{it.element.isPresent}.map {e->
        ScapCalculationInput(e.element.elementCode,e.element.importanceFactor,e.condition?.let{ScapPercentages.values(it)} ?: List(6){null})
    })}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Revisión de inspección",style=MaterialTheme.typography.titleLarge)
        Text("${s.inspection.bridgeName} · ${s.inspection.bridgeCode} · ${v["route"].orEmpty()} · ${v["progressive"].orEmpty()}")
        Text("${s.spans.size} tramos · ${s.substructures.count{it.kind=="PIER"}} pilares · ${s.supports.size} apoyos")
        Text("${review.present} elementos presentes · ${review.unevaluated} sin evaluar · ${s.defects.size} defectos · ${s.photos.size} fotos · ${s.sketches.size} croquis")
        Text("Datos generales",style=MaterialTheme.typography.titleMedium)
        catalog.fields("B","inspection").forEach {Text("${it.label}: ${v[it.key].orEmpty().ifBlank{"Pendiente"}}")}
        s.spans.sortedBy{it.spanIndex}.forEach {Text("Tramo ${it.spanIndex}: ${it.category} / ${it.type} · ${it.lengthM ?: "—"} m")}
        Text("Elementos seleccionados",style=MaterialTheme.typography.titleMedium)
        s.elements.filter{it.element.isPresent}.forEach{e->Text("${e.element.elementCode} · ${e.element.description} · ${e.element.quantity ?: "Metrado pendiente"} ${e.element.unit}")}
        Text("Defectos registrados",style=MaterialTheme.typography.titleMedium)
        s.defects.forEach{Text("${it.elementCode ?: "General"} · ${it.description} · ${it.locationDescription}")}
        HorizontalDivider()
        Text("Condición global SCAP",style=MaterialTheme.typography.titleMedium)
        if(result.value!=null) Text("${String.format(Locale.US,"%.3f",result.value)} · ${result.classification}",style=MaterialTheme.typography.headlineMedium)
        else Text("Sin resultado: ${result.reason}")
        Text("Se aplica la fórmula de la hoja G y la clasificación M12 del archivo de referencia. El resultado requiere revisión del ingeniero.")
        result.elements.forEach {Text("${it.code} · condición ${String.format(Locale.US,"%.3f",it.condition)} · contribución ${String.format(Locale.US,"%.3f",it.contribution)}")}
        Text("Errores que impiden cerrar (${review.errors.size})",style=MaterialTheme.typography.titleMedium)
        review.errors.forEach{Text("• $it",color=MaterialTheme.colorScheme.error)}
        Text("Pendientes para revisión (${review.pending.size})",style=MaterialTheme.typography.titleMedium)
        review.pending.forEach{Text("• $it")}
        ScapExportPanel(s,ready,controller)
    }
}
