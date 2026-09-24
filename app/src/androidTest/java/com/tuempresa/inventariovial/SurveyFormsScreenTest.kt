package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SurveyFormsScreenTest {
    @get:Rule val compose=createComposeRule()

    @Test fun verticalHasOnlyThreeTypesAndNoDimensions() {
        compose.setContent { InventarioVialTheme { Column {
            var state by remember {mutableStateOf(Sic22FormState())}
            Sic22Fields(state) {state=it}
        } } }
        compose.onNodeWithText("2 - Preventiva").assertIsSelected()
        compose.onNodeWithText("1 - Reglamento").performClick().assertIsSelected()
        compose.onNodeWithText("3 - Informativa").assertExists()
        compose.onNodeWithText("4 - Poste Kilométrico").assertDoesNotExist()
        compose.onNodeWithText("Ancho de señal (m)").assertDoesNotExist()
        compose.onNodeWithText("Mediciones adicionales del cliente").assertDoesNotExist()
    }

    @Test fun horizontalDoesNotAskForMaterial() {
        compose.setContent { InventarioVialTheme { Column { Sic21Fields(Sic21FormState(classCode="18")) {} } } }
        compose.onNodeWithText("Material").assertDoesNotExist()
        compose.onNodeWithText("Condición").assertExists()
    }

    @Test fun circularAndOvalCulvertsHaveDifferentRequiredMeasurements() {
        compose.setContent { InventarioVialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            var state by remember {mutableStateOf(Sic18FormState())}
            Sic18Fields(state) {state=it}
        } } }
        compose.onNodeWithText("Diámetro interior (m)").performScrollTo().assertExists()
        compose.onNodeWithText("Dimensión 2 · altura (m)").assertDoesNotExist()
        compose.onNodeWithText("Ovalada").performScrollTo().performClick()
        compose.onNodeWithText("Dimensión 2 · altura (m)").performScrollTo().assertExists()
        compose.onNodeWithText("Circular").performScrollTo().performClick()
        compose.onNodeWithText("Dimensión 2 · altura (m)").assertDoesNotExist()
    }

    @Test fun largeCatalogDropdownRetainsSelection() {
        compose.setContent { InventarioVialTheme { Column {
            var selected by remember {mutableStateOf("A")}
            ChoiceSelector(listOf("A","B","C","D"),selected) {selected=it}
        } } }
        compose.onNodeWithText("A").performClick()
        compose.onNodeWithText("D").performClick()
        compose.onNodeWithText("D").assertExists()
        compose.onNodeWithText("B").assertDoesNotExist()
    }
}
