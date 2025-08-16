package com.example.proba.ui.components

import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun LocationTracker(
    locationPermissionState: MultiplePermissionsState,
    userId: String, // Ostavljen ako ti nekad opet zatreba
    onLocationUpdate: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000L // Ažuriranje na 30 sekundi
            ).setMinUpdateIntervalMillis(10000L).build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        for (location: Location in locationResult.locations) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            onLocationUpdate(latLng)
                        }
                    }
                },
                null
            )
        }
    }
}
