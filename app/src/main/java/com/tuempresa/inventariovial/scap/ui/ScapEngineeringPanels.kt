package com.tuempresa.inventariovial.scap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.domain.ScapNumbers
import com.tuempresa.inventariovial.scap.domain.ScapFieldPolicy

internal val sketchDimensions=linkedMapOf(
    "bridgeWidth" to "Ancho total del puente", "roadwayWidth" to "Ancho de la calzada",
    "approachWidth" to "Ancho de la losa de aproximación", "sidewalkWidth" to "Ancho de aceras",
    "sidewalkHeight" to "Altura de aceras", "railingWidth" to "Ancho de barandas",
    "railingHeight" to "Altura de barandas", "dividerWidth" to "Ancho de divisorio de carriles",
    "dividerHeight" to "Altura de divisorio de carriles", "lowerClearance" to "Altura libre vertical inferior",
    "upperClearance" to "Altura libre vertical superior")

@Composable
fun ScapSketchDimensions(s:ScapInspectionSnapshot,c:ScapController,editable:Boolean) {
    val values=scapValues(s,c,"inspection")
    var confirm by remember {mutableStateOf<String?>(null)}
    Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("CARACTERÍSTICAS DE DISEÑO GEOMÉTRICO",style=MaterialTheme.typography.titleLarge)
        Text("Datos de apoyo para croquis · No se exportan al SCAP ni al SIC; no intervienen en cálculos.")
        val groups=linkedMapOf(
            "bridge" to listOf("bridgeWidth","roadwayWidth"),
            "approach" to listOf("approachWidth"),
            "sidewalk" to listOf("sidewalkWidth","sidewalkHeight"),
            "railing" to listOf("railingWidth","railingHeight"),
            "divider" to listOf("dividerWidth","dividerHeight"),
            "lower" to listOf("lowerClearance"),"upper" to listOf("upperClearance"))
        val labels=mapOf("approach" to "Losa de aproximación","sidewalk" to "Aceras","railing" to "Barandas","divider" to "Divisorio de carriles","lower" to "Altura libre inferior","upper" to "Altura libre superior")
        groups.forEach {(group,names)->
            val stateKey="internal.sketch.$group.state"
            val absent=values[stateKey]==ScapFieldPolicy.NOT_APPLICABLE
            if(group!="bridge") {
                HorizontalDivider()
                ScapChoice("${labels[group]} · tipo / existencia",values[stateKey].orEmpty(),listOf(ScapFieldPolicy.NOT_APPLICABLE),editable) {v->
                    if(v==ScapFieldPolicy.NOT_APPLICABLE && names.any {!values["internal.sketch.$it"].isNullOrBlank()}) confirm=stateKey
                    else c.edit(s.inspection.id,"inspection",stateKey,v)
                }
                Text(if(absent) "— No aplica · medidas conservadas" else "Registra las medidas o selecciona No aplica cuando el componente no exista.",style=MaterialTheme.typography.bodySmall)
            }
            names.forEach {name->
                val key="internal.sketch.$name";val value=values[key].orEmpty()
                OutlinedTextField(value,{c.edit(s.inspection.id,"inspection",key,it)},label={Text("${sketchDimensions[name]} (m)")},
                    enabled=editable && !absent,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                    isError=!absent && value.isNotBlank() && ScapNumbers.decimal(value)?.let {it>=0}!=true)
            }
        }
    }}
    confirm?.let {key->AlertDialog(onDismissRequest={confirm=null},title={Text("Marcar como No aplica")},
        text={Text("Este campo contiene información asociada. ¿Desea marcarlo como No aplica? Se conservará el valor.")},
        confirmButton={TextButton(onClick={c.edit(s.inspection.id,"inspection",key,ScapFieldPolicy.NOT_APPLICABLE);confirm=null}){Text("Marcar y conservar")}},
        dismissButton={TextButton(onClick={confirm=null}){Text("Cancelar")}})}
}
