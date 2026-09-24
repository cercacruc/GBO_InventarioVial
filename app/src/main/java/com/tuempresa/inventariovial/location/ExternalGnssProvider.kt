package com.tuempresa.inventariovial.location

import java.time.*
import kotlin.math.abs

/** A Bluetooth transport feeds complete sentences here; no vendor SDK or Bluetooth pairing is assumed. */
interface ExternalGnssProvider : LocationProvider {
    fun receiveNmea(sentence: String, receivedAt: Long = System.currentTimeMillis()): Boolean
}

class NmeaExternalGnssProvider(private val clock: () -> Long = System::currentTimeMillis) : ExternalGnssProvider {
    @Volatile private var latest: GeoLocation? = null
    override fun receiveNmea(sentence: String, receivedAt: Long): Boolean {
        val parsed=NmeaParser.parseGga(sentence,receivedAt) ?: return false
        latest=parsed
        return true
    }
    override fun getCurrentLocation(onSuccess: (GeoLocation)->Unit,onError: (String)->Unit) {
        val fix=latest
        if(fix==null || clock()-fix.timestamp !in 0..15_000) onError("No hay una posición GNSS externa reciente.") else onSuccess(fix)
    }
}

object NmeaParser {
    fun parseGga(sentence: String, receivedAt: Long): GeoLocation? = runCatching {
        val s=sentence.trim()
        require(s.startsWith('$') && s.contains('*'))
        val body=s.substring(1,s.indexOf('*'))
        require(body.fold(0) { crc,c->crc xor c.code } == s.substringAfter('*').toInt(16))
        val f=body.split(','); require(f[0].endsWith("GGA") && f.size>=15)
        val quality=f[6].toInt(); require(quality in setOf(1,2,4,5))
        fun coordinate(value: String,hemisphere: String,latitude: Boolean): Double {
            require(hemisphere in if(latitude) setOf("N","S") else setOf("E","W"))
            val n=value.toDouble(); val deg=(n/100).toInt(); val min=n-deg*100
            require(min in 0.0..<60.0)
            return (deg+min/60)*if(hemisphere in setOf("S","W")) -1 else 1
        }
        val lat=coordinate(f[2],f[3],true); val lon=coordinate(f[4],f[5],false)
        require(com.tuempresa.inventariovial.road.GeoMath.validCoordinate(lat,lon))
        val utc=Instant.ofEpochMilli(receivedAt).atZone(ZoneOffset.UTC)
        val time=f[1]; val seconds=time.substring(4).toDouble()
        require(seconds>=0 && seconds<60)
        val local=LocalTime.of(time.substring(0,2).toInt(),time.substring(2,4).toInt(),seconds.toInt(),((seconds%1)*1e9).toInt())
        val timestamp=(-1L..1L).map { utc.toLocalDate().plusDays(it).atTime(local).toInstant(ZoneOffset.UTC).toEpochMilli() }
            .minBy { abs(it-receivedAt) }
        require(abs(receivedAt-timestamp)<=15_000)
        val hdop=f[8].toDoubleOrNull()?.takeIf { it.isFinite() && it>=0 }
        // HDOP is not accuracy in meters. Unknown accuracy must never masquerade as an RTK precision.
        GeoLocation(lat,lon,Float.POSITIVE_INFINITY,
            altitude=f[9].toDoubleOrNull()?.takeIf { f[10]=="M" && it.isFinite() },source="NMEA_GGA",timestamp=timestamp,
            provider=when(quality) {4->GnssProvider.RTK_FIXED;5->GnssProvider.RTK_FLOAT;else->GnssProvider.EXTERNAL_GNSS},
            fixType=quality.toString(),satellites=f[7].toIntOrNull(),hdop=hdop,
            correctionAge=f[13].toDoubleOrNull(),isRtkFixed=quality==4)
    }.getOrNull()
}
