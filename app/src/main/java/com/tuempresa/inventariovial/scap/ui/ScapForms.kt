package com.tuempresa.inventariovial.scap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.scap.catalog.*
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.ScapValidation

@Composable
fun ScapChoice(label:String,selected:String,options:List<String>,enabled:Boolean=true,onSelect:(String)->Unit) {
    var expanded by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth()) {
        Text(label,style=MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(enabled=enabled,onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
                Text(selected.ifBlank {"Sin completar"},modifier=Modifier.weight(1f));Text("▾")
            }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=340.dp)) {
                DropdownMenuItem(text={Text("Sin completar")},onClick={onSelect("");expanded=false})
                options.distinct().forEach {option->DropdownMenuItem(text={Text(if(option==selected) "✓ $option" else option)},onClick={onSelect(option);expanded=false})}
            }
        }
    }
}

@Composable
fun scapValues(s:ScapInspectionSnapshot,controller:ScapController,owner:String):Map<String,String> {
    val overrides by controller.overrides.collectAsState()
    val prefix="${s.inspection.id}/$owner/"
    return s.values(owner)+overrides.filterKeys {it.startsWith(prefix)}.mapKeys {it.key.removePrefix(prefix)}
}

@Composable
fun ScapFields(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,section:String,owner:String="inspection",editable:Boolean) {
    val kind=com.tuempresa.inventariovial.scap.domain.ScapValidation.ownerType(s,owner) ?: return
    val values=scapValues(s,c,owner)
    catalog.fields(section,kind).filter {it.visible(values)}.forEach {field->
        val value=values[field.key].orEmpty()
        val label=if(field.key=="secondaryCharacteristic" && values["category"]=="ALCANTARILLA") "Ojos / Vanos" else field.label
        val onChange:(String)->Unit={c.edit(s.inspection.id,owner,field.key,it)}
        val choices=when(field.kind) {"DEPENDENT"->catalog.types(values["category"].orEmpty());"APPLICABILITY"->catalog.options("yesNo");else->catalog.options(field.catalog)}
        if(field.kind in setOf("CATALOG","DEPENDENT","APPLICABILITY")) ScapChoice(label,value,choices,editable,onChange)
        else {
            if(field.kind=="CHOICE_TEXT") ScapChoice("Valores observados · $label",value,choices,editable,onChange)
            val error=ScapValidation.fieldError(field,value,values,catalog)
            OutlinedTextField(value,onChange,label={Text(label)},enabled=editable,modifier=Modifier.fillMaxWidth(),
                isError=error!=null,keyboardOptions=KeyboardOptions(keyboardType=when(field.kind) {
                    "INTEGER","YEAR"->KeyboardType.Number;"DECIMAL","SIGNED_DECIMAL","PERCENT"->KeyboardType.Decimal;else->KeyboardType.Text}),
                supportingText={if(error!=null) Text(error) else if(field.kind=="DATE") Text("dd/mm/aaaa") else if(field.kind=="CHAINAGE") Text("Metros o km+metros; ejemplo de formato: 30+298")})
        }
    }
}

internal val structureNames=linkedMapOf("LEFT_ABUTMENT" to "Estribo izquierdo","RIGHT_ABUTMENT" to "Estribo derecho",
    "PIER" to "Pilar","LEFT_ANCHOR" to "Macizo / cámara de anclaje izquierda","RIGHT_ANCHOR" to "Macizo / cámara de anclaje derecha")

