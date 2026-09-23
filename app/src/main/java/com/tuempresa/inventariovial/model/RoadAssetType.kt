package com.tuempresa.inventariovial.model

enum class RoadAssetType(
    val title: String,
    val subtitle: String,
    val sicCode: String
) {

    SIGNALIZATION(
        "Señalización y seguridad",
        "Vertical, horizontal, tachas y seguridad vial",
        "SIC-21 / SIC-22"
    ),

    CULVERT(
        "Alcantarilla",
        "Obras de drenaje",
        "SIC-18"
    ),

    DITCH(
        "Cuneta",
        "Cunetas, canales y drenaje",
        "SIC-19"
    ),

    FORD(
        "Badén",
        "Badén / drenaje transversal",
        "SIC-20"
    ),

    BRIDGE(
        "Puente",
        "Inventario de puente",
        "SIC-17"
    )
}