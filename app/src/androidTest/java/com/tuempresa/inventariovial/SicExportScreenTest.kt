package com.tuempresa.inventariovial

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import org.junit.Rule
import org.junit.Test

class SicExportScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun exportMenuOffersAllQualifiedSicsAndReturnsHome() {
        compose.onNodeWithText("Exportar SIC a Excel").performScrollTo().performClick()
        compose.onNodeWithText("Inventario vial calificado · SIC-17 a SIC-23").assertIsDisplayed()
        compose.onNodeWithText("Todos los SIC (ZIP)").performClick()
        compose.onNodeWithText("SIC-23 · DERECHO DE VÍA").performScrollTo().performClick()
        compose.onNodeWithText("SIC-23 · DERECHO DE VÍA").assertIsDisplayed()
        compose.onNodeWithText("Proyecto o servicio (opcional)").assertExists()
        compose.onNodeWithText("Volver").performScrollTo().performClick()
        compose.onNodeWithText("Nuevo registro").assertExists()
    }
}
