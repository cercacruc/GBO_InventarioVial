package com.tuempresa.inventariovial.field

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.*
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.supplementary.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun deviceAccessGate(viewModel: InventoryViewModel): Boolean {
    val state by viewModel.field.access.collectAsState()
    if(state?.authorized==true) return true
    Column(Modifier.fillMaxSize().padding(32.dp),verticalArrangement=Arrangement.Center) {
        Text(if(state==null) "Verificando dispositivo…" else "DISPOSITIVO NO ACTIVADO",style=MaterialTheme.typography.headlineMedium)
        state?.let {
            Text("Código dispositivo: ${it.displayCode}")
            androidx.compose.foundation.text.selection.SelectionContainer { Text(it.fingerprint) }
            Text(it.message)
            OutlinedButton(onClick=viewModel.field::checkAccess) { Text("Reintentar activación") }
        }
    }
    return false
}

@Composable
fun SaveWarningsDialog(viewModel: InventoryViewModel) {
    val pending by viewModel.pendingSave.collectAsState()
    pending?.let { save ->
        AlertDialog(onDismissRequest=viewModel::dismissSaveWarnings,
            title={ Text("Verificar registro antes de guardar") },
            text={ Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                captureSummary(save.request).forEach { (label,value) -> Text("$label: $value") }; save.warnings.forEach { Text("• ${it.message}") }
                Text("Puedes corregir los datos o guardar después de revisar estos avisos.")
            } },
            confirmButton={ TextButton(onClick=viewModel::confirmSaveWarnings) { Text("Confirmar y guardar") } },
            dismissButton={ TextButton(onClick=viewModel::dismissSaveWarnings) { Text("Revisar") } })
    }
}

@Composable
fun FieldHomePanel(viewModel: InventoryViewModel,onResumeDraft: (InventoryRecordEntity)->Unit) {
    val field=viewModel.field
    val session by field.session.collectAsState()
    val allDrafts by field.drafts.collectAsState()
    val drafts = allDrafts.filter { it.sicCode != "SCAP" }
    val reference by field.reference.collectAsState()
    val referenceError by field.referenceError.collectAsState()
    var showSession by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Sesión de campo",style=MaterialTheme.typography.titleLarge)
            session?.let {
                Text("${it.project} · ${it.operator}")
                Text("Ruta: ${it.road ?: "Por elemento"} · Calzada: ${it.roadbed ?: "Por elemento"}")
                Text("Sentido: ${if(it.direction=="DECREASING") "Decreciente" else "Creciente"}")
                OutlinedButton(onClick={ field.endSession { message=it } }) { Text("Finalizar sesión") }
            } ?: OutlinedButton(onClick={ showSession=true }) { Text("Iniciar sesión de campo") }
            Text("Ejes: ${reference.segments.size} · PR oficiales: ${reference.prs.size}")
            if(reference.segments.isEmpty()) Text("Sin cartografía: ingresa la ubicación vial manualmente.")
            referenceError?.let { ErrorText(it) }
            OutlinedButton(onClick={ showSettings=true }) { Text("Ajustar umbrales GNSS") }
            KmlControls(viewModel)
            ObservationReportButton(viewModel)
            if(drafts.isNotEmpty()) {
                Text("Recorridos por completar",style=MaterialTheme.typography.titleMedium)
                drafts.forEach { draft ->
                    OutlinedButton(onClick={ onResumeDraft(draft) }) {
                        Text("Continuar ${draft.sicCode} · ${draft.routeCode} · ${draft.id.take(8)}")
                    }
                }
                Text("Los puntos están guardados. Completa los datos y las fotografías del registro.")
            }
            message?.let { Text(it) }
        }
    }
    if(showSession) SessionDialog(field,{showSession=false})
    if(showSettings) SettingsDialog(field,{showSettings=false})
}

@Composable
private fun SessionDialog(field: FieldController,onClose: ()->Unit) {
    var project by remember { mutableStateOf("") }; var operator by remember { mutableStateOf("") }
    var road by remember { mutableStateOf("") };var segment by remember { mutableStateOf("") };var roadbed by remember { mutableStateOf("") }
    var decreasing by remember { mutableStateOf(false) };var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=onClose,title={Text("Nueva sesión")},text={ Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(project,{project=it},label={Text("Proyecto")})
        OutlinedTextField(operator,{operator=it},label={Text("Operador")})
        OutlinedTextField(road,{road=it},label={Text("Ruta (opcional)")})
        Text("Tramo")
        ChoiceSelector(SurveyPreferences.segments,segment,{segment=it})
        Text("Calzada")
        ChoiceSelector(listOf("UC","UD","CD","A1","A2"),roadbed,{roadbed=it})
        Row { Checkbox(decreasing,{decreasing=it});Text("Recorrido decreciente") }
        error?.let { ErrorText(it) }
    } },confirmButton={TextButton(onClick={
        if(project.isBlank() || operator.isBlank()) error="Completa proyecto y operador." else {
            field.startSession(project,operator,road,segment,roadbed,if(decreasing) "DECREASING" else "INCREASING",onClose) { error=it }
        }
    }) {Text("Iniciar")} },dismissButton={TextButton(onClick=onClose){Text("Cancelar")}})
}

