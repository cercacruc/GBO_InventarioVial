package com.tuempresa.inventariovial.scap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.scap.calculator.ScapPercentages
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import java.util.UUID

@Composable
fun ScapElements(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean,onPhoto:(String?)->Unit) {
    var query by rememberSaveable {mutableStateOf("")}
    var searching by rememberSaveable {mutableStateOf(false)}
    var absent by remember {mutableStateOf<String?>(null)}
    val codes=ScapPresentation.visibleElements(s)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("F.2 · CONDICIÓN GLOBAL DEL PUENTE",style=MaterialTheme.typography.headlineSmall)
            Text("Los elementos visibles son recordatorios. Confirma su presencia y evalúa únicamente lo observado.")
        }
        ScapPresentation.groups.forEachIndexed {index,group->
            item {Text("${listOf("I","II","III","IV","V")[index]}. $group",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)}
            items(codes.filter {catalog.element(it)?.group==group},key={"element:$it"}) {code->
                val item=requireNotNull(catalog.element(code))
                val entry=s.elements.find {it.element.elementCode==code}
                ScapElementCard("$code · ${item.name}") {
                    Text("Estado: ${ScapPresentation.presence(s,code)}")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled=editable,onClick={c.submit {it.selectElement(s.inspection.id,code,true)}}) {Text("Presente")}
                        TextButton(enabled=editable,onClick={absent=code}) {Text("No aplica")}
                    }
                    if(entry?.element?.isPresent==true) ElementConditionFields(s,c,entry,editable)
                    s.defects.filter {it.elementCode==code}.sortedBy {it.createdAt}.ifEmpty {listOf(ScapDefectEntity(
                        "${s.inspection.id}:observation:$code",s.inspection.id,code,"","",null,false,true,System.currentTimeMillis()))}.forEach {defect->key(defect.id) {
                        ScapDefectFields(s,c,catalog,defect,editable)
                    }}
                    val photos=s.photos.count {photo->photo.scapElementCode==code || s.defects.any {d->d.elementCode==code && d.photoId==photo.id}}
                    Text("Fotos asociadas: $photos")
                    OutlinedButton(enabled=editable,onClick={onPhoto(code)}) {Text("+ Añadir foto")}
                    AddDefectButton(s,c,code,editable)
                }
            }
        }
        item {
            OutlinedButton(enabled=editable,onClick={searching=!searching}) {Text("+ BUSCAR / AÑADIR OTRO ELEMENTO")}
            if(searching) OutlinedTextField(query,{query=it},label={Text("Buscar en los ${catalog.elements.size} elementos por código o nombre")},modifier=Modifier.fillMaxWidth())
        }
        if(searching) items(catalog.elements.filter {it.code.contains(query,true)||it.name.contains(query,true)},key={"search:${it.code}"}) {e->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${e.code} · ${e.name}",Modifier.weight(1f))
                OutlinedButton(enabled=editable && e.code !in codes,onClick={c.submit {it.addElementForReview(s.inspection.id,e.code)}}) {Text(if(e.code in codes) "Visible" else "Añadir")}
            }
        }
        item {
            Text("OBSERVACIONES GENERALES",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
            AddDefectButton(s,c,null,editable)
        }
        items(s.defects.filter {it.elementCode==null || catalog.element(it.elementCode)==null},key={"general:${it.id}"}) {
            ScapElementCard("Observación general") {ScapDefectFields(s,c,catalog,it,editable)}
        }
    }
    absent?.let {code->AlertDialog(onDismissRequest={absent=null},title={Text("Marcar $code como No aplica")},
        text={Text("Se conservarán porcentajes, observaciones y fotos. El elemento no participará en el cálculo mientras esté marcado No aplica.")},
        confirmButton={TextButton(onClick={c.submit {it.selectElement(s.inspection.id,code,false)};absent=null}){Text("Marcar y conservar")}},
        dismissButton={TextButton(onClick={absent=null}){Text("Cancelar")}})}
}

