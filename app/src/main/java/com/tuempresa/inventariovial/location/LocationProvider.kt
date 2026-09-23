package com.tuempresa.inventariovial.location

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyHorizontal: Float,
    val altitude: Double? = null,
    val source: String = "TABLET_GNSS",
    val timestamp: Long = System.currentTimeMillis()
)

interface LocationProvider {
    fun getCurrentLocation(
        onSuccess: (GeoLocation) -> Unit,
        onError: (String) -> Unit
    )
}
