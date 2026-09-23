package com.tuempresa.inventariovial.model.form

data class CommonInventoryFormState(

    // =====================================================
    // UBICACIÓN VIAL
    // =====================================================

    val routeCode: String = "",

    val roadbedCode: String = "",

    val startPrCode: String = "",

    val startDistanceM: String = "",

    val endPrCode: String = "",

    val endDistanceM: String = "",

    val sideCode: String = "D",


    // =====================================================
    // GEOREFERENCIACIÓN
    // =====================================================

    val latitude: Double? = null,

    val longitude: Double? = null,

    val altitudeM: Double? = null,

    val gpsAccuracyM: Float? = null,


    // =====================================================
    // REGISTRO
    // =====================================================

    val surveyDate: String = "",

    val observations: String = ""
)