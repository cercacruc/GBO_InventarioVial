package com.tuempresa.inventariovial

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.location.TabletLocationProvider
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import com.tuempresa.inventariovial.road.SequencedRecord
import com.tuempresa.inventariovial.supplementary.SupplementaryFormat
import com.tuempresa.inventariovial.field.SupplementaryScreen
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity

@Composable
fun FinalLocationCapture(location: GeoLocation?, onLocation: (GeoLocation) -> Unit) {
    val context = LocalContext.current
    val provider = remember(context) { TabletLocationProvider(context) }
    var locating by remember { mutableStateOf(value = false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun capture() {
        locating = true
        error = null
        provider.getCurrentLocation(
            onSuccess = { locating = false; onLocation(it) },
            onError = { locating = false; error = it },
        )
    }
    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) capture()
        else error = "Debes permitir el acceso a la ubicación."
    }
    OutlinedButton(enabled = !locating, onClick = {
        if (
            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED)
        ) {
            capture()
        } else {
            permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }) { Text(if (locating) "Obteniendo GPS final…" else "Capturar GPS final") }
    if (location == null) Text("GPS final: pendiente (opcional)")
    else Text("GPS final: ${location.latitude}, ${location.longitude} · ± ${location.accuracyHorizontal} m")
    error?.let { ErrorText(it) }
}

