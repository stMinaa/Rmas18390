package com.example.proba.ui.components

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun LocationTracker(
    locationPermissionState: MultiplePermissionsState,
    userId: String,
    onLocationUpdate: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 60000L // update na 5 sekundi
            ).setMinUpdateIntervalMillis(60000L).build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        for (location: Location in locationResult.locations) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            // 1) Update UI
                            onLocationUpdate(latLng)

                            // 2) Save to Firestore
                            saveLocationToFirestore(db, userId, location)
                        }
                    }
                },
                null
            )
        }
    }
}

fun saveLocationToFirestore(db: FirebaseFirestore, userId: String, location: Location) {
    val locationData = hashMapOf(
        "userId" to userId,
        "latitude" to location.latitude,
        "longitude" to location.longitude,
        "timestamp" to Timestamp.now()
    )

    db.collection("locations")
        .add(locationData)
        .addOnSuccessListener {
            Log.d("Firestore", "Lokacija sačuvana: $locationData")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Greška pri čuvanju lokacije", e)
        }
}
