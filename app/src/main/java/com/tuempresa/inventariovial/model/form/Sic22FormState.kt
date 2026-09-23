package com.tuempresa.inventariovial.model.form

data class Sic22FormState(

    // =====================================================
    // SIC-22 · SEÑALIZACIÓN VERTICAL
    // =====================================================

    // La clase de señalización vertical
    // es siempre 20.
    val classCode: String = "20",


    // 1 - Reglamento
    // 2 - Preventivo
    // 3 - Informativo
    // 4 - Poste Kilométrico
    // 5 - Semáforos
    // 6 - Postes SOS
    val typeCode: String = "2",


    // 1 - Fibra de vidrio
    // 2 - Acero
    // 3 - Concreto
    // 4 - Madera
    // 5 - Otro
    val materialCode: String = "2",


    // Aplica a:
    // 1 Reglamento
    // 2 Preventivo
    // 3 Informativo
    val signalCode: String = "",


    // Aplica a tipo 4:
    // Poste Kilométrico.
    val kilometerPostNumber: String = "",


    // 1 - Buena
    // 2 - Regular
    // 3 - Mala
    val conditionCode: String = "1",


    // =====================================================
    // CAMPOS ADICIONALES SOLICITADOS POR EL CLIENTE
    //
    // NO SON CAMPOS OFICIALES DEL SIC-22.
    // =====================================================

    val signWidthM: String = "",

    val signHeightM: String = "",

    val lowerEdgeHeightM: String = ""
) {

    val usesSignalCode: Boolean
        get() =
            typeCode == "1" ||
                    typeCode == "2" ||
                    typeCode == "3"


    val usesKilometerPostNumber: Boolean
        get() = typeCode == "4"
}