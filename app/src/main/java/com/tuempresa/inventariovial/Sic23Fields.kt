package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.tuempresa.inventariovial.model.form.Sic23FormState

@Composable
fun Sic23Fields(state: Sic23FormState, onStateChange: (Sic23FormState) -> Unit) {
    SectionTitle("SIC-23 · Derecho de vía")
    Text("Clase")
    ChoiceSelector(Sic23FormState.classOptions,
        optionForCode(Sic23FormState.classOptions, state.classCode),
        { onStateChange(state.selectClass(codeFromOption(it))) })
    if (state.typeOptions.isNotEmpty()) {
        Text("Tipo")
        ChoiceSelector(state.typeOptions, optionForCode(state.typeOptions, state.typeCode),
            { onStateChange(state.selectType(codeFromOption(it))) })
    } else Text("Tipo: sin objeto")
    if (state.usesWidth) {
        OutlinedTextField(value = state.widthM, onValueChange = { onStateChange(state.copy(widthM = it)) },
            label = { Text(if (state.typeCode == "1") "Ancho del derecho de vía (m)" else "Ancho de berma central (m)") },
            supportingText = { Text("Obligatorio. Hasta dos decimales; ejemplo: 12.50") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth())
    } else Text("Ancho: sin objeto para esta clase y tipo")
    val hint = when (state.classCode) {
        "22" -> "Nombre de la zona urbana"
        "23" -> if (state.typeCode == "1") "Código del desvío, calle o plaza" else "Mercado, edificio u otro punto"
        "24" -> "Roca, roca meteorizada, grava de río o arena"
        else -> when (state.typeCode) {
            "2" -> "Vegetación, grava, pavimento o suelo"
            "3" -> "Viviendas, terrenos, cercos, cultivos o plantaciones"
            "4" -> "Líneas eléctricas, fibra óptica, alcantarillado o canales de regadío"
            else -> "Comentario libre"
        }
    }
    OutlinedTextField(value = state.description,
        onValueChange = { onStateChange(state.copy(description = it)) },
        label = { Text("Descripción") }, placeholder = { Text(hint) },
        supportingText = { Text("Se guarda en mayúsculas, sin tildes ni caracteres especiales.") },
        modifier = Modifier.fillMaxWidth(), minLines = 2)
}
