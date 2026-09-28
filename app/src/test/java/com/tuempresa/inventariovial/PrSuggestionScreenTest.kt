package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.field.PrSuggestionCard
import com.tuempresa.inventariovial.field.PrSuggestionPanel
import com.tuempresa.inventariovial.road.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrSuggestionScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun displayedSuggestionDoesNotReplaceManualInputUntilButtonIsPressed() {
        var start by mutableStateOf(PrEntry("0042", "684"))
        var end by mutableStateOf(PrEntry("0050", "12"))
        val engine = PrSuggestionEngine(prSuggestionReference())
        val initial = engine.suggest("R", "CD", 0.0, .0053, 1.0)
        val final = engine.suggest("R", "CD", 0.0, .012, 1.0)
        compose.setContent { Column {
            Text("Manual inicio: ${start.prCode} + ${start.distanceM}")
            PrSuggestionCard(initial, "inicio") { start = start.confirm(it) }
            Text("Manual fin: ${end.prCode} + ${end.distanceM}")
            PrSuggestionCard(final, "fin") { end = end.confirm(it) }
        } }
        compose.onNodeWithText("PR 0010 + 2650 m").assertExists()
        compose.onNodeWithText("Manual inicio: 0042 + 684").assertExists()
        compose.runOnIdle { assertEquals("MANUAL", start.source); assertEquals("MANUAL", end.source) }
        compose.onAllNodesWithText("Usar sugerencia")[0].performClick()
        compose.onNodeWithText("Manual inicio: 0010 + 2650").assertExists()
        compose.onNodeWithText("Manual fin: 0050 + 12").assertExists()
        compose.runOnIdle { assertEquals(PrEntrySource.CONFIRMED, start.source); assertEquals("MANUAL", end.source) }
        compose.onAllNodesWithText("Usar sugerencia")[1].performClick()
        compose.runOnIdle { assertEquals("0015", end.prCode); assertEquals(PrEntrySource.CONFIRMED, end.source) }
    }

    @Test fun emptyCatalogKeepsManualUiAndCannotConfirmAnInventedPr() {
        compose.setContent { Column {
            Text("PR manual 0042 + 684 m")
            PrSuggestionPanel(RoadReferenceData(), MatchConfig(), "R", "CD", 0.0, .0053, 1.0, "inicio") { error("No debe confirmar") }
        } }
        compose.onNodeWithText("PR manual 0042 + 684 m").assertExists()
        compose.onNodeWithText("Usar sugerencia").assertDoesNotExist()
    }

    @Test fun changingRoadbedDiscardsOldSuggestionWithoutChangingManualData() {
        var bed by mutableStateOf("CD")
        var confirmed = false
        val reference = prSuggestionReference()
        compose.setContent {
            PrSuggestionPanel(reference, MatchConfig(), "R", bed, 0.0, .0053, 1.0, "inicio") { confirmed = true }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Usar sugerencia").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { bed = "UC" }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Sin catálogo PR para esta ruta/calzada.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Usar sugerencia").assertDoesNotExist()
        assertFalse(confirmed)
    }
}
