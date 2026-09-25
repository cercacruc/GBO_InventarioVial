package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel

@Composable
fun DriveStatusPanel(vm:InventoryViewModel) {
    val history by vm.history.collectAsState()
    val feedback by vm.driveFeedback.collectAsState()
    val configError=DriveUploadPolicy.configurationError()
    val photos=history.filter{it.record.status=="ACTIVE" && it.record.sicCode!="SCAP"}.flatMap{it.photos}.filter{it.scapInspectionId==null}
    val counts=photos.groupingBy{DriveUploadPolicy.statusLabel(it.syncStatus)}.eachCount()
    Card(Modifier.fillMaxWidth().padding(top=8.dp)) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("Drive · ${if(configError==null) "Configuración disponible" else "SIN CONFIGURAR"}",style=MaterialTheme.typography.titleMedium)
            configError?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Text(listOf("PENDIENTE","PROGRAMADA","SUBIENDO","SINCRONIZADA","ERROR").joinToString(" · "){"$it: ${counts[it] ?: 0}"})
            if(feedback!=null && feedback!=configError)Text(feedback.orEmpty())
            Text("Estos estados corresponden a fotos SIC. SCAP permanece local.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