@Composable
private fun SettingsDialog(field: FieldController,onClose: ()->Unit) {
    val settings by field.settings.collectAsState()
    var axis by remember { mutableStateOf(settings.axisThresholdM.toString()) }
    var accuracy by remember { mutableStateOf(settings.trackAccuracyM.toString()) }
    var distance by remember { mutableStateOf(settings.minTrackDistanceM.toString()) }
    var quality by remember { mutableStateOf(settings.qualityAccuracyM.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=onClose,title={Text("Umbrales GNSS (metros)")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(axis,{axis=it},label={Text("Distancia máxima al eje")})
        OutlinedTextField(accuracy,{accuracy=it},label={Text("Precisión máxima para recorrido")})
        OutlinedTextField(distance,{distance=it},label={Text("Separación mínima de puntos")})
        OutlinedTextField(quality,{quality=it},label={Text("Umbral de aviso de precisión")})
        Text("Los umbrales no certifican la precisión del receptor.")
        error?.let { ErrorText(it) }
    }},confirmButton={TextButton(onClick={
        fun number(text:String)=text.replace(',','.').toDoubleOrNull() ?: Double.NaN
        val value=FieldSettings(number(axis),number(accuracy),number(distance),number(quality))
        if(value.validate()) { field.saveSettings(value);onClose() } else error="Ingresa valores mayores que cero y hasta 1000 m."
    }) {Text("Guardar")} },dismissButton={TextButton(onClick=onClose){Text("Cancelar")}})
}

@Composable
private fun KmlControls(viewModel: InventoryViewModel) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var file by remember { mutableStateOf<File?>(null) };var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.google-earth.kml+xml")) { uri ->
        if(uri!=null) scope.launch {
            message = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        file!!.inputStream().use { it.copyTo(output) }
                    } ?: error("No se pudo abrir el destino.")
                }
                "KML guardado."
            } catch(error:Exception) {
                error.message
            }
        }
    }
    OutlinedButton(enabled=!busy,onClick={scope.launch {
        busy=true
        try { file=viewModel.field.kml.prepare(ChainageCalculator(viewModel.field.reference.value.prs));message="KML preparado con los registros activos." }
        catch(error:Exception) {message=error.message} finally {busy=false}
    }}) {Text(if(busy) "Generando KML…" else "EXPORTAR KML")}
    file?.let { prepared ->
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={ try { context.startActivity(viewModel.field.kml.intent(prepared,true)) } catch(_:Exception) {message="No hay una aplicación compatible. Puedes compartir o guardar el KML."} }) {Text("Abrir")}
            TextButton(onClick={ try { context.startActivity(Intent.createChooser(viewModel.field.kml.intent(prepared,false),"Compartir KML")) } catch(error:Exception) {message=error.message} }) {Text("Compartir")}
            TextButton(onClick={save.launch("inventario.kml")}) {Text("Guardar")}
        }
    }
    message?.let {Text(it)}
}

@Composable
fun SupplementaryScreen(viewModel: InventoryViewModel,record: InventoryRecordEntity,format: SupplementaryFormat,onBack: ()->Unit) {
    var state by remember(record.id,format) { mutableStateOf(SupplementaryFormState(format)) }
    var error by remember { mutableStateOf<String?>(null) };var loaded by remember { mutableStateOf(false) };var saving by remember { mutableStateOf(false) }
    BackHandler(onBack=onBack)
    LaunchedEffect(record.id,format) { try { state=viewModel.supplementaryState(record.id,format);loaded=true } catch(e:Exception) {error=e.message} }
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick=onBack) {Text("Volver")}
        Text(format.title,style=MaterialTheme.typography.headlineMedium)
        Text("${record.routeCode} / ${record.roadbedCode} · PR ${record.startPrCode} + ${record.startDistanceM}")
        Text("Fecha: ${record.surveyDate} · UUID: ${record.id}")
        if(!format.enabled) Text("Formato deshabilitado por configuración.") else if(loaded) {
            Text("Ficha complementaria; los campos pendientes se pueden completar después.")
            SupplementaryForms.fields(format,state.values).forEach { field ->
                val value=state.values[field.key].orEmpty()
                if(field.kind in setOf(FieldKind.CODE,FieldKind.DESCRIBED_CODE)) {
                    Text(field.label)
                    ChoiceSelector(listOf("Sin completar")+field.options,
                        field.options.find { if(field.kind==FieldKind.CODE) it.substringBefore(" - ")==value else it==value } ?: "Sin completar",
                        { option ->
                            val selected=if(option=="Sin completar") "" else if(field.kind==FieldKind.CODE) option.substringBefore(" - ") else option
                            var values=state.values+(field.key to selected)
                            if(field.key=="classCode") values = values + ("typeCode" to "")
                            state=state.copy(values=values)
                        })
                } else OutlinedTextField(value,{state=state.copy(values=state.values+(field.key to it))},label={Text(field.label)},
                    readOnly=field.key=="bridgeCode",modifier=Modifier.fillMaxWidth())
            }
            Button(enabled=!saving,onClick={saving=true;viewModel.saveSupplementary(record.id,state,
                {saving=false;onBack()},{saving=false;error=it})}) {Text("Guardar ${format.title}")}
        }
        error?.let {ErrorText(it)}
    }
}