@Composable
fun ScapTechnicalSection(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,section:String,editable:Boolean) {
    var spanId by rememberSaveable(s.inspection.id) {mutableStateOf<String?>(null)}
    var structureId by rememberSaveable(s.inspection.id) {mutableStateOf<String?>(null)}
    var supportId by rememberSaveable(s.inspection.id) {mutableStateOf<String?>(null)}
    var profileId by rememberSaveable(s.inspection.id) {mutableStateOf<String?>(null)}
    var removeId by remember {mutableStateOf<String?>(null)}
    var addKind by remember {mutableStateOf("PIER")}
    var access by rememberSaveable {mutableStateOf("ACCESS_LEFT")}
    val id=s.inspection.id
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        when(section) {
            "C1" -> {
                Text("Número de tramos: ${s.spans.size}",style=MaterialTheme.typography.titleMedium)
                ScapFields(s,c,catalog,section,editable=editable)
                s.spans.sortedBy {it.spanIndex}.forEach {Text("Tramo ${it.spanIndex}: ${it.lengthM?.let {n->"$n m"} ?: "Longitud pendiente"}")}
                Text("Agrega o edita cada tramo desde C2.")
            }
            "C2","C3" -> {
                val spans=s.spans.sortedBy {it.spanIndex}
                val current=spans.find {it.id==spanId} ?: spans.firstOrNull()
                ScapChoice("Tramo",current?.let {"Tramo ${it.spanIndex}"}.orEmpty(),spans.map {"Tramo ${it.spanIndex}"}) {value->spanId=spans.find {"Tramo ${it.spanIndex}"==value}?.id}
                if(section=="C2") Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled=editable,onClick={c.submit {it.addRow(id,"SPAN")}}) {Text("Añadir tramo")}
                    if(current!=null) TextButton(enabled=editable,onClick={removeId=current.id}) {Text("Retirar tramo")}
                }
                current?.let {ScapFields(s,c,catalog,section,it.id,editable)} ?: Text("Agrega un tramo en C2 para completar su configuración y tablero.")
            }
            "C4","D1" -> {
                val rows=s.substructures.filter {section!="D1" || "ANCHOR" !in it.kind}.sortedWith(compareBy<ScapSubstructureEntity>{it.kind}.thenBy {it.elementIndex})
                fun label(e:ScapSubstructureEntity)="${structureNames[e.kind]}${if(e.kind=="PIER") " ${e.elementIndex}" else ""}"
                val current=rows.find {it.id==structureId} ?: rows.firstOrNull()
                if(section=="C4") {
                    Text("Incluye solo la subestructura existente. Los bloques no agregados no se muestran.")
                    ScapChoice("Elemento para agregar",structureNames[addKind].orEmpty(),structureNames.filterKeys {k->k=="PIER" || rows.none {it.kind==k}}.values.toList(),editable) {v->addKind=structureNames.entries.find {it.value==v}?.key ?: "PIER"}
                    OutlinedButton(enabled=editable && (addKind=="PIER" || rows.none {it.kind==addKind}),onClick={c.submit {it.addRow(id,addKind)}}) {Text("Añadir elemento de subestructura")}
                }
                ScapChoice("Elemento",current?.let(::label).orEmpty(),rows.map(::label)) {v->structureId=rows.find {label(it)==v}?.id}
                current?.let {e->
                    ScapFields(s,c,catalog,section,e.id,editable)
                    if(section=="C4") TextButton(enabled=editable,onClick={removeId=e.id}) {Text("Retirar este elemento")}
                } ?: Text("Sin elementos aplicables registrados. Agrega estribos o pilares en C4 cuando existan.")
                if(section=="D1") ScapFields(s,c,catalog,section,editable=editable)
            }
            "C5" -> {
                ScapFields(s,c,catalog,section,editable=editable)
                HorizontalDivider();Text("APOYOS · ${s.supports.size}",style=MaterialTheme.typography.titleMedium)
                val rows=s.supports.sortedBy {it.supportIndex};val current=rows.find {it.id==supportId} ?: rows.firstOrNull()
                ScapChoice("Apoyo",current?.let {"Apoyo ${it.supportIndex}"}.orEmpty(),rows.map {"Apoyo ${it.supportIndex}"}) {v->supportId=rows.find {"Apoyo ${it.supportIndex}"==v}?.id}
                OutlinedButton(enabled=editable,onClick={c.submit {it.addRow(id,"BEARING")}}) {Text("Añadir apoyo")}
                current?.let {ScapFields(s,c,catalog,section,it.id,editable);TextButton(enabled=editable,onClick={removeId=it.id}){Text("Retirar apoyo")}}
            }
            "C6","C7" -> {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(access=="ACCESS_LEFT",{access="ACCESS_LEFT"},label={Text("Acceso izquierdo")})
                    FilterChip(access=="ACCESS_RIGHT",{access="ACCESS_RIGHT"},label={Text("Acceso derecho")})
                }
                ScapFields(s,c,catalog,section,access,editable)
            }
            "D4" -> {
                Text("Registra las cotas del perfil longitudinal respecto a un punto fijo.")
                ScapFields(s,c,catalog,section,editable=editable)
                val rows=s.profile.sortedBy {it.pointIndex};val current=rows.find {it.id==profileId} ?: rows.firstOrNull()
                Text("Número de puntos: ${rows.size}")
                ScapChoice("Punto",current?.let {"Punto ${it.pointIndex}"}.orEmpty(),rows.map {"Punto ${it.pointIndex}"}) {v->profileId=rows.find {"Punto ${it.pointIndex}"==v}?.id}
                OutlinedButton(enabled=editable,onClick={c.submit {it.addRow(id,"PROFILE")}}) {Text("Añadir punto del perfil")}
                current?.let {ScapFields(s,c,catalog,section,it.id,editable);TextButton(enabled=editable,onClick={removeId=it.id}){Text("Retirar punto")}}
            }
            else -> ScapFields(s,c,catalog,section,editable=editable)
        }
    }
    removeId?.let {rowId->AlertDialog(onDismissRequest={removeId=null},title={Text("Retirar elemento de la ficha")},
        text={Text("Se retirarán sus datos de esta inspección. Los demás elementos y fotografías se conservan.")},
        confirmButton={TextButton(onClick={c.submit {it.removeRow(id,rowId)};removeId=null}){Text("Retirar")}},
        dismissButton={TextButton(onClick={removeId=null}){Text("Cancelar")}})}
}
