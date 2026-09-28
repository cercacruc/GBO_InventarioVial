package com.tuempresa.inventariovial.road

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object PrEntrySource {
    const val MANUAL = "MANUAL"
    const val CONFIRMED = "GNSS_PR_SUGGESTION_CONFIRMED"
}

data class PrSuggestion(val suggestedPrCode: String, val suggestedDistanceFromPrM: Double,
    val routeCode: String, val roadbedCode: String, val match: RoadMatchResult) {
    val distanceText: String get() = BigDecimal.valueOf(suggestedDistanceFromPrM)
        .setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

data class PrSuggestionResult(val suggestion: PrSuggestion? = null, val message: String? = null)

/** A PR code is an identifier, never an inferred kilometre. Contractual roadbed stays independent of EJE. */
class PrSuggestionEngine(private val reference: RoadReferenceData, private val config: MatchConfig = MatchConfig(),
    private val chainageToleranceM: Double = 5.0) {
    fun suggest(routeCode: String, roadbedCode: String, latitude: Double, longitude: Double, accuracy: Double?): PrSuggestionResult {
        if (reference.prs.isEmpty()) return PrSuggestionResult()
        val route = normalizedRoadCode(routeCode)
        val bed = normalizedRoadCode(roadbedCode)
        val prs = reference.prs.filter { normalizedRoadCode(it.routeCode) == route && normalizedRoadCode(it.roadbedCode) == bed }
        if (route.isBlank() || bed.isBlank() || prs.isEmpty()) return PrSuggestionResult(message = "Sin catálogo PR para esta ruta/calzada.")
        val segments = reference.forSelectedRouteAndRoadbed(route, bed).segments
        if (segments.isEmpty()) return PrSuggestionResult(message = "Sin eje compatible para validar los PR.")
        // Empty prs avoids treating an unvalidated catalogue as an official suggestion inside RoadMatcher.
        val axis = reference.copy(segments = segments, prs = emptyList())
        val engine = LinearReferenceEngine(axis, config)
        val match = accuracy?.let { engine.locate(latitude, longitude, it) }
            ?: return PrSuggestionResult(message = "Posición sobre eje no determinada.")
        if (prs.map { normalizedPr(it.prCode) }.distinct().size != prs.size)
            return PrSuggestionResult(message = "Catálogo PR duplicado; revisa la referencia.")
        val projected = prs.map { pr ->
            val projection = engine.locate(pr.latitude, pr.longitude, 0.0)
            if (!pr.prCode.matches(Regex("[0-9]{1,4}")) || !pr.chainageM.isFinite() || projection == null ||
                abs(projection.chainageM - pr.chainageM) > chainageToleranceM)
                return PrSuggestionResult(message = "PR ${pr.prCode} no validado sobre el eje; revisa su coordenada y chainageM.")
            pr to projection
        }
        // Do not cross reference segments that can have independent distance origins.
        val sameSegment = projected.filter { it.second.segmentId == match.segmentId }.map { it.first }
        val previous = LinearReferenceEngine.previousPr(sameSegment, route, bed, match.chainageM)
            ?: return PrSuggestionResult(message = "No existe PR anterior en este segmento de ruta/calzada.")
        if (sameSegment.count { it.chainageM == previous.chainageM } > 1)
            return PrSuggestionResult(message = "PR anterior ambiguo; revisa el catálogo.")
        val distance = match.chainageM - previous.chainageM
        val matchedPr = match.copy(prCode = normalizedPr(previous.prCode), distanceFromPrM = distance)
        return PrSuggestionResult(PrSuggestion(normalizedPr(previous.prCode), distance, route, bed, matchedPr))
    }
}

/** Immutable input: suggestion calculation cannot replace manual values. Only confirm() applies it. */
data class PrEntry(val prCode: String, val distanceM: String, val source: String = PrEntrySource.MANUAL) {
    fun confirm(suggestion: PrSuggestion) = copy(prCode = suggestion.suggestedPrCode,
        distanceM = suggestion.distanceText, source = PrEntrySource.CONFIRMED)
}
