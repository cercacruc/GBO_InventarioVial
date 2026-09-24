package com.tuempresa.inventariovial.road

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Importers for KML/KMZ can implement this contract without changing the matching engine. */
interface RoadReferenceImporter { fun parse(text: String): RoadReferenceData }

class JsonRoadReferenceImporter : RoadReferenceImporter {
    override fun parse(text: String): RoadReferenceData {
        val root = JSONObject(text)
        val segments = mutableListOf<RoadSegment>()
        val prs = mutableListOf<RoadPr>()
        if(root.optString("type")=="FeatureCollection") {
            root.getJSONArray("features").objects().forEach { feature ->
                val p = feature.getJSONObject("properties"); val g = feature.getJSONObject("geometry")
                val route = normalizedRoadCode(p.getString("routeCode")); val roadbed = normalizedRoadCode(p.optString("roadbedCode"))
                when(g.getString("type")) {
                    "LineString" -> {
                        val id=p.getString("segmentId"); val coordinates=g.getJSONArray("coordinates")
                        val chainages=p.getJSONArray("chainageM")
                        require(chainages.length()==coordinates.length()) { "Cada vértice requiere progresiva oficial chainageM." }
                        val points=(0 until coordinates.length()).map { i ->
                            val xy=coordinates.getJSONArray(i)
                            RoadPolylinePoint(route,roadbed,id,i,xy.getDouble(1),xy.getDouble(0),chainages.getDouble(i))
                        }
                        segments += RoadSegment(route,roadbed,id,points)
                    }
                    "Point" -> {
                        val xy=g.getJSONArray("coordinates")
                        prs += RoadPr(route,roadbed,normalizedPr(p.getString("prCode")),xy.getDouble(1),xy.getDouble(0),p.getDouble("chainageM"))
                    }
                    else -> error("Geometría no soportada: ${g.getString("type")}")
                }
            }
        } else {
            root.optJSONArray("segments")?.objects()?.forEach { s ->
                val route=normalizedRoadCode(s.getString("routeCode")); val roadbed=normalizedRoadCode(s.optString("roadbedCode")); val id=s.getString("segmentId")
                segments += RoadSegment(route,roadbed,id,s.getJSONArray("points").objects().map { p ->
                    RoadPolylinePoint(route,roadbed,id,p.getInt("sequence"),p.getDouble("latitude"),p.getDouble("longitude"),p.getDouble("chainageM"))
                }.sortedBy { it.sequence })
            }
            root.optJSONArray("prs")?.objects()?.forEach { p ->
                prs += RoadPr(normalizedRoadCode(p.getString("routeCode")),normalizedRoadCode(p.optString("roadbedCode")),
                    normalizedPr(p.getString("prCode")),p.getDouble("latitude"),p.getDouble("longitude"),p.getDouble("chainageM"))
            }
        }
        require(segments.map { Triple(it.routeCode,it.roadbedCode,it.segmentId) }.distinct().size==segments.size) { "Segmentos duplicados." }
        require(prs.map { Triple(it.routeCode,it.roadbedCode,it.prCode) }.distinct().size==prs.size) { "PR duplicados." }
        segments.forEach { s ->
            require(s.routeCode.isNotBlank() && s.segmentId.isNotBlank() && s.points.size>=2) { "Eje incompleto." }
            require(s.points.map { it.sequence }.distinct().size==s.points.size) { "Secuencias duplicadas." }
            s.points.forEach { require(GeoMath.validCoordinate(it.latitude,it.longitude) && it.chainageM.isFinite()) { "Coordenada/progresiva inválida." } }
            val differences=s.points.zipWithNext().map { (a,b)->b.chainageM-a.chainageM }
            require(differences.all { it>0 } || differences.all { it<0 }) { "El eje debe tener progresiva monótona; dividir los ramales." }
        }
        prs.forEach { require(it.routeCode.isNotBlank() && it.prCode.matches(Regex("[0-9]{4}")) && GeoMath.validCoordinate(it.latitude,it.longitude) && it.chainageM.isFinite()) { "PR inválido." } }
        return RoadReferenceData((segments.map { it.routeCode }+prs.map { it.routeCode }).distinct().map { RoadRoute(it) },segments,prs)
    }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
}

class RoadReferenceRepository(private val context: Context, private val importer: RoadReferenceImporter = JsonRoadReferenceImporter()) {
    suspend fun loadFromAssets(assetPath: String = "road_reference.json"): RoadReferenceData = withContext(Dispatchers.IO) {
        context.assets.open(assetPath).bufferedReader().use { importer.parse(it.readText()) }
    }
}
