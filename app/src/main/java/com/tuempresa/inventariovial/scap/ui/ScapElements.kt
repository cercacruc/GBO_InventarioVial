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
import com.tuempresa.inventariovial.scap.domain.ScapNumbers
import java.util.UUID

@Composable
fun ScapElements(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean) {
    var query by rememberSaveable {mutableStateOf("")}
    var selectedId by rememberSaveable(s.inspection.id){mutableStateOf<String?>(null)}
    var selecting by rememberSaveable {mutableStateOf(false)}
    val selected=s.elements.filter{it.element.isPresent}.sortedBy{it.element.elementCode}
    val current=selected.find{it.element.id==selectedId} ?: selected.firstOrNull()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {
            Text("Elementos presentes: ${selected.size}",style=MaterialTheme.typography.titleLarge)
            Text("Selecciona únicamente los elementos existentes en el puente. Cada uno requiere sus seis porcentajes de condición.")
            OutlinedButton(onClick={selecting=!selecting}){Text(if(selecting) "Volver a evaluación" else "Buscar y seleccionar elementos")}
        }
        if(selecting) {
            item {OutlinedTextField(query,{query=it},label={Text("Buscar por código o nombre")},modifier=Modifier.fillMaxWidth())}
            items(catalog.elements.filter{it.code.contains(query,true)||it.name.contains(query,true)},key={it.code}) {e->
                Row(Modifier.fillMaxWidth()) {
                    Checkbox(selected.any{it.element.elementCode==e.code},onCheckedChange={present->c.submit{it.selectElement(s.inspection.id,e.code,present)}},enabled=editable)
                    Column(Modifier.weight(1f).padding(top=8.dp)) {
                        Text("${e.code} · ${e.name}");Text("${e.group} · ${e.unit} · Factor ${e.importanceFactor}",style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else if(current!=null) {
            item {ScapChoice("Elemento a evaluar","${current.element.elementCode} · ${current.element.description}",selected.map{"${it.element.elementCode} · ${it.element.description}"}) {v->
                selectedId=selected.find{"${it.element.elementCode} · ${it.element.description}"==v}?.element?.id
            }}
            item(key=current.element.id) {
                val e=current.element
                val values=scapValues(s,c,e.id)
                val p=(0..5).map{ScapNumbers.decimal(values["percent$it"])}
                val error=ScapPercentages.error(p)
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Unidad: ${e.unit} · Factor de importancia: ${e.importanceFactor}")
                    Text("Grupo: ${e.group}")
                    val quantity=values["quantity"].orEmpty()
                    OutlinedTextField(quantity,{c.edit(s.inspection.id,e.id,"quantity",it)},label={Text("Metrado (${e.unit})")},enabled=editable,
                        isError=quantity.isNotBlank() && ScapNumbers.decimal(quantity)?.let{it>=0}!=true,
                        modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                    Text("Condición del elemento (%)",style=MaterialTheme.typography.titleMedium)
                    (0..5).chunked(2).forEach {levels->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        levels.forEach {level->
                            val value=values["percent$level"].orEmpty()
                            OutlinedTextField(value,{c.edit(s.inspection.id,e.id,"percent$level",it)},label={Text("Nivel $level (%)")},
                                enabled=editable,modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                                isError=value.isNotBlank() && ScapNumbers.decimal(value)?.let{it in 0.0..100.0}!=true)
                        }
                    }}
                    Text("Suma: ${p.filterNotNull().sum()}% · ${p.count{it!=null}}/6 niveles")
                    if(error!=null) Text(error,color=MaterialTheme.colorScheme.error) else Text("Evaluación válida: suma 100%.")
                    Text("La ficha se guarda aunque la evaluación esté incompleta. Los porcentajes deben ser válidos para cerrar la inspección.")
                }
            }
        } else item {Text("No se han seleccionado elementos. Abre la búsqueda para agregarlos.")}
    }
}

@Composable
fun ScapDefects(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean) {
    var selectedId by rememberSaveable(s.inspection.id){mutableStateOf<String?>(null)}
    var removing by remember{mutableStateOf<String?>(null)}
    val defects=s.defects.sortedBy{it.createdAt}
    val current=defects.find{it.id==selectedId} ?: defects.firstOrNull()
    fun label(d:ScapDefectEntity)="${defects.indexOf(d)+1}. ${d.elementCode ?: "General"} · ${d.description.take(55).ifBlank{"Sin descripción"}}"
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {
            Text("Defectos observados: ${defects.size}",style=MaterialTheme.typography.titleLarge)
            Button(enabled=editable,onClick={val id=UUID.randomUUID().toString();c.submit{it.saveDefect(s.inspection.id,
                ScapDefectEntity(id,s.inspection.id,null,"","",null,false,true,System.currentTimeMillis()))};selectedId=id}){Text("Agregar defecto")}
            ScapChoice("Defecto",current?.let(::label).orEmpty(),defects.map(::label)) {v->selectedId=defects.find{label(it)==v}?.id}
        }
        current?.let {d->item(key=d.id) {
            var description by remember(d.id){mutableStateOf(d.description)}
            var location by remember(d.id){mutableStateOf(d.locationDescription)}
            val codes=s.elements.filter{it.element.isPresent}.map{it.element.elementCode}.toSet()+listOfNotNull(d.elementCode)
            val choices=catalog.elements.filter{it.code in codes}.map{"${it.code} · ${it.name}"}
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                ScapChoice("Elemento SCAP (opcional)",catalog.element(d.elementCode.orEmpty())?.let{"${it.code} · ${it.name}"}.orEmpty(),choices,editable) {v->
                    c.submit{it.setDefectField(s.inspection.id,d.id,"elementCode",v.substringBefore(" · "))}
                }
                OutlinedTextField(description,{description=it;c.submit{r->r.setDefectField(s.inspection.id,d.id,"description",it)}},label={Text("Descripción del defecto")},enabled=editable,modifier=Modifier.fillMaxWidth(),minLines=3,isError=description.isBlank())
                OutlinedTextField(location,{location=it;c.submit{r->r.setDefectField(s.inspection.id,d.id,"locationDescription",it)}},label={Text("Ubicación del defecto")},enabled=editable,modifier=Modifier.fillMaxWidth())
                fun photoLabel(id:String)=s.photos.find{it.id==id}?.let{"Foto ${it.photoIndex} · ${photoLabels[it.photoCategory] ?: it.photoCategory}"}.orEmpty()
                ScapChoice("Fotografía asociada",d.photoId?.let(::photoLabel).orEmpty(),s.photos.map{photoLabel(it.id)},editable) {v->
                    c.submit{it.setDefectField(s.inspection.id,d.id,"photoId",s.photos.find{photoLabel(it.id)==v}?.id.orEmpty())}
                }
                if(d.photoId!=null) s.photos.find{it.id==d.photoId}?.let{ScapImage(it.originalPath ?: it.localPath)}
                if(d.aiSuggested) {
                    Text("Sugerencia de IA: requiere verificación del ingeniero.")
                    Row {Checkbox(d.validatedByUser,{v->c.submit{it.setDefectField(s.inspection.id,d.id,"validatedByUser",v.toString())}},enabled=editable);Text("Validado por el inspector")}
                } else Text("Registro manual del inspector.")
                TextButton(enabled=editable,onClick={removing=d.id}){Text("Eliminar defecto")}
            }
        }}
        item {Text("La asistencia visual de IA no está conectada. La evaluación y las mediciones corresponden al inspector.")}
    }
    removing?.let{id->AlertDialog(onDismissRequest={removing=null},title={Text("Eliminar defecto")},text={Text("Se elimina la anotación. La fotografía permanece en el panel.")},
        confirmButton={TextButton(onClick={c.submit{it.removeDefect(s.inspection.id,id)};removing=null}){Text("Eliminar")}},dismissButton={TextButton(onClick={removing=null}){Text("Cancelar")}})}
}
