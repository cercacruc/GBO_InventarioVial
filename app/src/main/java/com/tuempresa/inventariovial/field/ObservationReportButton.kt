package com.tuempresa.inventariovial.field

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.export.ObservationReport
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ObservationReportButton(viewModel: InventoryViewModel) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember {mutableStateOf(false)}
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        if(uri!=null) scope.launch {
            busy=true
            try {
                withContext(Dispatchers.IO) {
                    val rows=viewModel.field.dao.recordsForExport(null,null).map {it.record}
                    val html=ObservationReport.render(rows)
                    context.contentResolver.openOutputStream(uri,"wt")?.bufferedWriter(Charsets.UTF_8)?.use {it.write(html)}
                        ?: error("No se pudo abrir el destino.")
                }
                message="Informe de observaciones guardado. Puedes abrirlo en un navegador e imprimirlo como PDF."
            } catch(e: Exception) {message=e.message ?: "No se pudo exportar el informe."}
            finally {busy=false}
        }
    }
    OutlinedButton(enabled=!busy,onClick={save.launch("Informe_observaciones.html")}) {Text("Exportar observaciones para informe")}
    message?.let { Text(it) }
}
