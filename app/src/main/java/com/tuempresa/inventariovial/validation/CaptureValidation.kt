package com.tuempresa.inventariovial.validation

import com.tuempresa.inventariovial.model.form.InventorySaveRequest
import com.tuempresa.inventariovial.model.form.SicFormDetail
import com.tuempresa.inventariovial.road.*

data class ValidationWarning(val code: String,val message: String)
data class ValidationError(val field: String,val message: String)
data class QualityConfig(val maxAccuracyM: Double = 1.0,val maxAgeMs: Long = 30_000,val minPhotoEdgePx: Int = 800)
data class PhotoDimensions(val width: Int,val height: Int)

object CaptureValidation {
    fun errors(request: InventorySaveRequest): List<ValidationError> = buildList {
        (request.detail as? SicFormDetail.Sic22)?.let {
            if(it.state.typeCode !in com.tuempresa.inventariovial.catalog.SicCatalogRepository.newSic22Types)
                add(ValidationError("typeCode","Nueva señal SIC-22: selecciona Reglamento, Preventiva o Informativa. El hito kilométrico es Informativa."))
        }
        if(request.routeCode.isBlank()) add(ValidationError("routeCode","Ruta obligatoria."))
        if(request.roadbedCode.isBlank()) add(ValidationError("roadbedCode","Calzada obligatoria."))
        if(!request.startPrCode.trim().matches(Regex("[0-9]{1,4}"))) add(ValidationError("startPrCode","PR inicial: de 1 a 4 dígitos."))
        fun distance(value: String?) = value?.trim()?.replace(',','.')?.toDoubleOrNull()?.let { it.isFinite() && it>=0 }==true
        if(!distance(request.startDistanceM)) add(ValidationError("startDistanceM","Distancia inicial inválida."))
        if(request.endPrCode.isNullOrBlank()!=request.endDistanceM.isNullOrBlank()) add(ValidationError("endPrCode","Completa ambos campos de ubicación final."))
        if(!request.endPrCode.isNullOrBlank() && (!request.endPrCode.trim().matches(Regex("[0-9]{1,4}")) || !distance(request.endDistanceM))) add(ValidationError("endDistanceM","Ubicación final inválida."))
        if(!GeoMath.validCoordinate(request.latitude,request.longitude)) add(ValidationError("location","Coordenadas inválidas."))
        if((request.endLatitude==null)!=(request.endLongitude==null) || request.endLatitude?.let { !GeoMath.validCoordinate(it,request.endLongitude ?: Double.NaN) }==true) add(ValidationError("endLocation","Coordenadas finales inválidas."))
        if(request.sideCode!=null && request.sideCode !in setOf("D","I","S")) add(ValidationError("side","Lado inválido."))
        if(request.detail is SicFormDetail.Sic18) with(request.detail.state) {
            fun positive(value: String) = value.replace(',','.').toDoubleOrNull()?.let {it.isFinite() && it>0} == true
            if(!positive(dimension1M)) add(ValidationError("dimension1M","Ancho o diámetro debe ser mayor que cero."))
            if(usesDimension2 && !positive(dimension2M)) add(ValidationError("dimension2M","Altura debe ser mayor que cero."))
            if(spans.toIntOrNull()?.let {it>0} != true) add(ValidationError("spans","Ingresa al menos un ojo / vano."))
        }
    }
    fun warnings(request: InventorySaveRequest, data: RoadReferenceData, now: Long,
        photos: List<PhotoDimensions> = emptyList(),quality: QualityConfig = QualityConfig(),matchConfig: MatchConfig = MatchConfig()): List<ValidationWarning> = buildList {
        val accuracy=request.location?.horizontalAccuracy ?: request.gpsAccuracyM
        if(accuracy==null || !accuracy.isFinite() || accuracy>quality.maxAccuracyM)
            add(ValidationWarning("GPS_ACCURACY","Precisión GNSS insuficiente o desconocida; umbral ${quality.maxAccuracyM} m."))
        val timestamp=request.location?.timestamp
        if(timestamp==null || now-timestamp !in 0..quality.maxAgeMs) add(ValidationWarning("GPS_STALE","La posición GNSS es antigua o no tiene hora válida."))
        if(requiresEndLocation(request) && (request.endLatitude==null || request.endLongitude==null)) add(ValidationWarning("MISSING_END_GPS","Falta GPS final del elemento lineal."))
        if(request.photoPaths.isEmpty()) add(ValidationWarning("MISSING_PHOTO","El registro no tiene fotografías."))
        if(photos.any { minOf(it.width,it.height)<quality.minPhotoEdgePx }) add(ValidationWarning("SMALL_PHOTO","Una fotografía tiene resolución pequeña o no puede leerse."))
        // New captures explicitly use kilometre + offset; legacy requests can still use official PR identifiers.
        val prs=if(request.segment.isNotBlank()) emptyList() else data.prs.filter { normalizedRoadCode(it.routeCode)==normalizedRoadCode(request.routeCode) && normalizedRoadCode(it.roadbedCode)==normalizedRoadCode(request.roadbedCode) }
        val pr=prs.find { normalizedPr(it.prCode)==normalizedPr(request.startPrCode) }
        if(prs.isNotEmpty() && pr==null) add(ValidationWarning("PR_OUT_OF_RANGE","El PR no está en el catálogo de esta ruta y calzada."))
        val distance=request.startDistanceM.replace(',','.').toDoubleOrNull()
        val endDistance=request.endDistanceM?.replace(',','.')?.toDoubleOrNull()
        if(distance!=null && endDistance!=null && !request.endPrCode.isNullOrBlank()) {
            val calculator=ChainageCalculator(if(request.segment.isNotBlank()) emptyList() else data.prs)
            val start=calculator.calculate(request.routeCode,request.roadbedCode,request.startPrCode,distance).chainageM
            val end=calculator.calculate(request.routeCode,request.roadbedCode,request.endPrCode,endDistance).chainageM
            if(start!=null && end!=null && end<start && request.direction!="DECREASING") add(ValidationWarning("DECREASING_INTERVAL","La ubicación final es anterior a la inicial. Confirma el sentido decreciente del recorrido."))
            if(prs.isNotEmpty() && prs.none { normalizedPr(it.prCode)==normalizedPr(request.endPrCode) }) add(ValidationWarning("END_PR_OUT_OF_RANGE","El PR final no está en el catálogo de esta ruta y calzada."))
        }
        if(pr!=null && distance!=null) {
            val next=prs.filter { it.chainageM>pr.chainageM }.minByOrNull { it.chainageM }
            if(next!=null && pr.chainageM+distance>next.chainageM) add(ValidationWarning("DISTANCE_PAST_NEXT_PR","La distancia supera el PR siguiente del catálogo."))
        }
        val endPr=prs.find { normalizedPr(it.prCode)==request.endPrCode?.let(::normalizedPr) }
        if(endPr!=null && endDistance!=null) {
            val next=prs.filter { it.chainageM>endPr.chainageM }.minByOrNull { it.chainageM }
            if(next!=null && endPr.chainageM+endDistance>next.chainageM) add(ValidationWarning("END_DISTANCE_PAST_NEXT_PR","La distancia final supera el PR siguiente del catálogo."))
        }
        if(data.segments.isNotEmpty()) {
            val broad=RoadMatcher(data,matchConfig.copy(maxDistanceToAxisM=Double.MAX_VALUE,maxAccuracyM=Double.MAX_VALUE))
                .match(request.latitude,request.longitude,0.0)
            if(broad!=null && broad.distanceToRoadAxisM>matchConfig.maxDistanceToAxisM) add(ValidationWarning("FAR_FROM_AXIS","Punto a ${"%.1f".format(broad.distanceToRoadAxisM)} m del eje vial."))
        }
    }
}
