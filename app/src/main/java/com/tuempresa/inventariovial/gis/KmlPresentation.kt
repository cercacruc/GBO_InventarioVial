package com.tuempresa.inventariovial.gis

import com.tuempresa.inventariovial.road.AssetFamily
import com.tuempresa.inventariovial.road.AssetPresentationCatalog

data class KmlLegendEntry(val text: String, val styleId: String, val rgb: Int)

object KmlPresentation {
    const val AXIS_COLOR = "ff00ffff"
    const val AXIS_RGB = 0xFFFF00
    const val OUTLINE_COLOR = "ff000000"
    const val AXIS_WIDTH = 5
    const val OUTLINE_WIDTH = 8
    val legend: List<KmlLegendEntry> get() = AssetPresentationCatalog.legend.map { family ->
        val color = when (family) {
            AssetFamily.ALCANTARILLA -> "Azul"
            AssetFamily.SENALIZACION_VERTICAL -> "Rojo"
            AssetFamily.PUENTE -> "Verde"
            AssetFamily.CUNETA -> "Naranja"
            AssetFamily.BADEN -> "Violeta"
            else -> "Gris"
        }
        KmlLegendEntry("${family.abbreviation} · ${family.label} · $color", "asset_${family.name}", family.rgb)
    }.toMutableList().apply { add(size - 1, KmlLegendEntry("EJE · Ruta · Amarillo", "axis", AXIS_RGB)) }
}
