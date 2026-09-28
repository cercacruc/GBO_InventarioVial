package com.tuempresa.inventariovial.field

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PrSuggestionPanel(reference: RoadReferenceData, config: MatchConfig, route: String, roadbed: String,
    latitude: Double?, longitude: Double?, accuracy: Double?, endpoint: String, onConfirm: (PrSuggestion) -> Unit) {
    if (reference.prs.isEmpty()) return
    // A new input immediately hides the previous result; an old async result cannot be confirmed for a new fix.
    val state = remember(reference, config, route, roadbed, latitude, longitude, accuracy) { mutableStateOf<PrSuggestionResult?>(null) }
    LaunchedEffect(state) {
        state.value = if (latitude == null || longitude == null) PrSuggestionResult(message = "Obtén el GPS de $endpoint para sugerir un PR.")
        else withContext(Dispatchers.Default) { PrSuggestionEngine(reference, config).suggest(route, roadbed, latitude, longitude, accuracy) }
    }
    PrSuggestionCard(state.value, endpoint, onConfirm)
}

@Composable
fun PrSuggestionCard(result: PrSuggestionResult?, endpoint: String, onConfirm: (PrSuggestion) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Ubicación sugerida por GNSS · $endpoint", style = MaterialTheme.typography.titleMedium)
            val suggestion = result?.suggestion
            if (suggestion != null) {
                Text("PR ${suggestion.suggestedPrCode} + ${suggestion.distanceText} m")
                Text("${suggestion.routeCode} / ${suggestion.roadbedCode} · Confirma antes de usar como dato contractual.")
                OutlinedButton(onClick = { onConfirm(suggestion) }) { Text("Usar sugerencia") }
            } else Text(result?.message ?: "Validando PR sobre el eje…")
        }
    }
}
