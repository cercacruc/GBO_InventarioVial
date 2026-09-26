package com.tuempresa.inventariovial.field

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.ChoiceSelector

@Composable
fun SurveyHeader(preferences: SurveyPreferences, segment: String, route: String, roadbed: String, direction: String,
    onSegment: (String)->Unit, onRoute: (String)->Unit, onRoadbed: (String)->Unit, onDirection: (String)->Unit) {
    var catalogVersion by remember { mutableIntStateOf(0) }
    val routes = remember(segment,catalogVersion) { preferences.routes(segment) }
    var editCatalog by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Tramo",style=MaterialTheme.typography.titleMedium)
        ChoiceSelector(SurveyPreferences.segments,segment) { if(it!=segment) onSegment(it) }
        Text("Ruta",style=MaterialTheme.typography.titleMedium)
        if(routes.isEmpty()) {
            OutlinedTextField(route,{onRoute(it.uppercase())},label={Text("Código de ruta")},modifier=Modifier.fillMaxWidth())
            Text("Catálogo pendiente del cliente. Ingresa el código real o configura las rutas.")
        } else {
            ChoiceSelector(routes,route) { if(it!=route) onRoute(it) }
            val index=routes.indexOf(route)
            if(preferences.hasConfiguredCatalog(segment) && index>=0 && index<routes.lastIndex) OutlinedButton(onClick={onRoute(routes[index+1])}) {Text("Siguiente ruta: ${routes[index+1]}")}
            if(!preferences.hasConfiguredCatalog(segment)) {
                Text("Catálogo inicial común. La relación con los tramos y su orden están pendientes de confirmación.")
                OutlinedTextField(route,{onRoute(it.uppercase())},label={Text("Ruta seleccionada / ingreso manual")},modifier=Modifier.fillMaxWidth())
            }
        }
        OutlinedButton(enabled=segment.isNotBlank(),onClick={editCatalog=true}) {Text("Configurar rutas de este tramo")}
        Text("Calzada",style=MaterialTheme.typography.titleMedium)
        ChoiceSelector(com.tuempresa.inventariovial.catalog.SicCatalogRepository.roadbedCodes + "Otro",roadbed) {onRoadbed(if(it=="Otro") "" else it)}
        if(roadbed !in com.tuempresa.inventariovial.catalog.SicCatalogRepository.roadbedCodes) OutlinedTextField(roadbed,{onRoadbed(it.uppercase())},label={Text("Código de calzada")},modifier=Modifier.fillMaxWidth())
        Text("Sentido de las progresivas")
        ChoiceSelector(listOf("Creciente", "Decreciente"),if(direction=="DECREASING") "Decreciente" else "Creciente") {
            onDirection(if(it=="Decreciente") "DECREASING" else "INCREASING")
        }
        OutlinedButton(onClick={reset=true}) {Text("Iniciar otro recorrido")}
    }
    if(reset) AlertDialog(onDismissRequest={reset=false},title={Text("Nuevo recorrido")},
        text={Text("Reinicia la comprobación de continuidad para regresar a una ruta o cambiar de sentido. Los registros guardados se conservan.")},
        confirmButton={TextButton(onClick={preferences.reset();reset=false}){Text("Reiniciar continuidad")}},
        dismissButton={TextButton(onClick={reset=false}){Text("Cancelar")}})
    if(editCatalog) {
        var text by remember(segment) {mutableStateOf(routes.joinToString("\n"))}
        var error by remember {mutableStateOf<String?>(null)}
        AlertDialog(onDismissRequest={editCatalog=false},title={Text("Rutas · $segment")},
            text={Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Un código por línea, en el orden de visita acordado. Vacío permite captura manual. Este orden no cambia con el sentido de la progresiva.")
                OutlinedTextField(text,{text=it},label={Text("Rutas autorizadas para el tramo")},minLines=4)
                error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            }},confirmButton={TextButton(onClick={runCatching {preferences.saveRoutes(segment,text.lines())}
                .onSuccess {catalogVersion++;editCatalog=false}.onFailure {error=it.message}}){Text("Guardar catálogo")}},
            dismissButton={TextButton(onClick={editCatalog=false}){Text("Cancelar")}})
    }
}
