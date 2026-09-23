package com.tuempresa.inventariovial.model.form

data class Sic20FormState(

    // =====================================================
    // SIC-20 · BADENES, TÚNELES Y MUROS
    // =====================================================

    // 12 - Badén
    // 13 - Túnel
    // 14 - Muro
    val classCode: String = "12",


    // El catálogo de tipos depende de la clase.
    val typeCode: String = "2",


    // Dimensión 1:
    //
    // Badén:
    // ancho de rodadura.
    //
    // Túnel:
    // ancho.
    //
    // Muro:
    // altura promedio del cuerpo.
    val dimension1M: String = "",


    // Dimensión 2:
    //
    // Badén:
    // ancho total incluyendo protección
    // contra erosión.
    //
    // Túnel:
    // altura útil.
    //
    // Muro:
    // no aplica.
    val dimension2M: String = "",


    // 1 - Buena
    // 2 - Regular
    // 3 - Mala
    val structuralConditionCode: String = "1",


    // Se utiliza funcionalmente para badenes.
    //
    // 1 - Buena / limpia
    // 2 - Regular / parcialmente obstruida
    // 3 - Mala / totalmente obstruida
    val functionalConditionCode: String = "1"
) {

    val usesDimension2: Boolean
        get() = classCode != "14"


    val usesFunctionalCondition: Boolean
        get() = classCode == "12"
}