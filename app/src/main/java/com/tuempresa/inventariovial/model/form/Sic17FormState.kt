package com.tuempresa.inventariovial.model.form

data class Sic17FormState(

    // =====================================================
    // SIC-17 · PUENTES
    // =====================================================

    // 01 - Puente Definitivo
    // 02 - Puente Provisional
    // 03 - Estructura Artesanal
    // 04 - Puente Histórico
    val classCode: String = "01",


    // El significado depende de la clase.
    // Por eso se guarda como String.
    val typeCode: String = "1",


    // Código oficial del puente.
    val bridgeCode: String = "",


    // S = Sí
    // N = No
    val inventoriedCode: String = "S",


    // Número de vanos.
    // Se mantiene String mientras está en formulario.
    val spans: String = "",


    // Dimensión 1:
    // longitud total del puente en metros.
    val dimension1LengthM: String = "",


    // Dimensión 2:
    // altura libre inferior.
    val dimension2LowerHeightM: String = "",


    // 1 - Buena
    // 2 - Regular
    // 3 - Mala
    val structuralConditionCode: String = "1",


    // 1 - Buena
    // 2 - Regular
    // 3 - Mala
    val functionalConditionCode: String = "1",


    // Tipo de servicio:
    // 0 - Fuera de servicio
    // 1 - Vehicular
    // 2 - Ferroviario
    // 3 - Peatonal
    // 4 - Otro
    val serviceTypeCode: String = "1",


    // Singularidad salvada:
    // 1 - Río
    // 2 - Quebrada
    // 3 - Canal
    // 4 - Camino
    // 5 - Vía férrea
    // 6 - Otro
    val singularityCode: String = "1",


    // Nombre del río, quebrada,
    // carretera, vía férrea, etc.
    val singularityName: String = "",


    // Dimensión 3:
    // altura libre superior.
    val dimension3UpperHeightM: String = ""
)