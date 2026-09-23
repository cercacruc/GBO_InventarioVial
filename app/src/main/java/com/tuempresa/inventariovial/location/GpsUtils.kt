package com.tuempresa.inventariovial.location

import android.annotation.SuppressLint
import android.content.Context

import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource


@SuppressLint("MissingPermission")
fun getCurrentGpsLocation(
    context: Context,
    onSuccess: (
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ) -> Unit,
    onError: (String) -> Unit
) {

    val client =
        LocationServices
            .getFusedLocationProviderClient(
                context
            )

    val cancellationToken =
        CancellationTokenSource()

    client.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        cancellationToken.token
    )
        .addOnSuccessListener { location ->

            if (location != null) {

                onSuccess(
                    location.latitude,
                    location.longitude,
                    location.accuracy
                )

            } else {

                onError(
                    "No se pudo obtener la ubicación. " +
                            "Verifica que el GPS esté activado."
                )
            }
        }
        .addOnFailureListener { error ->

            onError(
                error.message
                    ?: "Error obteniendo ubicación GPS."
            )
        }
}