@Composable
private fun ElementConditionFields(s:ScapInspectionSnapshot,c:ScapController,entry:ScapElementWithCondition,editable:Boolean) {
    val e=entry.element
    val values=scapValues(s,c,e.id)
    val p=(0..5).map {i->if(values.containsKey("percent$i")) ScapNumbers.decimal(values["percent$i"]) else entry.condition?.let {ScapPercentages.values(it)[i]}}
    Text("Unidad: ${e.unit} · Factor: ${e.importanceFactor}")
    OutlinedTextField(values["quantity"] ?: e.quantity?.toString().orEmpty(),{c.edit(s.inspection.id,e.id,"quantity",it)},
        label={Text("Metrado (${e.unit})")},enabled=editable,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
    (0..5).chunked(2).forEach {levels->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {levels.forEach {level->
        val value=values["percent$level"] ?: p[level]?.toString().orEmpty()
        OutlinedTextField(value,{c.edit(s.inspection.id,e.id,"percent$level",it)},label={Text("Nivel $level (%)")},enabled=editable,
            modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            isError=value.isNotBlank() && ScapNumbers.decimal(value)?.let {it in 0.0..100.0}!=true)
    }}}
    Text("Suma: ${p.filterNotNull().sum()}% · ${p.count {it!=null}}/6 niveles")
    ScapPercentages.error(p)?.let {Text(it,color=MaterialTheme.colorScheme.error)}
}

@Composable
private fun AddDefectButton(s:ScapInspectionSnapshot,c:ScapController,code:String?,editable:Boolean) {
    OutlinedButton(enabled=editable,onClick={c.submit {it.saveDefect(s.inspection.id,ScapDefectEntity(
        UUID.randomUUID().toString(),s.inspection.id,code,"","",null,false,true,System.currentTimeMillis()))}}) {Text("+ Añadir observación / falla")}
}

@Composable
internal fun ScapDefectFields(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,d:ScapDefectEntity,editable:Boolean) {
    var description by remember(d.id) {mutableStateOf(d.description)}
    var location by remember(d.id) {mutableStateOf(d.locationDescription)}
    var removing by remember {mutableStateOf(false)}
    OutlinedTextField(description,{description=it;c.submit {r->r.setDefectField(s.inspection.id,d.id,"description",it,d)}},label={Text("Descripción observada")},enabled=editable,modifier=Modifier.fillMaxWidth(),minLines=2)
    OutlinedTextField(location,{location=it;c.submit {r->r.setDefectField(s.inspection.id,d.id,"locationDescription",it,d)}},label={Text("Ubicación")},enabled=editable,modifier=Modifier.fillMaxWidth())
    if(d.elementCode==null) ScapChoice("Elemento SCAP (opcional)","",catalog.elements.map {"${it.code} · ${it.name}"},editable) {v->c.submit {it.setDefectField(s.inspection.id,d.id,"elementCode",v.substringBefore(" · "))}}
    fun label(id:String)=s.photos.find {it.id==id}?.let {"Foto ${it.photoIndex} · ${ScapPhotoCategories.label(it.photoCategory)}"}.orEmpty()
    ScapChoice("Foto asociada",d.photoId?.let(::label).orEmpty(),s.photos.map {label(it.id)},editable) {v->c.submit {it.setDefectField(s.inspection.id,d.id,"photoId",s.photos.find {label(it.id)==v}?.id.orEmpty(),d)}}
    if(d.aiSuggested) Row {Checkbox(d.validatedByUser,{v->c.submit {it.setDefectField(s.inspection.id,d.id,"validatedByUser",v.toString())}},enabled=editable);Text("Validado por el inspector")}
    if(s.defects.any {it.id==d.id}) TextButton(enabled=editable,onClick={removing=true}) {Text("Eliminar observación")}
    if(removing) AlertDialog(onDismissRequest={removing=false},title={Text("Eliminar observación")},text={Text("La fotografía se conservará.")},
        confirmButton={TextButton(onClick={c.submit {it.removeDefect(s.inspection.id,d.id);description="";location=""};removing=false}){Text("Eliminar")}},dismissButton={TextButton(onClick={removing=false}){Text("Cancelar")}})
}
