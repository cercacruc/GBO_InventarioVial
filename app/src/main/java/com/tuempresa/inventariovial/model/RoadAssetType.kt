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
        "Badén, túnel o muro",
        "Badenes, túneles y muros",
        "SIC-20"
    ),

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