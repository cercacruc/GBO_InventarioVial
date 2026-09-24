package com.tuempresa.inventariovial.field

import android.content.Context
import com.tuempresa.inventariovial.model.form.InventorySaveRequest
import org.json.JSONArray
import org.json.JSONObject

data class SurveyPosition(val segment: String, val route: String, val roadbed: String,
    val pr: String, val distance: Double, val direction: String, val sessionId: String? = null)

/** A kilometre and its offset form a measured position, not a record counter. */
object SurveyOrder {
    fun kilometreAndOffset(chainage: Double): Pair<String, String> {
        require(chainage.isFinite() && chainage >= 0 && chainage < 10_000_000)
        val centimetres = kotlin.math.round(chainage * 100).toLong()
        return (centimetres / 100_000).toString().padStart(4,'0') to
            String.format(java.util.Locale.US,"%.2f",(centimetres % 100_000) / 100.0)
    }
    fun chainage(pr: String, distance: String): Double? {
        val km = pr.trim().takeIf { it.matches(Regex("[0-9]{1,4}")) }?.toIntOrNull() ?: return null
        val metres = distance.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return if (metres.isFinite() && metres >= 0 && metres < 1000) km * 1000.0 + metres else null
    }

    fun error(previous: SurveyPosition?, current: SurveyPosition, orderedRoutes: List<String>): String? {
        if (previous == null || previous.sessionId != current.sessionId) return null
        if (previous.segment != current.segment) return null
        if (previous.route != current.route) {
            val from = orderedRoutes.indexOf(previous.route)
            val to = orderedRoutes.indexOf(current.route)
            if (from >= 0 && to >= 0 && to < from) return "La ruta ya fue recorrida. Inicia un nuevo recorrido para regresar."
            return null
        }
        if (previous.roadbed != current.roadbed) return null
        if (previous.direction != current.direction) return "Para cambiar de sentido inicia un nuevo recorrido."
        val before = chainage(previous.pr, previous.distance.toString()) ?: return null
        val after = chainage(current.pr, current.distance.toString()) ?: return null
        if (current.direction == "INCREASING" && after < before || current.direction == "DECREASING" && after > before)
            return "La progresiva contradice el sentido del recorrido. Corrige la ubicación o inicia otro recorrido."
        return null
    }
}

class SurveyPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("survey_context", Context.MODE_PRIVATE)
    fun routes(segment: String): List<String> = runCatching {
        val array = JSONArray(prefs.getString("routes_$segment", "[]"))
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())

    fun saveRoutes(segment: String, routes: List<String>) {
        require(segment in segments)
        val normalized = routes.map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        require(normalized.distinct().size == normalized.size) { "Hay códigos de ruta repetidos." }
        require(normalized.all { it.length <= 30 && it.matches(Regex("[A-Z0-9][A-Z0-9._ -]*")) }) { "Revisa los códigos de ruta." }
        prefs.edit().putString("routes_$segment", JSONArray(normalized).toString()).apply()
    }

    fun last(): SurveyPosition? = runCatching {
        val obj = JSONObject(prefs.getString("last", null) ?: return null)
        SurveyPosition(obj.getString("segment"),obj.getString("route"),obj.getString("roadbed"),
            obj.getString("pr"),obj.getDouble("distance"),obj.getString("direction"),
            obj.optString("sessionId").takeIf { it.isNotBlank() })
    }.getOrNull()

    fun reset() { prefs.edit().remove("last").apply() }

    fun validate(request: InventorySaveRequest): String? {
        if (request.segment !in segments) return "Selecciona uno de los tres tramos."
        if (request.direction !in listOf("INCREASING", "DECREASING")) return "Selecciona el sentido del recorrido."
        val routes = routes(request.segment)
        if (routes.isNotEmpty() && request.routeCode.trim().uppercase() !in routes) return "La ruta no pertenece al catálogo del tramo."
        val start = SurveyOrder.chainage(request.startPrCode, request.startDistanceM)
            ?: return "Progresiva inicial: kilómetro entero de hasta cuatro dígitos y metros desde 0 hasta menos de 1000."
        if (!request.endPrCode.isNullOrBlank()) {
            val end = SurveyOrder.chainage(request.endPrCode,request.endDistanceM.orEmpty()) ?: return "Progresiva final inválida."
            if (request.direction == "INCREASING" && end < start || request.direction == "DECREASING" && end > start)
                return "La progresiva final no coincide con el sentido seleccionado."
        }
        return SurveyOrder.error(last(), position(request),routes)
    }

    private fun position(r: InventorySaveRequest) = SurveyPosition(r.segment,r.routeCode.trim().uppercase(),
        r.roadbedCode.trim().uppercase(),r.startPrCode.trim(),r.startDistanceM.replace(',','.').toDouble(),r.direction,r.sessionId)

    fun remember(request: InventorySaveRequest) {
        val p = position(request)
        prefs.edit().putString("last",JSONObject().put("segment",p.segment).put("route",p.route)
            .put("roadbed",p.roadbed).put("pr",p.pr).put("distance",p.distance).put("direction",p.direction)
            .put("sessionId",p.sessionId.orEmpty()).toString()).apply()
    }

    companion object { val segments = listOf("Tramo 1", "Tramo 2", "Tramo 3") }
}
