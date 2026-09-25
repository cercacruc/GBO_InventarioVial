package com.tuempresa.inventariovial.field

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.supplementary.SupplementaryFormState
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Sic18AExportButton(record:InventoryRecordEntity,state:SupplementaryFormState) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var message by remember {mutableStateOf<String?>(null)};var busy by remember {mutableStateOf(false)}
    var pending by remember {mutableStateOf<SupplementaryFormState?>(null)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SicExcelWriter.XLSX_MIME)) {uri->
        val captured=pending
        if(uri!=null && captured!=null) {busy=true;scope.launch {try {
            withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri).use {out->Sic18AExporter.write(requireNotNull(out),record,captured)}}
            message="✓ SIC-18A exportado"
        } catch(e:Exception){message=e.message} finally {busy=false}}}
    }
    OutlinedButton(enabled=!busy,onClick={
        val errors=state.validate()
        if(errors.isNotEmpty()) message=errors.joinToString("\n") else {pending=state;export.launch("SIC-18A_${record.routeCode}_${record.id.take(8)}.xlsx")}
    }) {Text("Exportar SIC-18A a XLSX")}
    message?.let {Text(it)}
}
