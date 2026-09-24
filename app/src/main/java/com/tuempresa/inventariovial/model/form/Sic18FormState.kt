package com.tuempresa.inventariovial.model.form

data class Sic18FormState(

    // =====================================================
    // SIC-18 · ALCANTARILLAS
    // =====================================================

    // 06 - Alcantarilla Definitiva
    // 07 - Alcantarilla Estructura Artesanal
    val classCode: String = "06",


    // Tipo según clase seleccionada.
    val typeCode: String = "1",


    // Ojos / Vanos.
    val spans: String = "1",


    // Sección transversal:
    // 1 - Marco
    // 2 - Circular / Ovalada
    // 3 - Arco
    // 4 - Pórtico
    // 5 - Otro
    val crossSectionCode: String = "2",


    // Ancho o diámetro.
    val dimension1M: String = "",


    // Altura.
    val dimension2M: String = "",


    // 1 - Buena
    // 2 - Regular
    // 3 - Mala
    val structuralConditionCode: String = "1",


    // 1 - Buena / limpia
    // 2 - Regular / parcialmente obstruida
    // 3 - Mala / totalmente obstruida
    val functionalConditionCode: String = "1",
    val sectionShape: String = "CIRCULAR",
    val structuralDamagePercent: String = "",
    val functionalObstructionPercent: String = ""
) {
    val usesDimension2: Boolean get() = crossSectionCode != "2" || sectionShape != "CIRCULAR"
}