@Composable
fun RecordHistoryScreen(viewModel: InventoryViewModel, onScap: (() -> Unit)? = null, onBack: () -> Unit) {
    val ordered by viewModel.sequencedHistory.collectAsState()
    var showAnnulled by remember { mutableStateOf(false) }
    val records = ordered.filter { it.item.record.sicCode != "SCAP" && (showAnnulled || it.item.record.status == "ACTIVE") }
    var technicalId by remember {mutableStateOf<String?>(null)}
    technicalId?.let {id->com.tuempresa.inventariovial.field.EngineeringEditScreen(id) {technicalId=null};return}
    var complementary by remember { mutableStateOf<Pair<InventoryRecordEntity, SupplementaryFormat>?>(null) }
    complementary?.let { (record,format) ->
        SupplementaryScreen(viewModel,record,format) { complementary=null }
        return
    }
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedButton(onClick = onBack) { Text("Volver") }
            Text("Registros locales", style = MaterialTheme.typography.headlineMedium)
            onScap?.let { open -> OutlinedButton(onClick = open) { Text("Inspecciones SCAP de puentes") } }
            Text("Ordenados por ruta, calzada y progresiva")
            Row { Checkbox(showAnnulled,{showAnnulled=it});Text("Mostrar también anulados") }
            error?.let { ErrorText(it) }
        }
        if (records.isEmpty()) item { Text("Todavía no hay registros guardados.") }
        items(
            items = records,
            key = { item: SequencedRecord -> item.item.record.id }
        ) { row ->
            HistoryRecordCard(row, viewModel, {technicalId=row.item.record.id}, { complementary=row.item.record to it }) { error = it }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    row: SequencedRecord,
    viewModel: InventoryViewModel,
    onTechnical: ()->Unit,
    onSupplementary: (SupplementaryFormat)->Unit,
    onError: (String) -> Unit
) {
    val item = row.item
    val record = item.record
    var editing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var route by remember(record, editing) { mutableStateOf(record.routeCode) }
    var roadbed by remember(record, editing) { mutableStateOf(record.roadbedCode) }
    var startPr by remember(record, editing) { mutableStateOf(record.startPrCode) }
    var startDistance by remember(record, editing) { mutableStateOf(record.startDistanceM.toString()) }
    var endPr by remember(record, editing) { mutableStateOf(record.endPrCode.orEmpty()) }
    var endDistance by remember(record, editing) { mutableStateOf(record.endDistanceM?.toString().orEmpty()) }
    var side by remember(record, editing) { mutableStateOf(record.sideCode.orEmpty()) }
    var observations by remember(record, editing) { mutableStateOf(record.observations.orEmpty()) }
    var editError by remember { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${record.sicCode} · ${record.assetType}", style = MaterialTheme.typography.titleMedium)
            Text("${row.computedSequence?.toString()?.padStart(3,'0') ?: "Sin número operativo"} · ${row.position.chainageM?.let { String.format(java.util.Locale.US,"%.2f m",it) } ?: "Progresiva no determinada"}")
            Text(when(row.position.source) {
                com.tuempresa.inventariovial.road.PositionSource.OFFICIAL_PR -> "Progresiva: catálogo oficial de PR"
                com.tuempresa.inventariovial.road.PositionSource.ESTIMATED_FROM_PR_CODE -> "Progresiva ESTIMADA desde código PR"
                com.tuempresa.inventariovial.road.PositionSource.KILOMETRIC -> "Progresiva kilométrica registrada"
                else -> "Revisar PR en catálogo"
            })
            Text("${record.routeCode} / ${record.roadbedCode} · PR ${record.startPrCode} + ${record.startDistanceM}")
            record.segment?.let { Text("$it · Sentido: ${if(record.surveyDirection=="DECREASING") "Decreciente" else "Creciente"}") }
            item.sic18?.let { detail ->
                if(detail.crossSectionCode=="2") Text("Forma: ${when(detail.sectionShape) { "CIRCULAR" -> "Circular"; "OVAL" -> "Ovalada"; else -> "Circular / ovalada (registro anterior)" }}")
                Text("Dimensión 1: ${detail.dimension1M ?: "Sin dato"} m · Dimensión 2: ${detail.dimension2M?.let { "$it m" } ?: "No aplica / sin dato"}")
            }
            Text("Fotos: ${item.photos.size} · Pendientes: ${item.photos.count { it.syncStatus != "SYNCED" }}")
            DriveUploadPolicy.configurationError()?.let { Text("Drive · SIN CONFIGURAR: $it") }
            item.photos.sortedBy { it.photoIndex }.forEach { photo ->
                Text("Foto ${photo.photoIndex} · ${DriveUploadPolicy.statusLabel(photo.syncStatus)}")
            }
            Text(if (record.status == "ACTIVE") "Estado: activo" else "Estado: anulado")
            if(com.tuempresa.inventariovial.server.ServerConfiguration.enabled) {
                Text(if(com.tuempresa.inventariovial.server.ServerConfiguration.enabled) "Servidor: ${record.serverSyncStatus}" else "Servidor: sin configurar · datos guardados localmente")
                record.serverSyncError?.let { ErrorText(it) }
            }
            Text("GPS inicial: ${record.latitude}, ${record.longitude}")
            if(com.tuempresa.inventariovial.validation.requiresEndLocation(record.sicCode,record.assetType,item.sic20?.classCode,item.sic23?.classCode)) {
                record.endLatitude?.let { Text("GPS final: $it, ${record.endLongitude}") }
            }
            item.sic23?.let { detail ->
                Text("Clase: ${detail.classCode} · Tipo: ${detail.typeCode ?: "Sin objeto"}")
                Text("Ancho: ${detail.widthM?.let { String.format(java.util.Locale.US, "%.2f m", it) } ?: "Sin objeto"}")
                if (detail.description.isNotBlank()) Text("Descripción: ${detail.description}")
                Text("Fin: PR ${record.endPrCode} + ${record.endDistanceM} · Lado: ${record.sideCode}")
            }
            item.sic18?.let {d->
                val state = com.tuempresa.inventariovial.supplementary.Sic18AStatus.label(d,item.sic18a)
                Text("SIC-18A: $state",style=MaterialTheme.typography.titleMedium,
                    color=if(state=="Pendiente") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                if(item.sic18a!=null) com.tuempresa.inventariovial.field.Sic18AExportButton(record,
                    com.tuempresa.inventariovial.supplementary.SupplementaryFormState(SupplementaryFormat.SIC18A,
                        com.tuempresa.inventariovial.supplementary.Sic18AStatus.inherited(d,item.sic18a)))
                Text("Estructural: ${d.structuralConditionCode} · Funcional: ${d.functionalConditionCode}")
                if(com.tuempresa.inventariovial.catalog.EngineeringConditions.badCulvert(d.structuralConditionCode,d.functionalConditionCode)) Text("! Condición mala detectada",color=MaterialTheme.colorScheme.error)
                com.tuempresa.inventariovial.catalog.EngineeringConditions.culvertPhotos.forEach {(key,label)->Text("$label: ${item.photos.count {it.photoCategory==key}}")}
                if(item.photos.any {it.photoCategory==null}) Text("Fotos históricas sin categoría: ${item.photos.count {it.photoCategory==null}}")
            }
            item.sic19?.let {d->
                Text("Estructural: ${d.structuralConditionCode} · Funcional: ${d.functionalConditionCode}")
                Text(com.tuempresa.inventariovial.catalog.EngineeringConditions.structural(d.typeCode,d.structuralCriterion)[d.structuralConditionCode] ?: "! Criterio estructural pendiente")
            }
            item.sic20?.let {d->
                Text("Estructural: ${d.structuralConditionCode} · Funcional: ${d.functionalConditionCode ?: "Pendiente"}")
                if(d.classCode=="14") {Text("Altura promedio del cuerpo: ${d.dimension1M ?: "Pendiente"} m");Text("Longitud del muro (interno): ${d.wallLengthMeters ?: "Pendiente"} m")}
            }
            item.sic18a?.let {d->
                Text("✓ SIC-18A guardado · mismo UUID de SIC-18",style=MaterialTheme.typography.titleMedium)
                com.tuempresa.inventariovial.supplementary.SupplementaryForms.fields(SupplementaryFormat.SIC18A,d.values()).forEach {f->
                    val v=d.values()[f.key].orEmpty();Text("${f.label}: ${f.options.find {it.substringBefore(" - ")==v} ?: v}")
                }
            }
            if(record.status=="ACTIVE" && record.sicCode in listOf("SIC-18","SIC-19","SIC-20")) OutlinedButton(onClick=onTechnical) {Text("Editar ficha técnica y fotografías")}
            if (!editing) {
                if(record.status=="ACTIVE") SupplementaryFormat.entries.filter { it.enabled &&
                    if(record.sicCode=="SIC-17") it!=SupplementaryFormat.SIC18A else record.sicCode=="SIC-18" && it==SupplementaryFormat.SIC18A
                }.forEach { format -> OutlinedButton(onClick={onSupplementary(format)}) {Text(if(format==SupplementaryFormat.SIC18A && item.sic18a!=null) "Editar SIC-18A" else "Completar ${format.title}")} }
                record.observations?.let { Text(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { editing = true; editError = null }) { Text("Editar") }
                    OutlinedButton(onClick = {
                        viewModel.setRecordStatus(record.id, record.status != "ACTIVE", onError)
                    }) { Text(if (record.status == "ACTIVE") "Anular" else "Restaurar") }
                }
            } else {
                OutlinedTextField(value = route, onValueChange = { route = it }, label = { Text("Ruta") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = roadbed, onValueChange = { roadbed = it }, label = { Text("Calzada") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = startPr, onValueChange = { startPr = it }, label = { Text("PR inicio") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = startDistance, onValueChange = { startDistance = it }, label = { Text("Distancia inicio (m)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = endPr, onValueChange = { endPr = it }, label = { Text("PR fin") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = endDistance, onValueChange = { endDistance = it }, label = { Text("Distancia fin (m)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = side, onValueChange = { side = it }, label = { Text("Lado") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = observations, onValueChange = { observations = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth())
                editError?.let { ErrorText(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(enabled = !saving, onClick = {
                        val start = startDistance.trim().replace(',', '.').toDoubleOrNull()
                        val end = endDistance.trim().replace(',', '.').toDoubleOrNull()
                        when {
                            start == null || !start.isFinite() || start < 0 ->
                                editError = "La distancia inicial debe ser un número positivo o cero."
                            endDistance.isNotBlank() && (end == null || !end.isFinite() || end < 0) ->
                                editError = "La distancia final debe ser un número positivo o cero."
                            endPr.isBlank() != endDistance.isBlank() ->
                                editError = "Completa el PR y la distancia final."
                            else -> {
                                saving = true
                                viewModel.updateCoreFields(record.copy(
                                    routeCode = route, roadbedCode = roadbed, startPrCode = startPr,
                                    startDistanceM = start, endPrCode = endPr.ifBlank { null },
                                    endDistanceM = end, sideCode = side.trim().uppercase().ifBlank { null },
                                    observations = observations.trim().ifBlank { null }),
                                    onSuccess = { saving = false; editing = false },
                                    onError = { saving = false; editError = it })
                            }
                        }
                    }) { Text("Guardar cambios") }
                    OutlinedButton(enabled = !saving, onClick = { editing = false }) { Text("Cancelar") }
                }
            }
        }
    }
}
