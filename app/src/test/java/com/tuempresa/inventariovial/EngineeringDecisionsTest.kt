package com.tuempresa.inventariovial

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.runBlocking
import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.validation.CaptureValidation
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class EngineeringDecisionsTest {
    private fun request(detail:SicFormDetail)=InventorySaveRequest("SIC-22","VERTICAL","PE-3N","CD","12","350",null,null,null,
        -12.0,-77.0,1f,"24/09/2026","","",detail,photoPaths=emptyList(),segment="Tramo 1")
    @Test fun newVerticalTypesAreExactlyThreeAndKilometreMarkerIsInformative() {
        assertEquals(setOf("1","2","3"),SicCatalogRepository.newSic22Types)
        for(type in listOf("1","2","3","4","5","6","")) {
            val errors=CaptureValidation.errors(request(SicFormDetail.Sic22(Sic22FormState(typeCode=type))))
            assertEquals(type !in setOf("1","2","3"),errors.any{it.field=="typeCode"})
        }
        assertEquals("3",SicCatalogRepository.kilometerMarkerType)
        assertFalse(Sic22FormState(typeCode=SicCatalogRepository.kilometerMarkerType).usesKilometerPostNumber)
    }
    @Test fun legacyVerticalTypesStillExportIncludingKilometreNumber() {
        for(type in listOf("4","5","6")) {
            val row=InventoryRecordForExport(testRecord().copy(sicCode="SIC-22"),sic22=Sic22Entity("r","17",type,"1",null,"123","1",null,null,null))
            val cells=SicExportFormat.SIC22.values(row)
            assertEquals(type,cells[8]);assertEquals(if(type=="4")"123" else null,cells[11])
        }
    }
    @Test fun legacyKilometrePostCanStillBeReadFromRoom() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),InventoryDatabase::class.java).build()
        try {
            val dao=db.inventoryDao();val record=testRecord().copy(sicCode="SIC-22")
            dao.insertRecord(record)
            dao.insertSic22(Sic22Entity(record.id,"17","4","1",null,"0012","1",null,null,null))
            db.openHelper.readableDatabase.query("SELECT typeCode,kilometerPostNumber FROM sic22_details WHERE recordId=?",arrayOf(record.id)).use{c->
                assertTrue(c.moveToFirst());assertEquals("4",c.getString(0));assertEquals("0012",c.getString(1))
            }
        } finally{db.close()}
    }
    @Test fun circularExportDiscardsSecondDimensionButOvalAndLegacyPreserveIt() {
        for(shape in listOf("CIRCULAR","OVAL",null)) {
            val row=InventoryRecordForExport(testRecord().copy(sicCode="SIC-18"),sic18=Sic18Entity("r","06","1",1,"2",1.2,0.9,"2","3",shape,90.0,80.0))
            val cells=SicExportFormat.SIC18.values(row)
            assertEquals(13,cells.size)
            assertEquals(if(shape=="CIRCULAR")null else 0.9,cells[9]);assertEquals("2",cells[10]);assertEquals("3",cells[11])
        }
    }
    @Test fun horizontalMaterialIsEmptyWhileSecurityMaterialSurvives() {
        for(cls in listOf("18","19","20")) {
            val row=InventoryRecordForExport(testRecord().copy(sicCode="SIC-21"),sic21=Sic21Entity("r",cls,"1","5","1"))
            assertEquals(if(cls=="19")"5" else null,SicExportFormat.SIC21.values(row)[9])
        }
    }
    @Test fun conditionsAreLiteralAndAuxiliaryPercentagesAreAbsentFromSummary() {
        assertEquals(listOf("1 - Bueno","2 - Regular","3 - Malo"),SicCatalogRepository.options("sic18.structural.0"))
        assertEquals("Quebrado o en menos del 30% de la longitud o con ligera 20% deformación.",SicCatalogRepository.sic18StructuralDescriptions["2"])
        val summary=captureSummary(request(SicFormDetail.Sic18(Sic18FormState(dimension1M="1",structuralDamagePercent="20",functionalObstructionPercent="40"))))
        assertFalse(summary.any{it.first.contains("%") || it.first.contains("Porcentaje",true)})
    }
    @Test fun initialRoutesAreCommonUnorderedAndSelectedProgressivePersists() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("survey_context",Context.MODE_PRIVATE).edit().clear().commit()
        val prefs=SurveyPreferences(context)
        val expected=listOf("PE-3N","PE-22A","PE-28C","PE-12A","PE-3NG","PE-28H")
        SurveyPreferences.segments.forEach{assertEquals(expected,prefs.routes(it));assertFalse(prefs.hasConfiguredCatalog(it))}
        val r=request(SicFormDetail.Sic22(Sic22FormState())).copy(routeCode="PE-28H")
        prefs.remember(r)
        assertNull(prefs.validate(r.copy(routeCode="PE-3N")))
        assertNull(prefs.validate(r.copy(routeCode="RUTA-SIN-MAPEO")))
        val reopened=SurveyPreferences(context)
        assertEquals("PE-28H",reopened.last()!!.route);assertEquals("12",reopened.last()!!.pr);assertEquals(350.0,reopened.last()!!.distance,0.0)
        prefs.saveRoutes("Tramo 1",listOf("PE-3N","PE-28H"))
        assertNotNull(prefs.validate(r.copy(routeCode="PE-3N")))
        assertEquals(expected,prefs.routes("Tramo 2"))
    }
}
