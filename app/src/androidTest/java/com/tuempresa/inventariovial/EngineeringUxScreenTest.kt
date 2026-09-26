package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import android.graphics.Bitmap
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.domain.ScapFieldPolicy
import com.tuempresa.inventariovial.scap.ui.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import kotlinx.coroutines.*
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EngineeringUxScreenTest {
    @get:Rule val compose=createComposeRule()
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private var db:InventoryDatabase?=null
    private var scope:CoroutineScope?=null
    @After fun cleanup(){scope?.cancel();db?.close()}
    private fun screenshot(name:String) {
        compose.waitForIdle()
        val bitmap=requireNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(context.filesDir,name).outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun homeUsesCorporateLogoAndTouchableCards() {
        compose.setContent {InventarioVialTheme {Surface(color=MaterialTheme.colorScheme.background){HomeScreen(12,3,{})}}}
        compose.onNodeWithContentDescription("GBO Ingenieros Consultores").assertExists()
        compose.onNodeWithText("Inventario vial").assertExists()
        screenshot("ux-home.png")
    }
    @Test fun sic18StructuralExplanationIsBeforeFunctionalAndNoPercentInputsExist() {
        compose.setContent {InventarioVialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {Sic18Fields(Sic18FormState(structuralConditionCode="2")) {}}}}
        val structural=compose.onNodeWithText("Quebrado o en menos del 30% de la longitud o con ligera 20% deformación.")
        structural.performScrollTo().assertExists()
        val functional=compose.onNodeWithText("Condición funcional")
        Assert.assertTrue(structural.fetchSemanticsNode().positionInRoot.y<functional.fetchSemanticsNode().positionInRoot.y)
        compose.onNodeWithText("Daño estructural (%)").assertDoesNotExist()
        screenshot("ux-conditions.png")
    }
    @Test fun sic19OtherRequiresExplicitCriterionAndEarthShowsErosion() {
        compose.setContent {InventarioVialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {
            var s by remember {mutableStateOf(Sic19FormState(typeCode="4"))};Sic19Fields(s){s=it}
        }}}
        compose.onNodeWithText("Criterio para Otro").performScrollTo().assertExists()
        compose.onNodeWithText("Sin completar").performScrollTo().performClick()
        compose.onNodeWithText("Tierra",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Problema grave de erosión.").performScrollTo().assertExists()
    }
    @Test fun culvertHasThreeExplicitPhotoSections() {
        compose.setContent {InventarioVialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {CulvertPhotos(emptyList(),emptyMap()){_,_->}}}}
        listOf("FOTO PANORÁMICA","FOTO DE ENTRADA","FOTO DE SALIDA").forEach {compose.onNodeWithText(it).performScrollTo().assertExists()}
    }
    @Test fun wallHasFunctionalConditionAndInternalLength() {
        compose.setContent {InventarioVialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {Sic20Fields(Sic20FormState(classCode="14")){}}}}
        compose.onNodeWithText("Longitud del muro (m)").performScrollTo().assertExists()
        compose.onNodeWithText("Condición funcional").performScrollTo().assertExists()
        compose.onNodeWithText("Mala (totalmente obstruida)").performScrollTo().assertExists()
    }
    @Test fun noApplyConfirmsAndDisablesWithoutLosingMaterialThenRestores()=runBlocking {
        val database=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build();db=database
        val catalog=ScapCatalog.load(context);val repo=ScapRepository(database,catalog);val id=repo.create("I","D",null)
        repo.setField(id,"inspection","railingType","Parapeto");repo.setField(id,"inspection","railingMaterial","Acero")
        val initial=repo.dao.snapshot(id)!!
        val jobs=CoroutineScope(SupervisorJob()+Dispatchers.Main);scope=jobs
        val c=ScapController(context,database,jobs)
        compose.setContent {InventarioVialTheme {Column {ScapFields(initial,c,catalog,"C5",editable=true,keys=listOf("railingType","railingMaterial","railingSecondary"))}}}
        compose.onNodeWithText("Parapeto").performClick();compose.onNodeWithText("No aplica").performClick()
        compose.onNodeWithText("Marcar y conservar datos").assertExists().performClick()
        compose.onNodeWithText("Acero").assertIsNotEnabled()
        compose.onNodeWithText("No aplica").performClick();compose.onNodeWithText("Parapeto").performClick()
        compose.onNodeWithText("Acero").assertIsEnabled()
        compose.onNodeWithText("Aplica").assertDoesNotExist()
    }
    @Test fun c5ShowsSubsectionsInTemplateOrderAndJointAddAction():Unit=runBlocking {
        val database=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build();db=database
        val catalog=ScapCatalog.load(context);val repo=ScapRepository(database,catalog);val id=repo.create("I","D",null)
        val initial=repo.dao.snapshot(id)!!;val jobs=CoroutineScope(SupervisorJob()+Dispatchers.Main);scope=jobs
        val controller=ScapController(context,database,jobs)
        compose.setContent {InventarioVialTheme {Surface(color=MaterialTheme.colorScheme.background){ScapTechnicalSection(initial,controller,catalog,"C5",true)}}}
        screenshot("ux-scap-c5.png")
        ScapFieldPolicy.detailGroups.keys.forEach {compose.onNodeWithText(it).performScrollTo().assertExists()}
        compose.onNodeWithText("+ Añadir junta").performScrollTo().assertExists()
    }
    @Test fun photographicPanelAndGlobalConditionHaveDifferentTitles()=runBlocking {
        val database=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build();db=database
        val catalog=ScapCatalog.load(context);val repo=ScapRepository(database,catalog);val id=repo.create("I","D",null)
        val initial=repo.dao.snapshot(id)!!
        val jobs=CoroutineScope(SupervisorJob()+Dispatchers.Main);scope=jobs
        val controller=ScapController(context,database,jobs)
        compose.setContent {InventarioVialTheme {ScapElements(initial,controller,catalog,true){}}}
        compose.onNodeWithText("F.2 · CONDICIÓN GLOBAL DEL PUENTE").assertExists()
        listOf("I. SUPERESTRUCTURA","II. SUBESTRUCTURA","III. DETALLES","IV. CAUCE","V. ACCESOS").forEach {compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(it));compose.onNodeWithText(it).assertExists()}
        compose.onNodeWithText("F.2 · PANEL FOTOGRÁFICO").assertDoesNotExist()
    }
}
