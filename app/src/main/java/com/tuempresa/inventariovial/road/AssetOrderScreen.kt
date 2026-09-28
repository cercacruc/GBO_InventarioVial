package com.tuempresa.inventariovial.road

import android.content.Intent
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
import com.tuempresa.inventariovial.ChoiceSelector
import com.tuempresa.inventariovial.DrivePathPlanner
import com.tuempresa.inventariovial.field.SurveyPreferences
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun AssetOrderScreen(viewModel: InventoryViewModel, onBack: () -> Unit) {
    val history by viewModel.history.collectAsState()
    val reference by viewModel.field.reference.collectAsState()
    val referenceError by viewModel.field.referenceError.collectAsState()
    val busy by viewModel.axisBusy.collectAsState()
    val feedback by viewModel.axisFeedback.collectAsState()
    var route by remember { mutableStateOf(SurveyPreferences.initialRoutes.first()) }
    val rows = remember(history, reference, route) { RoadOrdering.orderAssetsForRoute(history, ChainageCalculator(reference.prs), route) }
    val plan = remember(history, reference, route) { DrivePathPlanner.plan(history.filter { normalizedRoadCode(it.record.routeCode) == route }, ChainageCalculator(reference.prs)) }
    val folders = remember(plan) { plan.folders.associateBy { it.asset.recordId } }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var file by remember(route) { mutableStateOf<File?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.google-earth.kmz")) { uri ->
        val prepared = file
        if (uri != null && prepared != null) scope.launch {
            message = try {
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { output ->
                    prepared.inputStream().use { it.copyTo(output) }
                } ?: error("No se pudo abrir el destino.") }
                "KMZ guardado."
            } catch (e: Exception) { e.message }
        }
    }
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedButton(onClick = onBack) { Text("Volver") }
            Text("Orden de elementos", style = MaterialTheme.typography.headlineMedium)
            ChoiceSelector(SurveyPreferences.initialRoutes, route) { if (!exporting) route = it }
            Text("Orden por posición sobre eje. La numeración es independiente por ruta, SIB y familia.")
            Text("Sin matching: al final, por PR calculable; luego fecha de captura y UUID. El PR ingresado se conserva.")
            OutlinedButton(enabled = !busy, onClick = { viewModel.recalculateOrder(route) }) {
                Text(if (busy) "Recalculando…" else "Recalcular orden")
            }
            feedback?.let { Text(it) }
            referenceError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(enabled = !busy && !exporting, onClick = { scope.launch {
                exporting = true
                file = null
                try { file = viewModel.field.kml.prepare(ChainageCalculator(reference.prs), route, kmz = true); message = "KMZ preparado con eje y leyenda." }
                catch (e: Exception) { message = e.message }
                finally { exporting = false }
            } }) { Text(if (exporting) "Generando KMZ…" else "Exportar KMZ de la ruta") }
            file?.let { prepared ->
                Row {
                    TextButton(onClick = { try { context.startActivity(viewModel.field.kml.intent(prepared, true)) }
                        catch (_: Exception) { message = "No hay aplicación compatible; guarda o comparte el KMZ." } }) { Text("Abrir") }
                    TextButton(onClick = { try { context.startActivity(Intent.createChooser(viewModel.field.kml.intent(prepared, false), "Compartir KMZ")) }
                        catch (e: Exception) { message = e.message } }) { Text("Compartir") }
                    TextButton(onClick = { save.launch("$route.kmz") }) { Text("Guardar") }
                }
            }
            message?.let { Text(it) }
            if (rows.isEmpty()) Text("No hay elementos activos en esta ruta.")
        }
        items(rows, key = { it.recordId }) { asset ->
            val r = asset.item.record
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${asset.shortCode} · ${asset.assetFamily.label} · ${asset.sibCode ?: "SIB no configurado"}", style = MaterialTheme.typography.titleMedium)
                    Text(asset.axisMeasureM?.let { String.format(Locale.US, "Posición sobre eje: %.3f km", it / 1000) } ?: "Posición sobre eje no determinada")
                    Text(r.distanceToRoadAxisM?.let { String.format(Locale.US, "Distancia al eje: %.1f m", it) } ?: "Distancia al eje: no determinada")
                    Text("PR ingresado: ${r.startPrCode} + ${r.startDistanceM} m")
                    Text("GPS: ${r.latitude}, ${r.longitude} · ± ${r.gpsAccuracyM ?: "?"} m")
                    Text(when (asset.orderSource) {
                        AssetOrderSource.GNSS_MAP_MATCH -> "Estado: posición GNSS sobre eje"
                        AssetOrderSource.MANUAL_CHAINAGE_FALLBACK -> "Estado: respaldo por PR (${asset.fallbackPosition.source})"
                        AssetOrderSource.CREATED_AT_FALLBACK -> "Estado: respaldo por fecha y UUID"
                    })
                    Text("UUID: ${asset.recordId}")
                    asset.warnings.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                    folders[asset.recordId]?.let { folder ->
                        Text("Carpeta prevista: ${folder.plannedPath ?: "Sin plan"}")
                        folder.photos.forEach { Text(it.plannedPath) }
                    }
                }
            }
        }
    }
}
