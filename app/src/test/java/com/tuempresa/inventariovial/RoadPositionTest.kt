package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import com.tuempresa.inventariovial.road.*
import org.junit.Assert.*
import org.junit.Test

class RoadPositionTest {
    @Test fun estimatedPrIsExplicitAndNotAnIdentity() {
        assertEquals(RoadPosition(45273.0,PositionSource.ESTIMATED_FROM_PR_CODE),ChainageCalculator().calculate("R","CD","0045",273.0))
        assertEquals(273.5,ChainageCalculator().calculate("R","CD","0",273.5).chainageM!!,0.0)
    }
    @Test fun officialCatalogWinsEvenWhenPrCodeIsNotKilometer() {
        val calculator=ChainageCalculator(testAxis().prs)
        assertEquals(RoadPosition(1234.5,PositionSource.OFFICIAL_PR),calculator.calculate(" r ","cd","10",234.5))
        assertEquals(PositionSource.UNKNOWN,calculator.calculate("R","CD","0045",1.0).source)
        assertEquals(PositionSource.ESTIMATED_FROM_PR_CODE,calculator.calculate("R","UC","0045",1.0).source)
    }
    @Test fun rejectsUnparseableAndNonFinitePositions() {
        for(pr in listOf("","A1","-1","10000","1.2")) assertNull(ChainageCalculator().calculate("R","CD",pr,0.0).chainageM)
        for(distance in listOf(-1.0,Double.NaN,Double.POSITIVE_INFINITY)) assertNull(ChainageCalculator().calculate("R","CD","1",distance).chainageM)
    }
    @Test fun retroactiveInsertAndAnnulmentRenumberWithoutChangingUuidsOrPhotos() {
        val records=listOf(testRecord("A"),testRecord("B",distance=700.0,created=2),testRecord("C",pr="0011",distance=100.0,created=3),testRecord("D",distance=450.0,created=4))
        fun order(items:List<com.tuempresa.inventariovial.data.entity.InventoryRecordEntity>)=RoadOrdering.order(items.map {InventoryRecordWithPhotos(it,emptyList())},ChainageCalculator())
        val initial=order(records)
        assertEquals(listOf("A","D","B","C"),initial.map {it.item.record.id})
        assertEquals(listOf(1,2,3,4),initial.map {it.computedSequence})
        val annulled=order(records.map {if(it.id=="B") it.copy(status="ANNULLED") else it})
        assertEquals(listOf(1,2,null,3),annulled.map {it.computedSequence})
        assertEquals(4,annulled.size)
        assertEquals(listOf("A","D","B","C"),annulled.map {it.item.record.id})
    }
    @Test fun tiesUseCreationThenUuidAndCountersRestartPerRouteAndRoadbed() {
        val records=listOf(testRecord("B",created=2),testRecord("A",created=2),testRecord("C",created=1),
            testRecord("D",roadbed="UC"),testRecord("E",route="S"),testRecord("draft",status="DRAFT"))
        val sorted=RoadOrdering.order(records.map {InventoryRecordWithPhotos(it,emptyList())},ChainageCalculator())
        assertEquals(listOf("C","A","B","D","E"),sorted.map {it.item.record.id})
        assertEquals(listOf(1,2,3,1,1),sorted.map {it.computedSequence})
    }
    @Test fun unknownPrSortsAfterKnownRatherThanAsZero() {
        val rows=listOf(testRecord("unknown",pr="ZZ"),testRecord("known",pr="1")).map {InventoryRecordWithPhotos(it,emptyList())}
        assertEquals("known",RoadOrdering.order(rows,ChainageCalculator()).first().item.record.id)
    }
}
