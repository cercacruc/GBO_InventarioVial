package com.tuempresa.inventariovial.model.form

data class Sic19FormState(

    // =====================================================
    // SIC-19
    // CUNETAS, CANALES, BAJADAS DE AGUA
    // Y ZANJAS DE DRENAJE
    // =====================================================

    // 08 - Cuneta
    // 09 - Canal
    // 10 - Bajada de Agua
    // 11 - Zanja de Drenaje
    // 12 - Zanja de Coronación
    // 13 - Cuneta de Banqueta
    val classCode: String = "08",


    // 1 - Tierra
    // 2 - Concreto
    // 3 - Mampostería
    // 4 - Otro
    val typeCode: String = "1",


    // 1 - Triangular
    // 2 - Trapezoidal
    // 3 - Rectangular
    // 4 - Otro
    val crossSectionCode: String = "1",


    // 1 - Buena
    // 2 - Regular
    // 3 - Mala
    val structuralConditionCode: String = "1",


    // 1 - Buena / limpia
    // 2 - Regular / parcialmente obstruida
    // 3 - Mala / totalmente obstruida
    val functionalConditionCode: String = "1"
)