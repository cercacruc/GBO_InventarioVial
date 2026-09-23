package com.tuempresa.inventariovial.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class TabletLocationProvider(context: Context) : LocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(
        onSuccess: (GeoLocation) -> Unit,
        onError: (String) -> Unit
    ) {
        val cancellationToken = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location == null) {
                    onError("No se pudo obtener la ubicación. Verifica que el GPS esté activado.")
                } else {
                    onSuccess(
                        GeoLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyHorizontal = location.accuracy,
                            altitude = if (location.hasAltitude()) location.altitude else null
                        )
                    )
                }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error obteniendo ubicación GNSS.")
            }
    }
}
