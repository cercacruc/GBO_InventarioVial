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
        "Inventario de badenes",
        "SIC-20"
    ),

    TUNNEL("Túnel", "Inventario de túneles", "SIC-20"),
    WALL("Muro", "Inventario de muros", "SIC-20"),

    RIGHT_OF_WAY(
        "Derecho de vía",
        "Derecho de vía, zonas urbanas, puntos específicos y canteras",
        "SIC-23"
    ),

    BRIDGE(
        "Puente",
        "Inventario de puente",
        "SIC-17"
    )
}