package com.tuempresa.inventariovial.scap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.ScapSicExporter

@Composable
fun ScapSicSupplementPanel(s: ScapInspectionSnapshot, c: ScapController, editable: Boolean) {
    val values = scapValues(s,c,"inspection")
    val preview = ScapSicExporter.preview(ScapToSicMapper.mapToSic17(s))
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("DATOS COMPLEMENTARIOS PARA SIC-17",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
        Text("No forman parte de la ficha SCAP")
        ScapToSicMapper.mapToSic17(s).fields.filter { !it.scapField.startsWith(ScapSicSupplement.PREFIX) }.forEach {
            Text("${when(it.sicField) { "routeCode" -> "Ruta"; "bridgeCode" -> "Código del puente"; "surveyDate" -> "Fecha"; "spans" -> "Vanos"; "startPrCode" -> "PR"; "startDistanceM" -> "Distancia"; "dimension1LengthM" -> "Longitud"; "dimension2LowerHeightM" -> "Altura inferior"; "dimension3UpperHeightM" -> "Altura superior"; else -> "Nombre de singularidad" }}: ${it.value} · ✓ SCAP")
        }
        ScapSicSupplement.labels.forEach { (key,label) ->
            if (key != "singularityName" || values["singularityName"].isNullOrBlank()) {
                val storedKey=ScapSicSupplement.PREFIX+key
                val value=values[storedKey].orEmpty()
                val options=ScapSicSupplement.options(key,values)
                if(key=="singularityName") OutlinedTextField(value,{c.edit(s.inspection.id,"inspection",storedKey,it)},label={Text(label)},enabled=editable,modifier=Modifier.fillMaxWidth())
                else {
                    ScapChoice(label,options.find { it.substringBefore(" - ")==value } ?: value,
                        options + if(key=="roadbedCode") listOf("Otro") else emptyList(),editable) { option ->
                        c.edit(s.inspection.id,"inspection",storedKey,if(option=="Otro") "" else option.substringBefore(" - "))
                        if(key=="classCode") c.edit(s.inspection.id,"inspection",ScapSicSupplement.PREFIX+"typeCode","")
                    }
                    if(key=="roadbedCode" && value !in options) OutlinedTextField(value,{c.edit(s.inspection.id,"inspection",storedKey,it.uppercase())},label={Text("Código de calzada")},enabled=editable,modifier=Modifier.fillMaxWidth())
                }
            }
        }
        if(preview.missingRequired.isEmpty()) Text("✓ Datos suficientes para exportar SIC-17",color=com.tuempresa.inventariovial.ui.theme.FieldSuccess)
        else {
            Text("⚠ Faltan ${preview.missingRequired.size} datos para SIC-17:",color=com.tuempresa.inventariovial.ui.theme.FieldPending)
            preview.missingRequired.forEach { Text("• $it") }
            Text("Completa los datos heredados en su sección SCAP; los códigos SIC se completan aquí. Exportación disponible en G.")
        }
    }}
}
