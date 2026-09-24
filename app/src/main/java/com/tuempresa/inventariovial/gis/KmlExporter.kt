package com.tuempresa.inventariovial.gis

import com.tuempresa.inventariovial.data.entity.InventorySnapshot
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.tracking.TrackContinuity
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** WGS84 KML 2.2; independent of Google Maps, Drive and network access. */
object KmlExporter {
    fun render(snapshots: List<InventorySnapshot>, calculator: ChainageCalculator = ChainageCalculator()): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><name>Inventario vial</name>")
        snapshots.filter { it.record.status=="ACTIVE" }.forEach { snapshot ->
            val r=snapshot.record
            require(GeoMath.validCoordinate(r.latitude,r.longitude)) { "Coordenada inválida: ${r.id}" }
            val position=calculator.calculate(r)
            val condition=snapshot.sic17?.structuralConditionCode ?: snapshot.sic18?.structuralConditionCode ?:
                snapshot.sic19?.structuralConditionCode ?: snapshot.sic20?.structuralConditionCode ?:
                snapshot.sic21?.conditionCode ?: snapshot.sic22?.conditionCode
            append("<Placemark><name>${xml(r.sicCode+" · "+r.assetType+" · "+r.routeCode)}</name><ExtendedData>")
            val fields=linkedMapOf("UUID" to r.id,"SIC" to r.sicCode,"tipoElemento" to r.assetType,"ruta" to r.routeCode,
                "calzada" to r.roadbedCode,"PR" to r.startPrCode,"distanciaDesdePR" to r.startDistanceM.toString(),
                "progresivaM" to position.chainageM?.toString(),"fuenteProgresiva" to position.source.name,
                "lado" to r.sideCode,"latitud" to r.latitude.toString(),"longitud" to r.longitude.toString(),
                "condicion" to condition,"fecha" to r.surveyDate,"origenUbicacion" to r.locationSource)
            val linear=r.sicCode in setOf("SIC-19","SIC-21") || r.assetType in setOf("MURO","TUNEL") ||
                (r.sicCode=="SIC-20" && snapshot.sic20?.classCode in setOf("13","14")) ||
                (r.sicCode=="SIC-23" && snapshot.sic23?.classCode !in setOf("23","24"))
            val track=snapshot.track.sortedBy { it.sequence }
            val geometries=when {
                linear && track.size>=2 -> TrackContinuity.runs(track).map { run -> run.map { Triple(it.latitude,it.longitude,it.altitude) } }
                linear && r.endLatitude!=null && r.endLongitude!=null -> listOf(listOf(Triple(r.latitude,r.longitude,r.altitudeM),Triple(r.endLatitude,r.endLongitude,null)))
                else -> listOf(listOf(Triple(r.latitude,r.longitude,r.altitudeM)))
            }
            fields["geometria"]=if(geometries.size>1) "RECORRIDO_GNSS_CON_INTERRUPCIONES" else if(linear && geometries.single().size<2) "PUNTO_INCOMPLETO_SIN_GPS_FINAL" else if(track.size>=2 && linear) "RECORRIDO_GNSS" else if(linear) "ESTIMACION_INICIO_FIN" else "PUNTO"
            fields.forEach { (key,value) ->
                if(value!=null) append("<Data name=\"${xml(key)}\"><value>${xml(value)}</value></Data>")
            }
            append("</ExtendedData>")
            if(geometries.size>1) append("<MultiGeometry>")
            geometries.forEach { coordinates ->
                val tag=if(linear && coordinates.size>=2) "LineString" else "Point"
                append("<$tag><altitudeMode>clampToGround</altitudeMode><coordinates>")
                coordinates.forEach { (lat,lon,alt) ->
                    require(GeoMath.validCoordinate(lat,lon)) { "Coordenada de recorrido inválida: ${r.id}" }
                    append("$lon,$lat,${alt?.takeIf { it.isFinite() } ?: 0.0} ")
                }
                append("</coordinates></$tag>")
            }
            if(geometries.size>1) append("</MultiGeometry>")
            append("</Placemark>")
        }
        append("</Document></kml>")
    }
    fun writeKmz(output: OutputStream, kml: String) {
        ZipOutputStream(output).use { zip -> zip.putNextEntry(ZipEntry("doc.kml"));zip.write(kml.toByteArray(Charsets.UTF_8));zip.closeEntry() }
    }
    private fun xml(value: String) = value.filter { it=='\n' || it=='\r' || it=='\t' || it>=' ' }
        .replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
}
