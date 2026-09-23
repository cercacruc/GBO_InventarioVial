package com.tuempresa.inventariovial.model.form

data class Sic21FormState(

    // =====================================================
    // SIC-21
    // SEGURIDAD Y SEÑALIZACIÓN HORIZONTAL
    // =====================================================

    // 18 - Señalización Horizontal - Marcas
    // 19 - Seguridad
    // 20 - Señalización Horizontal - Tachas
    val classCode: String = "18",


    // El significado depende de classCode.
    val typeCode: String = "1",


    // 1 - Acero
    // 2 - Concreto
    // 3 - Mampostería
    // 4 - Plástico
    // 5 - Otro
    //
    // Para marcas horizontales mantenemos
    // "5 - Otro" como valor inicial.
    val materialCode: String = "5",


    // 1 - Buena
    // 2 - Regular
    // 3 - Mala
    val conditionCode: String = "1"
)