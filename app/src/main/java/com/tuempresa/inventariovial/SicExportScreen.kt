package com.tuempresa.inventariovial

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.export.ExportHeading
import com.tuempresa.inventariovial.export.SicExcelWriter
import com.tuempresa.inventariovial.export.SicExportFormat
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel

@Composable
fun SicExportScreen(viewModel: InventoryViewModel, onBack: () -> Unit) {
    val exporter = viewModel.localExport
    val state by exporter.state.collectAsState()
    val history by viewModel.history.collectAsState()
    var formatCode by rememberSaveable { mutableStateOf<String?>(null) }
    var route by rememberSaveable { mutableStateOf<String?>(null) }
    var project by rememberSaveable { mutableStateOf(exporter.savedHeading.project) }
    var road by rememberSaveable { mutableStateOf(exporter.savedHeading.road) }
    var section by rememberSaveable { mutableStateOf(exporter.savedHeading.section) }
    var picking by rememberSaveable { mutableStateOf(value = false) }
    var pickerMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val format = SicExportFormat.entries.firstOrNull { it.code == formatCode }
    val active = history.asSequence().map { it.record }.filter {
        (it.status == "ACTIVE" &&
            SicExportFormat.entries.any { f -> f.code == it.sicCode })
    }.toList()
    val routes = active.map { it.routeCode }.distinct().sorted()
    val count = active.count { (format == null || it.sicCode == format.code) && (route == null || it.routeCode == route) }
    val locked = state.busy || picking
    val xlsxPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SicExcelWriter.XLSX_MIME)) { uri ->
        picking = false
        if (uri != null) { pickerMessage = null; exporter.save(uri) }
        else pickerMessage = "Guardado cancelado. El archivo preparado sigue disponible."
    }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        picking = false
        if (uri != null) { pickerMessage = null; exporter.save(uri) }
        else pickerMessage = "Guardado cancelado. El archivo preparado sigue disponible."
    }
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBack) { Text("Volver") }
        Text("Exportar SIC a Excel", style = MaterialTheme.typography.headlineMedium)
        Text("Inventario vial calificado · SIC-17 a SIC-23")
        Text("Un Excel por SIC, con una hoja por ruta. Solo se incluyen registros activos. La generación funciona sin internet.")
        ExportChoice("Formato", format?.let { "${it.code} · ${it.title}" } ?: "Todos los SIC (ZIP)",
            listOf(null to "Todos los SIC (ZIP)") + SicExportFormat.entries.map { it.code to "${it.code} · ${it.title}" },
            !locked) { formatCode = it }
        ExportChoice("Ruta", route ?: "Todas las rutas", listOf(null to "Todas las rutas") + routes.map { it to it },
            !locked) { route = it }
        Text("Registros activos en la selección: $count")
        OutlinedTextField(project, { project = it }, label = { Text("Proyecto o servicio (opcional)") },
            enabled = !locked, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(road, { road = it }, label = { Text("Carretera (opcional)") },
            enabled = !locked, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(section, { section = it }, label = { Text("Tramo (opcional)") },
            supportingText = { Text("El encabezado se aplica a todas las rutas seleccionadas.") },
            enabled = !locked, modifier = Modifier.fillMaxWidth())
        Button(enabled = !locked && count > 0, onClick = {
            pickerMessage = null
            exporter.prepare(format, route, ExportHeading(project.trim(), road.trim(), section.trim()))
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.busy) "Procesando…" else if (format == null) "Generar ZIP de Excel" else "Generar Excel")
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { Text(it) }
        state.prepared?.let { prepared ->
            HorizontalDivider()
            Text("Archivo preparado: ${prepared.file.name}")
            Text("${prepared.recordCount} registros · ${prepared.workbookCount} Excel. Si cambias la selección o los encabezados, vuelve a generar.")
            Button(enabled = !locked, onClick = {
                try {
                    picking = true
                    if (prepared.isZip) zipPicker.launch(prepared.file.name) else xlsxPicker.launch(prepared.file.name)
                } catch (error: Exception) {
                    picking = false
                    pickerMessage = "No se pudo abrir el selector de archivos: ${error.message}"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar en el dispositivo") }
            Text("En el selector de Android elige Descargas u otra carpeta local.")
        }
        pickerMessage?.let { Text(it) }
    }
}

@Composable
private fun ExportChoice(label: String, selected: String, options: List<Pair<String?, String>>,
                         enabled: Boolean, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(enabled = enabled, onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); expanded = false })
                }
            }
        }
    }
}
