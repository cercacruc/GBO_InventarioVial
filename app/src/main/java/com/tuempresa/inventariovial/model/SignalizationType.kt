package com.tuempresa.inventariovial.model

enum class SignalizationType(
    val title: String,
    val subtitle: String,
    val sicCode: String
) {

    VERTICAL(
        "Señal vertical",
        "Reglamento, preventiva, informativa, poste km, semáforo o SOS",
        "SIC-22"
    ),

    HORIZONTAL_MARKS(
        "Marcas horizontales",
        "Marcas centrales, laterales o centrales y laterales",
        "SIC-21"
    ),

    HORIZONTAL_STUDS(
        "Tachas",
        "Tachas centrales, laterales o centrales y laterales",
        "SIC-21"
    ),

    SAFETY(
        "Seguridad vial",
        "Guardavías, postes delineadores, barreras y resaltos",
        "SIC-21"
    )
}