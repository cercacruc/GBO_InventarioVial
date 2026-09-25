package com.tuempresa.inventariovial.field

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.EngineeringEdits
import kotlinx.coroutines.launch

@Composable
fun EngineeringEditScreen(id:String,onBack:()->Unit) {
    val context=LocalContext.current;val db=remember {InventoryDatabase.getInstance(context)};val scope=rememberCoroutineScope()
    var detail by remember(id) {mutableStateOf<SicFormDetail?>(null)}
    var paths by remember(id) {mutableStateOf<List<String>>(emptyList())}
    var categories by remember(id) {mutableStateOf<Map<String,String>>(emptyMap())}
    var error by remember {mutableStateOf<String?>(null)};var saving by remember {mutableStateOf(false)}
    LaunchedEffect(id) {runCatching {val s=requireNotNull(db.inventoryDao().snapshot(id));detail=EngineeringEdits.form(s);paths=s.photos.sortedBy {it.photoIndex}.map {it.localPath};categories=s.photos.mapNotNull {p->p.photoCategory?.let {p.localPath to it}}.toMap()}.onFailure {error=it.message}}
    BackHandler(enabled=!saving,onBack=onBack)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        OutlinedButton(enabled=!saving,onClick=onBack) {Text("Volver")}
        Text("Editar ficha técnica",style=MaterialTheme.typography.headlineMedium)
        when(val d=detail) {
            is SicFormDetail.Sic18 -> {Sic18Fields(d.state) {detail=SicFormDetail.Sic18(it)};CulvertPhotos(paths,categories) {p,c->paths=p;categories=c}}
            is SicFormDetail.Sic19 -> Sic19Fields(d.state) {detail=SicFormDetail.Sic19(it)}
            is SicFormDetail.Sic20 -> Sic20Fields(d.state) {detail=SicFormDetail.Sic20(it)}
            else -> Text("Cargando ficha…")
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(enabled=detail!=null && !saving,onClick={val d=detail ?: return@Button;saving=true;scope.launch {
            try {EngineeringEdits.save(db,id,d,paths,categories);onBack()} catch(e:Exception){error=e.message} finally {saving=false}
        }},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Guardar cambios")}
    }
}
