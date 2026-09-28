package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.field.SurveyOrder

/** Keep the legacy kilometre rules only when no catalogue exists for the contractual route/roadbed. */
object ContractualPrInput {
    fun catalog(prs: List<RoadPr>, route: String, bed: String) = prs.filter {
        normalizedRoadCode(it.routeCode) == normalizedRoadCode(route) && normalizedRoadCode(it.roadbedCode) == normalizedRoadCode(bed)
    }
    fun valid(pr: String, distance: String, prs: List<RoadPr>, route: String, bed: String): Boolean {
        if (catalog(prs, route, bed).isEmpty()) return SurveyOrder.chainage(pr, distance) != null
        val metres = distance.trim().replace(',', '.').toDoubleOrNull()
        return pr.trim().matches(Regex("[0-9]{1,4}")) && metres != null && metres.isFinite() && metres >= 0
    }
    fun measure(pr: String, distance: String, prs: List<RoadPr>, route: String, bed: String): Double? {
        val scoped = catalog(prs, route, bed)
        if (scoped.isEmpty()) return SurveyOrder.chainage(pr, distance)
        val metres = distance.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return ChainageCalculator(scoped).calculate(route, bed, pr, metres).chainageM
    }
}
