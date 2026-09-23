package com.tuempresa.inventariovial.location
import android.content.Context
fun getCurrentGpsLocation(context: Context,
    onSuccess: (latitude: Double, longitude: Double, accuracy: Float) -> Unit,
    onError: (String) -> Unit
) {
    TabletLocationProvider(context).getCurrentLocation(
        onSuccess = { onSuccess(it.latitude, it.longitude, it.accuracyHorizontal) },
        onError = onError)
}
