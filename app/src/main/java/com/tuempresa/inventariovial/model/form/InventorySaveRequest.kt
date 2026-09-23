package com.tuempresa.inventariovial.model.form


sealed interface SicFormDetail {

    data class Sic17(
        val state: Sic17FormState
    ) : SicFormDetail


    data class Sic18(
        val state: Sic18FormState
    ) : SicFormDetail


    data class Sic19(
        val state: Sic19FormState
    ) : SicFormDetail


    data class Sic20(
        val state: Sic20FormState
    ) : SicFormDetail


    data class Sic21(
        val state: Sic21FormState
    ) : SicFormDetail


    data class Sic22(
        val state: Sic22FormState
    ) : SicFormDetail
}


data class InventorySaveRequest(

    val sicCode: String,

    val assetType: String,

    val routeCode: String,

    val roadbedCode: String,

    val startPrCode: String,

    val startDistanceM: String,

    val endPrCode: String?,

    val endDistanceM: String?,

    val sideCode: String?,

    val latitude: Double,

    val longitude: Double,

    val gpsAccuracyM: Float?,

    val surveyDate: String,

    val observations: String,

    val photoPath: String,

    val detail: SicFormDetail
)