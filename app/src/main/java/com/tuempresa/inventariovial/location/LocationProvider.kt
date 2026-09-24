package com.tuempresa.inventariovial.location


data class GeoLocation(

    val latitude: Double,

    val longitude: Double,

    val accuracyHorizontal: Float,

    val altitude: Double? = null,

    val source: String = "TABLET_GNSS",

    val timestamp: Long = System.currentTimeMillis(),
    val verticalAccuracy: Float? = null,
    val provider: GnssProvider = GnssProvider.TABLET,
    val fixType: String? = null,
    val satellites: Int? = null,
    val hdop: Double? = null,
    val correctionAge: Double? = null,
    val isRtkFixed: Boolean = false
) {
    val horizontalAccuracy: Float get() = accuracyHorizontal
}

enum class GnssProvider { TABLET, EXTERNAL_GNSS, RTK_FIXED, RTK_FLOAT }


interface LocationProvider {

    fun getCurrentLocation(
        onSuccess: (GeoLocation) -> Unit,
        onError: (String) -> Unit
    )
}
