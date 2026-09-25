package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import com.tuempresa.inventariovial.export.ObservationReport
import com.tuempresa.inventariovial.validation.CaptureValidation
import org.junit.Assert.*
import org.junit.Test

class SurveyCorrectionsTest {
    private fun position(pr: String="12",distance: Double=350.0,direction:String="INCREASING",route:String="R1") =
        SurveyPosition("Tramo 1",route,"CD",pr,distance,direction)
    private fun request(detail:SicFormDetail)=InventorySaveRequest("SIC-18","ALCANTARILLA","R1","CD","12","350",null,null,null,
        -12.0,-77.0,1f,"24/09/2026","Revisar","",detail,photoPaths=emptyList(),segment="Tramo 1")

    @Test fun increasingAndDecreasingSurveysRejectOnlyMovementAgainstChosenDirection() {
        assertNull(SurveyOrder.error(position(),position(distance=400.0),emptyList()))
        assertNotNull(SurveyOrder.error(position(),position(distance=300.0),emptyList()))
        assertNull(SurveyOrder.error(position(direction="DECREASING"),position(distance=300.0,direction="DECREASING"),emptyList()))
        assertNotNull(SurveyOrder.error(position(direction="DECREASING"),position(distance=400.0,direction="DECREASING"),emptyList()))
        assertNull(SurveyOrder.error(position(),position(),emptyList())) // Several assets at one station are valid.
    }
    @Test fun kilometreBoundaryDoesNotBehaveLikeARecordCounter() {
        assertNull(SurveyOrder.error(position(distance=999.0),position(pr="13",distance=0.0),emptyList()))
        assertEquals(12350.5,SurveyOrder.chainage("0012","350,50")!!,0.0)
        for(value in listOf("1000","-1","NaN","Infinity")) assertNull(SurveyOrder.chainage("12",value))
    }
    @Test fun routeOrderRequiresExplicitRestartForAnEarlierRouteButAllowsBothKilometreDirections() {
        assertNotNull(SurveyOrder.error(position(route="R2"),position(route="R1"),listOf("R1","R2")))
        assertNull(SurveyOrder.error(position(route="R1"),position(route="R2",direction="DECREASING"),listOf("R1","R2")))
        assertNull(SurveyOrder.error(null,position(route="R1"),listOf("R1","R2")))
        assertNull(SurveyOrder.error(position(route="R2"),position(route="R1").copy(sessionId="new"),listOf("R1","R2")))
    }
    @Test fun circularCulvertsNeedOneDimensionAndOvalCulvertsNeedTwo() {
        val circular=Sic18FormState(dimension1M="1.2")
        assertFalse(circular.usesDimension2)
        assertTrue(CaptureValidation.errors(request(SicFormDetail.Sic18(circular))).isEmpty())
        val oval=circular.copy(sectionShape="OVAL")
        assertTrue(oval.usesDimension2)
        assertTrue(CaptureValidation.errors(request(SicFormDetail.Sic18(oval))).any {it.field=="dimension2M"})
        assertTrue(CaptureValidation.errors(request(SicFormDetail.Sic18(oval.copy(dimension2M="0.9")))).isEmpty())
        assertTrue(circular.copy(crossSectionCode="1").usesDimension2)
    }
    @Test fun historicalAuxiliaryPercentagesDoNotControlNewCulvertValidation() {
        val valid=Sic18FormState(dimension1M="1",structuralDamagePercent="0",functionalObstructionPercent="100")
        assertTrue(CaptureValidation.errors(request(SicFormDetail.Sic18(valid))).isEmpty())
        assertTrue(CaptureValidation.errors(request(SicFormDetail.Sic18(valid.copy(structuralDamagePercent="101")))).isEmpty())
        assertTrue(CaptureValidation.errors(request(SicFormDetail.Sic18(valid.copy(functionalObstructionPercent="NaN")))).isEmpty())
        assertTrue(CaptureValidation.errors(request(SicFormDetail.Sic18(valid.copy(structuralDamagePercent="",functionalObstructionPercent="")))).isEmpty())
    }
    @Test fun reviewHasDescriptionsAndOmitsRemovedFields() {
        val vertical=captureSummary(request(SicFormDetail.Sic22(Sic22FormState(signalCode="P-2B")))).toMap()
        assertEquals("2 - Preventiva",vertical["Tipo"])
        assertFalse(vertical.keys.any {it.contains("Ancho") || it.contains("Altura")})
        val horizontal=captureSummary(request(SicFormDetail.Sic21(Sic21FormState()))).toMap()
        assertFalse(horizontal.containsKey("Material"))
        assertEquals(3,SicCatalogRepository.options("sic22.type.0").size)
    }
    @Test fun separateReportPreservesLiteralObservationsAndExcludesAnnulledRecords() {
        val active=testRecord().copy(observations="<script>alert('x')</script> & revisar",segment="Tramo 1")
        val hidden=testRecord(id="hidden",status="ANNULLED").copy(observations="NO PUBLICAR")
        val html=ObservationReport.render(listOf(active,hidden))
        assertFalse(html.contains("<script>"));assertTrue(html.contains("&lt;script&gt;"))
        assertFalse(html.contains("NO PUBLICAR"));assertTrue(html.contains("Tramo 1"))
    }
}
