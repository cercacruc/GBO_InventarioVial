package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.model.form.Sic23FormState
import org.junit.Assert.*
import org.junit.Test

class Sic23Test {
    @Test fun widthAcceptsDecimalCommaAndRejectsInvalidMeasurements() {
        for (value in listOf("12,50", "12.50", "0", "0.00", " 3.2 "))
            assertNull(value, Sic23FormState(widthM = value).validationError())
        for (value in listOf("", "-1", "NaN", "Infinity", "1.234", "1e3", "abc"))
            assertNotNull(value, Sic23FormState(widthM = value).validationError())
    }

    @Test fun onlyTotalWidthAndCentralMedianRequireWidth() {
        for (type in listOf("1", "2")) assertNotNull(Sic23FormState(typeCode = type).validationError())
        for (type in listOf("3", "4", "5")) assertNull(Sic23FormState(typeCode = type).validationError())
        for (code in listOf("22", "24")) {
            val state = Sic23FormState(widthM = "10.00").selectClass(code)
            assertEquals("", state.typeCode)
            assertEquals("", state.widthM)
            assertNull(state.validationError())
        }
        assertNull(Sic23FormState(classCode = "23", typeCode = "2").validationError())
        assertNotNull(Sic23FormState(classCode = "23", typeCode = "3").validationError())
        assertNotNull(Sic23FormState(classCode = "22", typeCode = "1").validationError())
        assertNotNull(Sic23FormState(classCode = "25").validationError())
        assertEquals("", Sic23FormState(widthM = "10").selectType("3").widthM)
    }

    @Test fun descriptionMatchesManualTextConvention() {
        assertEquals("LINEAS ELECTRICAS CANON 12", Sic23FormState(
            description = "  Líneas eléctricas / cañón #12  ").normalizedDescription())
    }

    @Test fun intervalNeedsBothEndpointsAndValidSide() {
        fun error(start: String = "0005", startDistance: String = "10,50", end: String? = "0005",
                  endDistance: String? = "10,50", side: String? = "S") =
            Sic23FormState.locationError("PE-1N", "CD", start, startDistance, end, endDistance, side)
        assertNull(error()) // A point can have the same start and end.
        assertNull(error(start = "5", end = "6", endDistance = "0"))
        assertNotNull(error(end = null))
        assertNotNull(error(endDistance = null))
        assertNotNull(error(endDistance = "-1"))
        assertNotNull(error(startDistance = "NaN"))
        assertNotNull(error(endDistance = "Infinity"))
        assertNotNull(error(endDistance = "5"))
        assertNotNull(error(start = "10000"))
        assertNotNull(error(side = "X"))
    }

    @Test fun allSic23ClassesHaveAPhotoDestination() {
        for (code in listOf("21", "22", "23", "24"))
            assertEquals("SIB-02", DriveFolderRouter.resolveSib("SIC-23", Sic23FormState().selectClass(code).assetName))
        assertEquals("SIB-08", DriveFolderRouter.resolveSib("SIC-17", "PUENTE"))
        assertEquals("SIB-07", DriveFolderRouter.resolveSib("SIC-22", "VERTICAL"))
    }
}
