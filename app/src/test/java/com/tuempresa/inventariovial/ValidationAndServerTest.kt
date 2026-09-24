package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.location.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.server.*
import com.tuempresa.inventariovial.supplementary.*
import com.tuempresa.inventariovial.validation.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ValidationAndServerTest {
    private fun request()=InventorySaveRequest("SIC-19","DRENAJE","R","CD","0010","600","0020","200","D",
        0.01,0.005,30f,"23/09/2026","","",SicFormDetail.Sic19(Sic19FormState()),photoPaths=emptyList(),
        location=GeoLocation(0.01,0.005,30f,timestamp=1000))
    @Test fun qualityWarningsDoNotBecomeStructuralErrors() {
        val request=request()
        assertTrue(CaptureValidation.errors(request).isEmpty())
        val codes=CaptureValidation.warnings(request,testAxis(),100000,listOf(PhotoDimensions(400,300))).map {it.code}.toSet()
        assertTrue(codes.containsAll(setOf("GPS_ACCURACY","GPS_STALE","MISSING_END_GPS","MISSING_PHOTO","SMALL_PHOTO","DISTANCE_PAST_NEXT_PR","FAR_FROM_AXIS")))
        assertTrue(CaptureValidation.errors(request.copy(startDistanceM="NaN")).isNotEmpty())
        assertTrue(CaptureValidation.errors(request.copy(endDistanceM=null)).isNotEmpty())
    }
    @Test fun missingPrIsFlaggedAndDecreasingIntervalsNeedReview() {
        assertTrue(CaptureValidation.warnings(request().copy(startPrCode="0030"),testAxis(),1000).any {it.code=="PR_OUT_OF_RANGE"})
        assertTrue(CaptureValidation.warnings(request().copy(startDistanceM="900",endDistanceM="0"),testAxis(),1000).any {it.code=="DECREASING_INTERVAL"})
    }
    @Test fun serverPayloadContainsAllRelationsWithoutPhotoBinaryOrLocalPaths() {
        val snapshot=testSnapshot().copy(photos=listOf(com.tuempresa.inventariovial.data.entity.PhotoEntity("p","id",1,"/private/photo.jpg",true,"existing.jpg",null,null,"PENDING",1,stampedPath="/private/stamp.jpg")))
        val payload=ServerPayload.encode(snapshot)
        assertEquals("id",payload.getString("id"));assertEquals(1L,payload.getLong("updatedAt"))
        assertEquals("08",payload.getJSONObject("sic").getJSONObject("SIC-19").getString("classCode"))
        assertTrue(payload.getJSONArray("photos").getJSONObject(0).getBoolean("hasStampedCopy"))
        assertFalse(payload.toString().contains("/private/"));assertFalse(payload.toString().contains("base64"))
        val unknown=ServerPayload.encode(snapshot.copy(record=snapshot.record.copy(gpsAccuracyM=Double.POSITIVE_INFINITY)))
        assertTrue(unknown.getJSONObject("location").isNull("horizontalAccuracyM"))
        assertTrue(ServerConfiguration.validBaseUrl("https://inventory.example/api"))
        for(url in listOf("","http://inventory.example","https://user:secret@inventory.example","https://inventory.example?token=x")) assertFalse(ServerConfiguration.validBaseUrl(url))
    }
    @Test fun supplementaryCatalogRetainsAmbiguitiesWithoutInventingCodes() {
        val fields=SupplementaryForms.fields(SupplementaryFormat.SIC17B)
        assertTrue(fields.any {it.key=="maximumCapacity"});assertTrue(fields.any {it.key=="designLoad"})
        assertEquals(2,fields.single {it.key=="wearingSurface"}.options.count {it.startsWith("3 - ")})
        assertTrue(SupplementaryFormState(SupplementaryFormat.SIC17A,mapOf("latitude" to "91")).validate().isNotEmpty())
        assertTrue(SupplementaryFormState(SupplementaryFormat.SIC18A,mapOf("classCode" to "07","typeCode" to "4")).validate().isNotEmpty())
    }
    @Test fun nmeaChecksChecksumUtcTimeFixAndDoesNotTreatHdopAsAccuracy() {
        val body="GNGGA,120000.00,1200.000,S,07700.000,W,4,18,0.7,123.4,M,0.0,M,1.2,"
        val sentence="\u0024$body*${body.fold(0) {crc,c->crc xor c.code}.toString(16).uppercase().padStart(2,'0')}"
        val now=Instant.parse("2026-09-23T12:00:01Z").toEpochMilli()
        val fix=NmeaParser.parseGga(sentence,now)!!
        assertEquals(-12.0,fix.latitude,0.0);assertEquals(-77.0,fix.longitude,0.0)
        assertTrue(fix.isRtkFixed);assertEquals(GnssProvider.RTK_FIXED,fix.provider)
        assertEquals(18,fix.satellites);assertEquals(0.7,fix.hdop!!,0.0)
        assertTrue(fix.horizontalAccuracy.isInfinite())
        assertNull(NmeaParser.parseGga(sentence.dropLast(2)+"00",now))
        assertNull(NmeaParser.parseGga(sentence,now+100000))
    }
}
