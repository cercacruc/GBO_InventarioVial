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
import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.location.TabletLocationProvider
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel

@Composable
fun FinalLocationCapture(location: GeoLocation?, onLocation: (GeoLocation) -> Unit) {
    val context = LocalContext.current
    val provider = remember(context) { TabletLocationProvider(context) }
    var locating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun capture() {
        locating = true
        error = null
        provider.getCurrentLocation(
            onSuccess = { locating = false; onLocation(it) },
            onError = { locating = false; error = it })
    }
    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) capture()
        else error = "Debes permitir el acceso a la ubicación."
    }
    OutlinedButton(enabled = !locating, onClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) capture()
        else permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }) { Text(if (locating) "Obteniendo GPS final…" else "Capturar GPS final") }
    if (location == null) Text("GPS final: pendiente (opcional)")
    else Text("GPS final: ${location.latitude}, ${location.longitude} · ± ${location.accuracyHorizontal} m")
    error?.let { ErrorText(it) }
}

@Composable
fun RecordHistoryScreen(viewModel: InventoryViewModel, onBack: () -> Unit) {
    val records by viewModel.history.collectAsState()
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
            Text("Ordenados por ruta, calzada y progresiva")
            error?.let { ErrorText(it) }
        }
        if (records.isEmpty()) item { Text("Todavía no hay registros guardados.") }
        items(
            items = records,
            key = { item: InventoryRecordWithPhotos -> item.record.id }
        ) { item ->
            HistoryRecordCard(item, viewModel) { error = it }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    item: InventoryRecordWithPhotos,
    viewModel: InventoryViewModel,
    onError: (String) -> Unit
) {
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
            Text("${record.routeCode} / ${record.roadbedCode} · PR ${record.startPrCode} + ${record.startDistanceM}")
            Text("Fotos: ${item.photos.size} · Pendientes: ${item.photos.count { it.syncStatus != "SYNCED" }}")
            Text(if (record.status == "ACTIVE") "Estado: activo" else "Estado: anulado")
            Text("GPS inicial: ${record.latitude}, ${record.longitude}")
            record.endLatitude?.let { Text("GPS final: $it, ${record.endLongitude}") }
            item.sic23?.let { detail ->
                Text("Clase: ${detail.classCode} · Tipo: ${detail.typeCode ?: "Sin objeto"}")
                Text("Ancho: ${detail.widthM?.let { String.format(java.util.Locale.US, "%.2f m", it) } ?: "Sin objeto"}")
                if (detail.description.isNotBlank()) Text("Descripción: ${detail.description}")
                Text("Fin: PR ${record.endPrCode} + ${record.endDistanceM} · Lado: ${record.sideCode}")
            }
            if (!editing) {
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

