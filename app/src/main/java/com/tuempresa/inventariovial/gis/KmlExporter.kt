package com.tuempresa.inventariovial.gis

import com.tuempresa.inventariovial.data.entity.InventorySnapshot
import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.tracking.TrackContinuity
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** WGS84 KML 2.2; independent of Google Maps, Drive and network access. */
object KmlExporter {
    // legendHref retained for caller compatibility; the compatible variant never emits ScreenOverlay.
    @Suppress("UNUSED_PARAMETER")
    fun render(snapshots: List<InventorySnapshot>, calculator: ChainageCalculator = ChainageCalculator(),
        reference: RoadReferenceData = RoadReferenceData(), legendHref: String? = null, iconHref: String? = null,
        inspectors: Map<String, String> = emptyMap()): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><name>Inventario vial</name>")
        append("<description>${xml("Posición geométrica sobre eje; el PR contractual se conserva. Leyenda: " + KmlPresentation.legend.joinToString("; ") { it.text })}</description>")
        AssetFamily.entries.forEach { family ->
            append("<Style id=\"asset_${family.name}\"><IconStyle><color>${family.kmlColor}</color><scale>0.8</scale>")
            iconHref?.let { append("<Icon><href>${xml(it)}</href></Icon>") }
            append("</IconStyle><LabelStyle><scale>0.8</scale></LabelStyle><LineStyle><color>${family.kmlColor}</color><width>4</width></LineStyle></Style>")
        }
        append("<Style id=\"axis_outline\"><LineStyle><color>${KmlPresentation.OUTLINE_COLOR}</color><width>${KmlPresentation.OUTLINE_WIDTH}</width></LineStyle></Style>")
        append("<Style id=\"axis\"><LineStyle><color>${KmlPresentation.AXIS_COLOR}</color><width>${KmlPresentation.AXIS_WIDTH}</width></LineStyle></Style>")
        listOf("start" to AssetFamily.PUENTE, "end" to AssetFamily.SENALIZACION_VERTICAL).forEach { (id, family) ->
            append("<Style id=\"$id\"><IconStyle><color>${family.kmlColor}</color><scale>1.4</scale>")
            iconHref?.let { append("<Icon><href>${xml(it)}</href></Icon>") }
            append("</IconStyle><LabelStyle><scale>1.1</scale></LabelStyle></Style>")
        }
        append("<Folder><name>LEYENDA</name><open>1</open>")
        KmlPresentation.legend.forEach { entry ->
            // Geometry-free folders are visible in the sidebar without adding fictitious map features.
            append("<Folder><name>${xml(entry.text)}</name><description>${xml(entry.text)}</description><styleUrl>#${entry.styleId}</styleUrl></Folder>")
        }
        append("</Folder>")
        val active = snapshots.filter { it.record.status == "ACTIVE" && it.record.sicCode != "SCAP" }
        val byId = active.associateBy { it.record.id }
        val ordered = RoadOrdering.orderAssetsForRoute(active.map { s ->
            InventoryRecordWithPhotos(s.record, s.photos, s.sic23, s.sic18, s.sic20, s.sic19, s.sic18a)
        }, calculator)
        val routes = (ordered.map { it.routeCode } + reference.segments.map { normalizedRoadCode(it.routeCode) }).distinct().sorted()
        routes.forEach { route ->
            append("<Folder><name>RUTA ${xml(route)}</name><Folder><name>EJE</name>")
            val segments = reference.segments.filter { normalizedRoadCode(it.routeCode) == route }.sortedBy { it.segmentId }
            segments.forEach { segment ->
                val geometry = buildString {
                    append("<LineString><tessellate>1</tessellate><coordinates>")
                    segment.points.sortedBy { it.sequence }.forEach { append("${it.longitude},${it.latitude},0 ") }
                    append("</coordinates></LineString>")
                }
                // The same serialized LineString is reused byte for byte, lower layer first.
                append("<Placemark><name>BORDE EJE ${xml(segment.segmentId)}</name><styleUrl>#axis_outline</styleUrl>$geometry</Placemark>")
                append("<Placemark><name>EJE ${xml(segment.segmentId)}</name><styleUrl>#axis</styleUrl>$geometry</Placemark>")
            }
            append("</Folder><Folder><name>INICIO / FIN</name>")
            segments.forEach { segment ->
                val points = segment.points.sortedBy { it.sequence }
                listOf(Triple("INICIO", "start", points.first()), Triple("FIN", "end", points.last())).forEach { (label, style, p) ->
                    append("<Placemark><name>$label ${xml(route)}</name><description>${xml(segment.segmentId)}</description><styleUrl>#$style</styleUrl><Point><coordinates>${p.longitude},${p.latitude},0</coordinates></Point></Placemark>")
                }
            }
            append("</Folder>")
            listOf("PUENTES", "ALCANTARILLAS", "CUNETAS", "BADENES", "SEÑALIZACIÓN", "OTROS").forEach { folder ->
                append("<Folder><name>${xml(folder)}</name>")
                ordered.filter { it.routeCode == route && it.assetFamily.mapFolder == folder }.forEach { asset ->
                    appendAsset(byId.getValue(asset.recordId), asset, calculator, inspectors)
                }
                append("</Folder>")
            }
            append("</Folder>")
        }
        append("</Document></kml>")
    }

    private fun StringBuilder.appendAsset(snapshot: InventorySnapshot, asset: OrderedAsset,
        calculator: ChainageCalculator, inspectors: Map<String, String>) {
            val r=snapshot.record
            require(GeoMath.validCoordinate(r.latitude,r.longitude)) { "Coordenada inválida: ${r.id}" }
            val position=calculator.calculate(r)
            val condition=snapshot.sic17?.structuralConditionCode ?: snapshot.sic18?.structuralConditionCode ?:
                snapshot.sic19?.structuralConditionCode ?: snapshot.sic20?.structuralConditionCode ?:
                snapshot.sic21?.conditionCode ?: snapshot.sic22?.conditionCode
            append("<Placemark><name>${xml(asset.shortCode)}</name><styleUrl>#asset_${asset.assetFamily.name}</styleUrl><ExtendedData>")
            val fields=linkedMapOf("UUID" to r.id,"SIC" to r.sicCode,"tipoElemento" to r.assetType,"ruta" to r.routeCode,
                "calzada" to r.roadbedCode,"PR" to r.startPrCode,"distanciaDesdePR" to r.startDistanceM.toString(),
                "progresivaM" to position.chainageM?.toString(),"fuenteProgresiva" to position.source.name,
                "lado" to r.sideCode,"latitud" to r.latitude.toString(),"longitud" to r.longitude.toString(),
                "condicion" to condition,"fecha" to r.surveyDate,"origenUbicacion" to r.locationSource)
            fields.putAll(linkedMapOf("shortCode" to asset.shortCode, "assetFamily" to asset.assetFamily.name,
                "SIB" to asset.sibCode.orEmpty(), "routeCode" to r.routeCode, "roadbedCode" to r.roadbedCode,
                "manualPrCode" to r.startPrCode, "manualDistanceM" to r.startDistanceM.toString(),
                "manualEndPrCode" to r.endPrCode.orEmpty(), "manualEndDistanceM" to r.endDistanceM?.toString().orEmpty(),
                "startPrSource" to r.locationSource, "endPrSource" to r.endLocationSource,
                "axisMeasureM" to asset.axisMeasureM?.toString().orEmpty(), "distanceToRoadAxisM" to r.distanceToRoadAxisM?.toString().orEmpty(),
                "roadMatchConfidence" to r.roadMatchConfidence?.toString().orEmpty(), "matchedSegmentId" to r.matchedSegmentId.orEmpty(),
                "roadMatchSource" to (if (asset.axisMeasureM != null) "GNSS_MAP_MATCH" else ""), "orderSource" to asset.orderSource.name,
                "latitude" to r.latitude.toString(), "longitude" to r.longitude.toString(),
                "projectedLatitude" to r.projectedLatitude?.toString().orEmpty(), "projectedLongitude" to r.projectedLongitude?.toString().orEmpty(),
                "gpsAccuracyM" to r.gpsAccuracyM?.toString().orEmpty(), "surveyDate" to r.surveyDate,
                "inspector" to inspectors[r.sessionId].orEmpty(), "presentationWarnings" to asset.warnings.joinToString("; ")))
            val linear=com.tuempresa.inventariovial.validation.requiresEndLocation(r.sicCode,r.assetType,snapshot.sic20?.classCode,snapshot.sic23?.classCode)
            fields["observaciones"]=r.observations
            fields["condicionFuncional"]=snapshot.sic17?.functionalConditionCode ?: snapshot.sic18?.functionalConditionCode ?:
                snapshot.sic19?.functionalConditionCode ?: snapshot.sic20?.functionalConditionCode
            // Use the contractual projection for attributes as well as XLSX (same null/material/dimension rules).
            val format=com.tuempresa.inventariovial.export.SicExportFormat.entries.find {it.code==r.sicCode}
            val item=com.tuempresa.inventariovial.data.entity.InventoryRecordForExport(r,snapshot.sic17,snapshot.sic18,snapshot.sic19,snapshot.sic20,snapshot.sic21,snapshot.sic22,snapshot.sic23)
            val hasDetail=when(r.sicCode) {"SIC-17"->snapshot.sic17!=null;"SIC-18"->snapshot.sic18!=null;"SIC-19"->snapshot.sic19!=null;"SIC-20"->snapshot.sic20!=null;"SIC-21"->snapshot.sic21!=null;"SIC-22"->snapshot.sic22!=null;"SIC-23"->snapshot.sic23!=null;else->false}
            if(format!=null && hasDetail) format.values(item).forEachIndexed {i,value->fields["SIC_${i+1}_${format.columns[i].label}"]=value?.toString()}
            snapshot.photos.sortedBy {it.photoIndex}.forEachIndexed {i,p->
                fields["foto_${i+1}_UUID"]=p.id
                fields["foto_${i+1}_categoria"]=p.photoCategory
                fields["foto_${i+1}_descripcion"]=p.description
                fields["foto_${i+1}_DriveId"]=p.driveFileId
            }
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
    fun writeKmz(output: OutputStream, kml: String, resources: Map<String, ByteArray> = emptyMap()) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("doc.kml"));zip.write(kml.toByteArray(Charsets.UTF_8));zip.closeEntry()
            resources.toSortedMap().forEach { (name, bytes) ->
                require(name !in setOf("doc.kml", "..") && !name.contains("..") && !name.startsWith("/") && !name.contains('\\'))
                zip.putNextEntry(ZipEntry(name));zip.write(bytes);zip.closeEntry()
            }
        }
    }
    private fun xml(value: String) = value.filter { it=='\n' || it=='\r' || it=='\t' || it>=' ' }
        .replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
}
