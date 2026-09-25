package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.scap.calculator.*
import com.tuempresa.inventariovial.scap.catalog.*
import com.tuempresa.inventariovial.scap.domain.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

internal fun testScapCatalog()=ScapCatalog.parse(
    File("src/main/assets/scap_elements.json").readText(),File("src/main/assets/scap_options.json").readText(),File("src/main/assets/scap_fields.json").readText())

class ScapCalculationTest {
    @Test fun aguaBlancaReproducesEveryElementAndGlobalConditionFromWorkbook() {
        val fixture=JSONObject(javaClass.getResource("/scap_agua_blanca.json")!!.readText())
        val source=fixture.getJSONArray("elements");val catalog=testScapCatalog()
        val inputs=List(source.length()){i->val e=source.getJSONObject(i)
            val code=e.getString("code");val factor=catalog.element(code)!!.importanceFactor
            assertEquals(e.getDouble("importanceFactor"),factor,0.0)
            ScapCalculationInput(code,factor,List(6){e.getJSONArray("percentages").getDouble(it)})
        }
        val result=ScapConditionCalculator.calculate(inputs)
        assertNull(result.reason);assertEquals(14,result.elements.size)
        result.elements.forEachIndexed{i,e->assertEquals(source.getJSONObject(i).getDouble("expectedCondition"),e.condition,1e-9)}
        assertEquals(2.070,result.value!!,0.001)
        assertEquals(fixture.getDouble("expected"),result.value!!,1e-10)
        assertEquals("REGULAR",result.classification)
    }
    @Test fun acceptsOnlySixFiniteBoundedPercentagesSummingTo100WithinTolerance() {
        assertNull(ScapPercentages.error(listOf(0.0,90.0,10.0,0.0,0.0,0.0)))
        assertNull(ScapPercentages.error(listOf(0.0,99.99,0.0,0.0,0.0,0.0)))
        assertNotNull(ScapPercentages.error(listOf(0.0,99.98,0.0,0.0,0.0,0.0)))
        assertNotNull(ScapPercentages.error(listOf(0.0,99.0,0.0,0.0,0.0,0.0)))
        assertNotNull(ScapPercentages.error(listOf(0.0,100.0,1.0,0.0,0.0,0.0)))
        assertNotNull(ScapPercentages.error(listOf(-1.0,100.0,1.0,0.0,0.0,0.0)))
        assertNotNull(ScapPercentages.error(listOf(101.0,0.0,0.0,0.0,0.0,0.0)))
        assertNotNull(ScapPercentages.error(listOf(Double.NaN,0.0,0.0,0.0,0.0,0.0)))
        assertNotNull(ScapPercentages.error(listOf(100.0,0.0,0.0,0.0,0.0,null)))
        assertNotNull(ScapPercentages.error(listOf(100.0)))
    }
    @Test fun undefinedWorkbookCasesNeverPublishFabricatedGlobalValues() {
        fun input(code:String,p:List<Double?>,factor:Double=1.0)=ScapCalculationInput(code,factor,p)
        val good=listOf(0.0,100.0,0.0,0.0,0.0,0.0)
        assertNull(ScapConditionCalculator.calculate(listOf(input("1",good))).value)
        assertNull(ScapConditionCalculator.calculate(listOf(input("1",good,0.0),input("2",good,0.0))).value)
        assertNull(ScapConditionCalculator.calculate(listOf(input("1",listOf(0.0,96.0,0.0,0.0,0.0,4.0)),input("2",good))).value)
        assertNull(ScapConditionCalculator.calculate(listOf(input("1",good),input("2",List(6){null}))).value)
        assertNull(ScapConditionCalculator.calculate(listOf(input("1",good,Double.NaN))).value)
    }
    @Test fun classificationUsesUnroundedM12Thresholds() {
        // Two identical contributions x yield global x+1. This isolates each strict threshold.
        fun classify(factor:Double)=ScapConditionCalculator.calculate(List(2){ScapCalculationInput("$it",factor,listOf(0.0,0.0,0.0,0.0,100.0,0.0))}).classification
        assertEquals("BUENO",classify(0.25));assertEquals("REGULAR",classify(0.25001))
        assertEquals("REGULAR",classify(0.5));assertEquals("MALO",classify(0.50001))
        assertEquals("MALO",classify(0.75));assertEquals("MUY MALO",classify(0.75001))
    }
    @Test fun catalogsContain112UniqueOfficialElementsAndDependentTypes() {
        val c=testScapCatalog()
        assertEquals(112,c.elements.size);assertEquals(112,c.elements.map{it.code}.toSet().size)
        assertEquals(0.6,c.element("104")!!.importanceFactor,0.0);assertEquals(1.0,c.element("110")!!.importanceFactor,0.0)
        assertTrue(c.elements.all{it.name.isNotBlank() && it.source.startsWith("AUXILIAR!")})
        assertEquals(setOf("SUPERESTRUCTURA","SUBESTRUCTURA","DETALLES","CAUCE","ACCESOS"),c.elements.map{it.group}.toSet())
        assertEquals(listOf("DEFINITIVO","PROVISIONALES","ALCANTARILLA","ARTESANALES"),c.options("category"))
        assertTrue("Losa" in c.types("DEFINITIVO"));assertFalse("Losa" in c.types("PROVISIONALES"))
        assertTrue(c.types("NO_EXISTE").isEmpty());assertEquals(141,c.fields.size)
    }
    @Test fun corruptedDuplicateOrMixedCatalogSourcesFailLoading() {
        val path=File("src/main/assets");val e=JSONObject(File(path,"scap_elements.json").readText())
        val options=File(path,"scap_options.json").readText();val fields=File(path,"scap_fields.json").readText()
        e.getJSONArray("elements").put(e.getJSONArray("elements").getJSONObject(0))
        assertTrue(runCatching{ScapCatalog.parse(e.toString(),options,fields)}.isFailure)
        e.getJSONArray("elements").remove(e.getJSONArray("elements").length()-1)
        e.put("sourceSha256","wrong-version")
        assertTrue(runCatching{ScapCatalog.parse(e.toString(),options,fields)}.isFailure)
    }
    @Test fun dimensionalCountsDatesAndConditionalInputsFollowFieldSemantics() {
        val c=testScapCatalog()
        fun error(key:String,value:String,owner:String="inspection",values:Map<String,String> = emptyMap())=
            ScapValidation.fieldError(c.fields.first{it.key==key && it.owner==owner},value,values,c)
        assertNotNull(error("lanes","2.5"));assertNull(error("lanes","2"))
        assertNull(error("totalLengthM","125,50"));assertNotNull(error("totalLengthM","-1"))
        assertNotNull(error("lastInspection","31/02/2026"));assertNull(error("lastInspection","24/09/2026"))
        assertNotNull(error("routeType","VALOR INVENTADO"))
        assertNull(error("requiredLengthM","-1",values=mapOf("acceptableLength" to "Si")))
        assertNotNull(error("requiredLengthM","-1",values=mapOf("acceptableLength" to "No")))
        assertEquals(30298.5,ScapNumbers.chainage("30+298,5")!!,0.0)
        assertNull(ScapNumbers.chainage("30+1000"));assertNull(ScapNumbers.decimal("NaN"))
        val depth=c.fields.first{it.kind=="DIMENSION_RANGE"}
        assertNull(ScapValidation.fieldError(depth,"1.65-2.95m",emptyMap(),c))
        assertNotNull(ScapValidation.fieldError(depth,"2.95-1.65",emptyMap(),c))
    }
}
